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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.viewer.ViewManager;

public class MetadataBuilder {

    private static final Logger logger = LogManager.getLogger(MetadataBuilder.class);

    private final Map<String, List<IMetadataValue>> values;

    public MetadataBuilder(MetadataContainer metadata) {
        this(metadata.getMetadata());
    }

    public MetadataBuilder(SolrDocument doc) {
        this(MetadataContainer.createMetadataEntity(doc));
    }

    public MetadataBuilder(ComplexMetadata metadata) {
        this(metadata.getMetadata());
    }

    public MetadataBuilder(Map<String, List<IMetadataValue>> values) {
        this.values = values;
    }

    public IMetadataValue build(Metadata metadataConfiguration) {
        return createFromConfig(metadataConfiguration, values);
    }

    /**
     * 
     * @param config
     * @param metadata
     * @return
     */
    private static IMetadataValue createFromConfig(Metadata config, Map<String, List<IMetadataValue>> metadata) {
        IMetadataValue title = new MultiLanguageMetadataValue();
        for (MetadataParameter param : config.getParams()) {
            IMetadataValue value;
            IMetadataValue keyValue = Optional.ofNullable(param.getKey())
                    .map(metadata::get)
                    .map(l -> l.get(0))
                    .map(IMetadataValue::copy)
                    .orElse(new SimpleMetadataValue(""));
            IMetadataValue altKeyValue = Optional.ofNullable(param.getAltKey())
                    .map(metadata::get)
                    .map(l -> l.get(0))
                    .map(IMetadataValue::copy)
                    .orElse(new SimpleMetadataValue(""));
            switch (param.getType()) {
                case TRANSLATEDFIELD:
                    value = getTranslatedFieldValue(param, keyValue, altKeyValue);
                    break;
                case FIELD:
                    value = getFieldValue(param, keyValue, altKeyValue);
                    break;
                case DATEFIELD:
                    value = getDateFieldValue(param, keyValue, altKeyValue);
                    break;
                default:
                    value = Optional.ofNullable(metadata.get(param.getKey()))
                            .map(List::stream)
                            .flatMap(Stream::findFirst)
                            .orElse(Optional.ofNullable(metadata.get(param.getAltKey()))
                                    .map(List::stream)
                                    .flatMap(Stream::findFirst)
                                    .orElse(new SimpleMetadataValue("")));
                    break;
            }

            appendValue(config, title, param, value);
        }
        return title;
    }

    /**
     * 
     * @param param
     * @param keyValue
     * @param altKeyValue
     * @return
     */
    public static IMetadataValue getDateFieldValue(MetadataParameter param, IMetadataValue keyValue, IMetadataValue altKeyValue) {
        IMetadataValue value = new MultiLanguageMetadataValue();
        for (Locale locale : IPolyglott.getLocalesStatic()) {
            String outputPattern =
                    StringUtils.isNotBlank(param.getPattern()) ? param.getPattern() : BeanUtils.getNavigationHelper().getDatePattern();
            String altOutputPattern = outputPattern.replace("dd/", "");
            String dateString = "";

            try {
                try {
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(outputPattern);
                    LocalDate date = LocalDate.parse(keyValue.getValueOrFallback(locale));
                    dateString = date.format(dateTimeFormatter);
                } catch (DateTimeParseException e) {
                    // No-day format hack
                    try {
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(altOutputPattern);
                        LocalDate date = LocalDate.parse(keyValue.getValueOrFallback(locale) + "-01");
                        dateString = date.format(dateTimeFormatter);
                    } catch (DateTimeParseException e1) {
                        logger.warn("Error parsing {} as date", value);
                    }
                }
                dateString = dateString.replace(StringConstants.HTML_BR_ESCAPED, StringConstants.HTML_BR);
                value.setValue(dateString, locale);
            } catch (DateTimeParseException | NullPointerException e) {
                try {
                    try {
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(outputPattern);
                        LocalDate date = LocalDate.parse(altKeyValue.getValueOrFallback(locale));
                        dateString = date.format(dateTimeFormatter);
                    } catch (DateTimeParseException e1) {
                        // No-day format hack
                        try {
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(altOutputPattern);
                            LocalDate date = LocalDate.parse(altKeyValue.getValueOrFallback(locale) + "-01");
                            dateString = date.format(dateTimeFormatter);
                        } catch (DateTimeParseException e2) {
                            logger.warn("Error parsing {} as date", value);
                        }
                    }
                    dateString = dateString.replace(StringConstants.HTML_BR_ESCAPED, StringConstants.HTML_BR);
                    value.setValue(dateString, locale);
                } catch (DateTimeParseException | NullPointerException e1) {
                    value.setValue(keyValue.isEmpty() ? altKeyValue.getValueOrFallback(locale) : keyValue.getValueOrFallback(locale), locale);
                }
            }
        }
        if (value.isEmpty()) {
            value = ViewerResourceBundle.getTranslations(Optional.ofNullable(param.getDefaultValue()).orElse(""));
        }
        return value;
    }

    private static void appendValue(Metadata config, IMetadataValue title, MetadataParameter param, IMetadataValue value) {
        String placeholder1 = new StringBuilder("{").append(param.getKey()).append("}").toString();
        String placeholder2 = new StringBuilder("{").append(config.getParams().indexOf(param)).append("}").toString();
        if (!value.isEmpty() && StringUtils.isNotEmpty(param.getPrefix())) {
            String prefix = ViewerResourceBundle.getTranslation(param.getPrefix(), null);
            value.addPrefix(prefix);
        }
        if (!value.isEmpty() && StringUtils.isNotEmpty(param.getSuffix())) {
            String suffix = ViewerResourceBundle.getTranslation(param.getSuffix(), null);
            value.addSuffix(suffix);
        }
        Set<String> languages = new HashSet<>(value.getLanguages());
        languages.addAll(title.getLanguages());
        // Replace master value placeholders in the label object
        Map<String, String> languageLabelMap = new HashMap<>();
        for (String language : languages) {
            String langValue = title.getValue(language)
                    .orElse(title.getValue()
                            .orElse(ViewerResourceBundle.getTranslation(config.getMasterValue(), Locale.forLanguageTag(language), true)))
                    .replace(placeholder1, value.getValue(language).orElse(value.getValue().orElse("")))
                    .replace(placeholder2, value.getValue(language).orElse(value.getValue().orElse("")));
            languageLabelMap.put(language, langValue);
        }
        for (Entry<String, String> entry : languageLabelMap.entrySet()) {
            title.setValue(entry.getValue(), entry.getKey());
        }
    }

    private static IMetadataValue getFieldValue(MetadataParameter param, IMetadataValue keyValue, IMetadataValue altKeyValue) {
        IMetadataValue value;
        if (keyValue != null) {
            value = keyValue;
        } else if (altKeyValue != null) {
            value = altKeyValue;
        } else if (StringUtils.isNotBlank(param.getDefaultValue())) {
            // Translate key, if no index field found
            value = new SimpleMetadataValue(param.getDefaultValue());
        } else {
            value = new SimpleMetadataValue();
        }
        return value;
    }

    private static IMetadataValue getTranslatedFieldValue(MetadataParameter param, IMetadataValue keyValue, IMetadataValue altKeyValue) {
        IMetadataValue value;
        if (keyValue != null) {
            if (keyValue instanceof SimpleMetadataValue) {
                value = ViewerResourceBundle.getTranslations(keyValue.getValue().orElse(""));
            } else {
                value = keyValue;
            }
        } else if (altKeyValue != null) {
            if (altKeyValue instanceof SimpleMetadataValue) {
                value = ViewerResourceBundle.getTranslations(altKeyValue.getValue().orElse(""));
            } else {
                value = altKeyValue;
            }
        } else if (StringUtils.isNotBlank(param.getDefaultValue())) {
            // Translate key, if no index field found
            value = ViewerResourceBundle.getTranslations(param.getDefaultValue());
        } else {
            value = new SimpleMetadataValue();
        }
        return value;
    }

}
