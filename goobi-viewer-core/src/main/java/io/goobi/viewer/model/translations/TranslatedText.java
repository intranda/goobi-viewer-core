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

import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.api.rest.serialization.TranslatedTextSerializer;
import io.goobi.viewer.controller.JsonTools;

/**
 * Translations for some text for a set of locales. Text can be set and retrieved for each locale individually. There may be a "default language" text
 * which represent text that has not actual locale, whether it is text that has no translations or is an internal representation of the text. This
 * default language only exists if the text is initiated giving only a single text without locale or if it is explicitly added
 * 
 * @author florian
 *
 */
@JsonSerialize(using = TranslatedTextSerializer.class)
public class TranslatedText extends MultiLanguageMetadataValue implements IPolyglott, Serializable {

    private static final long serialVersionUID = -3725829912057184396L;

    /**
     * The locale that should be used when getting/setting text witout passing a particular locale
     */
    private Locale selectedLocale;

    /**
     * Create a text with the locales from {@link IPolyglott#getLocalesStatic()}
     */
    public TranslatedText() {
        this(IPolyglott.getLocalesStatic());
    }

    /**
     * Create a text with the given locales
     * 
     * @param locales
     */
    public TranslatedText(Collection<Locale> locales) {
        this(locales, IPolyglott.getDefaultLocale());
    }

    /**
     * Create a text with the given locales, setting the selected locale to the given initialLocale
     * 
     * @param locales
     * @param initalLocale
     */
    public TranslatedText(Collection<Locale> locales, Locale initalLocale) {
        super(locales.stream().collect(Collectors.toMap(Locale::getLanguage, l -> "")));
        this.selectedLocale = initalLocale;
    }

    /**
     * Create a text with the given locales, setting the selected locale to the given initialLocale. Initialize the values with the onces from the
     * given TranslatedText "orig". If a Locale contained in locales is not included in orig, its value remains blank. If a locale is contained in
     * orig, but not in locales, the value is set anyway
     * 
     * @param orig
     * @param locales
     * @param initialLocale
     */
    public TranslatedText(TranslatedText orig, Collection<Locale> locales, Locale initialLocale) {
        this(locales, initialLocale);
        orig.toMap().entrySet().forEach(entry -> {
            Locale l = entry.getKey();
            String value = entry.getValue();
            if (StringUtils.isNotBlank(value)) {
                this.setText(value, l);
            }
        });
    }

    /**
     * Create a copy of the given Translated text "orig"
     * 
     * @param orig
     */
    public TranslatedText(TranslatedText orig) {
        this(orig, IPolyglott.getLocalesStatic(), IPolyglott.getDefaultLocale());
    }

    /**
     * Create a text with the default language set to the given value
     * 
     * @param text
     */
    public TranslatedText(String text) {
        super();
        setText(text);

    }

    /**
     * Create a text using the values of the given IMetadataValue orig. If orig is a {@link SimpleMetadataValue}, its value is written to the default
     * locale. The selected locale is set to {@link IPolyglott#getCurrentLocale()}
     * 
     * @param orig
     */
    public TranslatedText(IMetadataValue orig) {
        this(orig, IPolyglott.getCurrentLocale());
    }

    /**
     * Create a text using the values of the given IMetadataValue orig. If orig is a {@link SimpleMetadataValue}, its value is written to the default
     * locale. The selected locale is set to the given initialLocale
     * 
     * @param orig
     * @param initialLocale
     */
    public TranslatedText(IMetadataValue orig, Locale initialLocale) {
        super(orig.getLanguages()
                .stream()
                .filter(lang -> orig.getValue(lang).isPresent())
                .collect(Collectors.toMap(lang -> lang, lang -> orig.getValue(lang).get())));
        this.selectedLocale = initialLocale;
    }

    /**
     * @return the {@link #selectedLocale}
     */
    public Locale getSelectedLocale() {
        return this.selectedLocale;
    }

    /**
     * Set the {@link #selectedLocale}
     * 
     * @param locale
     */
    public void setSelectedLocale(Locale locale) {
        this.selectedLocale = locale;
    }

    /**
     * Get the text for the given locale. If that does not exist, get the value for the default language and if that doesn't exist, an empty string
     * 
     * @param locale
     * @return Text for the given locale
     */
    public String getText(Locale locale) {
        return super.getValue(locale).orElse(getValue(DEFAULT_LANGUAGE).orElse(""));
    }

    /**
     * Get the text for {@link IPolyglott#getCurrentLocale()}, or, failing that, for {@link IPolyglott#getDefaultLocale()}, the internal default
     * language or finally an empty string
     * 
     * @return {@link String}
     */
    public String getTextOrDefault() {
        return getTextOrDefault(IPolyglott.getCurrentLocale(), IPolyglott.getDefaultLocale());
    }

    /**
     * Get the text for the given language, or, failing that, for the given defaultLocale, the internal default language or finally an empty string
     * 
     * @param locale The locale to return the text for
     * @param defaultLocale The fallback locale to use if no text exists for the given locale
     * @return {@link String}
     */
    public String getTextOrDefault(Locale locale, Locale defaultLocale) {
        return super.getValue(locale)
                .orElse(getValue(defaultLocale)
                        .orElse(getValue(DEFAULT_LANGUAGE)
                                .orElse("")));
    }

    /**
     * Set the text for the given locale
     * 
     * @param text
     * @param locale
     */
    public void setText(String text, Locale locale) {
        if (locale != null && StringUtils.isNotBlank(locale.getLanguage())) {
            super.setValue(text, locale);
        } else {
            super.setValue(text);
        }
    }

    /**
     * get the text for the current {@link #selectedLocale}
     * 
     * @return Text for selected locale
     */
    public String getText() {
        return this.getText(this.selectedLocale);
    }

    /**
     * set the text for the current {@link #selectedLocale}
     * 
     * @param text
     */
    public void setText(String text) {
        this.setText(text, this.selectedLocale);
    }

    /**
     * Get the values as a map of locales and associated texts. The default language text is never included since it does not have a locale
     * 
     * @return Map<Locale, String>
     */
    public Map<Locale, String> toMap() {
        return super.getValues().stream()
                .filter(vp -> StringUtils.isNotBlank(vp.getValue()))
                .collect(Collectors.toMap(ValuePair::getLocale, ValuePair::getValue));
    }

    /**
     * Alias for {@link #isValid(Locale)}
     */
    @Override
    public boolean isComplete(Locale locale) {
        return isValid(locale);
    }

    /**
     * @return true if at least one locale has a non empty text set
     */
    @Override
    public boolean isValid(Locale locale) {
        return getValue(locale).filter(StringUtils::isNotBlank).isPresent();
    }

    /**
     * @return an optional containing the text for the given locale if one exists, or an empty optional otherwise
     */
    @Override
    public Optional<String> getValue(Locale locale) {
        return Optional.ofNullable(locale).map(l -> getValue(l.getLanguage()).orElse(null));
    }

    /**
     * get the values of {@link IPolyglott#getLocalesStatic()}
     */
    @Override
    public Collection<Locale> getLocales() {
        return IPolyglott.getLocalesStatic();
    }

    /**
     * @param locale
     * @return true if the given locale is in the list of locales for which a text may be set, whether or not a text exists for that locale
     */
    public boolean hasLocale(Locale locale) {
        return super.getLanguages().stream().anyMatch(l -> l.equalsIgnoreCase(locale.getLanguage()));
    }

    /**
     * Alias for {@link #getText()}
     */
    @Override
    public String toString() {
        return getTextOrDefault();
    }

    @Override
    public int hashCode() {
        return this.getValues().stream().map(ValuePair::getValue).mapToInt(String::hashCode).sum();
    }

    /**
     * two TranslatedTexts are considered equal if the have the same locales and the same texts for each locale
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(TranslatedText.class)) {
            TranslatedText other = (TranslatedText) obj;
            if (other.getLanguages().size() != this.getLanguages().size()) {
                return false;
            }
            for (ValuePair pair : this.getValues()) {
                Locale locale = pair.getLocale();
                String value = pair.getValue();
                boolean same = Objects.equals(other.getValue(locale).orElse(""), value == null ? "" : value);
                if (!same) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * If required is true, true is returned if a non-empty value exists for the given locale. Otherwise return true unless defaultLocale has a
     * non-empty value while locale has not.
     * 
     * @param locale
     * @param defaultLocale
     * @param required
     * @return true if completeness not required or text not empty; false otherwise
     */
    public boolean isComplete(Locale locale, Locale defaultLocale, boolean required) {
        if (required || isComplete(defaultLocale)) {
            return isComplete(locale);
        }
        return true;
    }

    public String getAsJson() throws JsonProcessingException {
        return JsonTools.getAsJson(this);
    }
}
