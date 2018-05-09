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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.search.SearchHit;

/**
 * SitelinkBean
 */
@Named
@SessionScoped
public class SitelinkBean implements Serializable {

    private static final long serialVersionUID = -3131868167344465016L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SitelinkBean.class);

    private String value;
    private List<SearchHit> hits;

    /**
     * 
     * @return List of facet values for the configured field and query
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<String> getAvailableValues() throws PresentationException, IndexUnreachableException {
        return getAvailableValuesForField(DataManager.getInstance().getConfiguration().getSitelinksField(),
                DataManager.getInstance().getConfiguration().getSitelinksFilterQuery());
    }

    /**
     * 
     * @param field
     * @param filterQuery
     * @return List of facet values for the given field and query
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return all existing values for the given field
     */
    public List<String> getAvailableValuesForField(String field, String filterQuery) throws PresentationException, IndexUnreachableException {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (filterQuery == null) {
            throw new IllegalArgumentException("filterQuery may not be null");
        }

        filterQuery = SearchHelper.buildFinalQuery(filterQuery, false);
        QueryResponse qr =
                DataManager.getInstance().getSearchIndex().searchFacetsAndStatistics(filterQuery, Collections.singletonList(field), 1, false);
        if (qr != null) {
            FacetField facet = qr.getFacetField(field);
            if (facet != null) {
                List<String> ret = new ArrayList<>(facet.getValueCount());
                for (Count count : facet.getValues()) {
                    // Skip values starting with u0001
                    if (count.getName().charAt(0) != 0x01) {
                        ret.add(count.getName());
                    }
                }
                return ret;
            }
        }

        return Collections.emptyList();
    }

    /**
     * 
     * @param filterQuery
     * @return Target page
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public String searchAction() throws IndexUnreachableException, PresentationException, DAOException {
        String field = DataManager.getInstance().getConfiguration().getSitelinksField();
        String filterQuery = DataManager.getInstance().getConfiguration().getSitelinksFilterQuery();

        if (value == null) {
            hits = null;
            return "";
        }

        String query = SearchHelper.buildFinalQuery(field + ":" + value + (filterQuery != null ? " AND " + filterQuery : ""), true);
        logger.trace("q: {}", query);
        hits = SearchHelper.searchWithAggregation(query, 0, SolrSearchIndex.MAX_HITS, null, null, null, null, null, null, null);
        return "";
    }
    
    public String resetAction() {
        value = null;
        hits = null;
        
        return "";
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the hits
     */
    public List<SearchHit> getHits() {
        return hits;
    }
}
