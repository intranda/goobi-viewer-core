package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.administration.legal.ConsentScope;
import io.goobi.viewer.model.administration.legal.Disclaimer;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
@ViewScoped
public class DisclaimerBean implements Serializable {

    private static final long serialVersionUID = -6562240290914952926L;
    private static final Logger logger = LoggerFactory.getLogger(DisclaimerBean.class);

    private final IDAO dao;
    private final Disclaimer disclaimerForEdit;

    /**
     * Default constructor using the IDAO from the {@link DataManager} class
     */
    public DisclaimerBean() {
        dao = retrieveDAO();
        this.disclaimerForEdit = loadDisclaimerForEdit();
    }

    /**
     * Constructor for testing purposes
     * 
     * @param dao the IDAO implementation to use
     */
    public DisclaimerBean(IDAO dao) {
        this.dao = dao;
        this.disclaimerForEdit = loadDisclaimerForEdit();
    }

    private IDAO retrieveDAO() {
        try {
            return DataManager.getInstance().getDao();
        } catch (DAOException e) {
            logger.error("Error initializing DisclaimerBean: {}", e.toString());
            return null;
        }
    }

    /**
     * Get the stored disclaimer to display on a viewer web-page. Do not use for modifications
     * 
     * @return the cookie banner stored in the DAO
     */
    public Disclaimer getDisclaimer() {
        if (dao != null) {
            try {
                return Optional.ofNullable(dao.getDisclaimer()).orElse(new Disclaimer());
            } catch (DAOException e) {
                logger.error("Error retrieving disclaimer from dao: {}", e.toString());
                return null;
            }
        } else {
            return null;
        }
    }

    public void save() {
        if (this.disclaimerForEdit != null) {
            //            this.disclaimerForEdit.setAcceptanceScope(new ConsentScope(this.disclaimerForEdit.getAcceptanceScope().toString()));
            try {
                if (!this.dao.saveDisclaimer(this.disclaimerForEdit)) {
                    throw new DAOException("Saving disclaimer failed");
                }
                Messages.info("admin__legal__save_disclaimer__success");
            } catch (DAOException e) {
                Messages.error("admin__legal__save_disclaimer__error");
            }
        }
    }

    public Disclaimer getDisclaimerForEdit() {
        return disclaimerForEdit;
    }

    /**
     * Activate/deactivate the disclaimer. Applies directly to the persisted object
     * 
     * @param active
     * @throws DAOException
     */
    public void setDisclaimerActive(boolean active) throws DAOException {
        if (this.dao != null) {
            Disclaimer disclaimer = dao.getDisclaimer();
            disclaimer.setActive(active);
            dao.saveDisclaimer(disclaimer);
            if (this.disclaimerForEdit != null) {
                this.disclaimerForEdit.setActive(active);
            }
        }
    }

    /**
     * Check if the banner is active, i.e. should be displayed at all
     * 
     * @return true if the banner should be shown if appropriate
     */
    public boolean isDisclaimerActive() {
        return this.disclaimerForEdit.isActive();
    }

    /**
     * Set the {@link Disclaimer#getRequiresConsentAfter()} to the current time. Applies directly to the persisted object
     * 
     * @throws DAOException
     */
    public void resetUserConsent() throws DAOException {
        //save the current date both to the banner managed by the persistence context and to the copy we are editing
        //this way, saving the current banner is not required, but is a save is performed, the date is not overwritten
        if (this.dao != null) {
            Disclaimer disclaimer = dao.getDisclaimer();
            disclaimer.setRequiresConsentAfter(LocalDateTime.now());
            dao.saveDisclaimer(disclaimer);
            if (this.disclaimerForEdit != null) {
                this.disclaimerForEdit.setRequiresConsentAfter(disclaimer.getRequiresConsentAfter());
            }
        }
    }

    private Disclaimer loadDisclaimerForEdit() {
        try {
            Disclaimer persistedDisclaimer = dao.getDisclaimer();
            if (persistedDisclaimer == null) {
                persistedDisclaimer = new Disclaimer();
                dao.saveDisclaimer(persistedDisclaimer);
            }
            return new Disclaimer(persistedDisclaimer);
        } catch (DAOException e) {
            logger.error("Error synchronizing editable disclaimer with database", e);
            return new Disclaimer();
        }
    }

    public String getDisclaimerConfig() {
        if (dao != null) {
            try {
                Disclaimer disclaimer = dao.getDisclaimer();
                JSONObject json = new JSONObject();
                boolean active = disclaimer.isActive();
                if (active && BeanUtils.getNavigationHelper().isDocumentPage()) {
                    String pi = BeanUtils.getActiveDocumentBean().getPersistentIdentifier();
                    if (matchesRecord(disclaimer, pi)) {
                        ConsentScope scope = getConsentScope(disclaimer);
                        json.put("active", active);
                        json.put("lastEdited", disclaimer.getRequiresConsentAfter().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                        json.put("storage", scope.getStorageMode().toString().toLowerCase());
                        json.put("daysToLive", scope.getDaysToLive());
                        BeanUtils.getSessionId().ifPresent(id -> {
                            json.put("sessionId", id);
                        });
                        return json.toString();
                    }
                }
            } catch (DAOException | IndexUnreachableException e) {
                logger.error("Error loading disclaimer config", e);
                return "{}";
            }
        }
        return "{}";
    }

    private ConsentScope getConsentScope(Disclaimer disclaimer) throws DAOException {
        
        String licenceTypeName = LicenseType.LICENSE_TYPE_LEGAL_DISCLAIMER;
        
        LicenseType type = DataManager.getInstance().getDao().getLicenseType(licenceTypeName);      
        List<License> licenses = DataManager.getInstance().getDao().getLicenses(type);
        Optional<User> user = Optional.ofNullable(BeanUtils.getUserBean()).map(UserBean::getUser);
        List<UserGroup> userGroups = user.map(User::getAllUserGroups).orElse(Collections.emptyList());
        String ipAddress = NetTools.getIpAddress(BeanUtils.getRequest());
        List<IpRange> ipRanges = DataManager.getInstance().getDao().getAllIpRanges().stream().filter(range -> range.matchIp(ipAddress)).collect(Collectors.toList());
        
        List<License> applyingLicenses = licenses.stream()
        .filter(license -> {
           return user.map(u -> u.equals(license.getUser())).orElse(false)
                   || userGroups.contains(license.getUserGroup())
                   || ipRanges.contains(license.getIpRange());
        })
        .collect(Collectors.toList());
 
        License licenseToUse = applyingLicenses.stream()
                .filter(l -> {
                    
                })
        
        return disclaimer.getAcceptanceScope();
    }

    /**
     * Check if the given pi is a match for the query of the record note The pi is a match if the record note query combined with a query for the
     * given pi returns at least one result
     * 
     * @param pi
     * @return
     */
    private boolean matchesRecord(Disclaimer disclaimer, String pi) {
        if (StringUtils.isNotBlank(pi)) {
            String solrQuery = disclaimer.getQueryForSearch();
            String singleRecordQuery = "+({1}) +{2}".replace("{1}", solrQuery).replace("{2}", "PI:" + pi);

            try {
                return DataManager.getInstance()
                        .getSearchIndex()
                        .count(singleRecordQuery) > 0;
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error("Failed to test match for record note '{}': {}", this, e.toString());
                return false;
            }
        } else {
            return false;
        }
    }
}
