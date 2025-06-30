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
import java.util.Collections;
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
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.viewer.StructElement;

public class MetadataBuilder {

    private static final String IDENTIFIER_WHITESPACE_REPLACEMENT = "_";

    private static final int IDENTIFIER_MAX_LENGTH = 1000;

    private static final Logger logger = LogManager.getLogger(MetadataBuilder.class);

    private final Map<String, Map<String, List<IMetadataValue>>> values = new HashMap<>();

    public MetadataBuilder(MetadataContainer metadata) {
        this(metadata.getMetadata());
    }

    public MetadataBuilder(MetadataContainer metadata, MetadataContainer parent, MetadataContainer topStruct) {
        this(metadata.getMetadata(), parent.getMetadata(), topStruct.getMetadata(), Collections.emptyMap());
    }

    public MetadataBuilder(MetadataContainer metadata, MetadataContainer parent, MetadataContainer topStruct, MetadataContainer related) {
        this(metadata.getMetadata(), parent.getMetadata(), topStruct.getMetadata(), related.getMetadata());
    }

    /**
     * 
     * @param doc
     */
    public MetadataBuilder(SolrDocument doc) {
        this(MetadataContainer.createMetadataEntity(doc));
    }

    public MetadataBuilder(StructElement doc) {
        this(MetadataContainer.createMetadataEntity(doc));
    }

    /**
     * 
     * @param metadata
     */
    public MetadataBuilder(ComplexMetadata metadata) {
        this(metadata.getMetadata());
    }

    /**
     * 
     * @param values
     */
    public MetadataBuilder(Map<String, List<IMetadataValue>> values) {
        this.values.put("", values);
    }

    public MetadataBuilder(Map<String, List<IMetadataValue>> values, Map<String, List<IMetadataValue>> parent,
            Map<String, List<IMetadataValue>> topStruct, Map<String, List<IMetadataValue>> related) {
        this(values);
        this.values.put("parent", parent);
        this.values.put("topStruct", topStruct);
        this.values.put("related", related);
    }

    public IMetadataValue build(Metadata metadataConfiguration) {
        return createFromConfig(metadataConfiguration);
    }

    /**
     * 
     * @param config
     * @param metadata
     * @return {@link IMetadataValue}
     */
    private IMetadataValue createFromConfig(Metadata config) {
        IMetadataValue title = new MultiLanguageMetadataValue();
        for (MetadataParameter param : config.getParams()) {
            IMetadataValue value;
            IMetadataValue keyValue = Optional.ofNullable(param.getKey())
                    .map(key -> getValues(param).get(key))
                    .filter(l -> !l.isEmpty())
                    .map(l -> l.get(0))
                    .map(IMetadataValue::copy)
                    .orElse(new SimpleMetadataValue(""));
            IMetadataValue altKeyValue = Optional.ofNullable(param.getAltKey())
                    .map(key -> getValues(param).get(key))
                    .filter(l -> !l.isEmpty())
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
                case IDENTIFIER:
                    value = getFieldValue(param, keyValue, altKeyValue);
                    value.mapEach(s -> StringTools.convertToSingleWord(s, IDENTIFIER_MAX_LENGTH, IDENTIFIER_WHITESPACE_REPLACEMENT).toLowerCase());
                    break;
                default:
                    value = Optional.ofNullable(getValues(param).get(param.getKey()))
                            .map(List::stream)
                            .flatMap(Stream::findFirst)
                            .orElse(Optional.ofNullable(getValues(param).get(param.getAltKey()))
                                    .map(List::stream)
                                    .flatMap(Stream::findFirst)
                                    .orElse(new SimpleMetadataValue("")));
                    break;
            }

            appendValue(config, title, param, value);
        }
        return title;
    }

    private Map<String, List<IMetadataValue>> getValues(MetadataParameter param) {
        return this.values.getOrDefault(param.getScope(), Collections.emptyMap());
    }

    /**
     * 
     * @param param
     * @param keyValue
     * @param altKeyValue
     * @return {@link IMetadataValue}
     */
    public static IMetadataValue getDateFieldValue(MetadataParameter param, IMetadataValue keyValue, IMetadataValue altKeyValue) {
        IMetadataValue value = new MultiLanguageMetadataValue();
        for (Locale locale : IPolyglott.getLocalesStatic()) {
            String outputPattern =
                    StringUtils.isNotBlank(param.getOutputPattern()) ? param.getOutputPattern() : NavigationHelper.getDatePattern(locale);
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
                        // Keep original value
                        dateString = keyValue.getValueOrFallback(locale);
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
                            // Keep original value
                            dateString = altKeyValue.getValueOrFallback(locale);
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
        if (keyValue != null && !keyValue.isEmpty()) {
            value = keyValue;
        } else if (altKeyValue != null && !altKeyValue.isEmpty()) {
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
