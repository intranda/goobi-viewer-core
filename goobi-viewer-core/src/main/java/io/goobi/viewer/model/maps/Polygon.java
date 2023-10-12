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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONObject;

/**
 * @author florian
 *
 */
public class Polygon implements IArea {

    private final List<Point> vertices;

    public Polygon(List<Point> points) {
        this.vertices = points;
    }

    public Polygon(double[][] points) {
        List<Point> vertices = new ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            vertices.add(new Point(points[i][0], points[i][1]));
        }
        this.vertices = Collections.unmodifiableList(vertices);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.maps.IArea#getVertices()
     */
    @Override
    public double[][] getVertices() {
        double[][] points = new double[this.vertices.size()][2];
        for (int i = 0; i < points.length; i++) {
            points[i] = this.vertices.get(i).getVertices()[0];
        }
        return points;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.maps.IArea#getGeoJson()
     */
    @Override
    public String getGeoJson() {
        double[][][] coords = { getVertices() };
        JSONObject geometry = new JSONObject();
        geometry.put("coordinates", coords);
        geometry.put("type", "Polygon");
        return geometry.toString();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.maps.IArea#getDiameter()
     */
    @Override
    public double getDiameter() {
        double minLng = Double.MAX_VALUE;
        double maxLng = 0;
        double minLat = Double.MAX_VALUE;
        double maxLat = 0;
        for (Point point : vertices) {
            minLng = Math.min(minLng, point.lng);
            maxLng = Math.max(maxLng, point.lng);
            minLat = Math.min(minLat, point.lat);
            maxLat = Math.max(maxLat, point.lat);
        }
        return Math.sqrt((maxLng - minLng) * (maxLng - minLng) + (maxLat - minLat) * (maxLat - minLat));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.vertices.size();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            Polygon other = (Polygon) obj;
            return this.vertices.size() == other.vertices.size() &&
                    this.vertices.stream().filter(v -> other.vertices.contains(v)).count() == this.vertices.size();
        } else {
            return false;
        }
    }

}
