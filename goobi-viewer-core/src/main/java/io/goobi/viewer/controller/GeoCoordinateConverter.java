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
package io.goobi.viewer.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.model.StringMatchConfiguration;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.maps.GeoMapFeature;
import io.goobi.viewer.model.maps.IArea;
import io.goobi.viewer.model.maps.Location;
import io.goobi.viewer.model.maps.Point;
import io.goobi.viewer.model.maps.Polygon;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataBuilder;
import io.goobi.viewer.model.metadata.MetadataContainer;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.servlets.IdentifierResolver;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Utility methods for converting geo-coordinated between different formats
 * 
 * @author florian
 *
 */
public class GeoCoordinateConverter {

    private static final Logger logger = LogManager.getLogger(GeoCoordinateConverter.class);
    protected static final String POINT_LAT_LNG_PATTERN = "([\\dE.-]+)[\\s/]*([\\dE.-]+)";
    protected static final String POLYGON_LAT_LNG_PATTERN = "POLYGON\\(\\(([\\dE.-]+[\\s/]*[\\dE.-]+[,\\s]*)+\\)\\)"; //NOSONAR

    //    private final Configuration config;
    private final Map<String, Metadata> featureTitleConfigs;
    private final Map<String, Metadata> entityTitleConfigs;
    private final HttpServletRequest servletRequest;

    public GeoCoordinateConverter() {
        this("");
    }

    public GeoCoordinateConverter(String markerTitleConfig) {
        this.featureTitleConfigs = DataManager.getInstance().getConfiguration().getGeomapFeatureConfigurations(markerTitleConfig);
        this.entityTitleConfigs = DataManager.getInstance().getConfiguration().getGeomapEntityConfigurations(markerTitleConfig);
        this.servletRequest = null;
    }

    public GeoCoordinateConverter(Map<String, Metadata> featureTitleConfigs, Map<String, Metadata> entityTitleConfigs) {
        super();
        this.featureTitleConfigs = featureTitleConfigs;
        this.entityTitleConfigs = entityTitleConfigs;
        this.servletRequest = null;
    }

    public GeoCoordinateConverter(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
        this.featureTitleConfigs = DataManager.getInstance().getConfiguration().getGeomapFeatureConfigurations("");
        this.entityTitleConfigs = DataManager.getInstance().getConfiguration().getGeomapEntityConfigurations("");
    }

    /**
     * Collect all point coordinate in the given coordinate fields from solr documents returned by the given solr query
     * 
     * @param query Solr query to get documents
     * @param filterQueries filter for solr query
     * @param coordinateFields fields containing the coordinate points to collect
     * @param markerTitleField solr field containing a title for the coordinates
     * @param aggregateResults
     * @return a list of {@link GeoMapFeature}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<GeoMapFeature> getFeaturesFromSolrQuery(String query, List<String> filterQueries, List<String> coordinateFields,
            String markerTitleField, boolean aggregateResults)
            throws PresentationException, IndexUnreachableException {
        Map<SolrDocument, List<SolrDocument>> docs = StringUtils.isNotBlank(query)
                ? getSolrDocuments(query, filterQueries, coordinateFields, markerTitleField, aggregateResults) : Collections.emptyMap();

        List<GeoMapFeature> features = new ArrayList<>();
        for (Entry<SolrDocument, List<SolrDocument>> entry : docs.entrySet()) {
            SolrDocument doc = entry.getKey();
            List<SolrDocument> children = entry.getValue();
            for (String field : coordinateFields) {
                features.addAll(getGeojsonPoints(doc, field, markerTitleField));
                Map<String, List<SolrDocument>> metadataDocs =
                        children.stream().collect(Collectors.toMap(SolrTools::getReferenceId, List::of, ListUtils::union));
                for (List<SolrDocument> childDocs : metadataDocs.values()) {
                    Collection<GeoMapFeature> tempFeatures = getGeojsonPoints(doc, childDocs, field, markerTitleField);
                    features.addAll(tempFeatures);
                }
            }
        }

        Map<GeoMapFeature, List<GeoMapFeature>> featureMap = features
                .stream()
                .collect(Collectors.groupingBy(Function.identity()));

        features = featureMap.entrySet()
                .stream()
                .map(e -> {
                    GeoMapFeature f = e.getKey();
                    f.setCount(e.getValue().size());
                    f.setEntities(e.getValue().stream().flatMap(f1 -> f1.getEntities().stream()).collect(Collectors.toList()));
                    return f;
                })
                .collect(Collectors.toList());

        return features;
    }

    private Map<SolrDocument, List<SolrDocument>> getSolrDocuments(final String query, List<String> filterQueries, List<String> coordinateFields,
            String markerTitleField, boolean aggregateResults)
            throws PresentationException, IndexUnreachableException {

        String coordinateFieldsQuery = coordinateFields.stream().map(s -> s + ":*").collect(Collectors.joining(" "));
        String filterQuery = SearchHelper.getAllSuffixes(BeanUtils.getRequest(), true, true);
        String useQuery = query;
        if (!useQuery.startsWith("{!join")) {
            useQuery = String.format("+(%s)", useQuery);
        }
        String finalQuery = String.format("%s +(%s) +(%s *:*)", useQuery, coordinateFieldsQuery, filterQuery);
        Map<String, String> params = new HashMap<>();

        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(finalQuery, 0, 10_000, null, null, getSolrFieldsForMainQuery(coordinateFields, markerTitleField, aggregateResults),
                        filterQueries, params);
        SolrDocumentList docs = response.getResults();

        if (aggregateResults) {
            String expandQuery = useQuery.replaceAll("\\{\\!join[^}]+}", "");
            QueryResponse expandResponse = DataManager.getInstance()
                    .getSearchIndex()
                    .search(expandQuery, 0, 10_000, null, null, getSolrFieldsForExpandQuery(coordinateFields, markerTitleField), filterQueries,
                            params);
            SolrDocumentList expandDocs = expandResponse.getResults();

            Map<String, List<SolrDocument>> expandedResults = expandDocs.stream()
                    .collect(Collectors.toMap(doc -> SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI_TOPSTRUCT), List::of,
                            ListUtils::union));

            return docs.stream()
                    .collect(Collectors.toMap(doc -> doc,
                            doc -> expandedResults.getOrDefault(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT), new SolrDocumentList())));
        }
        return docs.stream().collect(Collectors.toMap(doc -> doc, doc -> new SolrDocumentList()));

    }

    public List<String> getSolrFieldsForMainQuery(List<String> coordinateFields, String markerTitleField, boolean aggregateResults) {
        //only get coordinate fields for main document if results are not aggregated. Otherwise coordinates come from expanded results
        Set<String> fieldList = aggregateResults ? new HashSet<>() : new HashSet<>(coordinateFields);
        fieldList.addAll(getLanguageFields(markerTitleField));
        fieldList.add(SolrConstants.DOCTYPE);
        fieldList.add(SolrConstants.DOCSTRCT);
        fieldList.add(SolrConstants.PI);
        fieldList.add(SolrConstants.PI_TOPSTRUCT);
        fieldList.add(SolrConstants.THUMBPAGENO);
        fieldList.add(SolrConstants.LOGID);
        fieldList.add(SolrConstants.MD_VALUE);
        fieldList.add(SolrConstants.MIMETYPE);
        fieldList.add(SolrConstants.ISWORK);
        fieldList.add(SolrConstants.THUMBNAIL);
        fieldList.addAll(getSolrFields(this.entityTitleConfigs.values()));
        fieldList.addAll(getSolrFields(this.featureTitleConfigs.values()));
        return new ArrayList<>(fieldList);
    }

    public List<String> getSolrFieldsForExpandQuery(List<String> coordinateFields, String markerTitleField) {
        Set<String> fieldList = new HashSet<>(coordinateFields);
        fieldList.add(SolrConstants.DOCTYPE);
        fieldList.add(SolrConstants.DOCSTRCT);
        fieldList.add(SolrConstants.PI_TOPSTRUCT);
        fieldList.add(SolrConstants.THUMBPAGENO);
        fieldList.add(SolrConstants.LOGID);
        fieldList.add(SolrConstants.MD_VALUE);
        fieldList.add(SolrConstants.MIMETYPE);
        fieldList.add(SolrConstants.ISWORK);
        fieldList.add(SolrConstants.THUMBNAIL);
        fieldList.add("MD_REFID");
        fieldList.addAll(getSolrFields(this.entityTitleConfigs.values()));
        fieldList.addAll(getSolrFields(this.featureTitleConfigs.values()));
        return new ArrayList<>(fieldList);
    }

    private List<String> getLanguageFields(String fieldName) {
        return List.of(fieldName, fieldName + "_LANG_*");
    }

    private List<String> getSolrFields(Collection<Metadata> metadataConfigs) {
        List<String> fields = new ArrayList<>();
        for (Metadata metadata : metadataConfigs) {
            fields.addAll(metadata.getParams()
                    .stream()
                    .map(param -> param.getKey())
                    .filter(StringUtils::isNotBlank)
                    .map(this::getLanguageFields)
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
            fields.addAll(metadata.getParams()
                    .stream()
                    .map(param -> param.getAltKey())
                    .filter(StringUtils::isNotBlank)
                    .map(this::getLanguageFields)
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
        }
        return fields;
    }

    /**
     * Collect all point coordinate in the given metadata field within the given solr document
     * 
     * @param doc the document containing the coordinates
     * @param children
     * @param metadataField The name of the solr field to search in
     * @param titleField solr field containing a title for the coordinates
     * @return Collection<GeoMapFeature>
     */
    public Collection<GeoMapFeature> getGeojsonPoints(SolrDocument doc, List<SolrDocument> children, String metadataField, String titleField) {
        String title = StringUtils.isBlank(titleField) ? null : SolrTools.getSingleFieldStringValue(doc, titleField);
        List<String> points = new ArrayList<>();
        points.addAll(children.stream()
                .map(c -> SolrTools.getMetadataValues(c, metadataField))
                .flatMap(List::stream)
                .filter(StringUtils::isNotBlank)
                .limit(1)
                .collect(Collectors.toList()));
        List<GeoMapFeature> docFeatures = getFeatures(points);
        addEntityToFeatures(doc, children, docFeatures);
        docFeatures.forEach(feature -> setLink(feature, doc));
        Metadata titleConfig = this.featureTitleConfigs.getOrDefault(children.stream()
                .findAny()
                .map(child -> SolrTools.getSingleFieldStringValue(child, SolrConstants.LABEL))
                .map(SolrTools::getBaseFieldName)
                .orElse(StringConstants.DEFAULT_NAME), this.featureTitleConfigs.get(StringConstants.DEFAULT_NAME));
        Metadata entityLabelConfig = this.entityTitleConfigs.getOrDefault(children.stream()
                .findAny()
                .map(child -> SolrTools.getSingleFieldStringValue(child, SolrConstants.LABEL))
                .map(SolrTools::getBaseFieldName)
                .orElse(StringConstants.DEFAULT_NAME), this.entityTitleConfigs.get(StringConstants.DEFAULT_NAME));
        setLabels(docFeatures, titleConfig, title, entityLabelConfig);

        return docFeatures;
    }

    public Collection<GeoMapFeature> getGeojsonPoints(SolrDocument doc, String metadataField, String titleField) {
        String title = StringUtils.isBlank(titleField) ? null : SolrTools.getSingleFieldStringValue(doc, titleField);
        List<String> points = new ArrayList<>();
        points.addAll(SolrTools.getMetadataValues(doc, metadataField));
        List<GeoMapFeature> docFeatures = getFeatures(points);
        addEntityToFeatures(doc, Collections.emptyList(), docFeatures);
        docFeatures.forEach(feature -> setLink(feature, doc));
        docFeatures.forEach(f -> f.setDocumentId((String) doc.getFieldValue(SolrConstants.LOGID)));
        Metadata titleConfig = this.featureTitleConfigs.getOrDefault(
                Optional.ofNullable(doc).map(mc -> mc.getFirstValue(SolrConstants.DOCSTRCT)).orElse(StringConstants.DEFAULT_NAME),
                this.featureTitleConfigs.get(StringConstants.DEFAULT_NAME));
        Metadata entityLabelConfig = this.entityTitleConfigs.getOrDefault(
                Optional.ofNullable(doc).map(mc -> mc.getFirstValue(SolrConstants.DOCSTRCT)).orElse(StringConstants.DEFAULT_NAME),
                this.entityTitleConfigs.get(StringConstants.DEFAULT_NAME));
        setLabels(docFeatures, titleConfig, title, entityLabelConfig);

        return docFeatures;
    }

    private void setLink(GeoMapFeature feature, SolrDocument doc) {
        URI link = UriBuilder.fromUri(DataManager.getInstance().getConfiguration().getViewerBaseUrl()).path(IdentifierResolver.constructUrl(doc, false)).build();
       feature.setLink(link.toString());
    }

    public Collection<GeoMapFeature> getGeojsonPoints(MetadataContainer doc, String metadataField, String titleField) {
        List<String> points = new ArrayList<>();
        points.addAll(doc.get(metadataField).stream().filter(Objects::nonNull).map(md -> md.getValueOrFallback(null)).collect(Collectors.toList()));
        List<GeoMapFeature> docFeatures = getFeatures(points);
        docFeatures.forEach(f -> f.addEntity(doc));
        String title = StringUtils.isBlank(titleField) ? null : doc.getFirstValue(titleField);
        Metadata titleConfig = this.featureTitleConfigs.getOrDefault(
                Optional.ofNullable(doc).map(mc -> mc.getFirstValue(SolrConstants.DOCSTRCT)).orElse(StringConstants.DEFAULT_NAME),
                this.featureTitleConfigs.get(StringConstants.DEFAULT_NAME));
        Metadata entityLabelConfig = this.entityTitleConfigs.getOrDefault(
                Optional.ofNullable(doc).map(mc -> mc.getFirstValue(SolrConstants.DOCSTRCT)).orElse(StringConstants.DEFAULT_NAME),
                this.entityTitleConfigs.get(StringConstants.DEFAULT_NAME));
        setLabels(docFeatures, titleConfig, title, entityLabelConfig);
        return docFeatures;
    }

    private static void setLabels(List<GeoMapFeature> docFeatures, Metadata titleConfiguration, String defaultTitle,
            Metadata entityLabelConfiguration) {

        docFeatures.forEach(f -> {

            IMetadataValue title = Optional.ofNullable(f.getEntities())
                    .filter(l -> !l.isEmpty())
                    .map(l -> l.get(0))
                    .map(MetadataContainer::getMetadata)
                    .map(metadata -> createTitle(titleConfiguration, metadata))
                    .filter(md -> !md.isEmpty())
                    .orElse(new SimpleMetadataValue(defaultTitle));
            f.setTitle(title);

            f.getEntities()
                    .forEach(entity -> Optional.ofNullable(entity.getMetadata())
                            .map(metadata -> createTitle(entityLabelConfiguration, metadata))
                            .filter(md -> !md.isEmpty())
                            .ifPresent(entity::setLabel));

        });
    }

    public static IMetadataValue createTitle(Metadata labelConfig, Map<String, List<IMetadataValue>> metadata) {
        if (labelConfig != null) {
            return new MetadataBuilder(metadata).build(labelConfig);
        }
        return new SimpleMetadataValue("");
    }

    private static List<GeoMapFeature> getFeatures(List<String> points) {
        List<GeoMapFeature> docFeatures = new ArrayList<>();
        for (String point : points) {
            try {
                if (point.matches(POINT_LAT_LNG_PATTERN)) { //NOSONAR  no catastrophic backtracking detected
                    GeoMapFeature feature = new GeoMapFeature();

                    Matcher matcher = Pattern.compile(POINT_LAT_LNG_PATTERN).matcher(point); // NOSONAR  no catastrophic backtracking detected
                    matcher.find();
                    Double lat = Double.valueOf(matcher.group(1));
                    Double lng = Double.valueOf(matcher.group(2));

                    JSONObject json = new JSONObject();
                    json.put("type", "Feature");
                    JSONObject geom = new JSONObject();
                    geom.put("type", "Point");
                    geom.put("coordinates", new double[] { lng, lat });
                    json.put("geometry", geom);
                    feature.setJson(json.toString());
                    docFeatures.add(feature);
                } else if (point.matches(POLYGON_LAT_LNG_PATTERN)) {
                    GeoMapFeature feature = new GeoMapFeature();

                    Matcher matcher = Pattern.compile(POLYGON_LAT_LNG_PATTERN).matcher(point); // NOSONAR  no catastrophic backtracking detected
                    matcher.find();
                    Double lat = Double.valueOf(matcher.group(1));
                    Double lng = Double.valueOf(matcher.group(2));

                    JSONObject json = new JSONObject();
                    json.put("type", "Feature");
                    JSONObject geom = new JSONObject();
                    geom.put("type", "Point");
                    geom.put("coordinates", new double[] { lng, lat });
                    json.put("geometry", geom);
                    feature.setJson(json.toString());
                    docFeatures.add(feature);
                } else {
                    docFeatures.addAll(createFeaturesFromJson(point));
                }
            } catch (JSONException | NumberFormatException e) {
                logger.error("Encountered non-json feature: {}", point);
            }
        }
        return docFeatures;
    }

    private static void addEntityToFeatures(SolrDocument doc, List<SolrDocument> children, List<GeoMapFeature> docFeatures) {

        MetadataContainer entity = MetadataContainer.createMetadataEntity(doc, children,
                getFeatureFieldFilter(children != null && !children.isEmpty()), getEntityFieldFilter());
        docFeatures.forEach(f -> f.addEntity(entity));
    }

    private static Predicate<String> getFeatureFieldFilter(boolean aggregateHits) {
        if (aggregateHits) {
            return new StringMatchConfiguration(".*",
                    "FACET_.*|BOOL_.*|CENTURY|DEFAULT|.*_UNTOKENIZED|WKT_COORDS|NORMDATATERMS|.*_NAME_SEARCH|NORM_NAME|MD_LOCATION|MD_DESCRIPTION.*");
        }
        return new StringMatchConfiguration(".*",
                "FACET_.*|BOOL_.*|CENTURY|DEFAULT|.*_UNTOKENIZED|WKT_COORDS|NORMDATATERMS|.*_NAME_SEARCH");
    }

    private static Predicate<String> getEntityFieldFilter() {
        return new StringMatchConfiguration(".*", "FACET_.*|BOOL_.*|CENTURY|DEFAULT|.*_UNTOKENIZED|WKT_COORDS|NORMDATATERMS|.*_NAME_SEARCH");
    }

    /**
     * Parse geo-coordinates from all fields of the given name fromt the given list of SOLR documents
     * 
     * @param solrFieldName Name of the SOLR fields to parse
     * @param results Documents to parse
     * @return A list of geo-coordinates as {@link Location}
     */
    public static List<Location> getLocations(String solrFieldName, SolrDocumentList results) {
        List<Location> locations = new ArrayList<>();
        for (SolrDocument doc : results) {
            try {
                String label = (String) doc.getFieldValue(SolrConstants.LABEL);
                String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
                String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
                String mimeType = (String) doc.getFieldValue(SolrConstants.MIMETYPE);
                boolean anchorOrGroup = SolrTools.isAnchor(doc) || SolrTools.isGroup(doc);
                Boolean hasImages = (Boolean) doc.getFieldValue(SolrConstants.BOOL_IMAGEAVAILABLE);
                locations.addAll(getLocations(doc.getFieldValue(solrFieldName))
                        .stream()
                        .map(p -> new Location(p, label,
                                Location.getRecordURI(pi, PageType.determinePageType(docStructType, mimeType, anchorOrGroup, hasImages, false),
                                        DataManager.getInstance().getUrlBuilder())))
                        .collect(Collectors.toList()));
            } catch (IllegalArgumentException e) {
                logger.error("Error parsing field {} of document {}: {}", solrFieldName, doc.get("IDDOC"), e.getMessage());
                logger.error(e.toString(), e);
            }
        }
        return locations;
    }

    /**
     * Parse geo-coordinates from the value of a SOLR field
     * 
     * @param o SOLR field value
     * @return a list of {@link IArea} representing the locations from the given value
     */
    public static List<IArea> getLocations(Object o) {
        List<IArea> locs = new ArrayList<>();
        if (o == null) {
            return locs;
        } else if (o instanceof List) {
            for (int i = 0; i < ((List) o).size(); i++) {
                locs.addAll(getLocations(((List) o).get(i)));
            }
            return locs;
        } else if (o instanceof String) {
            String s = (String) o;
            Matcher polygonMatcher = Pattern.compile(POLYGON_LAT_LNG_PATTERN).matcher(s); //NOSONAR   no catastrophic backtracking detected
            while (polygonMatcher.find()) {
                String match = polygonMatcher.group();
                locs.add(new Polygon(getPoints(match)));
                s = s.replace(match, "");
                polygonMatcher = Pattern.compile(POLYGON_LAT_LNG_PATTERN).matcher(s); //NOSONAR   no catastrophic backtracking detected
            }
            if (StringUtils.isNotBlank(s)) {
                locs.addAll(Arrays.asList(getPoints(s)).stream().map(p -> new Point(p[0], p[1])).collect(Collectors.toList()));
            }
            return locs;
        }
        throw new IllegalArgumentException(String.format("Unable to parse %s of type %s as location", o.toString(), o.getClass()));
    }

    private static double[][] getPoints(String value) {
        List<double[]> points = new ArrayList<>();
        Matcher matcher = Pattern.compile(POINT_LAT_LNG_PATTERN).matcher(value); //NOSONAR   no catastrophic backtracking detected
        while (matcher.find() && matcher.groupCount() == 2) {
            points.add(parsePoint(matcher.group(1), matcher.group(2)));
        }
        return points.toArray(new double[points.size()][2]);
    }

    /**
     * 
     * @param x
     * @param y
     * @return Parsed point as a double[]
     */
    private static double[] parsePoint(Object x, Object y) {
        if (x instanceof Number) {
            double[] loc = new double[2];
            loc[0] = ((Number) x).doubleValue();
            loc[1] = ((Number) y).doubleValue();
            return loc;
        } else if (x instanceof String) {
            try {
                double[] loc = new double[2];
                loc[0] = Double.parseDouble((String) x);
                loc[1] = Double.parseDouble((String) y);
                return loc;
            } catch (NumberFormatException e) {
                logger.debug(e.getMessage());
            }
        }
        throw new IllegalArgumentException(String.format("Unable to parse objects %s, %s to double array", x, y));
    }

    private static List<GeoMapFeature> createFeaturesFromJson(String point) {
        List<GeoMapFeature> features = new ArrayList<>();
        JSONObject json = new JSONObject(point);
        String type = json.getString("type");
        if ("FeatureCollection".equalsIgnoreCase(type)) {
            JSONArray array = json.getJSONArray("features");
            if (array != null) {
                array.forEach(f -> {
                    if (f instanceof JSONObject) {
                        JSONObject jsonObj = (JSONObject) f;
                        String jsonString = jsonObj.toString();
                        GeoMapFeature feature = new GeoMapFeature(jsonString);
                        if (!features.contains(feature)) {
                            features.add(feature);
                        }
                    }
                });
            }
        } else if ("Feature".equalsIgnoreCase(type)) {
            GeoMapFeature feature = new GeoMapFeature(json.toString());
            features.add(feature);
        }
        return features;
    }

}
