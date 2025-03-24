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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ocpsoft.pretty.PrettyContext;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.GeoCoordinateConverter;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

@Entity
@DiscriminatorValue("solr")
public class SolrFeatureSet extends FeatureSet {

    private static final long serialVersionUID = -9054215108168526688L;
    private static final Logger logger = LogManager.getLogger(SolrFeatureSet.class);

    @Column(name = "solr_query", columnDefinition = "TEXT")
    private String solrQuery = null;

    @Column(name = "aggregate_results")
    private boolean aggregateResults = false;

    /**
     * SOLR-Field to create the marker title from if the features are generated from a SOLR query
     */
    @Column(name = "marker_title_field")
    private String markerTitleField = "MD_VALUE";

    @Transient
    protected String featuresAsString = null;

    public SolrFeatureSet() {
        super();
    }

    public SolrFeatureSet(SolrFeatureSet blueprint) {
        super(blueprint);
        this.solrQuery = blueprint.solrQuery;
        this.markerTitleField = blueprint.markerTitleField;
        this.aggregateResults = blueprint.aggregateResults;
    }

    @Override
    public FeatureSet copy() {
        return new SolrFeatureSet(this);
    }

    @Override
    public String getFeaturesAsString() throws PresentationException {
        if (this.featuresAsString == null) {
            try {
                this.featuresAsString = createFeaturesAsString(false);
            } catch (IndexUnreachableException e) {
                throw new PresentationException("Error loading features", e);
            }
        }
        return this.featuresAsString;
    }

    @Override
    public String getFeaturesAsJsonString() throws PresentationException {
        try {
            return createFeaturesAsString(true);
        } catch (PresentationException | IndexUnreachableException e) {
            throw new PresentationException("Error loading features", e);
        }

    }

    public void setFeaturesAsString(String featuresAsString) {
        this.featuresAsString = null;
    }

    protected String createFeaturesAsString(boolean escapeJson) throws PresentationException, IndexUnreachableException {
        if (DataManager.getInstance().getConfiguration().useHeatmapForCMSMaps()) {
            //No features required since they will be loaded dynamically with the heatmap
            return "[]";
        }
        GeoCoordinateConverter converter = new GeoCoordinateConverter(this.markerTitleField);
        List<String> coordinateFields = DataManager.getInstance().getConfiguration().getGeoMapMarkerFields();
        Collection<GeoMapFeature> featuresFromSolr = converter.getFeaturesFromSolrQuery(getSolrQuery(isAggregateResults()), Collections.emptyList(),
                coordinateFields, getMarkerTitleField(), isAggregateResults());
        String ret = featuresFromSolr.stream()
                .distinct()
                .map(GeoMapFeature::getJsonObject)
                .map(Object::toString)
                .map(string -> escapeJson ? StringEscapeUtils.escapeJson(string) : string)
                .collect(Collectors.joining(","));

        return "[" + ret + "]";
    }

    public String getSolrQuery() {
        return this.solrQuery;
    }

    public String getSolrQuery(boolean aggregateResults) {
        if (false) {
            return String.format("{!join from=PI_TOPSTRUCT to=PI} %s", this.solrQuery);
        }

        return solrQuery;
    }

    public String getSolrQueryForSearchEncoded() {
        return StringTools.encodeUrl(getSolrQueryForSearch().replace("+", "%2B"));
    }

    public String getSolrQueryForSearch() {
        return this.getSolrQueryForSearch(isAggregateResults());
    }

    public String getSolrQueryForSearch(boolean aggregateResults) {
        if (aggregateResults) {
            return String.format("{!join from=PI_TOPSTRUCT to=PI} +(%s) +WKT_COORDS:\"Intersects(POINT({lng} {lat})) distErrPct=0\"", this.solrQuery);
        } else {
            return String.format("+(%s) +WKT_COORDS:\"Intersects(POINT({lng} {lat})) distErrPct=0\"", this.solrQuery);

        }
    }

    public String getSolrQueryEncoded() {
        return StringTools.encodeUrl(Optional.ofNullable(getSolrQuery()).orElse("")).replace("%2B", "+");
    }

    /**
     * <p>
     * getCoordinateSearchQueryTemplate.
     * </p>
     *
     * @param featureSet a {@link io.goobi.viewer.model.maps.SolrFeatureSet} object
     * @return String
     * @throws URISyntaxException
     */
    public String getCoordinateSearchQueryTemplate() throws URISyntaxException {
        URI mappedUrl = new URIBuilder(PrettyContext.getCurrentInstance()
                .getConfig()
                .getMappingById("newSearch5")
                .getPatternParser()
                .getMappedURL("-", getSolrQueryEncoded(), "1", "-", "-")
                .toURL()).build();
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + mappedUrl.toString();
    }

    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
        this.featuresAsString = null;
    }

    public boolean hasSolrQuery() {
        return StringUtils.isNotBlank(this.solrQuery);
    }

    public String getMarkerTitleField() {
        return markerTitleField;
    }

    public void setMarkerTitleField(String markerTitleField) {
        this.markerTitleField = markerTitleField;
    }

    @Override
    public void updateFeatures() {
        this.featuresAsString = null;
    }

    @Override
    public boolean hasFeatures() {
        try {
            return getFeaturesAsString().length() > 2; //empty features returns '[]', so check if result is longer than that
        } catch (PresentationException e) {
            logger.error("Error retrieving geoma features from solr", e);
            return false;
        }
    }

    @Override
    public boolean isQueryResultSet() {
        return true;
    }

    public boolean isAggregateResults() {
        return aggregateResults;
    }

    public void setAggregateResults(boolean aggregateResults) {
        this.aggregateResults = aggregateResults;
        this.featuresAsString = null;
    }

    @Override
    public String getType() {
        return "SOLR_QUERY";
    }
}
