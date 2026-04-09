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

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a configurable map marker used in geo maps, holding visual properties such as icon,
 * colors, shape, and marker type (default, ExtraMarkers, or DivIcon) for rendering on a Leaflet map.
 *
 * @author Florian Alpers
 */
public class GeoMapMarker {

    private String name;
    private String icon = "";
    private String markerColor = "blue";
    private String highlightColor = "cyan";
    private String shape = "circle";
    private String extraClasses = "";
    private String prefix = "fa";
    private String iconColor = "white";
    private int iconRotate = 0;
    private String number = "";
    private String highlightIcon = "";
    private boolean useDefault = false;
    private boolean svg = false;
    private boolean shadow = true;
    private MarkerType type = MarkerType.EXTRA_MARKERS;
    private String className = "";

    /**
     * Enumerates the supported Leaflet marker rendering strategies: default Leaflet markers,
     * ExtraMarkers plugin markers, and DivIcon-based markers.
     */
    public enum MarkerType {
        DEFAULT("default"),
        EXTRA_MARKERS("ExtraMarkers"),
        DIV_ICON("DivIcon");

        private final String name;

        private MarkerType(String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }

        public static MarkerType getTypeByName(String name) {
            for (MarkerType type : MarkerType.values()) {
                if (name != null && type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }

    }

    public GeoMapMarker(String name) {
        this.name = name;
    }

    public GeoMapMarker() {
    }

    
    public String getIcon() {
        return icon;
    }

    
    public void setIcon(String icon) {
        this.icon = icon;
    }

    
    public String getMarkerColor() {
        return markerColor;
    }

    
    public void setMarkerColor(String markerColor) {
        this.markerColor = markerColor;
    }

    
    public String getShape() {
        return shape;
    }

    
    public void setShape(String shape) {
        this.shape = shape;
    }

    
    public String getExtraClasses() {
        return extraClasses;
    }

    
    public void setExtraClasses(String extraClasses) {
        this.extraClasses = extraClasses;
    }

    
    public String getPrefix() {
        return prefix;
    }

    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    
    public String getIconColor() {
        return iconColor;
    }

    
    public void setIconColor(String iconColor) {
        this.iconColor = iconColor;
    }

    
    public int getIconRotate() {
        return iconRotate;
    }

    
    public void setIconRotate(int iconRotation) {
        this.iconRotate = iconRotation;
    }

    
    public String getNumber() {
        return number;
    }

    
    public void setNumber(String number) {
        this.number = number;
    }

    
    public boolean isSvg() {
        return svg;
    }

    
    public void setSvg(boolean svg) {
        this.svg = svg;
    }

    
    public String getName() {
        return name;
    }

    
    public void setName(String name) {
        this.name = name;
    }

    
    public boolean isShadow() {
        return shadow;
    }

    
    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    
    public String getHighlightColor() {
        return highlightColor;
    }

    
    public void setHighlightColor(String highlightColor) {
        this.highlightColor = highlightColor;
    }

    public String toJSONString() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static GeoMapMarker fromJSONString(String json) throws JsonMappingException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, GeoMapMarker.class);
    }

    
    public void setUseDefault(boolean useDefault) {
        this.useDefault = useDefault;
    }

    
    public boolean isUseDefault() {
        return useDefault;
    }

    
    public void setHighlightIcon(String highlightIcon) {
        this.highlightIcon = highlightIcon;
    }

    
    public String getHighlightIcon() {
        return highlightIcon;
    }

    public MarkerType getType() {
        return type;
    }

    public void setType(MarkerType type) {
        this.type = type;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String cssClass) {
        this.className = cssClass;
    }
}
