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
package io.goobi.viewer.model.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>GeoLocationInfo class.</p>
 *
 * @author Florian Alpers
 */
public class GeoLocationInfo {

    private static final String JSON_PROPERTYNAME_CENTER = "centerLocation";
    private static final String JSON_PROPERTYNAME_OVERLAY = "displayOverlay";
    private static final String JSON_PROPERTYNAME_LOCATIONS = "locations";

    private GeoLocation centerLocation = new GeoLocation();

    private List<GeoLocation> locationList = new ArrayList<>();

    /**
     * <p>Constructor for GeoLocationInfo.</p>
     */
    public GeoLocationInfo() {

    }

    /**
     * <p>Constructor for GeoLocationInfo.</p>
     *
     * @param json a {@link org.json.JSONObject} object.
     * @throws org.json.JSONException if any.
     * @throws org.json.JSONException if any.
     */
    public GeoLocationInfo(JSONObject json) throws JSONException {
        if (json.has(JSON_PROPERTYNAME_CENTER)) {
            setCenterLocation(new GeoLocation(json.getJSONObject(JSON_PROPERTYNAME_CENTER)));
        }
        JSONArray locations = json.getJSONArray(JSON_PROPERTYNAME_LOCATIONS);
        if (locations != null) {
            for (int i = 0; i < locations.length(); i++) {
                locationList.add(new GeoLocation(locations.getJSONObject(i)));
            }
        }
    }

    /**
     * <p>getAsJson.</p>
     *
     * @return a {@link org.json.JSONObject} object.
     */
    public JSONObject getAsJson() {
        Map<String, Object> map = new HashMap<>();
        map.put(JSON_PROPERTYNAME_CENTER, getCenterLocation().getAsJson());

        JSONArray locations = new JSONArray();
        for (GeoLocation geoLocation : locationList) {
            JSONObject obj = geoLocation.getAsJson();
            locations.put(obj);
        }
        map.put(JSON_PROPERTYNAME_LOCATIONS, locations);

        JSONObject obj = new JSONObject(map);
        return obj;
    }

    /**
     * <p>Getter for the field <code>centerLocation</code>.</p>
     *
     * @return the centerLocation
     */
    public GeoLocation getCenterLocation() {
        return centerLocation;
    }

    /**
     * <p>Setter for the field <code>centerLocation</code>.</p>
     *
     * @param centerLocation the centerLocation to set
     */
    public void setCenterLocation(GeoLocation centerLocation) {
        this.centerLocation = centerLocation;
    }

    /**
     * <p>Getter for the field <code>locationList</code>.</p>
     *
     * @return the locationList
     */
    public List<GeoLocation> getLocationList() {
        return locationList;
    }

    /**
     * <p>Setter for the field <code>locationList</code>.</p>
     *
     * @param locationList the locationList to set
     */
    public void setLocationList(List<GeoLocation> locationList) {
        this.locationList = locationList;
    }

}
