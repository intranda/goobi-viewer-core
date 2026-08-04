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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.CmsCollectionsBean.CMSCollectionImageMode;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.collections.DynamicCollection;
import io.goobi.viewer.model.cms.collections.DynamicCollectionTranslation;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.inject.Named;

/**
 * Bean handling administration of database-defined dynamic collections. Each dynamic collection carries a unique name, a multilingual description, an
 * arbitrary Solr query defining its contents, and a representative image. Modeled on {@link CmsCollectionsBean} but managing a flat list.
 */
@Named
@SessionScoped
public class DynamicCollectionsBean implements Serializable {

    private static final long serialVersionUID = -4657434257888013106L;

    private static final Logger logger = LogManager.getLogger(DynamicCollectionsBean.class);

    /** Sentinel path parameter value indicating that a new collection should be created. */
    private static final String NEW_COLLECTION = "-";

    private List<DynamicCollection> collections;
    private DynamicCollection currentCollection;
    private DynamicCollection originalCollection; //collection from database, without any edits after last save
    private boolean piValid = true;
    private CMSCollectionImageMode imageMode = CMSCollectionImageMode.NONE;

    /**
     * Creates a new DynamicCollectionsBean instance.
     */
    public DynamicCollectionsBean() {
        try {
            updateCollections();
        } catch (DAOException e) {
            logger.error("Error initializing dynamic collections");
            collections = new ArrayList<>();
        }
    }

    /**
     * updateCollections.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void updateCollections() throws DAOException {
        this.collections = DataManager.getInstance().getDao().getAllDynamicCollections();
        if (this.currentCollection != null && !this.collections.contains(this.currentCollection)) {
            this.currentCollection = null;
        }
    }

    /**
     * Getter for the field <code>collections</code>.
     *
     * @return the list of all dynamic collections
     */
    public List<DynamicCollection> getCollections() {
        return collections;
    }

    /**
     * Getter for the field <code>currentCollection</code>.
     *
     * @return the dynamic collection currently being edited
     */
    public DynamicCollection getCurrentCollection() {
        return currentCollection;
    }

    /**
     * Setter for the field <code>currentCollection</code>.
     *
     * @param currentCollection the dynamic collection to set as the currently selected collection
     */
    public void setCurrentCollection(DynamicCollection currentCollection) {
        this.currentCollection = currentCollection;
    }

    /**
     * Loads the dynamic collection with the given name for editing, or prepares a new collection if the name is the {@value #NEW_COLLECTION}
     * sentinel. Called by the edit pretty-URL route.
     *
     * @param name unique name of the collection, or "-" to create a new one
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void setCollectionName(String name) throws DAOException {
        if (StringUtils.isBlank(name) || NEW_COLLECTION.equals(name)) {
            currentCollection = new DynamicCollection();
        } else {
            currentCollection = DataManager.getInstance().getDao().getDynamicCollection(name);
            if (currentCollection == null) {
                currentCollection = new DynamicCollection(name);
            }
        }
        currentCollection.populateLabels();
        currentCollection.populateDescriptions();
        initImageMode();
        originalCollection = new DynamicCollection(currentCollection);
    }

    /**
     * getCollectionName.
     *
     * @return the name of the current collection, or the new-collection sentinel if none/blank
     */
    public String getCollectionName() {
        if (currentCollection != null && StringUtils.isNotBlank(currentCollection.getName())) {
            return currentCollection.getName();
        }

        return NEW_COLLECTION;
    }

    /**
     * getCurrentLabel.
     *
     * @param language ISO language code to retrieve the label for
     * @return the label translation of the current collection for the given language
     */
    public DynamicCollectionTranslation getCurrentLabel(String language) {
        return getCurrentCollection().getLabelAsTranslation(language);
    }

    /**
     * getCurrentDescription.
     *
     * @param language ISO language code to retrieve the description for
     * @return the description translation of the current collection for the given language
     */
    public DynamicCollectionTranslation getCurrentDescription(String language) {
        return getCurrentCollection().getDescriptionAsTranslation(language);
    }

    /**
     * saveCurrentCollection.
     *
     * @return the pretty URL name for the collections overview after saving the current collection
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveCurrentCollection() throws DAOException {
        if (getCurrentCollection() != null) {
            // Remove thumbnail data for whatever mode is not selected
            switch (getImageMode()) {
                case NONE:
                    getCurrentCollection().setRepresentativeWorkPI(null);
                    getCurrentCollection().setMediaItem(null);
                    break;
                case IMAGE:
                    getCurrentCollection().setRepresentativeWorkPI(null);
                    break;
                case PI:
                    getCurrentCollection().setMediaItem(null);
                    break;
                default:
                    break;
            }
            if (getCurrentCollection().getId() != null) {
                DataManager.getInstance().getDao().updateDynamicCollection(getCurrentCollection());
            } else {
                DataManager.getInstance().getDao().addDynamicCollection(getCurrentCollection());
            }
            updateCollections();
            invalidateBrowseView(getCurrentCollection());
        }
        return "pretty:adminDynamicCollections";
    }

    /**
     * resetCurrentCollection.
     *
     * @return the pretty URL name for the collections overview after discarding changes to the current collection
     */
    public String resetCurrentCollection() {
        return "pretty:adminDynamicCollections";
    }

    /**
     * deleteCollection.
     *
     * @param collection collection to delete from the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteCollection(DynamicCollection collection) throws DAOException {
        DataManager.getInstance().getDao().deleteDynamicCollection(collection);
        invalidateBrowseView(collection);
        updateCollections();
    }

    /**
     * Removes any cached browse view for the given collection so that changes take effect immediately.
     *
     * @param collection collection whose cached browse view to invalidate
     */
    private static void invalidateBrowseView(DynamicCollection collection) {
        if (collection == null || StringUtils.isBlank(collection.getName())) {
            return;
        }
        CollectionBrowseBean collectionBrowseBean = BeanUtils.getCollectionBrowseBean();
        if (collectionBrowseBean != null) {
            collectionBrowseBean.removeDynamicCollectionView(collection.getName());
        }
    }

    /**
     * Validates the collection name entered in the edit form: it must be non-blank, contain neither ':' nor ';' (which would break the facet token
     * and URL round-trip), and be unique among dynamic collections.
     *
     * @param context current JSF faces context
     * @param comp UI component that triggered the validation
     * @param value name value submitted by the user
     * @throws jakarta.faces.validator.ValidatorException if the name is invalid
     */
    public void validateName(FacesContext context, UIComponent comp, Object value) throws ValidatorException {
        String name = value != null ? value.toString().trim() : "";
        if (StringUtils.isBlank(name)) {
            throw new ValidatorException(errorMessage(ViewerResourceBundle.getTranslation("admin__dynamic_collections_name_required", null)));
        }
        if (name.contains(":") || name.contains(";")) {
            throw new ValidatorException(errorMessage(ViewerResourceBundle.getTranslation("admin__dynamic_collections_name_invalid", null)));
        }
        try {
            DynamicCollection existing = DataManager.getInstance().getDao().getDynamicCollection(name);
            if (existing != null && (currentCollection == null || !existing.getId().equals(currentCollection.getId()))) {
                throw new ValidatorException(errorMessage(ViewerResourceBundle.getTranslation("admin__dynamic_collections_name_duplicate", null)));
            }
        } catch (DAOException e) {
            logger.error("Error validating dynamic collection name: {}", e.getMessage());
        }
    }

    private static FacesMessage errorMessage(String text) {
        FacesMessage msg = new FacesMessage(text, "");
        msg.setSeverity(FacesMessage.SEVERITY_ERROR);
        return msg;
    }

    /**
     * Checks the current collection for validity. Currently only checks whether a possibly entered PI exists in Solr.
     *
     * @return true if the current collection is valid, false otherwise
     */
    public boolean isCurrentCollectionValid() {
        if (getCurrentCollection() != null && StringUtils.isNotBlank(getCurrentCollection().getRepresentativeWorkPI())) {
            return piValid;
        }

        return true;
    }

    /**
     * validatePI.
     *
     * @param context current JSF faces context
     * @param comp UI component that triggered the validation
     * @param value PI value submitted by the user
     * @throws jakarta.faces.validator.ValidatorException if any.
     */
    public void validatePI(FacesContext context, UIComponent comp, Object value) throws ValidatorException {
        if (getCurrentCollection() != null && StringUtils.isNotBlank(getCurrentCollection().getRepresentativeWorkPI())) {
            try {
                if (!validatePI((String) value)) {
                    piValid = false;
                    throw new ValidatorException(errorMessage(ViewerResourceBundle.getTranslation("pi_errNotFound", null)));
                }
            } catch (IndexUnreachableException | PresentationException e) {
                piValid = true; //if the error is in reaching the index, allow saving regardless
                throw new ValidatorException(errorMessage(ViewerResourceBundle.getTranslation("pi_validationError", null)));
            }
        }
        piValid = true;
    }

    /**
     * Checks whether the given PI matches a known record in the Solr index. An empty PI is treated as valid (no representative record set).
     *
     * @param pi persistent identifier to look up in the Solr index
     * @return true if the PI is blank or matches a known record; false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public static boolean validatePI(String pi) throws IndexUnreachableException, PresentationException {
        if (StringUtils.isNotBlank(pi)) {
            SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
            return doc != null;
        }

        return true;
    }

    public CMSCollectionImageMode getImageMode() {
        return imageMode;
    }

    public void setImageMode(CMSCollectionImageMode imageMode) {
        this.imageMode = imageMode;
    }

    /**
     * Sets the value of <code>imageMode</code> depending on the properties of <code>currentCollection</code>.
     */
    public void initImageMode() {
        if (currentCollection == null) {
            return;
        }

        if (currentCollection.hasRepresentativeWork()) {
            imageMode = CMSCollectionImageMode.PI;
        } else if (currentCollection.hasMediaItem()) {
            imageMode = CMSCollectionImageMode.IMAGE;
        } else {
            imageMode = CMSCollectionImageMode.NONE;
        }
    }

    /**
     * @return true if the current collection has unsaved edits compared to the persisted state
     */
    public boolean isDirty() {
        return this.currentCollection != null && this.originalCollection != null && !this.currentCollection.contentEquals(this.originalCollection);
    }

    /**
     * Builds the search-result URL for the given collection using its dynamic-collection facet token.
     *
     * @param collection collection to build the search URL for
     * @return an absolute search page URL pre-filtered by the collection's facet
     */
    public String getSearchUrl(DynamicCollection collection) {
        String filter = SolrConstants.DYNCOL + ":" + collection.getName();
        filter = StringTools.encodeUrl(filter);
        return PrettyUrlTools.getAbsolutePageUrl("newSearch5", "-", "-", 1, "-", filter);
    }
}
