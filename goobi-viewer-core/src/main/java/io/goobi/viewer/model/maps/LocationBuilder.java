package io.goobi.viewer.model.maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.goobi.viewer.model.search.Search;

public class LocationBuilder {

    private static final Logger logger = LogManager.getLogger(Search.class);

    @SuppressWarnings("rawtypes")
    public List<IArea> getLocations(Object solrValue) {
        List<IArea> locs = new ArrayList<>();
        if (solrValue == null) {
            return locs;
        } else if (solrValue instanceof List) {
            for (int i = 0; i < ((List) solrValue).size(); i++) {
                locs.addAll(getLocations(((List) solrValue).get(i)));
            }
            return locs;
        } else if (solrValue instanceof String) {
            String s = (String) solrValue;
            Matcher polygonMatcher =
                    Pattern.compile("POLYGON\\(\\([0-9.\\-,E\\s]+\\)\\)").matcher(s); //NOSONAR no catastrophic backtracking detected
            while (polygonMatcher.find()) {
                String match = polygonMatcher.group();
                locs.add(new Polygon(getPoints(match)));
                s = s.replace(match, "");
                polygonMatcher = Pattern.compile("POLYGON\\(\\([0-9.\\-,E\\s]+\\)\\)").matcher(s); //NOSONAR no catastrophic backtracking detected
            }
            if (StringUtils.isNotBlank(s)) {
                locs.addAll(Arrays.asList(getPoints(s)).stream().map(p -> new Point(p[0], p[1])).collect(Collectors.toList()));
            }
            if (s.startsWith("{")) {//probably json 
                getAreaFromGeoJson(s).ifPresent(locs::add);
            }
            return locs;
        }
        throw new IllegalArgumentException(String.format("Unable to parse %s of type %s as location", solrValue.toString(), solrValue.getClass()));
    }

    /**
     * 
     * @param value
     * @return double[][]
     */
    private double[][] getPoints(String value) {
        List<double[]> points = new ArrayList<>();
        Matcher matcher = Pattern.compile("([0-9\\.\\-E]+)\\s([0-9\\.\\-E]+)").matcher(value); //NOSONAR   no catastrophic backtracking detected
        while (matcher.find() && matcher.groupCount() == 2) {
            points.add(parsePoint(matcher.group(1), matcher.group(2)));
        }
        return points.toArray(new double[points.size()][2]);
    }

    /**
     * 
     * @param x
     * @param y
     * @return double[][]
     */
    private double[] parsePoint(Object x, Object y) {
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

    private Optional<IArea> getAreaFromGeoJson(String geoJsonString) {
        try {
            JSONObject json = new JSONObject(geoJsonString);
            if ("FeatureCollection".equals(json.getString("type"))) {
                JSONArray features = json.getJSONArray("features");
                List<Point> coordinateList = new ArrayList<>();
                for (Object featureObject : features) {
                    if (featureObject instanceof JSONObject) {
                        JSONObject feature = (JSONObject) featureObject;
                        coordinateList.add(getPointFromFeature(feature));
                    }
                }
                if (coordinateList.size() > 1) {
                    IArea area = new Polygon(coordinateList);
                    return Optional.of(area);
                } else if (coordinateList.size() > 0) {
                    return Optional.of(coordinateList.get(0));
                } else {
                    return Optional.empty();
                }
            } else if ("Feature".equals(json.getString("type"))) {
                return Optional.of(getPointFromFeature(json));
            } else {
                throw new JSONException("Unrecognized type '" + json.getString("type") + "' of geojson. May be some other json object");
            }
        } catch (JSONException e) {
            logger.error("Cannot parse '{}' as geojson", geoJsonString, e);
            return Optional.empty();
        }
    }

    public Point getPointFromFeature(JSONObject feature) {
        try {
            JSONArray coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates");
            Double[] coords = new Double[coordinates.length()];
            for (int i = 0; i < coordinates.length(); i++) {
                coords[i] = coordinates.getDouble(i);
            }
            return new Point(coords[0], coords[1]);
        } catch (IndexOutOfBoundsException e) {
            throw new JSONException(e);
        }
    }

}
