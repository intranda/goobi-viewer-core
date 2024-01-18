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
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue.ValuePair;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.model.metadata.MetadataContainer;

/**
 * @author florian
 *
 */
public class GeoMapFeature {

    private IMetadataValue title;
    private IMetadataValue description;
    private String link;
    private String json;
    private int count = 1;
    //This is used to identify the feature with a certain document, specifically a LOGID of a TOC element
    private String documentId = null;
    private Integer pageNo = null;
    private List<MetadataContainer> entities = new ArrayList<>();

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
    public IMetadataValue getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(IMetadataValue title) {
        this.title = title;
    }

    /**
     * @return the description
     */
    public IMetadataValue getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(IMetadataValue description) {
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
    
    public Integer getPageNo() {
        return pageNo;
    }
    
    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
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

    public List<MetadataContainer> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    public void setEntities(List<MetadataContainer> entities) {
        this.entities = entities;
    }

    public void addEntity(MetadataContainer entity) {
        this.entities.add(entity);
    }

    public JSONObject getJsonObject() {

        JSONObject object = new JSONObject(this.json);
        JSONObject properties;
        try {
            properties = object.getJSONObject("properties");
        } catch (JSONException e) {
            properties = new JSONObject();
            object.put("properties", properties);
        }
        if (this.title != null && !this.title.isEmpty()) {
            properties.put("title", JsonTools.getAsObjectForJson(this.title));
        }
        if (this.description != null && !this.description.isEmpty()) {
            properties.put("description", JsonTools.getAsObjectForJson(this.description));
        }
        if (StringUtils.isNotBlank(this.link)) {
            properties.put("link", this.link);
        }
        if (StringUtils.isNotBlank(this.documentId)) {
            properties.put("documentId", this.documentId);
        }
        if (this.pageNo != null) {
            properties.put("page", this.pageNo);
        }
        if (!this.entities.isEmpty()) {
            addEntities(properties);
        }
        properties.put("count", this.count);
        return object;
    }

    public void addEntities(JSONObject properties) {
        JSONArray ents = new JSONArray();
        properties.put("entities", ents);
        for (MetadataContainer entity : this.entities) {
            JSONObject jsonMetadata = new JSONObject();
            jsonMetadata.put("title", JsonTools.getAsObjectForJson(entity.getLabel()));
            ents.put(jsonMetadata);
            for (Entry<String, List<IMetadataValue>> entry : entity.getMetadata().entrySet()) {
                String name = entry.getKey();
                if (name != null) {
                    List<IMetadataValue> values = entry.getValue();
                    JSONArray array = new JSONArray();
                    for (IMetadataValue value : values) {
                        array.put(JsonTools.getAsObjectForJson(value));
                    }
                    jsonMetadata.put(name, array);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int jsonCode = this.json == null ? "".hashCode() : this.json.hashCode();
        int titleCode = this.title == null ? "".hashCode() : getIndentifyingString(this.title).hashCode();
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
            GeoMapFeature other = (GeoMapFeature) obj;
            return Objects.equals(this.json, other.json) &&
                    Objects.equals(getIndentifyingString(this.title), getIndentifyingString(other.title)) &&
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

    private String getIndentifyingString(IMetadataValue md) {
        return md.getValues().stream().map(ValuePair::getValue).distinct().collect(Collectors.joining());
    }
}
