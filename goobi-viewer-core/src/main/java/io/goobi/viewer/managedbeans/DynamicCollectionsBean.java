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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.faces.validators.SolrQueryValidator;
import io.goobi.viewer.managedbeans.CmsCollectionsBean.CMSCollectionImageMode;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.collections.DynamicCollection;
import io.goobi.viewer.model.cms.collections.DynamicCollectionTranslation;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AjaxBehaviorEvent;
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

    /** Reserved value that must not be used as a collection name (it is the viewer's "empty/none" placeholder and would clash in URLs). */
    private static final String RESERVED_NAME = "-";

    private List<DynamicCollection> collections;
    private DynamicCollection currentCollection;
    private DynamicCollection originalCollection; //collection from database, without any edits after last save
    private boolean piValid = true;
    private CMSCollectionImageMode imageMode = CMSCollectionImageMode.NONE;
    /** Hit count of the current Solr query, computed on blur for a syntactically valid query; null if not yet evaluated or empty. */
    private Long queryHitCount = null;

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
     * Loads the existing dynamic collection with the given name for editing. Called by the edit pretty-URL route. New collections are created via
     * {@link #createNewCollection()} instead, so this never treats any name as a "create new" sentinel.
     *
     * @param name unique name of the collection to load
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should compute the query hit count when loading an existing collection
     */
    public void setCollectionName(String name) throws DAOException {
        currentCollection = DataManager.getInstance().getDao().getDynamicCollection(name);
        if (currentCollection == null) {
            logger.warn("No dynamic collection found for name '{}'", name);
            currentCollection = new DynamicCollection();
        }
        currentCollection.populateLabels();
        currentCollection.populateDescriptions();
        initImageMode();
        updateQueryHitCount();
        originalCollection = new DynamicCollection(currentCollection);
    }

    /**
     * Prepares a blank dynamic collection for creation. Called by the "new collection" pretty-URL route.
     *
     * @should reset the query hit count of a previously edited collection
     */
    public void createNewCollection() {
        currentCollection = new DynamicCollection();
        currentCollection.populateLabels();
        currentCollection.populateDescriptions();
        initImageMode();
        updateQueryHitCount();
        originalCollection = new DynamicCollection(currentCollection);
    }

    /**
     * getCollectionName.
     *
     * @return the name of the current collection, or the placeholder value if none/blank
     */
    public String getCollectionName() {
        if (currentCollection != null && StringUtils.isNotBlank(currentCollection.getName())) {
            return currentCollection.getName();
        }

        return RESERVED_NAME;
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
            // Do not persist empty label/description translation rows
            getCurrentCollection().pruneEmptyTranslations();
            if (getCurrentCollection().getId() != null) {
                DataManager.getInstance().getDao().updateDynamicCollection(getCurrentCollection());
            } else {
                DataManager.getInstance().getDao().addDynamicCollection(getCurrentCollection());
            }
            updateCollections();
            invalidateBrowseView(getCurrentCollection());
        }
        return "pretty:adminCmsCollections";
    }

    /**
     * resetCurrentCollection.
     *
     * @return the pretty URL name for the collections overview after discarding changes to the current collection
     */
    public String resetCurrentCollection() {
        return "pretty:adminCmsCollections";
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
     * Deletes the currently edited collection and returns to the overview page. Used by the delete button on the edit page.
     *
     * @return pretty URL outcome of the dynamic collections overview
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteCurrentCollection() throws DAOException {
        deleteCollection(currentCollection);
        return "pretty:adminCmsCollections";
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

    /** Allowed identifier characters: letters, digits, underscore and hyphen (URL-safe, no encoding surprises). */
    private static final String IDENTIFIER_PATTERN = "[A-Za-z0-9_-]+";

    /**
     * Validates the collection identifier entered in the edit form: it must be non-blank, match {@link #IDENTIFIER_PATTERN} (URL-safe characters
     * only), not be the reserved placeholder, and be unique among dynamic collections.
     *
     * @param context current JSF faces context
     * @param comp UI component that triggered the validation
     * @param value identifier value submitted by the user
     * @throws jakarta.faces.validator.ValidatorException if the identifier is invalid
     */
    public void validateIdentifier(FacesContext context, UIComponent comp, Object value) throws ValidatorException {
        String identifier = value != null ? value.toString().trim() : "";
        if (StringUtils.isBlank(identifier)) {
            throw new ValidatorException(errorMessage(ViewerResourceBundle.getTranslation("admin__dynamic_collections_identifier_required", null)));
        }
        if (!identifier.matches(IDENTIFIER_PATTERN) || RESERVED_NAME.equals(identifier)) {
            throw new ValidatorException(errorMessage(ViewerResourceBundle.getTranslation("admin__dynamic_collections_identifier_invalid", null)));
        }
        try {
            DynamicCollection existing = DataManager.getInstance().getDao().getDynamicCollection(identifier);
            if (existing != null && (currentCollection == null || !existing.getId().equals(currentCollection.getId()))) {
                throw new ValidatorException(
                        errorMessage(ViewerResourceBundle.getTranslation("admin__dynamic_collections_identifier_duplicate", null)));
            }
        } catch (DAOException e) {
            logger.error("Error validating dynamic collection identifier: {}", e.getMessage());
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
     * AJAX listener (fired on blur of the Solr query field) that computes the hit count of the current query. Only runs when the field's
     * {@code solrQueryValidator} has already accepted the syntax, so a syntax error is reported by the validator, not here.
     *
     * @param event the AJAX behavior event
     */
    public void checkQuery(AjaxBehaviorEvent event) {
        updateQueryHitCount();
    }

    /**
     * Recomputes {@link #queryHitCount} for the current collection's Solr query. Resets the count to null if the query is blank or counting fails.
     */
    private void updateQueryHitCount() {
        queryHitCount = null;
        String query = currentCollection != null ? currentCollection.getSolrQuery() : null;
        if (StringUtils.isBlank(query)) {
            return;
        }
        try {
            queryHitCount = SolrQueryValidator.getHitCount(query);
        } catch (SolrServerException | IOException e) {
            logger.error("Error counting hits for dynamic collection query '{}': {}", query, e.getMessage());
        }
    }

    /**
     * @return the hit count computed on page load or by the last {@link #checkQuery(AjaxBehaviorEvent)} call, or null if not evaluated / query empty
     */
    public Long getQueryHitCount() {
        return queryHitCount;
    }

    /**
     * Whether the current collection carries a non-blank label for every supported language. Used by the translation hint in the edit page sidebar.
     *
     * @return true if all label translations are filled (or no collection is loaded); false otherwise
     */
    public boolean isCurrentLabelsComplete() {
        if (currentCollection == null) {
            return true;
        }
        return currentCollection.getLabels().stream().allMatch(t -> StringUtils.isNotBlank(t.getTranslationValue()));
    }

    /**
     * Builds the search-result URL listing the records of the given collection. The collection's stored Solr query is used directly as the search
     * query, so the link works regardless of whether the {@code DC_DYNAMIC} facet is configured in the viewer config.
     *
     * @param collection collection to build the search URL for
     * @return an absolute search page URL listing the collection's records
     */
    public String getSearchUrl(DynamicCollection collection) {
        String query = StringTools.encodeUrl(StringUtils.trimToEmpty(collection.getSolrQuery()));
        return PrettyUrlTools.getAbsolutePageUrl("newSearch5", "-", query, 1, "-", "-");
    }
}
