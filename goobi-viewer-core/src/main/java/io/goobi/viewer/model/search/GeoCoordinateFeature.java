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
package io.goobi.viewer.model.search;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author florian
 *
 */
public class GeoCoordinateFeature {

    private static final Logger logger = LogManager.getLogger(GeoCoordinateFeature.class);

    private static final String REGEX_GEOCOORDS_SEARCH_STRING =
            "(IsWithin|Intersects|Contains|IsDisjointTo)\\((\\w+)\\(\\(?([\\s\\d\\-.,]+)\\)?\\)\\)(?:\\s*distErrPct=([\\d\\-.,]+))?"; //NOSONAR backtracking save

    private static final int REGEX_GEOCOORDS_SEARCH_GROUP_RELATION = 1;
    private static final int REGEX_GEOCOORDS_SEARCH_GROUP_SHAPE = 2;
    private static final int REGEX_GEOCOORDS_SEARCH_GROUP_POINTS = 3;
    private static final int REGEX_GEOCOORDS_SEARCH_GROUP_DIST_ERROR = 4;

    public static final String RELATION_PREDICATE_ISWITHIN = "ISWITHIN";
    public static final String RELATION_PREDICATE_INTERSECTS = "INTERSECTS";
    public static final String RELATION_PREDICATE_CONTAINS = "CONTAINS";
    public static final String RELATION_PREDICATE_ISDISJOINTTO = "ISDISJOINTTO";

    public static final String SHAPE_POLYGON = "POLYGON";

    private final JSONObject feature;
    private final String predicate;
    private final String shape;
    private final double distError;

    public GeoCoordinateFeature(String featureString, String predicate, String shape, double distError) throws JSONException {
        this.feature = new JSONObject(featureString);
        this.predicate = predicate;
        this.shape = shape;
        this.distError = distError;
    }

    /**
     * Initialize as a polygon feature with the given points as vertices
     * 
     * @param points
     * @param predicate
     * @param shape
     */
    public GeoCoordinateFeature(double[][] points, String predicate, String shape, double distError) {
        JSONObject json = new JSONObject();
        json.put("type", shape);
        JSONArray vertices = new JSONArray();
        for (int i = 0; i < points.length; i++) {
            List<Double> pointList = Arrays.asList(points[i][1], points[i][0]);
            JSONArray point = new JSONArray(pointList);
            vertices.put(point);
        }
        json.put("vertices", vertices);
        this.feature = json;
        this.predicate = predicate;
        this.shape = shape;
        this.distError = distError;

    }

    public String getFeatureAsString() {
        return this.feature.toString();
    }

    public String getType() {
        return feature.getString("type");
    }

    public double[][] getVertices() {
        JSONArray vertices = feature.getJSONArray("vertices");
        double[][] points = new double[vertices.length()][2];
        for (int i = 0; i < vertices.length(); i++) {
            JSONArray vertex = vertices.getJSONArray(i);
            points[i] = new double[] { vertex.getDouble(0), vertex.getDouble(1) };
        }
        return points;
    }

    public String getSearchString() {

        double[][] points = getVertices();
        String pointString = Arrays.stream(points).map(p -> Double.toString(p[1]) + " " + Double.toString(p[0])).collect(Collectors.joining(", "));

        if ("POINT".equalsIgnoreCase(this.shape)) {
            String template = "$P($S($V)) distErrPct=$E";
            return template
                    .replace("$P", this.predicate)
                    .replace("$S", this.shape)
                    .replace("$V", pointString)
                    .replace("$E", Double.toString(this.distError));
        } else {
            String template = "$P($S(($V)))";
            return template
                    .replace("$P", this.predicate)
                    .replace("$S", this.shape)
                    .replace("$V", pointString);
        }

    }

    public static String getPredicate(String searchString) {
        Matcher matcher = Pattern.compile(REGEX_GEOCOORDS_SEARCH_STRING, Pattern.CASE_INSENSITIVE).matcher(searchString);

        if (matcher.find()) {
            return matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_RELATION);
        }
        return RELATION_PREDICATE_ISWITHIN;
    }

    public static String getShape(String searchString) {
        Matcher matcher = Pattern.compile(REGEX_GEOCOORDS_SEARCH_STRING, Pattern.CASE_INSENSITIVE).matcher(searchString);

        if (matcher.find()) {
            return matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_SHAPE);
        }
        return SHAPE_POLYGON;
    }

    public static double getDistError(String searchString) {
        Matcher matcher = Pattern.compile(REGEX_GEOCOORDS_SEARCH_STRING, Pattern.CASE_INSENSITIVE).matcher(searchString);

        try {
            if (matcher.find()) {
                String distErrorString = matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_DIST_ERROR);
                if (StringUtils.isNotBlank(distErrorString)) {
                    return Double.parseDouble(distErrorString);
                }
            }
        } catch (NumberFormatException e) {
            logger.error("Error parsing \"distErrPct\" from searchString: {}", e.toString());
        }
        return 0;
    }

    public static double[][] getGeoSearchPoints(String searchString) {

        Matcher matcher = Pattern.compile(REGEX_GEOCOORDS_SEARCH_STRING, Pattern.CASE_INSENSITIVE).matcher(searchString);

        if (matcher.find()) {
            String allPoints = matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_POINTS);
            String[] strPoints = allPoints.split(", ");
            double[][] points = new double[strPoints.length][2];
            for (int i = 0; i < strPoints.length; i++) {
                try {
                    String[] strPoint = strPoints[i].split(" ");
                    points[i] = new double[] { Double.parseDouble(strPoint[0]), Double.parseDouble(strPoint[1]) };
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    logger.warn("Unable to parse {} as double array", strPoints[i]);
                }
            }
            return points;
        }
        return new double[0][2];
    }

    /**
     * @return true if number of vertices larger than 0; false otherwise
     */
    public boolean hasVertices() {
        return getVertices().length > 0;
    }

    /**
     * 
     * @return the shape
     */
    public String getShape() {
        return this.shape;
    }

    /**
     * 
     * @return the predicate
     */
    public String getPredicate() {
        return this.predicate;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            GeoCoordinateFeature other = (GeoCoordinateFeature) obj;
            return other.predicate.equals(this.predicate) && other.shape.equals(this.shape)
                    && Arrays.deepEquals(other.getVertices(), this.getVertices());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.feature.hashCode();
    }

    public double getDistError() {
        return distError;
    }

}
