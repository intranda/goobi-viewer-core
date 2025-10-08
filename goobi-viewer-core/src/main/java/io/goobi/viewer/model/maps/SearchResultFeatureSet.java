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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.GeoCoordinateConverter;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchHit;
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
        return getFeaturesAsString(BeanUtils.getSearchBean());
    }

    @Override
    public String getFeaturesAsJsonString() throws PresentationException {
        return getFeaturesAsJsonString(BeanUtils.getSearchBean());
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

            GeoCoordinateConverter converter = new GeoCoordinateConverter(this.getMarkerTitleField());
            List<String> coordinateFields = DataManager.getInstance().getConfiguration().getGeoMapMarkerFields();

            Search search = new Search(searchBean.getCurrentSearch());
            search.setCustomFilterQuery(getSolrQuery());
            SearchAggregationType aggregationType =
                    this.isAggregateResults() ? SearchAggregationType.NO_AGGREGATION : SearchAggregationType.AGGREGATE_TO_TOPSTRUCT;
            search.execute(searchBean.getFacets(), null, Integer.MAX_VALUE, BeanUtils.getLocale(), true, aggregationType);
            Map<SolrDocument, List<SolrDocument>> solrDocMap = search.getHits()
                    .stream()
                    .map(SearchHit::getSolrDoc)
                    .collect(Collectors.toMap(Function.identity(), List::of));

            List<GeoMapFeature> features = converter.getFeaturesFromSolrDocs(coordinateFields, getMarkerTitleField(), solrDocMap);

            String ret = features.stream()
                    .distinct()
                    .map(GeoMapFeature::getJsonObject)
                    .map(Object::toString)
                    .map(string -> escapeJson ? StringEscapeUtils.escapeJson(string) : string)
                    .collect(Collectors.joining(","));

            return "[" + ret + "]";
        } else {
            return "[]";
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
