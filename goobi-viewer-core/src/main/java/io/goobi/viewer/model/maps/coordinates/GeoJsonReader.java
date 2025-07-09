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
package io.goobi.viewer.model.maps.coordinates;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import mil.nga.sf.geojson.FeatureConverter;
import mil.nga.sf.geojson.Geometry;
import mil.nga.sf.geojson.GeometryCollection;

public class GeoJsonReader implements ICoordinateReader {

    @Override
    public boolean canRead(String value) {
        try {
            new JSONObject(value);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    @Override
    public Geometry read(String value) {
        JSONObject json = new JSONObject(value);
        if (json.has("coordinates")) {
            return FeatureConverter.toGeometry(value);
        } else if (json.has("geometry")) {
            return read(json.getJSONObject("geometry").toString());
        } else if (json.has("features")) {
            List<Geometry> geoEntries = new ArrayList<>();
            for (Object entry : json.getJSONArray("features")) {
                if (entry instanceof JSONObject) {
                    geoEntries.add(read(entry.toString()));
                }
            }
            if (geoEntries.size() == 1) {
                return geoEntries.get(0);
            } else {
                GeometryCollection collection = new GeometryCollection();
                collection.setGeometries(geoEntries);
                return collection;
            }
        } else {
            throw new IllegalArgumentException("Cannot read json '" + value + "' as geojson or geometry");
        }
    }

}
