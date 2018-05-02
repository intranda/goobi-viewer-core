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
package de.intranda.digiverso.presentation.model.metadata.multilanguage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.h2.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Florian Alpers
 *
 */
public class MultiLanguageMetadataValue implements IMetadataValue {

    public static final String DEFAULT_LANGUAGE = "_DEFAULT";

    private final Map<String, String> values;

    public MultiLanguageMetadataValue() {
        values = new HashMap<>();

    }

    /**
     * Creates values from a map with the language codes as keys and values which are either Strings or a list of Strings. In the latter case, the
     * first entry in the list is taken as value
     * 
     * @param metadataValuesForLanguage
     */
    public MultiLanguageMetadataValue(Map<String, ? extends Object> metadataValuesForLanguage) {
        this(metadataValuesForLanguage, 0);
    }

    /**
     * Creates values from a map with the language codes as keys and values which are either Strings or a list of Strings. In the latter case, the
     * entry of index {@code valueIndex} in the list is as taken as value
     * 
     * @param metadataValuesForLanguage
     */
    @SuppressWarnings("unchecked")
    public MultiLanguageMetadataValue(Map<String, ? extends Object> metadataValuesForLanguage, int valueIndex) {
        if (metadataValuesForLanguage.isEmpty()) {
            values = new HashMap<>();
        } else if (metadataValuesForLanguage.values().iterator().next() instanceof List) {
            this.values = new HashMap<>();
            for (String language : metadataValuesForLanguage.keySet()) {
                List<String> langValues = (List<String>) metadataValuesForLanguage.get(language);
                if (valueIndex < langValues.size()) {
                    this.values.put(language, langValues.get(valueIndex));
                }
            }
        } else {
            this.values = (Map<String, String>) metadataValuesForLanguage;
        }

    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#setValue(java.lang.String, java.util.Locale)
     */
    @Override
    public void setValue(String value, Locale locale) {
        this.values.put(locale.getLanguage(), value);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#getValue(java.util.Locale)
     */
    @Override
    public Optional<String> getValue(Locale language) {
        return getValue(language.getLanguage());
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#setValue(java.lang.String, java.lang.String)
     */
    @Override
    public void setValue(String value, String locale) {
        if (value == null) {
            value = "";
        }
        this.values.put(locale, value);
        if (this.values.get(DEFAULT_LANGUAGE) == null) {
            setValue(value);
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#setValue(java.lang.String)
     */
    @Override
    public void setValue(String value) {
        this.values.put(DEFAULT_LANGUAGE, value);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#getValue(java.lang.String)
     */
    @Override
    public Optional<String> getValue(String language) {
        return Optional.ofNullable(this.values.get(language));
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#getValue()
     */
    @Override
    @JsonIgnore
    public Optional<String> getValue() {
        if (this.values.containsKey(DEFAULT_LANGUAGE)) {
            return Optional.ofNullable(this.values.get(DEFAULT_LANGUAGE));
        } else if (!this.values.isEmpty()) {
            return Optional.ofNullable(this.values.values().iterator().next());
        } else {
            return Optional.empty();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (values.isEmpty()) {
            return "";
        } else {
            return values.toString();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return values.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj.getClass().equals(MultiLanguageMetadataValue.class)) {
            return ((MultiLanguageMetadataValue) obj).values.equals(this.values);
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#getLanguages()
     */
    @Override
    @JsonIgnore
    public Collection<String> getLanguages() {
        Set<String> languages = values.keySet();
        if (languages.isEmpty()) {
            return Collections.singleton(DEFAULT_LANGUAGE);
        } else {
            return languages;
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#addPrefix(java.lang.String)
     */
    @Override
    public void addPrefix(String prefix) {
        for (String lang : values.keySet()) {
            values.put(lang, prefix + values.get(lang));
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#addSuffix(java.lang.String)
     */
    @Override
    public void addSuffix(String suffix) {
        for (String lang : values.keySet()) {
            values.put(lang, values.get(lang) + suffix);
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#mapEach(java.util.function.UnaryOperator)
     */
    @Override
    public void mapEach(UnaryOperator<String> function) {
        for (String language : values.keySet()) {
            String newValue = function.apply(values.get(language));
            values.put(language, newValue);
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#isEmpty()
     */
    @Override
    @JsonIgnore
    public boolean isEmpty() {
        return values.isEmpty() || values.values().stream().noneMatch(value -> StringUtils.isNotBlank(value));
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#isEmpty(java.util.Locale)
     */
    @Override
    public boolean isEmpty(Locale locale) {
        return StringUtils.isBlank(values.get(locale.getLanguage()));
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#isEmpty(java.lang.String)
     */
    @Override
    public boolean isEmpty(String locale) {
        return StringUtils.isBlank(values.get(locale));

    }

    @JsonValue
    public List<ValuePair> getValues() {
        return this.values.entrySet().stream().map(entry -> new ValuePair(entry.getValue(), entry.getKey())).collect(Collectors.toList());
    }

    /**
     * Helper class for IIIF representation
     * 
     * @author Florian Alpers
     *
     */
    private static class ValuePair {

        private final String value;
        private final String language;

        public ValuePair(String value, String language) {
            this.value = value;
            this.language = language;
        }

        /**
         * @return the language
         */
        @JsonProperty("@language")
        public String getLanguage() {
            return language;
        }

        /**
         * @return the value
         */
        @JsonProperty("@value")
        public String getValue() {
            return value;
        }

    }

}
