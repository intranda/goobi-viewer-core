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
package io.goobi.viewer.model.maps;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchHelper;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("search")
public class SearchResultFeatureSet extends SolrFeatureSet {

    private static final long serialVersionUID = -1075760804768996150L;

    public SearchResultFeatureSet() {
        super();
    }

    public SearchResultFeatureSet(SearchResultFeatureSet searchResultFeatureSet) {
        super(searchResultFeatureSet);
    }

    @Override
    public String getFeaturesAsString() throws PresentationException {
        return getFeaturesAsString(getSearchBean());
    }

    SearchBean getSearchBean() {
        try {
            return BeanUtils.getSearchBean();
        } catch (DefinitionException e) {
            //outside a session.
            return null;
        }
    }

    @Override
    public String getFeaturesAsJsonString() throws PresentationException {
        return getFeaturesAsJsonString(getSearchBean());
    }

    public String getFeaturesAsString(SearchBean searchBean) throws PresentationException {
        try {
            this.featuresAsString = createFeaturesAsStringFromSearch(false, searchBean);
        } catch (IndexUnreachableException | DAOException | ViewerConfigurationException e) {
            throw new PresentationException("Error loading features", e);
        }
        return this.featuresAsString;
    }

    public String getFeaturesAsJsonString(SearchBean searchBean) throws PresentationException {
        try {
            this.featuresAsString = createFeaturesAsStringFromSearch(true, searchBean);
        } catch (IndexUnreachableException | DAOException | ViewerConfigurationException e) {
            throw new PresentationException("Error loading features", e);
        }
        return this.featuresAsString;
    }

    private String createFeaturesAsStringFromSearch(boolean escapeJson, SearchBean searchBean)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {

        if (searchBean != null && searchBean.getCurrentSearch() != null) {

            List<String> facetFilterQueries = searchBean.getFacets().generateFacetFilterQueries(true);
            String finalQuery = createFinalQuery(searchBean);

            Collection<GeoMapFeature> features = super.createFeatures(finalQuery, facetFilterQueries);
            String ret = toJsonString(escapeJson, features);

            return "[" + ret + "]";
        } else {
            return "[]";
        }
    }

    String createFinalQuery(SearchBean searchBean) {
        String query = searchBean.getCurrentSearch().getQuery();
        String combinedQuery = createCombinedQuery(query, getSolrQuery());
        String currentQuery = SearchHelper.prepareQuery(combinedQuery);
        SearchAggregationType aggregationType =
                this.isAggregateResults() ? SearchAggregationType.AGGREGATE_TO_TOPSTRUCT : SearchAggregationType.NO_AGGREGATION;
        String finalQuery =
                SearchHelper.buildFinalQuery(currentQuery, true, aggregationType);
        return finalQuery;
    }

    String createCombinedQuery(String query1, String query2) {
        if (StringUtils.isNoneBlank(query1, query2)) {
            return "+(%s) +(%s)".formatted(query1, query2);
        } else if (StringUtils.isNotBlank(query1)) {
            return query1;
        } else if (StringUtils.isNotBlank(query2)) {
            return query2;
        } else {
            return "";
        }
    }

    @Override
    public String getType() {
        return "SEARCH_RESULTS";
    }

    @Override
    public FeatureSet copy() {
        return new SearchResultFeatureSet(this);
    }

}
