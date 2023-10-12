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
 * @author florian
 *
 */
public class Point implements IArea {

    public final double lng;
    public final double lat;

    /**
     * First longitide in eastern direction, then latitude in northern direction
     * 
     * @param lng
     * @param lat
     */
    public Point(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    @Override
    public double[][] getVertices() {
        return new double[][] { { lng, lat } };
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.maps.IArea#getGeoJson()
     */
    @Override
    public String getGeoJson() {
        double[] coords = getVertices()[0];
        JSONObject geometry = new JSONObject();
        geometry.put("coordinates", coords);
        geometry.put("type", "Point");
        return geometry.toString();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.maps.IArea#getDiameter()
     */
    @Override
    public double getDiameter() {
        return 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Double.hashCode(lat * lng);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            Point other = (Point) obj;
            return this.lat == other.lat && this.lng == other.lng;
        } else {
            return false;
        }
    }

}
