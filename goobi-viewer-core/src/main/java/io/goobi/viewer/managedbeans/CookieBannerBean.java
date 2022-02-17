package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.administration.legal.CookieBanner;
import io.goobi.viewer.model.cms.CMSPage;

@Named
@ViewScoped
public class CookieBannerBean implements Serializable {

    private static final long serialVersionUID = -6562240290904952926L;
    private static final Logger logger = LoggerFactory.getLogger(CookieBannerBean.class);

    private final IDAO dao;
    private final CookieBanner editCookieBanner;
    private final Map<CMSPage, Boolean> cmsPageMap;
    private BeanStatus status = BeanStatus.UNITIALIZED;

    public static enum BeanStatus {
        UNITIALIZED,
        ERROR_DATABASE_CONNECTION,
        ERROR_RETRIEVING_DATABASE_OBJECT,
        INITIALIZED
    }

    public CookieBannerBean() {
        dao = retrieveDAO();
        editCookieBanner = new CookieBanner(getCookieBanner());
        this.cmsPageMap = buildCMSPageMap();
        if (BeanStatus.UNITIALIZED.equals(this.status)) {
            this.status = BeanStatus.INITIALIZED;
            updatePageMap(cmsPageMap, editCookieBanner.getIgnoreList());
        }
    }


    public CookieBannerBean(IDAO dao) {
        this.dao = dao;
        editCookieBanner = new CookieBanner(getCookieBanner());
        this.cmsPageMap = buildCMSPageMap();
        if (BeanStatus.UNITIALIZED.equals(this.status)) {
            this.status = BeanStatus.INITIALIZED;
            updatePageMap(cmsPageMap, editCookieBanner.getIgnoreList());

        }
    }

    public CookieBanner getCookieBanner() {
        if (dao != null) {
            try {
                return dao.getCookieBanner();
            } catch (DAOException e) {
                logger.error("Error retrieving cookie banner from dao: {}", e.toString());
                this.status = BeanStatus.ERROR_RETRIEVING_DATABASE_OBJECT;
                return null;
            }
        } else {
            return null;
        }
    }

    public BeanStatus getStatus() {
        return status;
    }

    public CookieBanner getCookieBannerForEdit() {
        return this.editCookieBanner;
    }

    public Map<CMSPage, Boolean> getCmsPageMap() {
        return cmsPageMap;
    }

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

    public void resetUserConsent() throws DAOException {
        //save the current date both to the banner managed by the persistence context and to the copy we are editing
        //this way, saving the current banner is not required, but is a save is performed, the date is not overwritten
        if (this.dao != null) {
            CookieBanner banner = dao.getCookieBanner();
            banner.setRequiresConsentAfter(LocalDateTime.now());
            dao.saveCookieBanner(banner);
            if (this.editCookieBanner != null) {
                this.editCookieBanner.setRequiresConsentAfter(LocalDateTime.now());
            }
        }
    }

    private List<Long> getCmsPageIdsToIgnore() {
        return this.cmsPageMap.keySet().stream().filter(p -> this.cmsPageMap.get(p)).map(CMSPage::getId).sorted().collect(Collectors.toList());
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

    private void updatePageMap(Map<CMSPage, Boolean> map, List<Long> ignoreList) {
        if(map != null && ignoreList != null) {
            ignoreList.forEach(id -> {
                map.keySet().stream().filter(page -> page.getId().equals(id)).findAny().ifPresent(page -> {
                    map.put(page, true);
                });
            });
        }
    }
    
    public void setBannerActive(boolean active) throws DAOException {
        if (this.dao != null) {
            CookieBanner banner = dao.getCookieBanner();
            banner.setActive(active);
            dao.saveCookieBanner(banner);
            if (this.editCookieBanner != null) {
                this.editCookieBanner.setActive(active);
            }
        }
    }
    
    public boolean isBannerActive() {
        return this.editCookieBanner.isActive();
    }
    
    public String getCookieBannerConfig() {
        if(dao != null) {            
            try {
                CookieBanner banner = dao.getCookieBanner();
                JSONObject json = new JSONObject();
                boolean active = banner.isActive();
                if(active && BeanUtils.getNavigationHelper().isCmsPage()) {
                    Long pageId = BeanUtils.getCmsBean().getCurrentPage().getId();
                    if(banner.getIgnoreList().contains(pageId)) {
                        active = false;
                    }
                }
                json.put("active", active);
                json.put("lastEditedHash", banner.getRequiresConsentAfter().atZone(ZoneId.systemDefault()).toEpochSecond());
                return json.toString();
            } catch (DAOException e) {
                return "{}";
            }
        } else {
            return "{}";
        }
    }
    
}
