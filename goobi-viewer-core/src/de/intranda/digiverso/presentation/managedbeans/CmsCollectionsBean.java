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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.cms.CMSCollection;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.cms.Translation;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * Bean handling cms settings for collections
 * 
 * @author Florian Alpers
 *
 */
@Named
@SessionScoped
public class CmsCollectionsBean implements Serializable {

    private static final long serialVersionUID = -2862611194397865986L;

    private static final Logger logger = LoggerFactory.getLogger(CmsCollectionsBean.class);
    private static final int MAX_IMAGES_PER_PAGE = 36;

    private CMSCollection currentCollection;
    private String solrField = SolrConstants.DC;
    private String solrFieldValue;
    private List<CMSCollection> collections;
    private CMSMediaItem selectedMediaItem = null;
    private boolean piValid = true;

    public CmsCollectionsBean() {
        try {
            updateCollections();
        } catch (DAOException e) {
            logger.error("Error initializing collections");
            collections = Collections.EMPTY_LIST;
        }
    }

    /**
     * @return the currentCollection
     */
    public CMSCollection getCurrentCollection() {
        return currentCollection;
    }

    /**
     * @param currentCollection the currentCollection to set
     */
    public void setCurrentCollection(CMSCollection currentCollection) {
        this.currentCollection = currentCollection;
    }

    /**
     * @return the solrField
     */
    public String getSolrField() {
        return solrField;
    }

    /**
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
     * @return the solrFieldValue
     */
    public String getSolrFieldValue() {
        return solrFieldValue;
    }

    /**
     * @param solrFieldValue the solrFieldValue to set
     */
    public void setSolrFieldValue(String solrFieldValue) {
        this.solrFieldValue = solrFieldValue;
    }

    public List<String> getAllCollectionFields() {
        List<String> collections = DataManager.getInstance().getConfiguration().getConfiguredCollections();
        return collections;
    }

    /**
     * @return the configuredColelctions
     */
    public List<CMSCollection> getCollections() {
        return collections;
    }

    public void updateCollections() throws DAOException {
        this.collections = DataManager.getInstance().getDao().getCMSCollections(getSolrField());
        this.collections.sort((c1, c2) -> Long.compare(c2.getId(), c1.getId()));
        //If a collection is selected that is no longer in the list, deselect it
        if (this.currentCollection != null && !this.collections.contains(this.currentCollection)) {
            this.currentCollection = null;
        }
    }

    public void addCollection() throws DAOException {
        CMSCollection collection = new CMSCollection(getSolrField(), getSolrFieldValue());
        DataManager.getInstance().getDao().addCMSCollection(collection);
        updateCollections();
        setSolrFieldValue("");//empty solr field value to avoid creating the same collection again
    }

    public void deleteCollection(CMSCollection collection) throws DAOException {
        DataManager.getInstance().getDao().deleteCMSCollection(collection);
        updateCollections();
    }

    public String editCollection(CMSCollection collection) {
        setCurrentCollection(collection);
        collection.populateDescriptions();
        collection.populateLabels();
        return "pretty:adminCmsEditCollection";
    }

    public Translation getCurrentLabel(String language) {
        return getCurrentCollection().getLabelAsTranslation(language);
    }

    public Translation getCurrentDescription(String language) {
        return getCurrentCollection().getDescriptionAsTranslation(language);
    }

    public String saveCurrentCollection() throws DAOException {
        if (getCurrentCollection() != null) {
            DataManager.getInstance().getDao().updateCMSCollection(getCurrentCollection());
            updateCollections();
        }
        return "pretty:adminCmsCollections";
    }

    public String resetCurrentCollection() throws DAOException {
        if (getCurrentCollection() != null) {
            DataManager.getInstance().getDao().refreshCMSCollection(getCurrentCollection());
        }
        return "pretty:adminCmsCollections";
    }

    /**
     * @return the selectedMediaItem
     */
    public CMSMediaItem getSelectedMediaItem() {
        return selectedMediaItem;
    }

    /**
     * @param selectedMediaItem the selectedMediaItem to set
     */
    public void setSelectedMediaItem(CMSMediaItem selectedMediaItem) {
        this.selectedMediaItem = selectedMediaItem;
    }

    public boolean isSelectedMediaItem(CMSMediaItem item) {
        if (selectedMediaItem == null && item == null) {
            return true;
        } else {
            return item != null && item.equals(selectedMediaItem);
        }
    }
    
    /**
     * Checks the current collection for validity. Currently only checks if a possibly entered PI exists in the solr
     * 
     * @return  false only if a current collection is selected and it has a non-black {@link CMSCollecion#getRepresentativeWorkPI()} 
     * which does not denote a work found in the solr index
     */
    public boolean isCurrentCollectionValid() {
        if(getCurrentCollection() != null && StringUtils.isNotBlank(getCurrentCollection().getRepresentativeWorkPI())) {
            return piValid;
        } else {
            return true;
        }
    }

    public void validatePI(FacesContext context, UIComponent comp, Object value) throws ValidatorException{
        if (getCurrentCollection() != null && StringUtils.isNotBlank(getCurrentCollection().getRepresentativeWorkPI())) {
            try {
                if (!validatePi((String) value)) {
                    FacesMessage msg = new FacesMessage(Helper.getTranslation("pi_errNotFound", null), "");
                    msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                    piValid = false;
                    throw new ValidatorException(msg);
                }
            } catch (IndexUnreachableException | PresentationException e) {
                FacesMessage msg = new FacesMessage(Helper.getTranslation("pi_validationError", null), "");
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
     * @throws PresentationException
     * @throws IndexUnreachableException
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
