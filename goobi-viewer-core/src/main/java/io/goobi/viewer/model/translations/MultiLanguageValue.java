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
package io.goobi.viewer.model.translations;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.weld.exceptions.IllegalArgumentException;

import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * @author florian
 *
 */
public class MultiLanguageValue implements IPolyglott {

    private Locale selectedLocale = BeanUtils.getLocale();

    private final String label;
    private final Map<Locale, Translation> translations;

    public MultiLanguageValue(String label, Locale initialLocale, Collection<Locale> locales) {
        this.label = label;
        this.selectedLocale = initialLocale;
        this.translations = locales.stream().collect(Collectors.toMap(l -> l, l -> new Translation(l.getLanguage(), this.label, "")));

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#isComplete(java.util.Locale)
     */
    @Override
    public boolean isComplete(Locale locale) {
        return !this.translations.get(locale).isEmpty();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#isValid(java.util.Locale)
     */
    @Override
    public boolean isValid(Locale locale) {
        return isComplete(locale);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#getSelectedLocale()
     */
    @Override
    public Locale getSelectedLocale() {
        return this.selectedLocale;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#setSelectedLocale(java.util.Locale)
     */
    @Override
    public void setSelectedLocale(Locale locale) {
        this.selectedLocale = locale;
    }

    /**
     *
     * @return the translation for the selected locale. Create a new translation if none exists
     */
    public Translation getSelectedTranslation() {
        return getTranslation(this.selectedLocale);
    }

    /**
     *
     * @param locale
     * @return the translation for the given language if one exists
     */
    public Translation getTranslation(Locale locale) {
        Translation t = translations.get(locale);
        if (t == null) {
            throw new IllegalArgumentException("Invalid locale for translation '" + locale + "'");
        }
        return t;

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#getLocales()
     */
    @Override
    public Collection<Locale> getLocales() {
        return translations.keySet();
    }

    public void setValue(String value) {
        getSelectedTranslation().setValue(value);
    }

    public String getValue() {
        return getSelectedTranslation().getValue();
    }

    public void setValueForLocale(Locale locale, String value) {
        this.getTranslation(locale).setValue(value);
    }

    public String getValueForLocale(Locale locale) {
        return this.getTranslation(locale).getValue();
    }

    public Stream<Translation> stream() {
        return this.translations.values().stream().filter(t -> !t.isEmpty());
    }

    public Map<Locale, String> map() {
        return this.translations.entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getValue()));
    }

    /**
     * @param locale
     * @return true if this list has an entry for the given locale
     */
    public boolean hasLocale(Locale locale) {
        return locale != null && this.translations.containsKey(locale);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#isEmpty(java.util.Locale)
     */
    @Override
    public boolean isEmpty(Locale locale) {
        return this.translations.get(locale).isEmpty();
    }

}
