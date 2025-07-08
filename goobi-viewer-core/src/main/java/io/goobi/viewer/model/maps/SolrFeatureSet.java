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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ocpsoft.pretty.PrettyContext;

import de.intranda.monitoring.timer.Time;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.maps.features.AbstractFeatureDataProvider;
import io.goobi.viewer.model.maps.features.FeatureGenerator;
import io.goobi.viewer.model.maps.features.IFeatureDataProvider;
import io.goobi.viewer.model.maps.features.LabelCreator;
import io.goobi.viewer.model.maps.features.MetadataDocument;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Transient;

@Entity
@DiscriminatorValue("solr")
public class SolrFeatureSet extends FeatureSet {

    private static final int MAX_RECORD_HITS = 50_000;
    private static final long serialVersionUID = -9054215108168526688L;
    private static final Logger logger = LogManager.getLogger(SolrFeatureSet.class);

    @Column(name = "solr_query", columnDefinition = "TEXT")
    private String solrQuery = null;

    @Column(name = "search_scope")
    @Enumerated(EnumType.STRING)
    private SolrSearchScope searchScope = SolrSearchScope.ALL;

    /**
     * SOLR-Field to create the marker title from if the features are generated from a SOLR query
     */
    @Deprecated
    @Column(name = "marker_title_field")
    private String markerTitleField = "MD_VALUE";

    /**
     * type of a metadata list configuration to use to construct the marker labels
     */
    @Column(name = "metadata_list_marker")
    private String markerMetadataList = "";

    @Column(name = "item_filter_field")
    private String itemFilterName = "";
    /**
     * type of a metadata list configuration to use to construct labels for items for a marker displayed as a list next to the map
     */
    @Column(name = "metadata_list_item")
    private String itemMetadataList = "";

    @Transient
    protected String featuresAsString = null;

    @Transient
    private final boolean useHeatmap;

    @Transient
    private GeomapItemFilter itemFilter;

    public SolrFeatureSet() {
        this(DataManager.getInstance().getConfiguration().useHeatmapForCMSMaps());
    }

    public SolrFeatureSet(boolean useHeatmap) {
        super();
        this.useHeatmap = useHeatmap;
    }

    public SolrFeatureSet(SolrFeatureSet blueprint) {
        super(blueprint);
        this.solrQuery = blueprint.solrQuery;
        this.markerTitleField = blueprint.markerTitleField;
        this.markerMetadataList = blueprint.markerMetadataList;
        this.itemMetadataList = blueprint.itemMetadataList;
        this.searchScope = blueprint.searchScope;
        this.useHeatmap = blueprint.useHeatmap;
        this.itemFilterName = blueprint.itemFilterName;
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
        if (this.useHeatmap) {
            //No features required since they will be loaded dynamically with the heatmap
            return "[]";
        }
        Collection<GeoMapFeature> featuresFromSolr = createFeatures();
        String ret = featuresFromSolr.stream()
                .distinct()
                .map(GeoMapFeature::getJsonObject)
                .map(Object::toString)
                .map(string -> escapeJson ? StringEscapeUtils.escapeJson(string) : string)
                .collect(Collectors.joining(","));

        return "[" + ret + "]";
    }

    protected Collection<GeoMapFeature> createFeatures() throws PresentationException, IndexUnreachableException {

        LabelCreator markerLabels =
                new LabelCreator(DataManager.getInstance().getConfiguration().getMetadataTemplates(getMarkerMetadataList()));
        LabelCreator itemLabels = new LabelCreator(DataManager.getInstance().getConfiguration().getMetadataTemplates(getItemMetadataList()));
        List<String> coordinateFields = DataManager.getInstance().getConfiguration().getGeoMapMarkerFields();

        List<MetadataDocument> hits;
        try (Time t = DataManager.getInstance().getTiming().takeTime("query")) {
            IFeatureDataProvider queryGenerator =
                    AbstractFeatureDataProvider.getDataProvider(getSearchScope() == null ? SolrSearchScope.ALL : getSearchScope(),
                            ListUtils.union(coordinateFields, ListUtils.union(markerLabels.getFieldsToQuery(), itemLabels.getFieldsToQuery())));
            hits = queryGenerator.getResults(getSolrQuery(), MAX_RECORD_HITS);
        }
        try (Time t = DataManager.getInstance().getTiming().takeTime("features")) {
            FeatureGenerator featureGenerator = new FeatureGenerator(coordinateFields, getItemFilter().getFields(), markerLabels, itemLabels);

            Collection<GeoMapFeature> featuresFromSolr = new ArrayList<>();
            for (MetadataDocument hit : hits) {
                Collection<GeoMapFeature> features = featureGenerator.getFeatures(hit, this.getSearchScope());
                featuresFromSolr.addAll(features);
            }

            Collection<GeoMapFeature> combinedFeatures = combineFeatures(featuresFromSolr);

            return combinedFeatures;
        }

    }

    private Collection<GeoMapFeature> combineFeatures(Collection<GeoMapFeature> singleFeatures) {
        Map<GeoMapFeature, List<GeoMapFeature>> featureMap = singleFeatures.stream().collect(Collectors.groupingBy(Function.identity()));
        Collection<GeoMapFeature> features = new ArrayList<>();
        for (Entry<GeoMapFeature, List<GeoMapFeature>> entry : featureMap.entrySet()) {
            GeoMapFeature feature = entry.getKey();
            Collection<GeoMapFeatureItem> items = entry.getValue().stream().flatMap(f -> f.getItems().stream()).toList();
            feature.setItems(items);
            feature.setCount(items.size());
            if (feature.getItems().size() == 1) {
                feature.getItems().stream().findAny().map(item -> item.getLink()).ifPresent(link -> feature.setLink(link));
            }
            features.add(feature);
        }
        return features;
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

    public String getSolrQueryForSearchEncoded() {
        return StringTools.encodeUrl(getSolrQueryForSearch().replace("+", "%2B"));
    }

    public String getSolrQueryForSearch() {
        return this.getSolrQueryForSearch(isAggregateResults());
    }

    public boolean isAggregateResults() {
        return getSearchScope() != SolrSearchScope.RECORDS;
    }

    public String getSolrQueryForSearch(boolean aggregateResults) {
        if (aggregateResults) {
            return String.format("{!join from=PI_TOPSTRUCT to=PI} +(%s) +WKT_COORDS:\"Intersects(POINT({lng} {lat})) distErrPct=0\"", this.solrQuery);
        } else {
            return String.format("+(%s) +WKT_COORDS:\"Intersects(POINT({lng} {lat})) distErrPct=0\"", this.solrQuery);

        }
    }

    public String getSolrQueryEncoded() {

        String baseQuery = Optional.ofNullable(getSolrQuery()).orElse("");
        String scopeQuery = Optional.ofNullable(this.getSearchScope()).map(SolrSearchScope::getQuery).orElse("");
        String query;
        if (StringUtils.isNoneBlank(baseQuery, scopeQuery)) {
            query = "+(%s) +(%s)".formatted(baseQuery, scopeQuery);
        } else {
            query = baseQuery + scopeQuery;
        }

        return StringTools.encodeUrl(query, true);
    }

    /**
     * <p>
     * getCoordinateSearchQueryTemplate.
     * </p>
     *
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

    @Deprecated
    public String getMarkerTitleField() {
        return markerTitleField;
    }

    @Deprecated
    public void setMarkerTitleField(String markerTitleField) {
        this.markerTitleField = markerTitleField;
    }

    @Override
    public void updateFeatures() {
        this.featuresAsString = null;
        this.itemFilter = null;
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

    public String getMarkerMetadataList() {
        if (StringUtils.isAllBlank(markerMetadataList) && StringUtils.isNotBlank(markerTitleField)) {
            this.markerMetadataList = DataManager.getInstance().getConfiguration().getMetadataListForGeomapMarkerConfig(markerTitleField);
        }
        return markerMetadataList;
    }

    public void setMarkerMetadataList(String markerMetadataList) {
        this.markerMetadataList = markerMetadataList;
    }

    public String getItemMetadataList() {
        if (StringUtils.isAllBlank(itemMetadataList) && StringUtils.isNotBlank(markerTitleField)) {
            this.itemMetadataList = DataManager.getInstance().getConfiguration().getMetadataListForGeomapItemConfig(markerTitleField);
        }
        return itemMetadataList;
    }

    public void setItemMetadataList(String itemMetadataList) {
        this.itemMetadataList = itemMetadataList;
    }

    @Override
    public boolean isQueryResultSet() {
        return true;
    }

    public SolrSearchScope getSearchScope() {
        return searchScope;
    }

    public void setSearchScope(SolrSearchScope searchScope) {
        this.searchScope = searchScope;
        this.featuresAsString = null;
    }

    @Override
    public String getType() {
        return "SOLR_QUERY";
    }

    public String getItemFilterName() {
        return itemFilterName;
    }

    public void setItemFilterName(String itemFilterName) {
        this.itemFilterName = itemFilterName;
    }

    public GeomapItemFilter getItemFilter() {
        if (itemFilter == null) {
            this.itemFilter = loadFilter(itemFilterName);
        }
        return itemFilter;
    }

    private GeomapItemFilter loadFilter(String name) {
        if (StringUtils.isNotBlank(name)) {
            return DataManager.getInstance()
                    .getConfiguration()
                    .getGeomapFilters()
                    .stream()
                    .filter(f -> name.equals(f.getName()))
                    .findAny()
                    .orElse(new GeomapItemFilter("", "", false, Collections.emptyList()));
        } else {
            return new GeomapItemFilter("", "", false, Collections.emptyList());
        }
    }

}
