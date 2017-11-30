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
package de.intranda.digiverso.presentation.model.misc;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 * @author Florian Alpers
 *
 */
public class GeoLocation {
    
    private static final String JSON_PROPERTYNAME_LONGITUDE = "longitude";
    private static final String JSON_PROPERTYNAME_LATITUDE = "latitude";


    private Double latitude = null;
    private Double longitude = null;

   
    public GeoLocation() {
        // TODO Auto-generated constructor stub
    }

    public GeoLocation(Double latitude, Double longitude) {
        super();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GeoLocation(JSONObject json) {
        setLatitude(json.getDouble(JSON_PROPERTYNAME_LATITUDE));
        setLongitude(json.getDouble(JSON_PROPERTYNAME_LONGITUDE));
    }

    /**
     * @return the langitude
     */
    public Double getLatitude() {
        return latitude;
    }
    /**
     * @param langitude the langitude to set
     */
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    /**
     * @return the longitude
     */
    public Double getLongitude() {
        return longitude;
    }
    /**
     * @param longitude the longitude to set
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public JSONObject getAsJson() {
        Map<String, Double> map = new HashMap<>();
        map.put(JSON_PROPERTYNAME_LONGITUDE, getLongitude());
        map.put(JSON_PROPERTYNAME_LATITUDE, getLatitude());
        
        JSONObject obj = new JSONObject(map);
        return obj;
    }
    
    /**
     *  
     * @return true if either latitude or longitude is not set or set to NULL
     */
    public boolean isEmpty() {
        return longitude == null || latitude == null;
    }
    
}
