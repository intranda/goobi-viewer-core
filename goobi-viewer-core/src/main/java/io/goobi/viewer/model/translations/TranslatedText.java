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

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.api.rest.serialization.TranslatedTextSerializer;

/**
 * @author florian
 *
 */
@JsonSerialize(using=TranslatedTextSerializer.class)
public class TranslatedText extends MultiLanguageMetadataValue implements IPolyglott {

    private Locale selectedLocale;
    
    public TranslatedText() {
        this(IPolyglott.getLocalesStatic());
    }
    
    public TranslatedText(Collection<Locale> locales) {
        this(locales, IPolyglott.getDefaultLocale());
    }
    
    public TranslatedText(Collection<Locale> locales, Locale initalLocale) {
        super(locales.stream().collect(Collectors.toMap(l -> l.getLanguage(), l -> "")));
        this.selectedLocale = initalLocale;
    }
    
    public TranslatedText(TranslatedText orig, Collection<Locale> locales, Locale initialLocale)  {
        this(locales, initialLocale);
        orig.toMap().entrySet().forEach(entry -> {
            Locale l = entry.getKey();
            String value = entry.getValue();
            if(StringUtils.isNotBlank(value)) {
                this.setText(value, l);
            }
        });
    }
    
    public TranslatedText(TranslatedText orig)  {
        this(orig, IPolyglott.getLocalesStatic(), IPolyglott.getDefaultLocale());
    }
    
    /**
     * @param dbData
     */
    public TranslatedText(String text) {
        super();
        setText(text);
        
    }

    /**
     * @param label
     */
    public TranslatedText(MultiLanguageMetadataValue label) {        
        super(label.getLanguages().stream()
                .filter(lang -> label.getValue(lang).isPresent())
                .collect(Collectors.toMap(lang -> lang, lang -> label.getValue(lang).get()))
               );
    }

    public Locale getSelectedLocale() {
        return this.selectedLocale;
    }

    public void setSelectedLocale(Locale locale) {
        this.selectedLocale = locale;
    }
    
    public String getText(Locale locale) {
        return super.getValue(locale).orElse(getValue(DEFAULT_LANGUAGE).orElse(""));
    }
    
    public String getTextOrDefault(Locale locale, Locale defaultLocale) {
        return super.getValue(locale)
                .orElse(getValue(defaultLocale)
                .orElse(getValue(DEFAULT_LANGUAGE)
                .orElse("")));
    }
    
    public void setText(String text, Locale locale) {
        if(locale != null && StringUtils.isNotBlank(locale.getLanguage())) {            
            super.setValue(text, locale);
        } else {
            super.setValue(text);
        }
    }
    
    public String getText() {
        return this.getText(this.selectedLocale);
    }
    
    public void setText(String text) {
        this.setText(text, this.selectedLocale);
    }
    
    public Map<Locale, String> toMap() {
        return super.getValues().stream()
                .filter(vp -> StringUtils.isNotBlank(vp.getValue()))
                .collect(Collectors.toMap(ValuePair::getLocale, ValuePair::getValue));
    }

    @Override
    public boolean isComplete(Locale locale) {
        return isValid(locale);
    }

    @Override
    public boolean isValid(Locale locale) {
        return getValue(locale).filter(StringUtils::isNotBlank).isPresent();
    }
    
    @Override
    public Optional<String> getValue(Locale locale) {
        return Optional.ofNullable(locale).map(l -> getValue(l.getLanguage()).orElse(null));
    }

    @Override
    public Collection<Locale> getLocales() {
        return IPolyglott.getLocalesStatic();
    }

    /**
     * @param locale
     * @return
     */
    public boolean hasLocale(Locale locale) {
        return super.getLanguages().stream().anyMatch(l -> l.equalsIgnoreCase(locale.getLanguage()));
    }

}
