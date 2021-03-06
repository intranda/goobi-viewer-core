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
package io.goobi.viewer.model.metadata;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * MetadataParameter class.
 * </p>
 */
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
        MILLISFIELD("millisfield"),
        NORMDATAURI("normdatauri"),
        NORMDATASEARCH("normdatasearch"),
        CITEPROC("citeproc");

        private static final Logger logger = LoggerFactory.getLogger(MetadataParameterType.class);

        private final String key;

        private MetadataParameterType(String key) {
            this.key = key;
        }

        public static MetadataParameterType getByKey(String key) {
            for (MetadataParameterType o : MetadataParameterType.values()) {
                if (o.getKey().equals(key) || FIELD.equals(o) && "multilanguagefield".equals(key)) {
                    return o;
                }
            }
            return null;
        }

        public String getKey() {
            return key;
        }

        public static MetadataParameterType getByString(String value) {
            if (value == null) {
                return FIELD;
            }

            MetadataParameterType type = getByKey(value);
            if (type == null) {
                logger.error("Metadata parameter type not found, please check configuration: {}", value);
                return FIELD;
            }
            return type;
        }
    }

    private static final long serialVersionUID = 8010945394600637854L;

    private MetadataParameterType type;
    private String source;
    private String destination;
    private String key;
    private String altKey;
    private String masterValueFragment;
    private String defaultValue;
    private String prefix;
    private String suffix;
    private String condition = "";
    private boolean addUrl = false;
    private boolean topstructValueFallback = false;
    private boolean topstructOnly = false;
    private List<MetadataReplaceRule> replaceRules = Collections.emptyList();

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
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
    /** {@inheritDoc} */
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
     * <p>
     * Getter for the field <code>source</code>.
     * </p>
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     * @return this
     */
    public MetadataParameter setSource(String source) {
        this.source = source;
        return this;
    }

    /**
     * @return the destination
     */
    public String getDestination() {
        return destination;
    }

    /**
     * @param destination the destination to set
     * @return this
     */
    public MetadataParameter setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     *
     * @return the type
     */
    public MetadataParameterType getType() {
        return type;
    }

    /**
     * <p>
     * Setter for the field <code>type</code>.
     * </p>
     *
     * @param type the type to set
     * @return this
     */
    public MetadataParameter setType(MetadataParameterType type) {
        this.type = type;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>key</code>.
     * </p>
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     * @return this
     */
    public MetadataParameter setKey(String key) {
        this.key = key;
        return this;
    }

    /**
     * @return the altKey
     */
    public String getAltKey() {
        return altKey;
    }

    /**
     * @param altKey the altKey to set
     * @return this
     */
    public MetadataParameter setAltKey(String altKey) {
        this.altKey = altKey;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>masterValueFragment</code>.
     * </p>
     *
     * @return the masterValueFragment
     */
    public String getMasterValueFragment() {
        return masterValueFragment;
    }

    /**
     * @param masterValueFragment the masterValueFragment to set
     * @return this
     */
    public MetadataParameter setMasterValueFragment(String masterValueFragment) {
        this.masterValueFragment = masterValueFragment;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>defaultValue</code>.
     * </p>
     *
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue the defaultValue to set
     * @return this
     */
    public MetadataParameter setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>prefix</code>.
     * </p>
     *
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param prefix the prefix to set
     * @return this
     */
    public MetadataParameter setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>suffix</code>.
     * </p>
     *
     * @return the suffix
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * @param suffix the suffix to set
     * @return this
     */
    public MetadataParameter setSuffix(String suffix) {
        this.suffix = suffix;
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
    public MetadataParameter setCondition(String condition) {
        this.condition = condition;
        return this;
    }

    /**
     * <p>
     * isAddUrl.
     * </p>
     *
     * @return the addUrl
     */
    public boolean isAddUrl() {
        return addUrl;
    }

    /**
     * @param addUrl the addUrl to set
     * @return this
     */
    public MetadataParameter setAddUrl(boolean addUrl) {
        this.addUrl = addUrl;
        return this;
    }

    /**
     * <p>
     * isTopstructValueFallback.
     * </p>
     *
     * @return the dontUseTopstructValue
     */
    public boolean isTopstructValueFallback() {
        return topstructValueFallback;
    }

    /**
     * @param topstructValueFallback the topstructValueFallback to set
     * @return this
     */
    public MetadataParameter setTopstructValueFallback(boolean topstructValueFallback) {
        this.topstructValueFallback = topstructValueFallback;
        return this;
    }

    /**
     * <p>
     * isTopstructOnly.
     * </p>
     *
     * @return the topstructOnly
     */
    public boolean isTopstructOnly() {
        return topstructOnly;
    }

    /**
     * @param topstructOnly the topstructOnly to set
     * @return this
     */
    public MetadataParameter setTopstructOnly(boolean topstructOnly) {
        this.topstructOnly = topstructOnly;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>replaceRules</code>.
     * </p>
     *
     * @return the replaceRules
     */
    public List<MetadataReplaceRule> getReplaceRules() {
        return replaceRules;
    }

    /**
     * @param replaceRules the replaceRules to set
     * @return tis
     */
    public MetadataParameter setReplaceRules(List<MetadataReplaceRule> replaceRules) {
        this.replaceRules = replaceRules;
        return this;
    }

    /** {@inheritDoc} */
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
