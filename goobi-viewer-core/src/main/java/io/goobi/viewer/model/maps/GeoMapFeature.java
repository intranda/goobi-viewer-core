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
package io.goobi.viewer.model.maps;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * @author florian
 *
 */
public class GeoMapFeature {
    
    private String title;
    private String description;
    private String json;
    
    public GeoMapFeature() {
    }
    
    
    /**
     * @param jsonString
     */
    public GeoMapFeature(String jsonString) {
        this.json = jsonString;
    }
    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * @return the json
     */
    public String getJson() {
        return json;
    }
    /**
     * @param json the json to set
     */
    public void setJson(String json) {
        this.json = json;
    }
    
    public JSONObject getJsonObject() {
        JSONObject object = new JSONObject(this.json);
        JSONObject properties = object.getJSONObject("properties");
        if (properties == null) {
            properties = new JSONObject();
            object.append("properties", properties);
        }
        if(StringUtils.isNotBlank(this.title)) {
            properties.append("title", this.title);
        }
        if(StringUtils.isNotBlank(this.description)) {
            properties.append("description", this.description);
        }
        return object;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.json == null ? "".hashCode() : this.json.hashCode();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(obj.getClass().equals(this.getClass())) {
            return ObjectUtils.equals(this.json, ((GeoMapFeature)obj).json);
        } else {
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.json;
    }

}
