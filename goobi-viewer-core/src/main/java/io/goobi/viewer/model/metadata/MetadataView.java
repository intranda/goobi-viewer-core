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
package io.goobi.viewer.model.metadata;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.model.viewer.StructElement;

/**
 * A MetadataView represents a single record metadata view page within a record (along with an own link in the views widget).
 */
public class MetadataView {

    public enum MetadataViewLocation {
        OBJECTVIEW,
        SIDEBAR;

        public static MetadataViewLocation getByName(String name) {
            if (OBJECTVIEW.name().equalsIgnoreCase(name)) {
                return OBJECTVIEW;
            }
            return SIDEBAR;
        }
    }

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(MetadataView.class);

    /** Metadata view index. The first entry implies the value 0, all subsequent entries must provide an index value. */
    private int index = 0;
    /** Optional label for the link and the metadata page title. */
    private String label;
    /** URL schema suffix for the metadata page (/metadata<url>/). */
    private String url = "";
    /** Optional condition for link display. May contain a Solr field name or name:value pair. */
    private String condition;
    /** Display location for the metadata. */
    private MetadataViewLocation location = MetadataViewLocation.SIDEBAR;

    /**
     * Checks link visibility conditions.
     *
     * @param se
     * @return true if conditions empty or satisfied; false otherwise
     * @should return true if condition null or empty
     * @should return false if struct element null
     * @should return true if field value pair found
     * @should return false if field value pair not found
     * @should return true if field name found
     * @should return false if field name not found
     */
    public boolean isVisible(StructElement se) {
        if (StringUtils.isBlank(condition)) {
            return true;
        }
        if (se == null) {
            return false;
        }

        // Field and value
        if (condition.contains(":")) {
            String[] conditionSplit = condition.split(":");
            switch (conditionSplit.length) {
                case 2:
                    List<String> values = se.getMetadataValues(conditionSplit[0]);
                    return values.contains(conditionSplit[1]);
                default:
                    logger.warn("Incorrect condition '{}'. Condition must be a Solr field or field:value pair.", condition);
                    return false;
            }

        }

        // Just field name
        return se.getMetadataValue(condition) != null;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     * @return this
     */
    public MetadataView setIndex(int index) {
        this.index = index;
        return this;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     * @return this
     */
    public MetadataView setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     * @return this
     */
    public MetadataView setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * @return the condition
     */
    public String getCondition() {
        return condition;
    }

    /**
     * @param condition the condition to set
     * @return this
     */
    public MetadataView setCondition(String condition) {
        this.condition = condition;
        return this;
    }

    /**
     * @return the location
     */
    public MetadataViewLocation getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     * @return this
     */
    public MetadataView setLocation(MetadataViewLocation location) {
        this.location = location;
        return this;
    }
}
