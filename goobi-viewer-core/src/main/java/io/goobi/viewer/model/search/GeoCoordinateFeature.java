/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.search;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.exceptions.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author florian
 *
 */
public class GeoCoordinateFeature {

    private static final Logger logger = LoggerFactory.getLogger(GeoCoordinateFeature.class);

    private static final String REGEX_GEOCOORDS_SEARCH_STRING = "(IsWithin|Intersects|IsDisjointTo)\\((\\w+)\\(\\(((?:[\\d.]+ [\\d.]+,?\\s?)+)\\)\\)\\)";
    private static final int REGEX_GEOCOORDS_SEARCH_GROUP_RELATION = 1;
    private static final int REGEX_GEOCOORDS_SEARCH_GROUP_SHAPE = 2;
    private static final int REGEX_GEOCOORDS_SEARCH_GROUP_POINTS = 3;
    
    private final JSONObject feature;
    
    public GeoCoordinateFeature(String featureString) throws JSONException {
        this.feature = new JSONObject(featureString);
    }
    
    /**
     * Initialize as a polygon feature with the given points as vertices
     * @param vertices
     */
    public GeoCoordinateFeature(double[][] points) {
        JSONObject json = new JSONObject();
        json.put("type", "polygon");
        JSONArray vertices = new JSONArray();
        for (int i = 0; i < points.length; i++) {
            List<Double> pointList = Arrays.asList(points[i][1], points[i][0]);
            JSONArray point = new JSONArray(pointList);
            vertices.put(point);
        }
        json.put("vertices", vertices);
        this.feature = json;
    }

    public String getFeatureAsString() {
        return this.feature.toString();
    }
    
    public String getType() {
        return feature.getString("type");
    }
    
    public double[][] getVertices() {
        JSONArray vertices =  feature.getJSONArray("vertices");
        double[][] points = new double[vertices.length()][2];
        for (int i = 0; i < vertices.length(); i++) {
            JSONArray vertex = vertices.getJSONArray(i);
            points[i] = new double[]{vertex.getDouble(0), vertex.getDouble(1)};
        }
        return points;
    }
    
    public String getSearchString() {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("IsWithin(POLYGON((");
        double[][] points = getVertices();
        String pointString = Arrays.stream(points).map(p -> Double.toString(p[1]) + " " + Double.toString(p[0])).collect(Collectors.joining(", "));
        sb.append(pointString);
        sb.append(")))");
        return sb.toString();
        
    }
    
    public static double[][] getGeoSearchPoints(String searchString) {
        
        Matcher matcher = Pattern.compile(REGEX_GEOCOORDS_SEARCH_STRING).matcher(searchString);
        
        if(matcher.find()) {
            String relation = matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_RELATION);
            String shape = matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_SHAPE);
            String allPoints = matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_POINTS);
            String[] strPoints = allPoints.split(", ");
            double[][] points = new double[strPoints.length][2];
            for (int i = 0; i < strPoints.length; i++) {
                try {                    
                    String[] strPoint = strPoints[i].split(" ");
                    points[i] = new double[]{Double.parseDouble(strPoint[0]), Double.parseDouble(strPoint[1])};
                } catch(NumberFormatException e) {
                    logger.warn("Unable to parse {} as double array", strPoints[i]);
                }
            }
            return points;
        } else {
            return new double[0][2];
        }
        
    }
}
