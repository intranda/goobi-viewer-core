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

/**
 * <p>
 * MetadataReplaceRule class.
 * </p>
 */
public class MetadataReplaceRule implements Serializable {

    private static final long serialVersionUID = 8010945394600637854L;

    public enum MetadataReplaceRuleType {
        CHAR,
        STRING,
        REGEX
    }

    private final MetadataReplaceRuleType type;
    private final Object key;
    private final String replacement;
    private final String conditions;

    /**
     * 
     * @param key
     * @param replacement
     * @param type
     */
    public MetadataReplaceRule(Object key, String replacement, MetadataReplaceRuleType type) {
        this(key, replacement, null, type);
    }

    /**
     * 
     * @param key
     * @param replacement
     * @param conditions Optional condition Solr query
     * @param type
     */
    public MetadataReplaceRule(Object key, String replacement, String conditions, MetadataReplaceRuleType type) {
        this.key = key;
        this.replacement = replacement;
        this.conditions = conditions;
        this.type = type;
    }

    /**
     * @return the type
     */
    public MetadataReplaceRuleType getType() {
        return type;
    }

    /**
     * @return the key
     */
    public Object getKey() {
        return key;
    }

    /**
     * @return the replacement
     */
    public String getReplacement() {
        return replacement;
    }

    /**
     * @return the conditions
     */
    public String getConditions() {
        return conditions;
    }
}
