/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

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
import io.goobi.viewer.model.administration.legal.ConsentScope;
import io.goobi.viewer.model.administration.legal.Disclaimer;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

/**
 * Bean to check whether the disclaimer applies to a page/record as well as provide a configuration json object for the javascript
 * This bean is session scoped, so all stored settings are discarded outside a jsf session
 * @author florian
 *
 */
@Named
@SessionScoped
public class DisclaimerBean implements Serializable {

    private static final long serialVersionUID = -6562240290914952926L;
    private static final Logger logger = LoggerFactory.getLogger(DisclaimerBean.class);

    @Inject
    ActiveDocumentBean activeDocumentBean;
    
    /**
     * the {@link LicenseType#LICENSE_TYPE_LEGAL_DISCLAIMER} core license type derived from the dao
     */
    private final LicenseType licenseType;

    private final IDAO dao;

    /**
     * the current user, which may be empty. Licenses applying to the disclaimer are only updated if the user changes
     */
    private Optional<User> currentUser = Optional.empty();
    /**
     * the consentScope to use as long as the user doesn't change
     */
    private Optional<ConsentScope> currentConsentScope = Optional.empty();
    /**
     * map storing PIs of records and whether the disclaimer applies to them 
     */
    private Map<String, Boolean> recordApplicabilityMap = new HashMap<>();
    
    
    /**
     * Default constructor using the IDAO from the {@link DataManager} class
     */
    public DisclaimerBean() {
        this(retrieveDAO());
    }

    /**
     * Constructor for testing purposes
     * 
     * @param dao the IDAO implementation to use
     */
    public DisclaimerBean(IDAO dao) {
        this.dao = dao;
        this.licenseType = getDisclaimerLicenseType(this.dao);

    }




    /**
     * Get the stored disclaimer to display on a viewer web-page. Do not use for modifications
     * 
     * @return the cookie banner stored in the DAO
     * @deprecated not needed if disclaimer is realized as a sweet alert which is created from {@link #getDisclaimerConfig()}
     */
    @Deprecated
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

    /**
     * The configuration object for the disclaimer to be used by the viewerJS.disclaimerModal module
     * @return  a json object
     */
    public String getDisclaimerConfig() {
        if (dao != null && BeanUtils.getNavigationHelper().isDocumentPage()) {
            try {
                Disclaimer disclaimer = dao.getDisclaimer();
                JSONObject json = new JSONObject();
                boolean active = disclaimer.isActive();
                if (active) {
                    if (appliesToRecord(disclaimer, BeanUtils.getActiveDocumentBean().getPersistentIdentifier())) {
                        ConsentScope scope = getConsentScope(disclaimer);
                        json.put("active", active);
                        json.put("lastEdited", disclaimer.getRequiresConsentAfter().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                        json.put("storage", scope.getStorageMode().toString().toLowerCase());
                        json.put("daysToLive", scope.getDaysToLive());
                        json.put("disclaimerText", disclaimer.getText().getText(BeanUtils.getLocale()));
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
    
    private boolean appliesToRecord(Disclaimer disclaimer, String pi) throws IndexUnreachableException {
        if(StringUtils.isNotBlank(pi)) {
            Boolean apply = recordApplicabilityMap.get(pi);
            if(apply != null) {
                return apply;
            } else {
                apply = matchesRecord(disclaimer, pi);
                recordApplicabilityMap.put(pi, apply);
                return apply;
            }
        } else {
            return false;
        }
    }

    /**
     * Checks the currently logged in user. If it matches the user stored in this bean return the stored consentScope.
     * Otherwise check if a license of type {@link LicenseType#LICENSE_TYPE_LEGAL_DISCLAIMER} applies to the current user,
     * any of its user groups or the current ip.
     * Then set the user stored in the bean to the current user and the stored consentScope to a consentScope from a license 
     * or from the disclaimer if there is no matching license.
     * Then return the stored consentScope
     * 
     * @param disclaimer    must not be null
     * @return  the applying consentScope, never null
     * @throws DAOException
     */
    private ConsentScope getConsentScope(Disclaimer disclaimer) throws DAOException {
        
        Optional<User> user = Optional.ofNullable(BeanUtils.getUserBean()).map(UserBean::getUser);
        if(user.equals(currentUser) && currentConsentScope.isPresent()) {
            return currentConsentScope.get();
        } else {
            currentUser = user;
            Optional<License> licenseToUse = getApplyingLicenses(user, this.licenseType).stream().findAny();
            currentConsentScope = Optional.of(licenseToUse.map(License::getDisclaimerScope).orElse(disclaimer.getAcceptanceScope()));
            return currentConsentScope.get();
        }
        
    }

    private List<License> getApplyingLicenses(Optional<User> user, LicenseType type) throws DAOException {
        List<License> licenses = DataManager.getInstance().getDao().getLicenses(type);
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
 
        return applyingLicenses.stream()
                .filter(l -> {
                    return applyingLicenses.stream()
                    .filter(ol -> !ol.equals(l))
                    .noneMatch(ol -> l.getLicenseType().getOverridingLicenseTypes().contains(ol.getLicenseType()));
                })
                .collect(Collectors.toList());
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
    
    private static IDAO retrieveDAO() {
        try {
            return DataManager.getInstance().getDao();
        } catch (DAOException e) {
            logger.error("Error initializing DisclaimerBean: {}", e.toString());
            return null;
        }
    }
    
    
    private static LicenseType getDisclaimerLicenseType(IDAO dao) {
        try {
            return dao.getLicenseType(LicenseType.LICENSE_TYPE_LEGAL_DISCLAIMER);
        } catch (DAOException e) {
            logger.error("Error initializing DisclaimerBean ", e);
            return null;
        }    
    }
}
