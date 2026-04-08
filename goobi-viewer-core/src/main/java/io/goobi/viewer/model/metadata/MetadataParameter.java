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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.model.metadata.MetadataReplaceRule.MetadataReplaceRuleType;

/**
 * Defines a single parameter within a metadata field configuration, specifying the Solr field and display options.
 */
public class MetadataParameter implements Serializable {

    /**
     * Needed for reading xml config.
     */
    private static final String XML_PATH_ATTRIBUTE_CONDITION = "[@condition]";
    /**
     * Needed for reading xml config.
     */
    private static final String XML_PATH_ATTRIBUTE_TYPE = "[@type]";
    /**
     * Needed for reading xml config.
     */
    private static final String XML_PATH_ATTRIBUTE_URL = "[@url]";

    public enum MetadataParameterType {

        FIELD("field"),
        TRANSLATEDFIELD("translatedfield"),
        DATEFIELD("datefield"),
        DATETIMEFIELD("datetimefield"),
        IDENTIFIER("identifier"),
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
        CITEPROC("citeproc"),
        RELATEDFIELD("related");

        private static final Logger logger = LogManager.getLogger(MetadataParameterType.class);

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
    private String inputPattern = "";
    private String outputPattern = "";
    private boolean addUrl = false;
    private boolean topstructValueFallback = false;
    private boolean removeHighlighting = false;
    private List<MetadataReplaceRule> replaceRules = Collections.emptyList();
    /**
     * Optional parameter which signals that the values should come from some other document, either "parent" (data comes from direct parent document)
     * or "topStruct" (data comes from the main record document).
     */
    private String scope = "";

    public MetadataParameter() {

    }

    public MetadataParameter(MetadataParameter orig) {
        this.type = orig.type;
        this.source = orig.source;
        this.destination = orig.destination;
        this.key = orig.key;
        this.altKey = orig.altKey;
        this.masterValueFragment = orig.masterValueFragment;
        this.defaultValue = orig.defaultValue;
        this.prefix = orig.prefix;
        this.suffix = orig.prefix;
        this.condition = orig.condition;
        this.inputPattern = orig.inputPattern;
        this.outputPattern = orig.outputPattern;
        this.addUrl = orig.addUrl;
        this.topstructValueFallback = orig.topstructValueFallback;
        this.removeHighlighting = orig.removeHighlighting;
        this.replaceRules = new ArrayList<>(orig.replaceRules);
        this.scope = orig.scope;
    }

    public MetadataParameter(MetadataParameterType type, String key) {
        this.type = type;
        this.key = key;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (key == null ? 0 : key.hashCode());
        result = prime * result + (type == null ? 0 : type.hashCode());
        return result;
    }

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

        return type == other.type;
    }

    /**
     * Getter for the field <code>source</code>.
     *
     * @return the Solr field name from which this parameter's value is read
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the Solr field name from which this parameter's value is read
     * @return this
     */
    public MetadataParameter setSource(String source) {
        this.source = source;
        return this;
    }

    
    public String getDestination() {
        return destination;
    }

    /**
     * @param destination the target placeholder name in the master value template where this parameter's value is inserted
     * @return this
     */
    public MetadataParameter setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    /**
     * Getter for the field <code>type</code>.
     *
     * @return the parameter type controlling how the value is retrieved and rendered
     */
    public MetadataParameterType getType() {
        return type;
    }

    /**
     * Setter for the field <code>type</code>.
     *
     * @param type the parameter type controlling how the value is retrieved and rendered
     * @return this
     */
    public MetadataParameter setType(MetadataParameterType type) {
        this.type = type;
        return this;
    }

    /**
     * Getter for the field <code>key</code>.
     *
     * @return the Solr field name used as the primary key for value lookup
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the Solr field name used as the primary key for value lookup
     * @return this
     */
    public MetadataParameter setKey(String key) {
        this.key = key;
        return this;
    }

    
    public String getAltKey() {
        return altKey;
    }

    /**
     * @param altKey the alternative Solr field name used as a fallback when the primary key yields no value
     * @return this
     */
    public MetadataParameter setAltKey(String altKey) {
        this.altKey = altKey;
        return this;
    }

    /**
     * Getter for the field <code>masterValueFragment</code>.
     *
     * @return the message key referencing a fragment of the master value template for this parameter
     */
    public String getMasterValueFragment() {
        return masterValueFragment;
    }

    /**
     * @param masterValueFragment the message key referencing a fragment of the master value template for this parameter
     * @return this
     */
    public MetadataParameter setMasterValueFragment(String masterValueFragment) {
        this.masterValueFragment = masterValueFragment;
        return this;
    }

    /**
     * Getter for the field <code>defaultValue</code>.
     *
     * @return the fallback value used when no value is found in the index
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue the fallback value to use when no value is found in the index
     * @return this
     */
    public MetadataParameter setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Getter for the field <code>prefix</code>.
     *
     * @return the string prepended to the rendered value in the output
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param prefix the string prepended to the rendered value in the output
     * @return this
     */
    public MetadataParameter setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Getter for the field <code>suffix</code>.
     *
     * @return the string appended to the rendered value in the output
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * @param suffix the string appended to the rendered value in the output
     * @return this
     */
    public MetadataParameter setSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public MetadataParameter setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public String getScope() {
        return scope;
    }

    
    public String getCondition() {
        return condition;
    }

    /**
     * @param condition the Solr-query-style condition that must be satisfied for this parameter to be rendered
     * @return this
     */
    public MetadataParameter setCondition(String condition) {
        this.condition = condition;
        return this;
    }

    
    public String getInputPattern() {
        return inputPattern;
    }

    /**
     * @param inputPattern the regular expression pattern applied to the raw input value before transformation
     * @return this
     */
    public MetadataParameter setInputPattern(String inputPattern) {
        this.inputPattern = inputPattern;
        return this;
    }

    
    public String getOutputPattern() {
        return outputPattern;
    }

    /**
     * @param outputPattern the replacement pattern applied to the value after input-pattern matching
     * @return this
     */
    public MetadataParameter setOutputPattern(String outputPattern) {
        this.outputPattern = outputPattern;
        return this;
    }

    /**
     * isAddUrl.
     *
     * @return true if a hyperlink URL should be generated and attached to this parameter's rendered value, false otherwise
     */
    public boolean isAddUrl() {
        return addUrl;
    }

    /**
     * @param addUrl true if a hyperlink URL should be generated and attached to this parameter's rendered value
     * @return this
     */
    public MetadataParameter setAddUrl(boolean addUrl) {
        this.addUrl = addUrl;
        return this;
    }

    /**
     * isTopstructValueFallback.
     *
     * @return true if the top-level structure element's value should be used as a fallback when no value is found, false otherwise
     */
    public boolean isTopstructValueFallback() {
        return topstructValueFallback;
    }

    /**
     * @param topstructValueFallback true if the top-level structure element's value should be used as a fallback when no value is found
     * @return this
     */
    public MetadataParameter setTopstructValueFallback(boolean topstructValueFallback) {
        this.topstructValueFallback = topstructValueFallback;
        return this;
    }

    
    public boolean isRemoveHighlighting() {
        return removeHighlighting;
    }

    /**
     * @param removeHighlighting true if search-term highlighting markup should be stripped from the rendered value
     * @return this
     */
    public MetadataParameter setRemoveHighlighting(boolean removeHighlighting) {
        this.removeHighlighting = removeHighlighting;
        return this;
    }

    /**
     * Getter for the field <code>replaceRules</code>.
     *
     * @return the list of find-and-replace rules applied to the value during rendering
     */
    public List<MetadataReplaceRule> getReplaceRules() {
        return replaceRules;
    }

    /**
     * @param replaceRules the list of find-and-replace rules applied to the value during rendering
     * @return {@link MetadataParameter}
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

    /**
     * 
     * @param config the hierarchical configuration node to read from
     * @param topstructValueFallbackDefaultValue whether to fall back to topstruct value as default
     * @return {@link MetadataParameter}
     */
    public static MetadataParameter createFromConfig(HierarchicalConfiguration<ImmutableNode> config, boolean topstructValueFallbackDefaultValue) {
        String fieldType = config.getString(XML_PATH_ATTRIBUTE_TYPE);
        String source = config.getString("[@source]", null);
        String dest = config.getString("[@dest]", null);
        String key = config.getString("[@key]");
        String altKey = config.getString("[@altKey]");
        String masterValueFragment = config.getString("[@value]");
        String defaultValue = config.getString("[@defaultValue]");
        String prefix = config.getString("[@prefix]", "").replace("_SPACE_", " ");
        String suffix = config.getString("[@suffix]", "").replace("_SPACE_", " ");
        String condition = config.getString(XML_PATH_ATTRIBUTE_CONDITION);
        String inputPattern = config.getString("[@inputPattern]");
        String outputPattern = config.getString("[@pattern]");
        String scope = config.getString("[@scope]", "");

        boolean addUrl = config.getBoolean(XML_PATH_ATTRIBUTE_URL, false);
        boolean topstructValueFallback = config.getBoolean("[@topstructValueFallback]", topstructValueFallbackDefaultValue);
        boolean removeHighlighting = config.getBoolean("[@removeHighlighting]", false);
        List<MetadataReplaceRule> replaceRules = Collections.emptyList();
        List<HierarchicalConfiguration<ImmutableNode>> replaceRuleElements = config.configurationsAt("replace");
        if (replaceRuleElements != null) {
            // Replacement rules can be applied to a character, a string or a regex
            replaceRules = new ArrayList<>(replaceRuleElements.size());
            for (Iterator<HierarchicalConfiguration<ImmutableNode>> it3 = replaceRuleElements.iterator(); it3.hasNext();) {
                HierarchicalConfiguration<ImmutableNode> sub3 = it3.next();
                String replaceCondition = sub3.getString(XML_PATH_ATTRIBUTE_CONDITION);
                Character character = null;
                try {
                    int charIndex = sub3.getInt("[@char]");
                    character = (char) charIndex;
                } catch (NoSuchElementException e) {
                    //
                }
                String string = null;
                try {
                    string = sub3.getString("[@string]");
                } catch (NoSuchElementException e) {
                    //
                }
                String regex = null;
                try {
                    regex = sub3.getString("[@regex]");
                } catch (NoSuchElementException e) {
                    //
                }
                String replaceWith = sub3.getString("");
                if (replaceWith == null) {
                    replaceWith = "";
                }
                if (character != null) {
                    replaceRules.add(new MetadataReplaceRule(character, replaceWith, replaceCondition, MetadataReplaceRuleType.CHAR));
                } else if (string != null) {
                    replaceRules.add(new MetadataReplaceRule(string, replaceWith, replaceCondition, MetadataReplaceRuleType.STRING));
                } else if (regex != null) {
                    replaceRules.add(new MetadataReplaceRule(regex, replaceWith, replaceCondition, MetadataReplaceRuleType.REGEX));
                }
            }
        }

        return new MetadataParameter().setType(MetadataParameterType.getByString(fieldType))
                .setSource(source)
                .setDestination(dest)
                .setKey(key)
                .setAltKey(altKey)
                .setMasterValueFragment(masterValueFragment)
                .setDefaultValue(defaultValue)
                .setPrefix(prefix)
                .setSuffix(suffix)
                .setCondition(condition)
                .setInputPattern(inputPattern)
                .setOutputPattern(outputPattern)
                .setAddUrl(addUrl)
                .setTopstructValueFallback(topstructValueFallback)
                .setRemoveHighlighting(removeHighlighting)
                .setScope(scope)
                .setReplaceRules(replaceRules);
    }
}
