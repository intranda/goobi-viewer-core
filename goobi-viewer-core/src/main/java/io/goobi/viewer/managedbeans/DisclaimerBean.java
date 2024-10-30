/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.administration.legal.ConsentScope;
import io.goobi.viewer.model.administration.legal.Disclaimer;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * Bean to check whether the disclaimer applies to a page/record as well as provide a configuration json object for the javascript This bean is
 * session scoped, so all stored settings are discarded outside a jsf session
 * 
 * @author florian
 *
 */
@Named
@SessionScoped
public class DisclaimerBean implements Serializable {

    private static final long serialVersionUID = -6562240290914952926L;
    private static final Logger logger = LogManager.getLogger(DisclaimerBean.class);

    @Inject
    private ActiveDocumentBean activeDocumentBean;
    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private UserBean userBean;

    /**
     * the {@link LicenseType#LICENSE_TYPE_LEGAL_DISCLAIMER} core license type derived from the dao
     */
    private final LicenseType licenseType;

    private final IDAO dao;
    private final SolrSearchIndex searchIndex;

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

    /**
     * Default constructor using the IDAO from the {@link DataManager} class
     */
    public DisclaimerBean() {
        this(retrieveDAO(), DataManager.getInstance().getSearchIndex());
    }

    /**
     * Constructor for testing purposes
     *
     * @param dao the IDAO implementation to use
     * @param searchIndex
     */
    public DisclaimerBean(IDAO dao, SolrSearchIndex searchIndex) {
        this.dao = dao;
        this.searchIndex = searchIndex;
        this.licenseType = getDisclaimerLicenseType(this.dao);

    }

    /**
     * Get the stored disclaimer to display on a viewer web-page. Do not use for modifications
     *
     * @return the cookie banner stored in the DAO
     * @deprecated not needed if disclaimer is realized as a sweet alert which is created from {@link #getDisclaimerConfig()}
     */
    @Deprecated(since = "24.10")
    public Disclaimer getDisclaimer() {
        if (dao != null) {
            try {
                return Optional.ofNullable(dao.getDisclaimer()).orElse(new Disclaimer());
            } catch (DAOException e) {
                logger.error("Error retrieving disclaimer from dao: {}", e.toString());
                return null;
            }
        }
        return null;
    }

    /**
     * The configuration object for the disclaimer to be used by the viewerJS.disclaimerModal module
     * 
     * @return a json object
     */
    public String getDisclaimerConfig() {
        if (dao != null) {
            try {
                Disclaimer disclaimer = dao.getDisclaimer();
                JSONObject json = new JSONObject();
                if (disclaimer != null && disclaimer.isActive()) {
                    if (disclaimer.getDisplayScope()
                            .appliesToPage(navigationHelper.getCurrentPageType(), activeDocumentBean.getPersistentIdentifier(), searchIndex)) {
                        ConsentScope scope = getConsentScope(disclaimer);
                        json.put("active", disclaimer.isActive());
                        json.put("lastEdited", disclaimer.getRequiresConsentAfter().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                        json.put("storage", scope.getStorageMode().toString().toLowerCase());
                        json.put("daysToLive", scope.getDaysToLive());
                        json.put("disclaimerText",
                                disclaimer.getText().getTextOrDefault(navigationHelper.getLocale(), navigationHelper.getDefaultLocale()));
                        navigationHelper.getSessionId().ifPresent(id -> {
                            json.put("sessionId", id);
                        });
                        return json.toString();
                    }
                }
            } catch (DAOException | IndexUnreachableException | JSONException | PresentationException e) {
                logger.error("Error loading disclaimer config", e);
                return "{}";
            }
        }
        return "{}";
    }

    /**
     * Checks the currently logged in user. If it matches the user stored in this bean return the stored consentScope. Otherwise check if a license of
     * type {@link LicenseType#LICENSE_TYPE_LEGAL_DISCLAIMER} applies to the current user, any of its user groups or the current ip. Then set the user
     * stored in the bean to the current user and the stored consentScope to a consentScope from a license or from the disclaimer if there is no
     * matching license. Then return the stored consentScope
     *
     * @param disclaimer must not be null
     * @return the applying consentScope, never null
     * @throws DAOException
     */
    private ConsentScope getConsentScope(Disclaimer disclaimer) throws DAOException {

        Optional<User> user = Optional.ofNullable(userBean).map(UserBean::getUser);
        if (user.equals(currentUser) && currentConsentScope.isPresent()) {
            return currentConsentScope.get();
        }
        currentUser = user;
        Optional<License> licenseToUse = getApplyingLicenses(user, this.licenseType).stream().findAny();
        currentConsentScope = Optional.of(licenseToUse.map(License::getDisclaimerScope).orElse(disclaimer.getAcceptanceScope()));
        return currentConsentScope.get();

    }

    private List<License> getApplyingLicenses(Optional<User> user, LicenseType type) throws DAOException {
        List<License> licenses = dao.getLicenses(type);
        List<UserGroup> userGroups = user.map(User::getAllUserGroups).orElse(Collections.emptyList());
        String ipAddress = navigationHelper.getSessionIPAddress();
        List<IpRange> ipRanges = dao.getAllIpRanges().stream().filter(range -> range.matchIp(ipAddress)).collect(Collectors.toList());

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
                            .noneMatch(ol -> l.getLicenseType().getOverriddenLicenseTypes().contains(ol.getLicenseType()));
                })
                .collect(Collectors.toList());
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

    /**
     * Setter for unit tests.
     * 
     * @param activeDocumentBean the activeDocumentBean to set
     */
    void setActiveDocumentBean(ActiveDocumentBean activeDocumentBean) {
        this.activeDocumentBean = activeDocumentBean;
    }

    /**
     * Setter for unit tests.
     * 
     * @param navigationHelper the navigationHelper to set
     */
    void setNavigationHelper(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
    }

    /**
     * Setter for unit tests.
     * 
     * @param userBean the userBean to set
     */
    void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }
}
