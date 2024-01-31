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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.administration.legal.CookieBanner;
import io.goobi.viewer.model.cms.pages.CMSPage;

/**
 * This this the java backend class for enabling and configuring the cookie banner feature. This bean is view scoped, i.e. created fresh for each new
 * page loaded
 **/
@Named
@ViewScoped
public class CookieBannerBean implements Serializable {

    private static final long serialVersionUID = -6562240290904952926L;
    private static final Logger logger = LogManager.getLogger(CookieBannerBean.class);

    private final IDAO dao;
    private final CookieBanner editCookieBanner;
    private final Map<CMSPage, Boolean> cmsPageMap;
    private BeanStatus status = BeanStatus.UNITIALIZED;

    public enum BeanStatus {
        UNITIALIZED,
        ERROR_DATABASE_CONNECTION,
        ERROR_RETRIEVING_DATABASE_OBJECT,
        INITIALIZED
    }

    /**
     * Default constructor using the IDAO from the {@link DataManager} class
     */
    public CookieBannerBean() {
        dao = retrieveDAO();
        editCookieBanner = new CookieBanner(getCookieBanner());
        this.cmsPageMap = buildCMSPageMap();
        if (BeanStatus.UNITIALIZED.equals(this.status)) {
            this.status = BeanStatus.INITIALIZED;
            updatePageMap(cmsPageMap, editCookieBanner.getIgnoreList());
        }
    }

    /**
     * Constructor for testing purposes
     * 
     * @param dao the IDAO implementation to use
     */
    public CookieBannerBean(IDAO dao) {
        this.dao = dao;
        editCookieBanner = new CookieBanner(getCookieBanner());
        this.cmsPageMap = buildCMSPageMap();
        if (BeanStatus.UNITIALIZED.equals(this.status)) {
            this.status = BeanStatus.INITIALIZED;
            updatePageMap(cmsPageMap, editCookieBanner.getIgnoreList());

        }
    }

    /**
     * Get the stored cookie banner to display on a viewer web-page. Do not use for modifications
     * 
     * @return the cookie banner stored in the DAO
     */
    public CookieBanner getCookieBanner() {
        if (dao != null) {
            try {
                return dao.getCookieBanner();
            } catch (DAOException e) {
                logger.error("Error retrieving cookie banner from dao: {}", e.toString());
                this.status = BeanStatus.ERROR_RETRIEVING_DATABASE_OBJECT;
                return null;
            }
        }
        return null;
    }

    /**
     * Get the initialization status of the bean. Useful for detecting problems with DAO communication
     * 
     * @return the status of the current bean
     */
    public BeanStatus getStatus() {
        return status;
    }

    /**
     * Get the copy of the stored cookie banner for editing. Changes are persisted to the object stored in the database by calling {@link #save()}
     * 
     * @return the cookie banner for editing
     */
    public CookieBanner getCookieBannerForEdit() {
        return this.editCookieBanner;
    }

    /**
     * To use when selecting CMS-Pages on which to ignore the cookie-banner. Pages mapped to "true" are stored in {@link CookieBanner#getIgnoreList()}
     * when calling {@link #save()}
     * 
     * @return Map
     */
    public Map<CMSPage, Boolean> getCmsPageMap() {
        return cmsPageMap;
    }

    /**
     * Save the current {@link #getCookieBannerForEdit()} to the DAO. Set the banners ignore list from {@link #getCmsPageMap()} before
     */
    public void save() {
        if (this.editCookieBanner != null) {
            this.editCookieBanner.setIgnoreList(getCmsPageIdsToIgnore());
            try {
                //save a clone of the editCookieBanner, so the editCookieBanner itself is never managed
                //and changes to it will not be reflected in the persistence context until save is called again
                this.dao.saveCookieBanner(new CookieBanner(editCookieBanner));
                Messages.info(null, "button__save__success", ViewerResourceBundle.getTranslation("label__cookie_banner", null));
            } catch (DAOException e) {
                logger.error("Error saving cookie banner", e);
                Messages.error("errSave");
            }
        }
    }

    /**
     * Set the {@link CookieBanner#getRequiresConsentAfter()} to the current time. Applies directly to the persisted object
     * 
     * @throws DAOException
     */
    public void resetUserConsent() throws DAOException {
        //save the current date both to the banner managed by the persistence context and to the copy we are editing
        //this way, saving the current banner is not required, but is a save is performed, the date is not overwritten
        if (this.dao != null) {
            CookieBanner banner = dao.getCookieBanner();
            banner.setRequiresConsentAfter(LocalDateTime.now());
            if (dao.saveCookieBanner(banner)) {
                if (this.editCookieBanner != null) {
                    this.editCookieBanner.setRequiresConsentAfter(LocalDateTime.now());
                    this.editCookieBanner.setId(banner.getId());
                }
                Messages.info("admin__legal__reset_cookie_banner_consent__success");
            } else {
                Messages.error("admin__legal__reset_cookie_banner_consent__error");
            }
        }
    }

    /**
     * Activate/deactivate the cookie banner. Applies directly to the persisted object
     * 
     * @param active
     * @throws DAOException
     */
    public void setBannerActive(boolean active) throws DAOException {
        if (this.dao != null) {
            CookieBanner banner = dao.getCookieBanner();
            banner.setActive(active);
            dao.saveCookieBanner(banner);
            if (this.editCookieBanner != null) {
                this.editCookieBanner.setActive(active);
                this.editCookieBanner.setId(banner.getId());
            }
        }
    }

    /**
     * Check if the banner is active, i.e. should be displayed at all
     * 
     * @return true if the banner should be shown if appropriate
     */
    public boolean isBannerActive() {
        return this.editCookieBanner.isActive();
    }

    /**
     * Return a json object to use a configuration object to the viewerJS.cookieBanner.js javascript
     * 
     * @return a json config object
     */
    public String getCookieBannerConfig() {
        if (dao != null) {
            try {
                CookieBanner banner = dao.getCookieBanner();
                JSONObject json = new JSONObject();
                boolean active = banner.isActive();
                if (active && BeanUtils.getNavigationHelper().isCmsPage()) {
                    Long pageId = BeanUtils.getCmsBean().getCurrentPage().getId();
                    if (banner.getIgnoreList().contains(pageId)) {
                        active = false;
                    }
                }
                json.put("active", active);
                json.put("lastEditedHash", banner.getRequiresConsentAfter().atZone(ZoneId.systemDefault()).toEpochSecond());
                return json.toString();
            } catch (DAOException e) {
                return "{}";
            }
        }
        return "{}";
    }

    private List<Long> getCmsPageIdsToIgnore() {
        return this.cmsPageMap.keySet().stream().filter(this.cmsPageMap::get).map(CMSPage::getId).sorted().collect(Collectors.toList());
    }

    private IDAO retrieveDAO() {
        try {
            return DataManager.getInstance().getDao();
        } catch (DAOException e) {
            logger.error("Error initializing CookieBannerBean: {}", e.toString());
            this.status = BeanStatus.ERROR_DATABASE_CONNECTION;
            return null;
        }
    }

    private Map<CMSPage, Boolean> buildCMSPageMap() {
        try {
            return dao.getAllCMSPages()
                    .stream()
                    .filter(CMSPage::isPublished)
                    .filter(p -> StringUtils.isNotBlank(p.getMenuTitle()))
                    .collect(Collectors.toMap(Function.identity(), p -> Boolean.FALSE));
        } catch (DAOException e) {
            logger.error(e.toString(), e);
            this.status = BeanStatus.ERROR_RETRIEVING_DATABASE_OBJECT;
            return Collections.emptyMap();
        }
    }

    private static void updatePageMap(Map<CMSPage, Boolean> map, List<Long> ignoreList) {
        if (map != null && ignoreList != null) {
            ignoreList.forEach(id -> map.keySet().stream().filter(page -> page.getId().equals(id)).findAny().ifPresent(page -> map.put(page, true)));
        }
    }

}
