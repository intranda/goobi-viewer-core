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
import io.goobi.viewer.model.administration.legal.Disclaimer;

@Named
@ViewScoped
public class DisclaimerEditBean implements Serializable {

    private static final long serialVersionUID = -6562240290914952926L;
    private static final Logger logger = LoggerFactory.getLogger(DisclaimerEditBean.class);

    private final IDAO dao;
    private final Disclaimer disclaimerForEdit;

    /**
     * Default constructor using the IDAO from the {@link DataManager} class
     */
    public DisclaimerEditBean() {
        dao = retrieveDAO();
        this.disclaimerForEdit = loadDisclaimerForEdit();
    }

    /**
     * Constructor for testing purposes
     * 
     * @param dao the IDAO implementation to use
     */
    public DisclaimerEditBean(IDAO dao) {
        this.dao = dao;
        this.disclaimerForEdit = loadDisclaimerForEdit();
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
    

    private IDAO retrieveDAO() {
        try {
            return DataManager.getInstance().getDao();
        } catch (DAOException e) {
            logger.error("Error initializing DisclaimerBean: {}", e.toString());
            return null;
        }
    }


}
