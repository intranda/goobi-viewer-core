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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Determines the visible area of a {@link GeoMap}. Consists of a zoom factor and a center point
 * 
 * @author florian
 *
 */
public class View {

    private final double zoom;
    private final Point center;

    public View(double zoom, Point center) {
        this.zoom = zoom;
        this.center = center;
    }

    public View(double zoom, double lng, double lat) {
        this(zoom, new Point(lng, lat));
    }

    public String getGeoJson() {
        JSONObject json = new JSONObject();
        json.put("zoom", this.zoom);
        json.put("center", List.of(this.center.lng, this.center.lat));
        return json.toString();
    }

    public static View fromGeoJson(String s) {
        JSONObject json = new JSONObject(s);
        double zoom = json.getLong("zoom");
        JSONArray pos = json.getJSONArray("center");
        double lng = pos.getDouble(0);
        double lat = pos.getDouble(1);
        return new View(zoom, lng, lat);
    }

    @Override
    public String toString() {
        return getGeoJson();
    }

}
