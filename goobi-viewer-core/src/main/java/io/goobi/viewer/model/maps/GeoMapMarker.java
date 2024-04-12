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
 * @author florian
 *
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

    /**
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * @return the markerColor
     */
    public String getMarkerColor() {
        return markerColor;
    }

    /**
     * @param markerColor the markerColor to set
     */
    public void setMarkerColor(String markerColor) {
        this.markerColor = markerColor;
    }

    /**
     * @return the shape
     */
    public String getShape() {
        return shape;
    }

    /**
     * @param shape the shape to set
     */
    public void setShape(String shape) {
        this.shape = shape;
    }

    /**
     * @return the extraClasses
     */
    public String getExtraClasses() {
        return extraClasses;
    }

    /**
     * @param extraClasses the extraClasses to set
     */
    public void setExtraClasses(String extraClasses) {
        this.extraClasses = extraClasses;
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @return the iconColor
     */
    public String getIconColor() {
        return iconColor;
    }

    /**
     * @param iconColor the iconColor to set
     */
    public void setIconColor(String iconColor) {
        this.iconColor = iconColor;
    }

    /**
     * @return the iconRotation
     */
    public int getIconRotate() {
        return iconRotate;
    }

    /**
     * @param iconRotation the iconRotation to set
     */
    public void setIconRotate(int iconRotation) {
        this.iconRotate = iconRotation;
    }

    /**
     * @return the number
     */
    public String getNumber() {
        return number;
    }

    /**
     * @param number the number to set
     */
    public void setNumber(String number) {
        this.number = number;
    }

    /**
     * @return the svg
     */
    public boolean isSvg() {
        return svg;
    }

    /**
     * @param svg the svg to set
     */
    public void setSvg(boolean svg) {
        this.svg = svg;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the shadow
     */
    public boolean isShadow() {
        return shadow;
    }

    /**
     * @param shadow the shadow to set
     */
    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    /**
     * @return the highlightColor
     */
    public String getHighlightColor() {
        return highlightColor;
    }

    /**
     * @param highlightColor the highlightColor to set
     */
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

    /**
     * @param useDefault the useDefault to set
     */
    public void setUseDefault(boolean useDefault) {
        this.useDefault = useDefault;
    }

    /**
     * @return the useDefault
     */
    public boolean isUseDefault() {
        return useDefault;
    }

    /**
     * @param highlightIcon the highlightIcon to set
     */
    public void setHighlightIcon(String highlightIcon) {
        this.highlightIcon = highlightIcon;
    }

    /**
     * @return the highlightIcon
     */
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
