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
package io.goobi.viewer.dao.converter;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author florian
 *
 */
@Converter
public class TranslatedTextConverter implements AttributeConverter<TranslatedText, String> {

    private final ObjectMapper mapper = new ObjectMapper();

    private Collection<Locale> configuredLocales;

    public TranslatedTextConverter() {
        configuredLocales = null;
    }

    public TranslatedTextConverter(Collection<Locale> locales) {
        this.configuredLocales = locales;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.Object)
     */
    @Override
    public String convertToDatabaseColumn(TranslatedText attribute) {
        if (attribute != null) {
            if (attribute.hasTranslations()) {
                try {
                    Map<String, String> map = attribute.toMap()
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(e -> e.getKey().getLanguage(), e -> StringEscapeUtils.escapeHtml4(e.getValue())));
                    new MultiLanguageMetadataValue(map);
                    return mapper.writeValueAsString(attribute);
                } catch (JsonProcessingException e1) {
                    throw new IllegalArgumentException("Cannot convert " + attribute + " to String");
                }
            }
            return attribute.getValue().orElse(null);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.AttributeConverter#convertToEntityAttribute(java.lang.Object)
     */
    @Override
    public TranslatedText convertToEntityAttribute(String dbData) {
        TranslatedText attribute = new TranslatedText(getConfiguredLocales());
        if (StringUtils.isBlank(dbData)) {
            return attribute;
        }

        try {
            IMetadataValue v = mapper.readValue(dbData, IMetadataValue.class);
            for (Locale locale : getConfiguredLocales()) {
                String s = v.getValue(locale).orElse("");
                s = StringEscapeUtils.unescapeHtml4(s);
                if (StringUtils.isNotBlank(s)) {
                    attribute.setText(s, locale);
                }
            }
            return attribute;
        } catch (JsonProcessingException e) {
            //assume a single language text
            attribute = new TranslatedText(dbData);
            return attribute;
        }
    }

    private Collection<Locale> getConfiguredLocales() {
        if (this.configuredLocales == null) {
            try {
                this.configuredLocales = IPolyglott.getLocalesStatic();
            } catch (NullPointerException | IllegalStateException e) {
                //too early?
                return Collections.emptyList();
            }
        }
        return this.configuredLocales;
    }

    private Collection<String> getConfiguredLanguages() {
        return getConfiguredLocales().stream().map(Locale::getLanguage).toList();
    }
}
