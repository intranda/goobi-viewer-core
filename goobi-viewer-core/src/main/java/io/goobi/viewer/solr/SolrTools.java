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
package io.goobi.viewer.solr;

import java.io.IOException;
import java.io.StringReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants.DocType;

/**
 * Static utility methods for Solr.
 */
public class SolrTools {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(SolrTools.class);

    private static final int MIN_SCHEMA_VERSION = 20230110;
    private static final String SCHEMA_VERSION_PREFIX = "goobi_viewer-";

    private static final String MULTILANGUAGE_FIELD_REGEX = "(\\w+)_LANG_(\\w{2,3})";
    private static final String SUFFIX_LANGUAGE_REGEX = SolrConstants.MIDFIX_LANG + "([A-Z]{2,3})$";

    /**
     * 
     */
    private SolrTools() {
        //
    }

    /**
     * Returns the comma-separated sorting fields in <code>solrSortFields</code> as a List<StringPair>.
     *
     * @param solrSortFields a {@link java.lang.String} object.
     * @param splitFieldsBy String by which the individual field configurations are split
     * @param splitNameOrderBy String by which the field name and sorting order are split
     * @should split fields correctly
     * @should split single field correctly
     * @should throw IllegalArgumentException if solrSortFields is null
     * @should throw IllegalArgumentException if splitFieldsBy is null
     * @should throw IllegalArgumentException if splitNameOrderBy is null
     * @return a {@link java.util.List} object.
     */
    public static List<StringPair> getSolrSortFieldsAsList(String solrSortFields, String splitFieldsBy, String splitNameOrderBy) {
        if (solrSortFields == null) {
            throw new IllegalArgumentException("solrSortFields may not be null");
        }
        if (splitFieldsBy == null) {
            throw new IllegalArgumentException("splitFieldsBy may not be null");
        }
        if (splitNameOrderBy == null) {
            throw new IllegalArgumentException("splitNameOrderBy may not be null");
        }

        if (StringUtils.isNotEmpty(solrSortFields)) {
            String[] solrSortFieldsSplit = solrSortFields.split(splitFieldsBy);
            List<StringPair> ret = new ArrayList<>(solrSortFieldsSplit.length);
            for (String fieldConfig : solrSortFieldsSplit) {
                if (StringUtils.isNotBlank(fieldConfig)) {
                    String[] fieldConfigSplit = fieldConfig.split(splitNameOrderBy);
                    switch (fieldConfigSplit.length) {
                        case 1:
                            ret.add(new StringPair(fieldConfigSplit[0].trim(), "asc"));
                            break;
                        case 2:
                            ret.add(new StringPair(fieldConfigSplit[0].trim(), fieldConfigSplit[1].trim()));
                            break;
                        default:
                            logger.warn("Cannot parse sorting field configuration");
                            break;
                    }

                }
            }
            return ret;
        }

        return Collections.emptyList();
    }

    /**
     * Parses a Solr-Field value in order to return it as String
     *
     * @param fieldValue a {@link java.lang.Object} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getAsString(Object fieldValue) {
        return getAsString(fieldValue, "\n");
    }

    /**
     * 
     * @param fieldValue
     * @return
     */
    public static Boolean getAsBoolean(Object fieldValue) {
        if (fieldValue instanceof Boolean) {
            return (Boolean) fieldValue;
        } else if (fieldValue != null) {
            return Boolean.parseBoolean(getAsString(fieldValue));
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 
     * @param fieldValue
     * @param separator
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String getAsString(Object fieldValue, String separator) {
        if (fieldValue == null) {
            return null;
        }
        if (fieldValue instanceof String) {
            return (String) fieldValue;
        } else if (fieldValue instanceof List) {
            StringBuilder sb = new StringBuilder();
            List<Object> list = (List<Object>) fieldValue;
            for (Object object : list) {
                sb.append(separator).append(getAsString(object));
            }
            return sb.toString().trim();
        } else {
            return fieldValue.toString();
        }
    }

    /**
     * <p>
     * getAsInt.
     * </p>
     *
     * @param fieldValue a {@link java.lang.Object} object.
     * @return a {@link java.lang.Integer} object.
     */
    public static Integer getAsInt(Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }
        if (fieldValue instanceof Integer) {
            return (Integer) fieldValue;
        }
        try {
            return Integer.parseInt(fieldValue.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * <p>
     * getSingleFieldValue.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.Object} object.
     */
    public static Object getSingleFieldValue(SolrDocument doc, String field) {
        Collection<Object> valueList = doc.getFieldValues(field);
        if (valueList != null && !valueList.isEmpty()) {
            return valueList.iterator().next();
        }

        return null;
    }

    /**
     * <p>
     * getSingleFieldStringValue.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param field a {@link java.lang.String} object.
     * @should return value as string correctly
     * @should not return null as string if value is null
     * @return a {@link java.lang.String} object.
     */
    public static String getSingleFieldStringValue(SolrDocument doc, String field) {
        Object val = getSingleFieldValue(doc, field);
        return val != null ? String.valueOf(val) : null;
    }

    /**
     * <p>
     * getSingleFieldIntegerValue.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.Integer} object.
     */
    public static Integer getSingleFieldIntegerValue(SolrDocument doc, String field) {
        Object val = getSingleFieldValue(doc, field);
        return SolrTools.getAsInt(val);
    }

    /**
     * <p>
     * getSingleFieldBooleanValue.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    public static boolean getSingleFieldBooleanValue(SolrDocument doc, String field) {
        Object val = getSingleFieldValue(doc, field);
        if (val == null) {
            return false;
        } else if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            return Boolean.valueOf((String) val);
        } else {
            return false;
        }
    }

    /**
     * Returns a list with all (string) values for the given field name in the given SolrDocument.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param fieldName a {@link java.lang.String} object.
     * @should return all values for the given field
     * @return a {@link java.util.List} object.
     */
    public static List<String> getMetadataValues(SolrDocument doc, String fieldName) {
        if (doc == null) {
            return Collections.emptyList();
        }

        Collection<Object> values = doc.getFieldValues(fieldName);
        if (values == null) {
            return Collections.emptyList();
        }

        List<String> ret = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value instanceof String) {
                ret.add((String) value);
            } else {
                ret.add(String.valueOf(value));
            }
        }

        return ret;
    }

    /**
     * Converts the given SolrDocument to a value map. IMAGEURN_OAI and PAGEURNS are not returned because they have no relevance in this application
     * and can get quite large.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @should return all fields in the given doc except page urns
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, List<String>> getFieldValueMap(SolrDocument doc) {
        Map<String, List<String>> ret = new HashMap<>();

        for (String fieldName : doc.getFieldNames()) {
            switch (fieldName) {
                case SolrConstants.IMAGEURN_OAI:
                case "PAGEURNS":
                    break;
                default:
                    List<String> values = getMetadataValues(doc, fieldName);
                    ret.put(fieldName, values);
                    break;
            }
        }

        return ret;
    }

    /**
     * Converts the given SolrDocument to a value map. IMAGEURN_OAI and PAGEURNS are not returned because they have no relevance in this application
     * and can get quite large.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @should return all fields in the given doc except page urns
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, List<IMetadataValue>> getMultiLanguageFieldValueMap(SolrDocument doc) {
        Map<String, List<IMetadataValue>> ret = new HashMap<>();

        for (String fieldName : doc.getFieldNames()) {
            switch (fieldName) {
                case SolrConstants.IMAGEURN_OAI:
                case "PAGEURNS":
                    break;
                default:
                    if (isLanguageCodedField(fieldName)) {
                        break;
                    }
                    Map<String, List<String>> mdValues = getMetadataValuesForLanguage(doc, fieldName);
                    List<IMetadataValue> values = getMultiLanguageMetadata(mdValues);
                    ret.put(fieldName, values);
            }
        }

        return ret;
    }

    /**
     * <p>
     * getMultiLanguageMetadata.
     * </p>
     *
     * @param mdValues a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     */
    public static List<IMetadataValue> getMultiLanguageMetadata(Map<String, List<String>> mdValues) {
        List<IMetadataValue> values = new ArrayList<>();
        int numValues = mdValues.values().stream().mapToInt(list -> list.size()).max().orElse(0);
        for (int i = 0; i < numValues; i++) {
            MultiLanguageMetadataValue value = new MultiLanguageMetadataValue();
            for (Entry<String, List<String>> entry : mdValues.entrySet()) {
                List<String> stringValues = entry.getValue();
                if (i < stringValues.size()) {
                    value.setValue(stringValues.get(i), entry.getKey());
                }
            }
            values.add(value);
        }
        return values;
    }

    /**
     * <p>
     * getMetadataValuesForLanguage.
     * </p>
     *
     * @param doc The document containing the metadata
     * @param key the metadata key without the '_LANG_...' suffix
     * @return A map with keys for each language and lists of all found metadata values for this language. Metadata that match the given key but have
     *         no language information are listed as language {@code _DEFAULT}
     */
    public static Map<String, List<String>> getMetadataValuesForLanguage(SolrDocument doc, String key) {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key may not be null");
        }

        List<String> fieldNames =
                doc.getFieldNames()
                        .stream()
                        .filter(field -> field.equals(key) || field.matches(key + "_LANG_\\w{2,3}"))
                        .collect(Collectors.toList());
        Map<String, List<String>> map = new HashMap<>(fieldNames.size());
        for (String languageField : fieldNames) {
            String locale = null;
            if (languageField.startsWith(key + SolrConstants.MIDFIX_LANG)) {
                locale = languageField.substring(languageField.lastIndexOf(SolrConstants.MIDFIX_LANG) + 6).toLowerCase();
            } else {
                locale = MultiLanguageMetadataValue.DEFAULT_LANGUAGE;
            }
            Collection<Object> languageValues = doc.getFieldValues(languageField);
            if (languageValues != null) {
                List<String> values = languageValues.stream().map(value -> String.valueOf(value)).collect(Collectors.toList());
                map.put(locale, values);
            }
        }

        return map;
    }

    /**
     * <p>
     * getMetadataValuesForLanguage.
     * </p>
     *
     * @param doc The document containing the metadata
     * @param key the metadata key without the '_LANG_...' suffix
     * @return A map with keys for each language and lists of all found metadata values for this language. Metadata that match the given key but have
     *         no language information are listed as language {@code _DEFAULT}
     */
    public static Map<String, List<String>> getMetadataValuesForLanguage(StructElement doc, String key) {
        Map<String, List<String>> map = new HashMap<>();
        if (doc != null) {
            List<String> fieldNames = doc.getMetadataFields()
                    .keySet()
                    .stream()
                    .filter(field -> field.equals(key) || field.startsWith(key + SolrConstants.MIDFIX_LANG))
                    .collect(Collectors.toList());
            for (String languageField : fieldNames) {
                String locale = null;
                if (languageField.matches(key + "_LANG_\\w{2,3}")) {
                    locale = languageField.substring(languageField.lastIndexOf(SolrConstants.MIDFIX_LANG) + 6).toLowerCase();
                } else {
                    locale = MultiLanguageMetadataValue.DEFAULT_LANGUAGE;
                }
                Collection<String> languageValues = doc.getMetadataValues(languageField);
                if (languageValues != null) {
                    List<String> values = languageValues.stream().map(value -> String.valueOf(value)).collect(Collectors.toList());
                    map.put(locale, values);
                }
            }
        }
        return map;
    }

    /**
     *
     * @param doc
     * @return
     */
    public static boolean isGroup(SolrDocument doc) {
        if (doc == null) {
            return false;
        }

        return DocType.GROUP.toString().equals(doc.getFieldValue(SolrConstants.DOCTYPE));
    }

    /**
     *
     * @param doc
     * @return
     */
    public static boolean isAnchor(SolrDocument doc) {
        if (doc == null) {
            return false;
        }

        return doc.containsKey(SolrConstants.ISANCHOR) && (Boolean) doc.getFieldValue(SolrConstants.ISANCHOR);
    }

    /**
     *
     * @param doc
     * @return
     */
    public static boolean isWork(SolrDocument doc) {
        if (doc == null) {
            return false;
        }

        return doc.containsKey(SolrConstants.ISWORK) && (Boolean) doc.getFieldValue(SolrConstants.ISWORK);
    }

    /**
     * @param fieldName
     * @return
     */
    public static boolean isLanguageCodedField(String fieldName) {
        return StringUtils.isNotBlank(fieldName) && fieldName.matches(MULTILANGUAGE_FIELD_REGEX);
    }

    /**
     * 
     * @param field
     * @param language
     * @return true if language code different
     * @should return true if language code different
     * @should return false if language code same
     * @should return false if no language code
     */
    public static boolean isHasWrongLanguageCode(String field, String language) {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (language == null) {
            throw new IllegalArgumentException("language may not be null");
        }

        return field.contains(SolrConstants.MIDFIX_LANG) && !field.endsWith(SolrConstants.MIDFIX_LANG + language.toUpperCase());
    }

    /**
     * <p>
     * isQuerySyntaxError.
     * </p>
     *
     * @param e a {@link java.lang.Exception} object.
     * @return a boolean.
     */
    public static boolean isQuerySyntaxError(Exception e) {
        return e.getMessage() != null && (e.getMessage().startsWith("org.apache.solr.search.SyntaxError")
                || e.getMessage().contains("Cannot parse")
                || e.getMessage().contains("Invalid Number")
                || e.getMessage().contains("undefined field")
                || e.getMessage().contains("field can't be found")
                || e.getMessage().contains("can not sort on multivalued field"));
    }

    /**
     * 
     * @param exceptionMessage
     * @return
     * @should return empty string if exceptionMessage empty
     * @should return exceptionMessage if no pattern match found
     * @should return title content correctly
     */
    public static String extractExceptionMessageHtmlTitle(String exceptionMessage) {
        if (StringUtils.isEmpty(exceptionMessage)) {
            return "";
        }

        Pattern p = Pattern.compile("<title>(.*)</title>");
        Matcher m = p.matcher(exceptionMessage);
        if (m.find()) {
            return m.group(1);
        }

        return exceptionMessage;
    }

    /**
     * <p>
     * getTranslations.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @return a {@link java.util.Optional} object.
     */
    public static Optional<IMetadataValue> getTranslations(String fieldName, SolrDocument doc) {
        Map<String, List<String>> translations = getMetadataValuesForLanguage(doc, fieldName);
        if (translations.size() > 1) {
            return Optional.of(new MultiLanguageMetadataValue(translations));
        } else if (translations.size() == 1) {
            return Optional.of(ViewerResourceBundle.getTranslations(translations.values().iterator().next().stream().findFirst().orElse("")));
        } else {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * getTranslations.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param combiner a {@link java.util.function.BinaryOperator} object.
     * @return a {@link java.util.Optional} object.
     */
    public static Optional<IMetadataValue> getTranslations(String fieldName, SolrDocument doc, BinaryOperator<String> combiner) {
        Map<String, List<String>> translations = getMetadataValuesForLanguage(doc, fieldName);
        if (translations.size() > 1) {
            return Optional.of(new MultiLanguageMetadataValue(translations, combiner));
        } else if (translations.size() == 1) {
            return Optional.of(ViewerResourceBundle.getTranslations(translations.values().iterator().next().stream().findFirst().orElse("")));
        } else {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * getTranslations.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param combiner a {@link java.util.function.BinaryOperator} object.
     * @return a {@link java.util.Optional} object.
     */
    public static Optional<IMetadataValue> getTranslations(String fieldName, StructElement doc, BinaryOperator<String> combiner) {
        return getTranslations(fieldName, doc, ViewerResourceBundle.getAllLocales(), combiner);
    }

    /**
     * <p>
     * getTranslations.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param combiner a {@link java.util.function.BinaryOperator} object.
     * @return a {@link java.util.Optional} object.
     */
    public static Optional<IMetadataValue> getTranslations(String fieldName, StructElement doc, List<Locale> translationLocales,
            BinaryOperator<String> combiner) {
        Map<String, List<String>> translations = getMetadataValuesForLanguage(doc, fieldName);
        if (translations.size() > 1) {
            return Optional.of(new MultiLanguageMetadataValue(translations, combiner));
        } else if (!translations.isEmpty()) {
            String value = translations.values().iterator().next().stream().reduce((s1, s2) -> combiner.apply(s1, s2)).orElse("");
            return Optional.ofNullable(ViewerResourceBundle
                    .getTranslations(value, translationLocales, false));
        } else {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * isHasImages.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object. Needs to contain metadata fields {@link SolrConstants.FILENAME} and
     *            {@link SolrConstants.THUMBNAIL}
     * @should return correct value for page docs
     * @should return correct value for docsctrct docs
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static boolean isHasImages(SolrDocument doc) throws IndexUnreachableException {
        StructElement structElement = new StructElement(0, doc);
        String fileExtension = "";

        String filename = structElement.getMetadataValue(SolrConstants.FILENAME);
        if (StringUtils.isEmpty(filename)) {
            filename = structElement.getMetadataValue(SolrConstants.THUMBNAIL);
        }
        if (filename != null) {
            fileExtension = FilenameUtils.getExtension(filename).toLowerCase();
        }

        return fileExtension != null && fileExtension.toLowerCase().matches("(tiff?|jpe?g|png|jp2|gif)");
    }

    /**
     *
     * @param conditions
     * @return
     */
    public static String getProcessedConditions(String conditions) {
        if (conditions == null) {
            return null;
        }

        if (conditions.contains("NOW/YEAR") && !conditions.contains("DATE_")) {
            // Hack for getting the current year as a number for non-date Solr fields
            conditions = conditions.replace("NOW/YEAR", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        }

        return conditions.trim();
    }

    /**
     * <p>
     * getAvailableValuesForField.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @param filterQuery a {@link java.lang.String} object.
     * @return List of facet values for the given field and query
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should return all existing values for the given field
     */
    public static List<String> getAvailableValuesForField(String field, String filterQuery) throws PresentationException, IndexUnreachableException {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (filterQuery == null) {
            throw new IllegalArgumentException("filterQuery may not be null");
        }
        String facettifiedField = SearchHelper.facetifyField(field);
        filterQuery = SearchHelper.buildFinalQuery(filterQuery, false, SearchAggregationType.NO_AGGREGATION);
        QueryResponse qr =
                DataManager.getInstance()
                        .getSearchIndex()
                        .searchFacetsAndStatistics(filterQuery, null, Collections.singletonList(facettifiedField), 1, false);
        if (qr != null) {
            FacetField facet = qr.getFacetField(facettifiedField);
            if (facet != null) {
                List<String> ret = new ArrayList<>(facet.getValueCount());
                for (Count count : facet.getValues()) {
                    // Skip inverted values
                    if (!StringTools.checkValueEmptyOrInverted(count.getName())) {
                        ret.add(count.getName());
                        // logger.trace(count.getName());
                    }
                }
                return ret;
            }
        }

        return Collections.emptyList();
    }

    /**
     *
     * @return List of existing values for the configured subtheme discriminator field
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return correct values
     */
    public static List<String> getExistingSubthemes() throws PresentationException, IndexUnreachableException {
        String subthemeDiscriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
        if (StringUtils.isEmpty(subthemeDiscriminatorField)) {
            return Collections.emptyList();
        }

        return getAvailableValuesForField(subthemeDiscriminatorField, SolrConstants.PI + ":*");
    }

    /**
     * Solr supports dynamic random_* sorting fields. Each value represents one particular order, so a random number is required.
     *
     * @return Randomized sorting field
     */
    public static String generateRandomSortField() {
        return "random_" + new SecureRandom().nextInt(Integer.MAX_VALUE);
    }

    /**
     * <p>
     * checkSolrSchemaName.
     * </p>
     *
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] checkSolrSchemaName() {
        String[] ret = { "200", "" };
        Document doc = getSolrSchemaDocument();
        if (doc != null) {
            Element eleRoot = doc.getRootElement();
            if (eleRoot != null) {
                String schemaName = eleRoot.getAttributeValue("name");
                if (StringUtils.isNotEmpty(schemaName)) {
                    try {
                        if (schemaName.length() > SCHEMA_VERSION_PREFIX.length()
                                && Integer.parseInt(schemaName.substring(SCHEMA_VERSION_PREFIX.length())) >= MIN_SCHEMA_VERSION) {
                            String msg = "Solr schema is up to date: " + SCHEMA_VERSION_PREFIX + MIN_SCHEMA_VERSION;
                            logger.trace(msg);
                            ret[0] = "200";
                            ret[1] = msg;
                            return ret;
                        }
                    } catch (NumberFormatException e) {
                        logger.error("Schema version must contain a number.");
                    }
                    String msg = "Solr schema is not up to date; required: " + SCHEMA_VERSION_PREFIX + MIN_SCHEMA_VERSION + ", found: " + schemaName;
                    logger.error(msg);
                    ret[0] = "417";
                    ret[1] = msg;
                }
            }
        } else {
            String msg = "Could not read the Solr schema name.";
            logger.error(msg);
            ret[0] = "500";
            ret[1] = msg;
        }

        return ret;
    }

    /**
     * 
     * @return
     */
    private static Document getSolrSchemaDocument() {
        try {
            NetTools.getWebContentGET(
                    DataManager.getInstance().getConfiguration().getSolrUrl() + "/admin/file/?contentType=text/xml;charset=utf-8&file=schema.xml");
            String responseBody = NetTools.getWebContentGET(
                    DataManager.getInstance().getConfiguration().getSolrUrl() + "/admin/file/?contentType=text/xml;charset=utf-8&file=schema.xml");
            try (StringReader sr = new StringReader(responseBody)) {
                return XmlTools.getSAXBuilder().build(sr);
            }
        } catch (ClientProtocolException | JDOMException | HTTPException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return null;
    }

    /**
     * Escapes all special characters used by SOLR (as detailed here:
     * https://solr.apache.org/guide/7_3/the-standard-query-parser.html#escaping-special-characters) as well as the characters '<' and '>' by adding a
     * '\' before them. Special characters which already are escaped by '\' are not escaped any further making this method idempotent
     * 
     * @param the string to escape
     * @return the escaped string. if the original string is null, null is also returned
     */
    public static String escapeSpecialCharacters(String string) {
        if (StringUtils.isNotBlank(string)) {
            return string.replaceAll("(?<!\\\\)([<>+\\-&||!(){}\\[\\]^\"~*?:/])", "\\\\$1");
        }
        return string;
    }

    /**
     * reverts the operation of {@link #escapeSpecialCharacters(String)}
     * 
     * @param string the string to unescape
     * @return the unescaped string
     */
    public static String unescapeSpecialCharacters(String string) {
        if (StringUtils.isNotBlank(string)) {
            return string.replaceAll("\\\\([<>+\\-&||!(){}\\[\\]^\\\"~*?:\\/])", "$1");
        }
        return string;
    }

    /**
     * 
     * @param query
     * @return cleaned up query
     * @should remove brace pairs
     * @should keep join parameter
     * @should keep single braces
     */
    public static String cleanUpQuery(String query) {
        if (StringUtils.isBlank(query)) {
            return query;
        }

        return query.replaceAll("\\{(.+)\\}", "$1").replace("!join from=PI_TOPSTRUCT to=PI", "{!join from=PI_TOPSTRUCT to=PI}");
    }

    /**
     * 
     * @param fieldName
     * @return
     */
    public static String getBaseFieldName(String fieldName) {
        if (StringUtils.isNotBlank(fieldName)) {
            return fieldName.replaceAll(SUFFIX_LANGUAGE_REGEX, "");
        }
        return fieldName;
    }

    /**
     * 
     * @param fieldName
     * @return
     */
    public static String getLanguage(String fieldName) {
        if (StringUtils.isNotBlank(fieldName)) {
            Matcher matcher = Pattern.compile(SUFFIX_LANGUAGE_REGEX).matcher(fieldName);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }
        return null;
    }

    /**
     * 
     * @param fieldName
     * @return
     */
    public static Locale getLocale(String fieldName) {
        String language = getLanguage(fieldName);
        if (StringUtils.isNotBlank(language)) {
            return Locale.forLanguageTag(language.toLowerCase());
        }
        return null;
    }

    /**
     * 
     * @param doc
     * @param fieldNameFilter
     * @return
     */
    public static Map<String, List<IMetadataValue>> getTranslatedMetadata(SolrDocument doc, Function<String, Boolean> fieldNameFilter) {
        return getTranslatedMetadata(doc, new HashMap<>(), null, fieldNameFilter);
    }

    /**
     * 
     * @param doc
     * @param metadata
     * @param documentLocale
     * @param fieldNameFilter
     * @return
     */
    public static Map<String, List<IMetadataValue>> getTranslatedMetadata(SolrDocument doc, Map<String, List<IMetadataValue>> metadata,
            Locale documentLocale, Function<String, Boolean> fieldNameFilter) {
        List<String> fieldNames = doc.getFieldNames().stream().filter(fieldNameFilter::apply).collect(Collectors.toList());
        String docType = SolrTools.getBaseFieldName(SolrTools.getSingleFieldStringValue(doc, SolrConstants.LABEL));

        for (String fieldName : fieldNames) {
            List<String> values = SolrTools.getMetadataValues(doc, fieldName);
            String baseFieldName = fieldName;
            Locale locale = documentLocale;
            if (SolrTools.isLanguageCodedField(fieldName)) {
                baseFieldName = SolrTools.getBaseFieldName(fieldName);
                locale = SolrTools.getLocale(fieldName);
            } else if ("MD_VALUE".equals(fieldName)) {
                baseFieldName = docType;
                metadata.put("METADATA_TYPE", Collections.singletonList(ViewerResourceBundle.getTranslations(baseFieldName, true)));
            }
            for (String strValue : values) {
                int valueIndex = values.indexOf(strValue);
                List<IMetadataValue> existingValues = metadata.get(baseFieldName);
                IMetadataValue existingValue = existingValues == null || existingValues.size() <= valueIndex ? null : existingValues.get(valueIndex);
                if (existingValue == null) {
                    if (locale == null) {
                        IMetadataValue value =
                                new MultiLanguageMetadataValue(new HashMap<>(Map.of(MultiLanguageMetadataValue.DEFAULT_LANGUAGE, strValue)));
                        metadata.computeIfAbsent(baseFieldName, l -> new ArrayList<>()).add(value);
                    } else {
                        IMetadataValue value = new MultiLanguageMetadataValue(new HashMap<>(Map.of(locale.getLanguage(), strValue)));
                        metadata.computeIfAbsent(baseFieldName, l -> new ArrayList<>()).add(value);
                    }
                } else {
                    if (locale == null) {
                        existingValue.setValue(strValue);
                    } else {
                        existingValue.setValue(strValue, locale);
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(docType) && metadata.containsKey(docType)) {
            metadata.put("MD_VALUE", metadata.get(docType));
        }
        return metadata;
    }

    /**
     * 
     * @param doc
     * @return
     */
    public static final String getReferenceId(SolrDocument doc) {
        String refId = getSingleFieldStringValue(doc, "MD_REFID");
        if (StringUtils.isBlank(refId)) {
            return getSingleFieldStringValue(doc, SolrConstants.IDDOC);
        }

        return refId;
    }

}
