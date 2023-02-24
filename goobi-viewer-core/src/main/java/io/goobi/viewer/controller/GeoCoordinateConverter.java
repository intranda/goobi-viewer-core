package io.goobi.viewer.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.maps.GeoMapFeature;
import io.goobi.viewer.model.maps.IArea;
import io.goobi.viewer.model.maps.Location;
import io.goobi.viewer.model.maps.Point;
import io.goobi.viewer.model.maps.Polygon;
import io.goobi.viewer.model.maps.SolrFeatureSet;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Utility methods for converting geo-coordinated between different formats
 * @author florian
 *
 */
public class GeoCoordinateConverter {

    private static final Logger logger = LogManager.getLogger(GeoCoordinateConverter.class);
    protected static final String POINT_LAT_LNG_PATTERN = "([\\dE.-]+)[\\s/]*([\\dE.-]+)";
    protected static final String POLYGON_LAT_LNG_PATTERN = "POLYGON\\(\\(([\\dE.-]+[\\s/]*[\\dE.-]+[,\\s]*)+\\)\\)"; //NOSONAR
    
    private GeoCoordinateConverter() {
        
    }
    
    /**
     * Collect all point coordinate in the given coordinate fields from solr documents returned by the given solr query
     * @param query     Solr query to get documents
     * @param filterQueries     filter for solr query
     * @param coordinateFields  fields containing the coordinate points to collect
     * @param markerTitleField  solr field containing a title for the coordinates
     * @return  a list of {@link GeoMapFeature} 
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static List<GeoMapFeature> getFeaturesFromSolrQuery(String query, List<String> filterQueries, List<String> coordinateFields, String markerTitleField)
            throws PresentationException, IndexUnreachableException {
        List<SolrDocument> docs;
        List<String> fieldList = new ArrayList<>(coordinateFields);
        fieldList.add(markerTitleField);
        String coordinateFieldsQuery = coordinateFields.stream().map(s -> s + ":*").collect(Collectors.joining(" "));
        String filterQuery = SearchHelper.getAllSuffixes(BeanUtils.getRequest(), true, true);

        String finalQuery = String.format("+(%s) +(%s) +(%s *:*)", query, coordinateFieldsQuery, filterQuery);
        docs = DataManager.getInstance().getSearchIndex().search(finalQuery, 0, 10_000, null, null, fieldList, filterQueries, null).getResults();
        List<GeoMapFeature> features = new ArrayList<>();
        for (SolrDocument doc : docs) {
            for (String field : coordinateFields) {
                Collection<GeoMapFeature> tempFeatures = getGeojsonPoints(doc, field, markerTitleField, null);
                features.addAll(tempFeatures);
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
                    return f;
                })
                .collect(Collectors.toList());

        return features;
    }
    
    /**
     * Collect all point coordinate in the given metadata field within the given solr document
     * @param doc   the document containing the coordinates
     * @param metadataField     The name of the solr field to search in
     * @param titleField    solr field containing a title for the coordinates
     * @param descriptionField    solr field containing a description for the coordinates
     */
    public static Collection<GeoMapFeature> getGeojsonPoints(SolrDocument doc, String metadataField, String titleField, String descriptionField) {
        String title = StringUtils.isBlank(titleField) ? null : SolrTools.getSingleFieldStringValue(doc, titleField);
        String desc = StringUtils.isBlank(descriptionField) ? null : SolrTools.getSingleFieldStringValue(doc, descriptionField);
        List<GeoMapFeature> docFeatures = new ArrayList<>();
        List<String> points = SolrTools.getMetadataValues(doc, metadataField);
        for (String point : points) {
            try {
                if (point.matches(POINT_LAT_LNG_PATTERN)) { //NOSONAR  no catastrophic backtracking detected
                    GeoMapFeature feature = new GeoMapFeature();
                    feature.setTitle(title);
                    feature.setDescription(desc);

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
                } else if(point.matches(POLYGON_LAT_LNG_PATTERN)){
                    GeoMapFeature feature = new GeoMapFeature();
                    feature.setTitle(title);
                    feature.setDescription(desc);

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
                    docFeatures.addAll(createFeaturesFromJson(title, desc, point));
                }
            } catch (JSONException | NumberFormatException e) {
                logger.error("Encountered non-json feature: {}", point);
            }
        }
        return docFeatures;
    }
    
    /**
     * Parse geo-coordinates from all fields of the given name fromt the given list of SOLR documents
     * @param solrFieldName     Name of the SOLR fields to parse
     * @param results   Documents to parse
     * @return  A list of geo-coordinates as {@link Location}
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
     * @param a SOLR field value
     * @return  a list of {@link IArea} representing the locations from the given value
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
     * @return
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
    

    
    private static List<GeoMapFeature> createFeaturesFromJson(String title, String desc, String point) {
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
                        feature.setTitle(title);
                        feature.setDescription(desc);
                        if (!features.contains(feature)) {
                            features.add(feature);
                        }
                    }
                });
            }
        } else if ("Feature".equalsIgnoreCase(type)) {
            GeoMapFeature feature = new GeoMapFeature(json.toString());
            feature.setTitle(title);
            feature.setDescription(desc);
            features.add(feature);
        }
        return features;
    }
    
}