package io.goobi.viewer.managedbeans;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.administration.legal.CookieBanner;

@Named
@ViewScoped
public class CookieBannerBean implements Serializable {

    private static final long serialVersionUID = -6562240290904952926L;
    private static final Logger logger = LoggerFactory.getLogger(CookieBannerBean.class);

    private final IDAO dao;
    private final CookieBanner editCookieBanner;
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
        if(editCookieBanner != null) {
            this.status = BeanStatus.INITIALIZED;
        }
    }

    public CookieBannerBean(IDAO dao) {
        this.dao = dao;
        editCookieBanner = new CookieBanner(getCookieBanner());
        if(editCookieBanner != null) {
            this.status = BeanStatus.INITIALIZED;
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

    public void saveCookieBannerForEdit() {
        if (this.editCookieBanner != null) {
            try {
                //save a clone of the editCookieBanner, so the editCookieBanner itself is never managed
                //and changes to it will not be reflected in the persistence context until save is called again
                this.dao.saveCookieBanner(new CookieBanner(editCookieBanner));
            } catch (DAOException e) {
                logger.error("Error saving cookie banner", e);

            }
        }
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

}
