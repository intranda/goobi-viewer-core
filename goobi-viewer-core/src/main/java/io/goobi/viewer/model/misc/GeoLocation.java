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
package io.goobi.viewer.model.misc;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single geographic location with coordinates and an optional display label.
 *
 * @author Florian Alpers
 */
public class GeoLocation {

    private static final String JSON_PROPERTYNAME_LONGITUDE = "longitude";
    private static final String JSON_PROPERTYNAME_LATITUDE = "latitude";
    private static final String JSON_PROPERTYNAME_INFO = "infos";
    private static final String JSON_PROPERTYNAME_LINK = "link";

    private Double latitude = null;
    private Double longitude = null;

    private String info = null;
    private String link = null;

    /**
     * Creates a new GeoLocation instance.
     */
    public GeoLocation() {
        //
    }

    /**
     * Creates a new GeoLocation instance.
     *
     * @param latitude geographic latitude in decimal degrees.
     * @param longitude geographic longitude in decimal degrees.
     */
    public GeoLocation(Double latitude, Double longitude) {
        super();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Creates a new GeoLocation instance.
     *
     * @param json JSON object containing latitude, longitude, info and link fields.
     * @throws org.json.JSONException if any.
     */
    public GeoLocation(JSONObject json) throws JSONException {
        if (json.has(JSON_PROPERTYNAME_LATITUDE)) {
            try {
                setLatitude(json.getDouble(JSON_PROPERTYNAME_LATITUDE));
            } catch (NullPointerException | IllegalArgumentException | JSONException e) {
                //
            }
        }
        if (json.has(JSON_PROPERTYNAME_LONGITUDE)) {
            try {
                setLongitude(json.getDouble(JSON_PROPERTYNAME_LONGITUDE));
            } catch (NullPointerException | IllegalArgumentException | JSONException e) {
                //
            }
        }
        if (json.has(JSON_PROPERTYNAME_INFO)) {
            setInfo(json.getString(JSON_PROPERTYNAME_INFO));
        }
        if (json.has(JSON_PROPERTYNAME_LINK)) {
            setLink(json.getString(JSON_PROPERTYNAME_LINK));
        }
    }

    /**
     * @param string raw JSON string to format for display
     * @return Formatted string
     */
    private static String formatJson(final String string) {
        String ret = StringEscapeUtils.unescapeJava(string);
        ret = ret.replaceAll("(\\r)?\\n", "<br/>");
        return ret;
    }

    /**
     * Getter for the field <code>latitude</code>.
     *

     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * Setter for the field <code>latitude</code>.
     *
     * @param latitude geographic latitude in decimal degrees.
     */
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /**
     * Getter for the field <code>longitude</code>.
     *

     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * Setter for the field <code>longitude</code>.
     *
     * @param longitude the geographic longitude in decimal degrees (WGS84)
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /**
     * getAsJson.
     *
     * @return a {@link org.json.JSONObject} object.
     */
    public JSONObject getAsJson() {
        Map<String, Object> map = new HashMap<>();
        map.put(JSON_PROPERTYNAME_LATITUDE, getLatitude() == null ? "" : getLatitude());
        map.put(JSON_PROPERTYNAME_LONGITUDE, getLongitude() == null ? "" : getLongitude());
        if (StringUtils.isNotBlank(getInfo())) {
            map.put(JSON_PROPERTYNAME_INFO, formatJson(getInfo()));
        }
        if (StringUtils.isNotBlank(getLink())) {
            map.put(JSON_PROPERTYNAME_LINK, getLink());
        }

        return new JSONObject(map);
    }

    /**
     * isEmpty.
     *
     * @return true if either latitude or longitude is not set or set to NULL
     */
    public boolean isEmpty() {
        return longitude == null || latitude == null;
    }

    /**
     * Getter for the field <code>info</code>.
     *

     */
    public String getInfo() {
        return info;
    }

    /**
     * Setter for the field <code>info</code>.
     *
     * @param info the descriptive text or HTML content shown in the map popup for this location
     */
    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * Getter for the field <code>link</code>.
     *

     */
    public String getLink() {
        return link;
    }

    /**
     * Setter for the field <code>link</code>.
     *
     * @param link the URL linked from the map popup for this location
     */
    public void setLink(String link) {
        this.link = link;
    }

}
