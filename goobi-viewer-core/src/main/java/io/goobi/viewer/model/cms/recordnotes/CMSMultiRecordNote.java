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
package io.goobi.viewer.model.cms.recordnotes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue.ValuePair;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

/**
 * Class holding a formatted text related to a single PI which may be edited in the admin/cms-backend and displayed in a (sidebar) widget
 *
 * @author florian
 *
 */
@Entity
@DiscriminatorValue("MULTI")
public class CMSMultiRecordNote extends CMSRecordNote {

    private static final long serialVersionUID = -7380697992750404715L;

    private static final Logger logger = LogManager.getLogger(CMSMultiRecordNote.class);

    /**
     * PI of the record this note relates to. Should be effectively final, but can't be for DAO compatibility
     */
    @Column(name = "query", nullable = true)
    private String query;

    /**
     * A list of PIs for all accessible records matching the query. If null, {@link #getRecords()} queries the solr and sets records to a list
     * (non-null) containing the matching results. Reset to null when loaded for editing and when the query is changed
     */
    @Transient
    private List<String> records = null;

    public CMSMultiRecordNote() {
    }

    /**
     * @param query
     */
    public CMSMultiRecordNote(String query) {
        super();
        this.query = query;
    }

    /**
     * @param source
     */
    public CMSMultiRecordNote(CMSRecordNote source) {
        super(source);
        if (source instanceof CMSMultiRecordNote note) {
            this.query = note.query;
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
        if (!StringUtils.equals(query, this.query)) {
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
        if (StringUtils.isNotBlank(filter)) {
            return getNoteTitle().getValues()
                    .stream()
                    .map(ValuePair::getValue)
                    .anyMatch(title -> title.toLowerCase().contains(filter.toLowerCase()));
        }

        return true;
    }

    public String toString() {
        return getQuery();
    }

    /**
     * Check if the given pi is a match for the query of the record note The pi is a match if the record note query combined with a query for the
     * given pi returns at least one result
     *
     * @param pi
     * @return true if pi matches query; false otherwise
     */
    public boolean matchesRecord(String pi) {

        //Can be called with empty pi, possibly by bots. In this case always return false
        if (StringUtils.isBlank(pi) || "-".equals(pi)) {
            return false;
        }

        String solrQuery = getQueryForSearch();
        String singleRecordQuery = "+({1}) +{2}".replace("{1}", solrQuery).replace("{2}", "PI:" + pi);

        try {
            return DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount(singleRecordQuery) > 0;
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error("Failed to test match for record note '{}': {}", this, e.toString());
            return false;
        }
    }

    @Override
    public CMSRecordNote copy() {
        return new CMSMultiRecordNote(this);
    }

}
