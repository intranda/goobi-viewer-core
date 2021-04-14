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
package io.goobi.viewer.model.translations;

import java.util.List;

import io.goobi.viewer.model.translations.TranslationGroup.TranslationGroupType;

public abstract class TranslationGroupKey {

    protected final String key;
    protected final boolean regex;
    protected List<String> values;

    /**
     * Factory method.
     * 
     * @param type
     * @param key
     * @param regex
     * @return
     */
    public static TranslationGroupKey create(TranslationGroupType type, String key, boolean regex) {
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }

        switch (type) {
            case SOLR_FIELD_NAMES:
                return new SolrFieldValueTranslationGroupKey(key, regex);
            case SOLR_FIELD_VALUES:
                return new SolrFieldValueTranslationGroupKey(key, regex);
            default:
                return new MessagesTranslationGroupKey(key, regex);
        }
    }

    /**
     * Protected constructor.
     * 
     * @param key
     * @param regex
     */
    protected TranslationGroupKey(String key, boolean regex) {
        this.key = key;
        this.regex = regex;
    }

    public boolean isTranslated() {
        return false; // TODO
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the regex
     */
    public boolean isRegex() {
        return regex;
    }

    /**
     * @return the values
     */
    public List<String> getValues() {
        if (values == null) {
            loadValues();
        }

        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(List<String> values) {
        this.values = values;
    }

    protected abstract void loadValues();
}
