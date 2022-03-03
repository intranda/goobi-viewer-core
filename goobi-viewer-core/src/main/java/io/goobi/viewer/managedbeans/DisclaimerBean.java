package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.administration.legal.CookieBanner;
import io.goobi.viewer.model.administration.legal.Disclaimer;

/**
 * This this the java backend class for enabling and configuring the disclaimer modal feature. 
 * This bean is view scoped, i.e. created fresh for each new page loaded
**/
@Named
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
        disclaimerForEdit = new Disclaimer(getDisclaimer());
    }

    /**
     * Constructor for testing purposes
     * @param dao   the IDAO implementation to use
     */
    public DisclaimerBean(IDAO dao) {
        this.dao = dao;
        disclaimerForEdit = new Disclaimer(getDisclaimer());
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
     * @return the cookie banner stored in the DAO
     */
    public Disclaimer getDisclaimer() {
        if (dao != null) {
            try {
                return dao.getDisclaimer();
            } catch (DAOException e) {
                logger.error("Error retrieving disclaimer from dao: {}", e.toString());
                return null;
            }
        } else {
            return null;
        }
    }
    
    public void save() {
        if(this.disclaimerForEdit != null) {
            try {
                if(!this.dao.saveDisclaimer(this.disclaimerForEdit)) {
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
                this.disclaimerForEdit.setId(disclaimer.getId());
            }
        }
    }
    
    /**
     * Check if the banner is active, i.e. should be displayed at all
     * @return true if the banner should be shown if appropriate
     */
    public boolean isDisclaimerActive() {
        return this.disclaimerForEdit.isActive();
    }
    
    /**
     * Set the {@link Disclaimer#getRequiresConsentAfter()} to the current time. Applies directly to the persisted object
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
                this.disclaimerForEdit.setId(disclaimer.getId());
            }
        }
    }
    
}
