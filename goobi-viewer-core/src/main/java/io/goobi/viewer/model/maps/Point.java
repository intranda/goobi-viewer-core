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

import org.json.JSONObject;

/**
 * @author Florian Alpers
 */
public class Point implements IArea {

    private final double lng;
    private final double lat;

    /**
     * First longitide in eastern direction, then latitude in northern direction.
     * 
     * @param lng longitude in eastern direction
     * @param lat latitude in northern direction
     */
    public Point(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    @Override
    public double[][] getVertices() {
        return new double[][] { { lng, lat } };
    }

    @Override
    public String getGeoJson() {
        double[] coords = getVertices()[0];
        JSONObject geometry = new JSONObject();
        geometry.put("coordinates", coords);
        geometry.put("type", "Point");
        return geometry.toString();
    }

    @Override
    public double getDiameter() {
        return 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(lat * lng);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            Point other = (Point) obj;
            return this.lat == other.lat && this.lng == other.lng;
        }
        return false;
    }

    /**

     */
    public double getLng() {
        return lng;
    }

    /**

     */
    public double getLat() {
        return lat;
    }
}
