package io.goobi.viewer.model.maps;

import java.util.ArrayList;
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
    
    @Column(name = "solr_query")
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
    
    @Transient
    private final GeoCoordinateConverter converter;

    public SolrFeatureSet() {
        this(new GeoCoordinateConverter());
    }

    public SolrFeatureSet(GeoCoordinateConverter converter) {
        super();
        this.converter = converter;
    }
    
    public SolrFeatureSet(SolrFeatureSet blueprint) {
        super(blueprint);
        this.solrQuery = blueprint.solrQuery;
        this.markerTitleField = blueprint.markerTitleField;
        this.aggregateResults = blueprint.aggregateResults;
        this.converter = blueprint.converter;
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
        List<String> coordinateFields = DataManager.getInstance().getConfiguration().getGeoMapMarkerFields();
        Collection<GeoMapFeature> featuresFromSolr = converter.getFeaturesFromSolrQuery(getSolrQuery(isAggregateResults()), Collections.emptyList(), coordinateFields, getMarkerTitleField(), isAggregateResults());
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
        if(aggregateResults) {
            return String.format("{!join from=PI_TOPSTRUCT to=PI} %s", this.solrQuery);
        } else {            
            return solrQuery;
        }
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
            return getFeaturesAsString().length() > 2;  //empty features returns '[]', so check if result is longer than that
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
