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

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.administration.legal.Disclaimer;

/**
 * Bean for editing the disclaimer. ViewScoped, so all settings are discarded when loading a new view.
 *
 * @author florian
 */
@Named
@ViewScoped
public class DisclaimerEditBean implements Serializable {

    private static final long serialVersionUID = -6562240290914952926L;
    private static final Logger logger = LogManager.getLogger(DisclaimerEditBean.class);

    private final IDAO dao;
    private final Disclaimer disclaimerForEdit;

    /**
     * Default constructor using the IDAO from the {@link io.goobi.viewer.controller.DataManager} class.
     */
    public DisclaimerEditBean() {
        dao = retrieveDAO();
        this.disclaimerForEdit = loadDisclaimerForEdit();
    }

    /**
     * Constructor for testing purposes.
     *
     * @param dao the IDAO implementation to use
     */
    public DisclaimerEditBean(IDAO dao) {
        this.dao = dao;
        this.disclaimerForEdit = loadDisclaimerForEdit();
    }

    /**
     * <p>save.</p>
     */
    public void save() {
        if (this.disclaimerForEdit != null) {
            //            this.disclaimerForEdit.setAcceptanceScope(new ConsentScope(this.disclaimerForEdit.getAcceptanceScope().toString()));
            try {
                if (!this.dao.saveDisclaimer(this.disclaimerForEdit)) {
                    throw new DAOException("Saving disclaimer failed");
                }
                Messages.info("admin__legal__disclaimer_save_success");
            } catch (DAOException e) {
                Messages.error("admin__legal__disclaimer_save_error");
            }
        }
    }

    /**
     * <p>Getter for the field <code>disclaimerForEdit</code>.</p>
     *
     * @return a {@link io.goobi.viewer.model.administration.legal.Disclaimer} object
     */
    public Disclaimer getDisclaimerForEdit() {
        return disclaimerForEdit;
    }

    /**
     * Activate/deactivate the disclaimer. Applies directly to the persisted object.
     *
     * @param active a boolean
     * @throws io.goobi.viewer.exceptions.DAOException
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
     * Check if the banner is active, i.e. should be displayed at all.
     *
     * @return true if the banner should be shown if appropriate
     */
    public boolean isDisclaimerActive() {
        return this.disclaimerForEdit.isActive();
    }

    /**
     * Set the {@link io.goobi.viewer.model.administration.legal.Disclaimer#getRequiresConsentAfter()} to the current time.
     * Applies directly to the persisted object.
     *
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public void resetUserConsent() throws DAOException {
        //save the current date both to the banner managed by the persistence context and to the copy we are editing
        //this way, saving the current banner is not required, but is a save is performed, the date is not overwritten
        if (this.dao != null) {
            Disclaimer disclaimer = dao.getDisclaimer();
            disclaimer.setRequiresConsentAfter(LocalDateTime.now());
            if (dao.saveDisclaimer(disclaimer)) {
                if (this.disclaimerForEdit != null) {
                    this.disclaimerForEdit.setRequiresConsentAfter(disclaimer.getRequiresConsentAfter());
                }
                Messages.info("admin__legal__disclaimer_reset_consent__success");
            } else {
                Messages.error("admin__legal__disclaimer_reset_consent__error");
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
