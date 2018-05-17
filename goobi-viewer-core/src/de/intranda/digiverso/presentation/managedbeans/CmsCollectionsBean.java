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

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSCollection;

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
    
    private CMSCollection currentCollection;
    private String solrField = SolrConstants.DC;
    private String solrFieldValue;
    private List<CMSCollection> collections;
    
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
    }
    
    public void addCollection() throws DAOException {
        CMSCollection collection = new CMSCollection(getSolrField(), getSolrFieldValue());
        DataManager.getInstance().getDao().addCMSCollection(collection);
        updateCollections();
    }
    
}
