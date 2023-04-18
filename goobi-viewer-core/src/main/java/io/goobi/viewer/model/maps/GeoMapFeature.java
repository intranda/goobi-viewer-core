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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.controller.JsonTools;

/**
 * @author florian
 *
 */
public class GeoMapFeature {

    private String title;
    private String description;
    private String link;
    private String json;
    private int count = 1;
    //This is used to identify the feature with a certain document, specifically a LOGID of a TOC element
    private String documentId = null;
    private Map<String, List<IMetadataValue>> metadata = new HashMap<>();

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
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * @param documentId the documentId to set
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * @return the documentId
     */
    public String getDocumentId() {
        return documentId;
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

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Adds the metadata entry of the given name if none exists yet; otherwise add the given values to the list of values 
     * mapped to the given metadata name
     * @param name
     * @param values
     */
    public void addMetadata(String name, List<IMetadataValue> values) {
        List<IMetadataValue> existingValues = this.metadata.get(name);
        if(existingValues == null) {
            existingValues = new ArrayList<>();
            this.metadata.put(name, existingValues);            
        }
        existingValues.addAll(values);
    }
        
    public Map<String, List<IMetadataValue>> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }
    
    public JSONObject getJsonObject() {

        JSONObject object = new JSONObject(this.json);
        JSONObject properties;
        try {
            properties = object.getJSONObject("properties");
        } catch(JSONException e) {
            properties = new JSONObject();
            object.put("properties", properties);
        }
        if (StringUtils.isNotBlank(this.title)) {
            properties.put("title", this.title);
        }
        if (StringUtils.isNotBlank(this.description)) {
            properties.put("description", this.description);
        }
        if (StringUtils.isNotBlank(this.link)) {
            properties.put("link", this.link);
        }
        if (StringUtils.isNotBlank(this.documentId)) {
            properties.put("documentId", this.documentId);
        }
        if(!this.metadata.isEmpty()) {
            JSONObject jsonMetadata = new JSONObject();
            for (Entry<String, List<IMetadataValue>> entry : this.metadata.entrySet()) {
                try {                    
                    String name = entry.getKey();
                    List<IMetadataValue> values = entry.getValue();
                    JSONArray array = new JSONArray();
                    for (IMetadataValue value : values) {
                        JSONObject o = JsonTools.getAsJson(value);
                        array.put(o);
                    }
                    jsonMetadata.put(name, array);
                } catch(JsonProcessingException e) {
                    //ignore
                }
            }
        }
        properties.put("count", this.count);
        return object;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int jsonCode = this.json == null ? "".hashCode() : this.json.hashCode();
        int titleCode = this.title == null ? "".hashCode() : this.title.hashCode();
        int linkCode = this.link == null ? "".hashCode() : this.link.hashCode();
        return jsonCode + 31 * (titleCode + 31 * linkCode);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass().equals(this.getClass())) {
            GeoMapFeature other = (GeoMapFeature)obj;
            return Objects.equals(this.json, other.json) &&
                    Objects.equals(this.title, other.title) &&
                    Objects.equals(this.link, other.link);
        }

        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.json;
    }

}
