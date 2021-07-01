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
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSCollection;
import io.goobi.viewer.model.cms.CMSCollectionTranslation;
import io.goobi.viewer.model.viewer.CollectionView;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Bean handling cms settings for collections
 *
 * @author Florian Alpers
 */
@Named
@SessionScoped
public class CmsCollectionsBean implements Serializable {

    private static final long serialVersionUID = -2862611194397865986L;

    private static final Logger logger = LoggerFactory.getLogger(CmsCollectionsBean.class);
    private static final int MAX_IMAGES_PER_PAGE = 36;

    @Inject
    CmsMediaBean cmsMediaBean;

    private CMSCollection currentCollection;
    private String solrField = SolrConstants.DC;
    private String solrFieldValue;
    private List<CMSCollection> collections;
    private boolean piValid = true;

    /**
     * <p>
     * Constructor for CmsCollectionsBean.
     * </p>
     */
    public CmsCollectionsBean() {
        try {
            updateCollections();
        } catch (DAOException e) {
            logger.error("Error initializing collections");
            collections = Collections.EMPTY_LIST;
        }
    }

    /**
     * <p>
     * Getter for the field <code>currentCollection</code>.
     * </p>
     *
     * @return the currentCollection
     */
    public CMSCollection getCurrentCollection() {
        return currentCollection;
    }

    /**
     * <p>
     * Setter for the field <code>currentCollection</code>.
     * </p>
     *
     * @param currentCollection the currentCollection to set
     */
    public void setCurrentCollection(CMSCollection currentCollection) {
        this.currentCollection = currentCollection;
    }

    /**
     * <p>
     * Getter for the field <code>solrField</code>.
     * </p>
     *
     * @return the solrField
     */
    public String getSolrField() {
        return solrField;
    }

    /**
     * <p>
     * Setter for the field <code>solrField</code>.
     * </p>
     *
     * @param solrField the solrField to set
     */
    public void setSolrField(String solrField) {
        this.solrField = solrField;
        try {
            updateCollections();
        } catch (DAOException e) {
            logger.error("Error initializing collections");
            collections = Collections.EMPTY_LIST;
        }
    }

    /**
     * <p>
     * Getter for the field <code>solrFieldValue</code>.
     * </p>
     *
     * @return the solrFieldValue
     */
    public String getSolrFieldValue() {
        return solrFieldValue;
    }

    /**
     * <p>
     * Setter for the field <code>solrFieldValue</code>.
     * </p>
     *
     * @param solrFieldValue the solrFieldValue to set
     */
    public void setSolrFieldValue(String solrFieldValue) {
        this.solrFieldValue = solrFieldValue;
    }

    /**
     * <p>
     * getAllCollectionFields.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getAllCollectionFields() {
        List<String> collections = DataManager.getInstance().getConfiguration().getConfiguredCollections();
        return collections;
    }

    /**
     * <p>
     * Getter for the field <code>collections</code>.
     * </p>
     *
     * @return the configuredColelctions
     */
    public List<CMSCollection> getCollections() {
        return collections;
    }

    /**
     * <p>
     * updateCollections.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void updateCollections() throws DAOException {
        this.collections = DataManager.getInstance().getDao().getCMSCollections(getSolrField());
        this.collections.sort((c1, c2) -> Long.compare(c2.getId(), c1.getId()));
        //If a collection is selected that is no longer in the list, deselect it
        if (this.currentCollection != null && !this.collections.contains(this.currentCollection)) {
            this.currentCollection = null;
        }
    }

    /**
     * <p>
     * addCollection.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void addCollection() throws DAOException {
        if (StringUtils.isNoneBlank(getSolrField(), getSolrFieldValue())) {
            CMSCollection collection = new CMSCollection(getSolrField(), getSolrFieldValue());
            DataManager.getInstance().getDao().addCMSCollection(collection);
            updateCollections();
            setSolrFieldValue("");//empty solr field value to avoid creating the same collection again
        } else {
            Messages.error("cms_collections_err_noselection");
        }
    }

    /**
     * @param collection
     */
    private void addToCollectionViews(CMSCollection collection) {
        CollectionView collectionView = BeanUtils.getBrowseBean().getCollection(collection.getSolrField());
        if (collectionView != null) {
            collectionView.setCollectionInfo(collection.getSolrFieldValue(), collection);
        }
        List<CollectionView> collections = BeanUtils.getCmsBean().getCollections(collection.getSolrField());
        collections.forEach(view -> view.setCollectionInfo(collection.getSolrFieldValue(), collection));
    }

    /**
     * @param collection
     */
    private void removeFromCollectionViews(CMSCollection collection) {
        CollectionView collectionView = BeanUtils.getBrowseBean().getCollection(collection.getSolrField());
        if (collectionView != null) {
            collectionView.removeCollectionInfo(collection.getSolrFieldValue());
        }
        List<CollectionView> collections = BeanUtils.getCmsBean().getCollections(collection.getSolrField());
        collections.forEach(view -> view.removeCollectionInfo(collection.getSolrFieldValue()));

    }

    /**
     * <p>
     * deleteCollection.
     * </p>
     *
     * @param collection a {@link io.goobi.viewer.model.cms.CMSCollection} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteCollection(CMSCollection collection) throws DAOException {
        DataManager.getInstance().getDao().deleteCMSCollection(collection);
        removeFromCollectionViews(collection);
        updateCollections();
    }

    /**
     * <p>
     * editCollection.
     * </p>
     *
     * @param collection a {@link io.goobi.viewer.model.cms.CMSCollection} object.
     * @return a {@link java.lang.String} object.
     */
    public String editCollection(CMSCollection collection) {
        setCurrentCollection(collection);
        collection.populateDescriptions();
        collection.populateLabels();
        return "pretty:adminCmsEditCollection";
    }

    /**
     * <p>
     * getCurrentLabel.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSCollectionTranslation} object.
     */
    public CMSCollectionTranslation getCurrentLabel(String language) {
        return getCurrentCollection().getLabelAsTranslation(language);
    }

    /**
     * <p>
     * getCurrentDescription.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSCollectionTranslation} object.
     */
    public CMSCollectionTranslation getCurrentDescription(String language) {
        return getCurrentCollection().getDescriptionAsTranslation(language);
    }

    /**
     * <p>
     * saveCurrentCollection.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveCurrentCollection() throws DAOException {
        if (getCurrentCollection() != null) {
            DataManager.getInstance().getDao().updateCMSCollection(getCurrentCollection());
            updateCollections();
            addToCollectionViews(getCurrentCollection());

        }
        return "pretty:adminCmsCollections";
    }

    /**
     * <p>
     * resetCurrentCollection.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String resetCurrentCollection() throws DAOException {
        if (getCurrentCollection() != null) {
            DataManager.getInstance().getDao().refreshCMSCollection(getCurrentCollection());
        }
        return "pretty:adminCmsCollections";
    }

    /**
     * Checks the current collection for validity. Currently only checks if a possibly entered PI exists in the solr
     *
     * @return a boolean.
     */
    public boolean isCurrentCollectionValid() {
        if (getCurrentCollection() != null && StringUtils.isNotBlank(getCurrentCollection().getRepresentativeWorkPI())) {
            return piValid;
        } else {
            return true;
        }
    }

    /**
     * <p>
     * validatePI.
     * </p>
     *
     * @param context a {@link javax.faces.context.FacesContext} object.
     * @param comp a {@link javax.faces.component.UIComponent} object.
     * @param value a {@link java.lang.Object} object.
     * @throws javax.faces.validator.ValidatorException if any.
     */
    public void validatePI(FacesContext context, UIComponent comp, Object value) throws ValidatorException {
        if (getCurrentCollection() != null && StringUtils.isNotBlank(getCurrentCollection().getRepresentativeWorkPI())) {
            try {
                if (!validatePi((String) value)) {
                    FacesMessage msg = new FacesMessage(ViewerResourceBundle.getTranslation("pi_errNotFound", null), "");
                    msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                    piValid = false;
                    throw new ValidatorException(msg);
                }
            } catch (IndexUnreachableException | PresentationException e) {
                FacesMessage msg = new FacesMessage(ViewerResourceBundle.getTranslation("pi_validationError", null), "");
                msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                piValid = true; //if the error is in reaching the index, allow saving regardless
                throw new ValidatorException(msg);
            }
        }
        piValid = true;
    }

    /**
     * Checks if the given pi matches a known PI in the solr index. If the pi is empty, true is returned to allow not setting any pi
     *
     * @return false if no current collection is set, the pi does not match any known work
     * @param pi a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public static boolean validatePi(String pi) throws IndexUnreachableException, PresentationException {
        if (StringUtils.isNotBlank(pi)) {
            SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
            return doc != null;
        } else {
            return true;
        }
    }

}
