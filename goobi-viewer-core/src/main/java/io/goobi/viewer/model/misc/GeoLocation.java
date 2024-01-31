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
 * <p>
 * GeoLocation class.
 * </p>
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
     * <p>
     * Constructor for GeoLocation.
     * </p>
     */
    public GeoLocation() {
        //
    }

    /**
     * <p>
     * Constructor for GeoLocation.
     * </p>
     *
     * @param latitude a {@link java.lang.Double} object.
     * @param longitude a {@link java.lang.Double} object.
     */
    public GeoLocation(Double latitude, Double longitude) {
        super();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * <p>
     * Constructor for GeoLocation.
     * </p>
     *
     * @param json a {@link org.json.JSONObject} object.
     * @throws org.json.JSONException if any.
     */
    public GeoLocation(JSONObject json) throws JSONException {
        if (json.has(JSON_PROPERTYNAME_LATITUDE)) {
            try {
                setLatitude(json.getDouble(JSON_PROPERTYNAME_LATITUDE));
            } catch (Exception e) {
                //
            }
        }
        if (json.has(JSON_PROPERTYNAME_LONGITUDE)) {
            try {
                setLongitude(json.getDouble(JSON_PROPERTYNAME_LONGITUDE));
            } catch (Exception e) {
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
     * @param string
     * @return Formatted string
     */
    private static String formatJson(final String string) {
        String ret = StringEscapeUtils.unescapeJava(string);
        ret = ret.replaceAll("(\\r)?\\n", "<br/>");
        return ret;
    }

    /**
     * <p>
     * Getter for the field <code>latitude</code>.
     * </p>
     *
     * @return the langitude
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * <p>
     * Setter for the field <code>latitude</code>.
     * </p>
     *
     * @param latitude a {@link java.lang.Double} object.
     */
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /**
     * <p>
     * Getter for the field <code>longitude</code>.
     * </p>
     *
     * @return the longitude
     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * <p>
     * Setter for the field <code>longitude</code>.
     * </p>
     *
     * @param longitude the longitude to set
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /**
     * <p>
     * getAsJson.
     * </p>
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
     * <p>
     * isEmpty.
     * </p>
     *
     * @return true if either latitude or longitude is not set or set to NULL
     */
    public boolean isEmpty() {
        return longitude == null || latitude == null;
    }

    /**
     * <p>
     * Getter for the field <code>info</code>.
     * </p>
     *
     * @return the info
     */
    public String getInfo() {
        return info;
    }

    /**
     * <p>
     * Setter for the field <code>info</code>.
     * </p>
     *
     * @param info the info to set
     */
    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * <p>
     * Getter for the field <code>link</code>.
     * </p>
     *
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * <p>
     * Setter for the field <code>link</code>.
     * </p>
     *
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
    }

}
