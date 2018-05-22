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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;

import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.messages.ViewerResourceBundle;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * Interface to access both single and multi language metadata. Both implementations of this interface contain simple Strings representing the
 * metadata value. In the case of SimpleMetadataValue there is only a single String, MultiLanguageMetadataValue on the other hand can include a number
 * of different value Strings for different languages, additionall to a default value which is either a String not assciated with a language or the
 * first language bound value to be set
 * 
 * @author Florian Alpers
 *
 */
public interface IMetadataValue {

    /**
     * Set the value for a specific locale
     * 
     * @param value
     * @param locale
     */
    public void setValue(String value, Locale locale);

    /**
     * Set the value for a specific locale
     * 
     * @param value
     * @param locale
     */
    public void setValue(String value, String locale);

    /**
     * Set the value for the default locale {@code _DEFAULT} or the first available locale if no default locale exists
     * 
     * @param value
     * @param locale
     */
    public void setValue(String value);

    /**
     * Get the value for a specific locale
     * 
     * @param value
     * @param locale
     */
    public Optional<String> getValue(Locale language);

    /**
     * Get the value for a specific locale
     * 
     * @param value
     * @param locale
     */
    public Optional<String> getValue(String language);

    /**
     * Get the value for the default locale {@code _DEFAULT}
     * 
     * @param value
     * @param locale
     */
    public Optional<String> getValue();

    /**
     * @return A collection of all languages for which values exist. If no language specific values exist, only {@code _DEFAULT} is returned
     */
    public Collection<String> getLanguages();

    /**
     * Prepend the given string to all values
     *
     * @param prefix
     */
    public void addPrefix(String prefix);

    /**
     * Append the given string to all values
     *
     * @param prefix
     */
    public void addSuffix(String suffix);

    /**
     * Sets each value to the result of the given {@link UnaryOpeator} with the original value as input parameter
     * 
     * @param function
     */
    public void mapEach(UnaryOperator<String> function);
    
    /**
     * Removed the translation of the given locale from the values
     * 
     * @param locale
     */
    public void removeTranslation(String locale);

    /**
     * @return true if no values are stored in this object
     */
    public boolean isEmpty();

    /**
     * @param locale
     * @return true if no entry is set for the given locale
     */
    public boolean isEmpty(Locale locale);

    /**
     * @param locale
     * @return true if no entry is set for the given locale
     */
    public boolean isEmpty(String locale);

    /**
     * Returns a Multilanguage metadata value containing all found translations for the {@code key}, or the key itself if not translations were found
     * 
     * @param key the message key
     * @return A Multilanguage metadata value containing all found translations for the {@code key}, or the key itself if not translations were found
     */
    public static IMetadataValue getTranslations(String key) {
        Map<String, String> translations = new HashMap<>();
        if (ViewerResourceBundle.getAllLocales() != null) {
            for (Locale locale : ViewerResourceBundle.getAllLocales()) {
                String translation = ViewerResourceBundle.getTranslation(key, locale, false);
                if (!key.equals(translation) && StringUtils.isNotBlank(translation)) {
                    translations.put(locale.getLanguage(), translation);
                }
            }
        }
        if (translations.isEmpty()) {
            return new SimpleMetadataValue(key);
        } else {
            return new MultiLanguageMetadataValue(translations);
        }
    }

    public static Optional<IMetadataValue> getTranslations(String fieldName, SolrDocument doc) {
        Map<String, List<String>> translations = SolrSearchIndex.getMetadataValuesForLanguage(doc, fieldName);
        if (translations.size() > 1) {
            return Optional.of(new MultiLanguageMetadataValue(translations));
        } else if(translations.size() == 1){
            return Optional.of(getTranslations(translations.values().iterator().next().stream().findFirst().orElse("")));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<IMetadataValue> getTranslations(String fieldName, StructElement doc, BinaryOperator<String> combiner) {

        Map<String, List<String>> translations = SolrSearchIndex.getMetadataValuesForLanguage(doc, fieldName);
        if (translations.size() > 1) {
            return Optional.of(new MultiLanguageMetadataValue(translations, combiner));
        } else if (!translations.isEmpty()) {
            return Optional.ofNullable(getTranslations(translations.values().iterator().next().stream().reduce((s1, s2) -> combiner.apply(s1, s2)).orElse("")));
        } else {
            return Optional.empty();
        }
    }
}
