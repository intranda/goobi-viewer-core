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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue.ValuePair;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.model.metadata.MetadataContainer;
import mil.nga.sf.geojson.FeatureConverter;
import mil.nga.sf.geojson.Geometry;
import mil.nga.sf.geojson.Point;
import mil.nga.sf.geojson.Polygon;
import mil.nga.sf.geojson.Position;

/**
 * @author florian
 *
 */
public class GeoMapFeature {

    private IMetadataValue title;
    private IMetadataValue description;
    private String link;
    private Geometry geometry;
    private int count = 1;
    //This is used to identify the feature with a certain document, specifically a LOGID of a TOC element
    private String documentId = null;
    private Integer pageNo = null;
    private final Map<String, String> properties = new HashMap<>();
    private List<MetadataContainer> entities = new ArrayList<>();

    public GeoMapFeature() {
    }

    public GeoMapFeature(Geometry geometry) {
        this.geometry = geometry;
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

    /**
     * @return the json
     */
    public String getJson() {
        return FeatureConverter.toStringValue(this.geometry);
    }

    /**
     * @param json the json to set
     */
    public void setJson(String json) {
        this.geometry = FeatureConverter.toGeometry(json);
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

    public double getLatitude() {
        if (this.geometry instanceof Point point) {
            return point.getPosition().getY();
        } else if (this.geometry instanceof Polygon polygon) {
            return polygon.getCoordinates().stream().flatMap(l -> l.stream()).mapToDouble(Position::getY).average().orElse(0);
        } else {
            throw new IllegalStateException("Not implemented for geometry " + this.geometry.getType());
        }
    }

    public double getLongitude() {
        if (this.geometry instanceof Point point) {
            return point.getPosition().getX();
        } else if (this.geometry instanceof Polygon polygon) {
            return polygon.getCoordinates().stream().flatMap(l -> l.stream()).mapToDouble(Position::getX).average().orElse(0);
        } else {
            throw new IllegalStateException("Not implemented for geometry " + this.geometry.getType());
        }
    }

    public JSONObject getJsonObject() {
        JSONObject object = new JSONObject(getJson());
        JSONObject jsonProperties = getProperties(object);
        if (this.title != null && !this.title.isEmpty()) {
            jsonProperties.put("title", JsonTools.getAsObjectForJson(this.title));
        }
        if (this.description != null && !this.description.isEmpty()) {
            jsonProperties.put("description", JsonTools.getAsObjectForJson(this.description));
        }
        this.properties.entrySet().forEach(entry -> jsonProperties.put(entry.getKey(), entry.getValue()));
        if (StringUtils.isNotBlank(this.link)) {
            jsonProperties.put("link", this.link);
        }
        if (StringUtils.isNotBlank(this.documentId)) {
            jsonProperties.put("documentId", this.documentId);
        }
        if (this.pageNo != null) {
            jsonProperties.put("page", this.pageNo);
        }
        if (!this.entities.isEmpty()) {
            addEntities(jsonProperties);
        }
        jsonProperties.put("count", this.count);
        return object;
    }

    public JSONObject getProperties(JSONObject object) {
        JSONObject properties;
        try {
            properties = object.getJSONObject("properties");
        } catch (JSONException e) {
            properties = new JSONObject();
            object.put("properties", properties);
        }
        return properties;
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
                        Object escapedValue = JsonTools.getAsObjectForJson(value);
                        array.put(escapedValue);
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
        int jsonCode = this.geometry == null ? "".hashCode() : this.geometry.hashCode();
        int titleCode = this.title == null ? "".hashCode() : getIndentifyingString(this.title).hashCode();
        return jsonCode + 31 * (titleCode);
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
            return Objects.equals(this.geometry, other.geometry)
                    && Objects.equals(getIndentifyingString(this.title), getIndentifyingString(other.title));
        }

        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getJson();
    }

    private String getIndentifyingString(IMetadataValue md) {
        return md.getValues().stream().map(ValuePair::getValue).distinct().collect(Collectors.joining());
    }

    public void setProperty(String name, String value) {
        this.properties.put(name, value);
    }

    public String getProperty(String name) {
        return this.properties.getOrDefault(name, "");
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
}
