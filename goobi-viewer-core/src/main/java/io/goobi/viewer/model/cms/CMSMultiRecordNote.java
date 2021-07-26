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
package io.goobi.viewer.model.cms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Class holding a formatted text related to a single PI which may be edited in the admin/cms-backend and displayed in a (sidebar) widget
 * 
 * @author florian
 *
 */
@Entity
@DiscriminatorValue("MULTI")
public class CMSMultiRecordNote extends CMSRecordNote {

    private static final Logger logger = LoggerFactory.getLogger(CMSMultiRecordNote.class);

    /**
     * PI of the record this note relates to. Should be effectively final, but can't be for DAO campatibility
     */
    @Column(name = "query", nullable = true)
    private String query;

    /**
     * A list of PIs for all accessible records matching the query. 
     * If null, {@link #getRecords()} queries the solr and sets records to a list (non-null) containing the matching results.
     * Reset to null when loaded for editing and when the query is changed
     */
    @Transient
    private List<String> records = null;

    public CMSMultiRecordNote() {
    }

    /**
     * @param pi
     */
    public CMSMultiRecordNote(String query) {
        super();
        this.query = query;
    }

    /**
     * @param o
     */
    public CMSMultiRecordNote(CMSRecordNote source) {
        super(source);
        if (source instanceof CMSMultiRecordNote) {
            this.query = ((CMSMultiRecordNote) source).query;
        }
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query the query to set
     */
    public void setQuery(String query) {
        if(!StringUtils.equals(query, this.query)) {            
            this.records = null;
        }
        this.query = query;
    }

    /**
     * @return a list of PIs of all records matching the query
     */
    public List<String> getRecords() {
        if (this.records == null) {
            try {
                this.records = searchRecords();
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error("Error querying records for MultiRecordNote", e);
                return Collections.emptyList();
            }
        }
        return this.records;
    }

    private List<String> searchRecords() throws PresentationException, IndexUnreachableException {
        String solrQuery = getQueryForSearch();

        List<String> pis = new ArrayList<>();

        SolrDocumentList solrDocs = DataManager.getInstance()
                .getSearchIndex()
                .search(solrQuery, 0, Integer.MAX_VALUE, null, null, Arrays.asList(SolrConstants.PI))
                .getResults();
        for (SolrDocument doc : solrDocs) {
            String pi = (String) SolrTools.getSingleFieldValue(doc, SolrConstants.PI);
            pis.add(pi);
        }
        return pis;
    }

    public String getQueryForSearch() {
        return "+(" + this.query + ") +(ISWORK:* ISANCHOR:*)";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSRecordNote#isSingleRecordNote()
     */
    @Override
    public boolean isSingleRecordNote() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSRecordNote#isMultiRecordNote()
     */
    @Override
    public boolean isMultiRecordNote() {
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSRecordNote#matchesFilter(java.lang.String)
     */
    @Override
    public boolean matchesFilter(String filter) {
        if(StringUtils.isNotBlank(filter)) {
            return getNoteTitle().getValues().stream().map(pair -> pair.getValue()).anyMatch(title -> title.toLowerCase().contains(filter.toLowerCase()));
        } else {
            return true;
        }
    }

}
