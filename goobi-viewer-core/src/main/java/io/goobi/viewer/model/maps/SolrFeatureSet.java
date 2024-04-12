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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.GeoCoordinateConverter;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
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
    private String featuresAsString = null;

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
                this.featuresAsString = createFeaturesAsString();
            } catch (IndexUnreachableException e) {
                throw new PresentationException("Error loading features", e);
            }
        }
        return this.featuresAsString;
    }

    public void setFeaturesAsString(String featuresAsString) {
        this.featuresAsString = null;
    }

    private String createFeaturesAsString() throws PresentationException, IndexUnreachableException {
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
                .collect(Collectors.joining(","));

        return "[" + ret + "]";

    }

    public String getSolrQuery() {
        return this.solrQuery;
    }

    public String getSolrQuery(boolean aggregateResults) {
        if (aggregateResults) {
            return String.format("{!join from=PI_TOPSTRUCT to=PI} %s", this.solrQuery);
        }

        return solrQuery;
    }

    public String getSolrQueryEncoded() {
        return StringTools.encodeUrl(getSolrQuery());
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
}
