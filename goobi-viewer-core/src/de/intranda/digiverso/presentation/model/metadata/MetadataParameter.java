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
package de.intranda.digiverso.presentation.model.metadata;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataParameter implements Serializable {

    public enum MetadataParameterType {

        FIELD("field"),
        TRANSLATEDFIELD("translatedfield"),
        WIKIFIELD("wikifield"),
        WIKIPERSONFIELD("wikipersonfield"),
        LINK_MAPS("linkMaps"),
        TOPSTRUCTFIELD("topstructfield"),
        ANCHORFIELD("anchorfield"),
        UNESCAPEDFIELD("unescapedfield"),
        URLESCAPEDFIELD("urlescapedfield"),
        HIERARCHICALFIELD("hierarchicalfield"),
        NORMDATAURI("normdatauri"),
        /**
         * Same as field but needs to be queried for all indexed languages
         */
        MULTILANGUAGEFIELD("multilanguagefield");

        private static final Logger logger = LoggerFactory.getLogger(MetadataParameterType.class);

        private final String key;

        private MetadataParameterType(String key) {
            this.key = key;
        }

        public static MetadataParameterType getByKey(String key) {
            for (MetadataParameterType o : MetadataParameterType.values()) {
                if (o.getKey().equals(key)) {
                    return o;
                }
            }
            return null;
        }

        public String getKey() {
            return key;
        }

        public static MetadataParameterType getByString(String value) {
            if (value != null) {
                MetadataParameterType type = getByKey(value);
                if (type == null) {
                    logger.error("Metadata parameter type not found, please check configuration: {}", value);
                    return FIELD;
                }
                return type;
            }

            return FIELD;
        }
    }

    private static final long serialVersionUID = 8010945394600637854L;

    private MetadataParameterType type;
    private final String source;
    private final String key;
    private final String overrideMasterValue;
    private final String defaultValue;
    private final String prefix;
    private final String suffix;
    private final boolean addUrl;
    private final boolean dontUseTopstructValue;

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (key == null ? 0 : key.hashCode());
        result = prime * result + (type == null ? 0 : type.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MetadataParameter other = (MetadataParameter) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }

    /**
     * 
     * @param type
     * @param source
     * @param key
     * @param overrideMasterValue
     * @param defaultValue
     * @param prefix
     * @param suffix
     * @param addUrl
     * @param dontUseTopstructValue
     */
    public MetadataParameter(MetadataParameterType type, String source, String key, String overrideMasterValue, String defaultValue, String prefix,
            String suffix, boolean addUrl, boolean dontUseTopstructValue) {
        this.type = type;
        this.source = source;
        this.key = key;
        this.overrideMasterValue = overrideMasterValue;
        this.defaultValue = defaultValue;
        this.prefix = prefix;
        this.suffix = suffix;
        this.addUrl = addUrl;
        this.dontUseTopstructValue = dontUseTopstructValue;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @return the type
     */
    public MetadataParameterType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(MetadataParameterType type) {
        this.type = type;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the overrideMasterValue
     */
    public String getOverrideMasterValue() {
        return overrideMasterValue;
    }

    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return the suffix
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * @return the addUrl
     */
    public boolean isAddUrl() {
        return addUrl;
    }

    /**
     * @return the dontUseTopstructValue
     */
    public boolean isDontUseTopstructValue() {
        return dontUseTopstructValue;
    }

    @Override
    public String toString() {
        return new StringBuilder("Key: ").append(key)
                .append("; Type: ")
                .append(type)
                .append("; prefix: ")
                .append(prefix)
                .append("; suffix: ")
                .append(suffix)
                .toString();
    }
}
