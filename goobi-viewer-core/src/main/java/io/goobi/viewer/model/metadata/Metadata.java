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

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.normdataimporter.model.NormData;
import de.intranda.digiverso.normdataimporter.model.Record;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.ActiveDocumentBean;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.citation.CitationProcessorWrapper;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.MetadataGroupType;
import io.goobi.viewer.solr.SolrTools;

/**
 * Metadata field configuration.
 */
public class Metadata implements Serializable {

    private static final long serialVersionUID = 5671775647919258310L;

    private static final Logger logger = LogManager.getLogger(Metadata.class);

    private static final String FIELD_NORM_TYPE = "NORM_TYPE";

    // Configuration

    /** Label from messages.properties. */
    private final String label;
    /** Value from messages.properties (with placeholders) */
    private final String masterValue;
    private String citationTemplate;
    private String sortField;
    /** Optional metadata field that will provide the label value (if singleString=true) */
    private String labelField;
    private String separator;
    private int type = 0;
    private int number = -1;
    private boolean group = false;
    private boolean singleString = true;
    private boolean hideIfOnlyMetadataField = false;
    private boolean topstructOnly = false;

    // Data

    private String ownerDocstrctType;
    /** ID of the owning StructElement. Used for constructing unique value IDs, where required. */
    private String ownerStructElementIddoc;
    private CitationProcessorWrapper citationProcessorWrapper;
    private int indentation = 0;
    private final List<MetadataValue> values = new ArrayList<>();
    private final List<MetadataParameter> params = new ArrayList<>();
    private final List<Metadata> childMetadata = new ArrayList<>();
    private Metadata parentMetadata;

    /**
     * <p>
     * Default constructor.
     * </p>
     */
    public Metadata() {
        this.ownerStructElementIddoc = "";
        this.label = "";
        this.masterValue = "";
    }

    /**
     * <p>
     * Constructor with a single metadata value.
     * </p>
     *
     * @param ownerIddoc
     * @param label a {@link java.lang.String} object.
     * @param masterValue a {@link java.lang.String} object.
     * @param paramValue a {@link java.lang.String} object.
     */
    public Metadata(String ownerIddoc, String label, String masterValue, String paramValue) {
        this.ownerStructElementIddoc = ownerIddoc;
        this.label = label;
        this.masterValue = masterValue;
        values.add(new MetadataValue(ownerIddoc + "_" + 0, masterValue, label));
        if (paramValue != null) {
            values.get(0).getParamValues().add(new ArrayList<>());
            values.get(0).getParamValues().get(0).add(paramValue);
        }
    }

    /**
     * <p>
     * Constructor with a {@link MetadataParameter} list.
     * </p>
     *
     * @param label a {@link java.lang.String} object.
     * @param masterValue a {@link java.lang.String} object.
     * @param params a {@link java.util.List} object.
     */
    public Metadata(String label, String masterValue, List<MetadataParameter> params) {
        this.label = label;
        this.masterValue = masterValue;
        this.params.addAll(params);
    }

    /**
     * <p>
     * Constructor for Metadata.
     * </p>
     *
     * @param ownerIddoc
     * @param label a {@link java.lang.String} object.
     * @param masterValue a {@link java.lang.String} object.
     * @param param a {@link io.goobi.viewer.model.metadata.MetadataParameter} object.
     * @param paramValue a {@link java.lang.String} object.
     * @param locale
     */
    public Metadata(String ownerIddoc, String label, String masterValue, MetadataParameter param, String paramValue, Locale locale) {
        this.ownerStructElementIddoc = ownerIddoc;
        this.label = label;
        this.masterValue = masterValue;
        params.add(param);
        values.add(new MetadataValue(ownerIddoc + "_" + 0, masterValue, label));
        if (paramValue != null) {
            setParamValue(0, 0, Collections.singletonList(paramValue), label, null, null, null, locale);
            //            values.get(0).getParamValues().add(new ArrayList<>());
            //            values.get(0).getParamValues().get(0).add(paramValue);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((masterValue == null) ? 0 : masterValue.hashCode());
        result = prime * result + type;
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Metadata other = (Metadata) obj;
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!label.equals(other.label)) {
            return false;
        }
        if (masterValue == null) {
            if (other.masterValue != null) {
                return false;
            }
        } else if (!masterValue.equals(other.masterValue)) {
            return false;
        }

        return type == other.type;
    }

    /**
     * <p>
     * isHasLabel.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHasLabel() {
        return StringUtils.isNotBlank(label);
    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * <p>
     * Getter for the field <code>masterValue</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @should return placeholders for every parameter for group metadata if masterValue empty
     * @should return single placeholder for non group metadata if masterValue empty
     */
    public String getMasterValue() {
        if (StringUtils.isEmpty(masterValue)) {
            if (group) {
                StringBuilder sb = new StringBuilder();
                int index = 1;
                for (int i = 0; i < params.size(); ++i) {
                    sb.append('{').append(index).append('}');
                    index += 2;
                }
                return sb.toString();
            }
            return "{0}";
        }

        return masterValue;
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     *
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @param type the type to set
     * @return this
     */
    public Metadata setType(int type) {
        this.type = type;
        return this;
    }

    /**
     *
     * @return List<StringPair>
     */
    public List<StringPair> getSortFields() {
        if (StringUtils.isEmpty(sortField)) {
            return null;
        }

        return Collections.singletonList(new StringPair(sortField, "asc"));
    }

    /**
     * @return the sortField
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * @param sortField the sortField to set
     * @return this
     */
    public Metadata setSortField(String sortField) {
        this.sortField = sortField;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>values</code>.
     * </p>
     *
     * @return the values
     */
    public List<MetadataValue> getValues() {
        return values;
    }

    /**
     *
     * @param ownerIddoc
     * @return Sublist of all values that belong to <code>ownerIddoc</code>; all values if <code>ownerIddoc</code> null
     * @should return all values if ownerIddoc null
     * @should return only values for the given ownerIddoc
     */
    public List<MetadataValue> getValuesForOwner(String ownerIddoc) {
        if (ownerIddoc == null) {
            return values;
        }

        List<MetadataValue> ret = new ArrayList<>(values.size());
        for (MetadataValue value : values) {
            if (ownerIddoc.equals(value.getOwnerIddoc())) {
                ret.add(value);
            }
        }

        return ret;
    }

    /**
     * 
     * @return First {@link MetadataValue}
     */
    public String getFirstValue() {
        if (!values.isEmpty()) {
            return values.get(0).getCombinedValue();
        }

        return null;
    }

    /**
     * <p>
     * setParamValue.
     * </p>
     *
     * @param valueIndex a int.
     * @param paramIndex a int.
     * @param inValues List with values
     * @param paramLabel a {@link java.lang.String} object.
     * @param url a {@link java.lang.String} object.
     * @param options a {@link java.util.Map} object.
     * @param groupType value of METADATATYPE, if available
     * @param locale a {@link java.util.Locale} object.
     * @should add multivalued param values correctly
     * @should set group type correctly
     */
    public void setParamValue(int valueIndex, int paramIndex, List<String> inValues, String paramLabel, String url, Map<String, String> options,
            String groupType, Locale locale) {
        setParamValue(valueIndex, paramIndex, inValues, new RelationshipMetadataContainer(Collections.emptyList(), Collections.emptyMap()),
                paramLabel, url, options, groupType, locale);
    }

    public void setParamValue(int valueIndex, int paramIndex, List<String> inValues, RelationshipMetadataContainer relatedMetadata, String paramLabel,
            String url, Map<String, String> options, String groupType, Locale locale) {
        // logger.trace("setParamValue: {}", label); //NOSONAR Debug
        if (inValues == null || inValues.isEmpty()) {
            return;
        }
        if (paramIndex >= params.size()) {
            logger.warn("No params defined");
            return;
        }

        // Adopt indexes to list sizes, if necessary
        while (values.size() - 1 < valueIndex) {
            MetadataValue mdValue = new MetadataValue(ownerStructElementIddoc + "_" + valueIndex, getMasterValue(), this.label);
            values.add(mdValue);
        }
        MetadataValue mdValue = values.get(valueIndex);
        mdValue.setGroupType(groupType);
        mdValue.setDocstrct(ownerDocstrctType);
        mdValue.setOwnerIddoc(ownerStructElementIddoc);
        if (StringUtils.isNotEmpty(citationTemplate) && citationProcessorWrapper != null) {
            try {
                mdValue.setCitationProcessor(citationProcessorWrapper.getCitationProcessor(citationTemplate));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            mdValue.setCitationItemDataProvider(citationProcessorWrapper.getCitationItemDataProvider());
        }

        MetadataParameter param = params.get(paramIndex);
        for (final String val : inValues) {
            if (param.getType() == null) {
                continue;
            }
            String value = val.trim();
            switch (param.getType()) {
                case RELATEDFIELD:
                    value = relatedMetadata.getMetadataValue(this.label,
                            RelationshipMetadataContainer.FIELD_IN_RELATED_DOCUMENT_PREFIX + param.getKey(), locale);
                case WIKIFIELD:
                case WIKIPERSONFIELD:
                    if (value.contains(",")) {
                        // Find and remove additional information in a person's name
                        Pattern p = Pattern.compile(StringTools.REGEX_PARENTHESES);
                        Matcher m = p.matcher(value);
                        while (m.find()) {
                            String cut = value.substring(m.start(), m.end());
                            value = value.replace(cut, "");
                            m = p.matcher(value);
                        }
                        // Revert the name around the comma (persons only)
                        if (param.getType().equals(MetadataParameterType.WIKIPERSONFIELD)) {
                            String[] valueSplit = value.split(",");
                            if (valueSplit.length > 1) {
                                value = valueSplit[1].trim() + "_" + valueSplit[0].trim();
                            }
                        }
                    }
                    value = value.trim();
                    value = value.replace("<", "");
                    value = value.replace(">", "");
                    value = value.replace(" ", "_");
                    break;
                case TRANSLATEDFIELD:
                    // Values that are message keys (or collection names, etc.)
                    value = ViewerResourceBundle.getTranslation(value, locale);
                    // convert line breaks back to HTML
                    value = value.replace(StringConstants.HTML_BR_ESCAPED, StringConstants.HTML_BR);
                    break;
                case DATEFIELD:
                    String outputPattern =
                            StringUtils.isNotBlank(param.getPattern()) ? param.getPattern() : BeanUtils.getNavigationHelper().getDatePattern();
                    String altOutputPattern = outputPattern.replace("dd/", "");
                    try {
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(outputPattern);
                        LocalDate date = LocalDate.parse(value);
                        value = date.format(dateTimeFormatter);
                    } catch (DateTimeParseException e) {
                        // No-day format hack
                        try {
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(altOutputPattern);
                            LocalDate date = LocalDate.parse(value + "-01");
                            value = date.format(dateTimeFormatter);
                        } catch (DateTimeParseException e1) {
                            logger.warn("Error parsing {} as date", value);
                        }
                    }
                    value = value.replace(StringConstants.HTML_BR_ESCAPED, StringConstants.HTML_BR);
                    break;
                case UNESCAPEDFIELD:
                    // convert line breaks back to HTML
                    value = value.replace(StringConstants.HTML_BR_ESCAPED, StringConstants.HTML_BR);
                    break;
                case URLESCAPEDFIELD:
                    // escape reserved URL characters
                    value = BeanUtils.escapeCriticalUrlChracters(value);
                    break;
                case HIERARCHICALFIELD:
                    // create a link for reach hierarchy level
                    NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
                    value = buildHierarchicalValue(paramLabel, value, locale, navigationHelper != null ? navigationHelper.getApplicationUrl() : null);
                    break;
                case MILLISFIELD:
                    // Create formatted date-time from millis
                    LocalDateTime ldt = DateTools.getLocalDateTimeFromMillis(Long.valueOf(value), false);
                    value = DateTools.formatDate(ldt, locale);
                    break;
                case NORMDATAURI:
                    if (StringUtils.isNotEmpty(value)) {
                        String normDataType = MetadataGroupType.OTHER.name();
                        // Use the last part of NORM_URI_* field name as the normdata type
                        if (param.getKey() != null) {
                            if (param.getKey().startsWith("NORM_URI_")) {
                                // Determine norm data set type from the URI field name
                                normDataType = param.getKey().replace("NORM_URI_", "");
                            } else if (param.getKey().equals("NORM_URI")) {
                                if (options != null && options.get(FIELD_NORM_TYPE) != null) {
                                    // Try local NORM_TYPE value, if given
                                    normDataType = MetadataTools.findMetadataGroupType(options.get(FIELD_NORM_TYPE));
                                } else {
                                    // Fetch authority data record and determine norm data set type from gndspec field 075$b
                                    Record authorityRecord = MetadataTools.getAuthorityDataRecord(value);
                                    if (authorityRecord != null && !authorityRecord.getNormDataList().isEmpty()) {
                                        for (NormData normData : authorityRecord.getNormDataList()) {
                                            if (FIELD_NORM_TYPE.equals(normData.getKey())) {
                                                String normVal = normData.getValues().get(0).getText();
                                                normDataType = MetadataTools.findMetadataGroupType(normVal);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Popup button
                        NavigationHelper nh = BeanUtils.getNavigationHelper();
                        if (nh != null) {
                            String html = ViewerResourceBundle.getTranslation("NORMDATA_BUTTON", locale);
                            html = html.replace("{0}", nh.getApplicationUrl())
                                    .replace("{1}", BeanUtils.escapeCriticalUrlChracters(value))
                                    .replace("{2}", normDataType == null ? MetadataGroupType.OTHER.name() : normDataType)
                                    .replace("{3}", nh.getLocaleString())
                                    .replace("{4}", ViewerResourceBundle.getTranslation("normdataExpand", locale))
                                    .replace("{5}", ViewerResourceBundle.getTranslation("normdataPopoverCloseAll", locale));
                            value = html;
                        }
                    }
                    break;
                case NORMDATASEARCH:
                    if (StringUtils.isNotEmpty(value)) {
                        NavigationHelper nh = BeanUtils.getNavigationHelper();
                        // Search button
                        String html = ViewerResourceBundle.getTranslation("NORMDATA_SEARCH_BUTTON", locale)
                                .replace("{0}",
                                        // pretty:search6 URL
                                        new StringBuilder().append(nh.getSearchUrl())
                                                .append("/-/")
                                                .append(param.getKey())
                                                .append(":%22")
                                                .append(value)
                                                .append("%22/1/-/-/-/")
                                                .toString())
                                .replace("{1}", ViewerResourceBundle.getTranslation("search", locale));
                        value = html;
                    }
                    break;
                case CITEPROC:
                    // Use original param index to retrieve the correct destination
                    String citationKey = param.getDestination();
                    if (StringUtils.isNotEmpty(citationKey)) {
                        List<String> citationValues = mdValue.getCitationValues().get(citationKey);
                        if (citationValues == null) {
                            citationValues = new ArrayList<>();
                            mdValue.getCitationValues().put(citationKey, citationValues);
                        }
                        citationValues.add(value);
                    }
                    value = MetadataParameterType.CITEPROC.getKey();
                    break;
                default:
                    // Values containing random HTML-like elements (e.g. 'V<a>e') will break the table, therefore escape the string
                    value = StringEscapeUtils.escapeHtml4(value);
                    // convert line breaks back to HTML
                    value = value.replace(StringConstants.HTML_BR_ESCAPED, StringConstants.HTML_BR);
            }
            value = value.replace("'", "&#39;");
            if (param.isRemoveHighlighting()) {
                value = SearchHelper.removeHighlightingPlaceholders(value);
            } else {
                value = SearchHelper.replaceHighlightingPlaceholders(value);
            }

            if (paramIndex >= 0) {
                while (mdValue.getParamLabels().size() <= paramIndex) {
                    mdValue.getParamLabels().add("");
                }
                mdValue.getParamLabels().set(paramIndex, paramLabel);
                while (mdValue.getParamValues().size() <= paramIndex) {
                    mdValue.getParamValues().add(new ArrayList<>());
                }
                mdValue.getParamValues().get(paramIndex).add(StringTools.intern(value));
                while (mdValue.getParamMasterValueFragments().size() <= paramIndex) {
                    mdValue.getParamMasterValueFragments().add("");
                }
                mdValue.getParamMasterValueFragments().set(paramIndex, param.getMasterValueFragment());
                while (mdValue.getParamPrefixes().size() <= paramIndex) {
                    mdValue.getParamPrefixes().add("");
                }
                mdValue.getParamPrefixes().add(paramIndex, param.getPrefix());
                while (mdValue.getParamSuffixes().size() <= paramIndex) {
                    mdValue.getParamSuffixes().add("");
                }
                mdValue.getParamSuffixes().add(paramIndex, param.getSuffix());
                while (mdValue.getParamUrls().size() <= paramIndex) {
                    mdValue.getParamUrls().add("");
                }
                mdValue.getParamUrls().add(paramIndex, url);
            }
            // Set metadata label from labelField
            if (StringUtils.isNotEmpty(labelField) && labelField.equals(param.getKey())) {
                mdValue.setLabel(value);
                // Remove value from the actual metadata value list if null master value is set
                if (MetadataValue.MASTERVALUE_NULL.equals(param.getMasterValueFragment())) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("clearing {}", mdValue.getParamValues().get(paramIndex).get(0));
                    }
                    mdValue.getParamValues().get(paramIndex).clear();
                }
            }
        }
    }

    /**
     *
     * @param field Index field
     * @param value Field value
     * @param locale Optional locale for value translation
     * @param applicationUrl Application root URL for hyperlinks; only the values will be included if url is null
     * @return Built hierarchical value
     * @should build value correctly
     * @should add configured collection sort field
     */
    static String buildHierarchicalValue(String field, String value, Locale locale, String applicationUrl) {
        String[] valueSplit = value.split("[.]");
        StringBuilder sbFullValue = new StringBuilder();
        StringBuilder sbHierarchy = new StringBuilder();
        Map<String, String> sortFields = DataManager.getInstance().getConfiguration().getCollectionDefaultSortFields(field);
        for (String s : valueSplit) {
            if (sbFullValue.length() > 0) {
                sbFullValue.append(" > ");
            }
            if (sbHierarchy.length() > 0) {
                sbHierarchy.append('.');
            }
            sbHierarchy.append(s);
            String displayValue = ViewerResourceBundle.getTranslation(sbHierarchy.toString(), locale);
            // Values containing random HTML-like elements (e.g. 'V<a>e') will break the table, therefore escape the string
            displayValue = StringEscapeUtils.escapeHtml4(displayValue);
            if (applicationUrl != null) {
                sbFullValue.append("<a href=\"").append(applicationUrl).append(PageType.browse.getName()).append("/-/1/");
                String sortField = "-";
                // Use configured collection sorting field, if available
                if (StringUtils.isNotEmpty(field)) {
                    String defaultSortField = CollectionView.getCollectionDefaultSortField(value, sortFields);
                    if (StringUtils.isNotEmpty(defaultSortField)) {
                        sortField = defaultSortField;
                    }

                }
                sbFullValue.append(sortField).append('/');
                if (StringUtils.isNotEmpty(field)) {
                    sbFullValue.append(field).append(':');
                }
                sbFullValue.append(sbHierarchy.toString()).append("/\">").append(displayValue).append("</a>");
            } else {
                sbFullValue.append(displayValue);
            }
        }

        return sbFullValue.toString();
    }

    /**
     * <p>
     * Getter for the field <code>params</code>.
     * </p>
     *
     * @return the params
     */
    public List<MetadataParameter> getParams() {
        return params;
    }

    /**
     * 
     * @return Configured index field names of parameters
     */
    public List<String> getParamFieldNames() {
        return getParams().stream().map(p -> p.getKey()).toList();
    }

    /**
     * <p>
     * hasParam.
     * </p>
     *
     * @param paramName a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasParam(String paramName) {
        for (MetadataParameter param : params) {
            if (param.getKey().equals(paramName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 
     * @return Number of params
     */
    public int getParamCount() {
        return params.size();
    }

    public String getParamValue(String field) {
        for (MetadataValue val : values) {
            String ret = val.getParamValue(field);
            if (StringUtils.isNotBlank(ret)) {
                return ret;
            }
        }

        return null;
    }

    /**
     * Checks whether any parameter values are set. 'empty' seems to be a reserved word in JSF, so use 'blank'.
     *
     * @return true if all paramValues are empty or blank; false otherwise.
     */
    public boolean isBlank() {
        return isBlank(null);
    }

    /**
     *
     * @param ownerIddoc
     * @return true if this metadata contains no non-blank values; false otherwise
     * @should return true if all paramValues are empty
     * @should return false if at least one paramValue is not empty
     * @should return true if all values have different ownerIddoc
     * @should return true if at least one value has same ownerIddoc
     */
    public boolean isBlank(String ownerIddoc) {
        if (values.isEmpty()) {
            return true;
        }

        for (MetadataValue value : getValuesForOwner(ownerIddoc)) {
            if (value.getParamValues().isEmpty()) {
                return true;
            }
            for (List<String> paramValues : value.getParamValues()) {
                for (String paramValue : paramValues) {
                    if (StringUtils.isNotBlank(paramValue)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * 
     * @param se
     * @param ownerIddoc
     * @param sortFields
     * @param locale
     * @return a boolean.
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public boolean populate(StructElement se, String ownerIddoc, List<StringPair> sortFields, Locale locale)
            throws IndexUnreachableException, PresentationException {
        return populate(se, null, ownerIddoc, sortFields, null, 0, locale);
    }

    /**
     * Populates the parameters of the given metadata with values from the given StructElement.
     *
     * @param se a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param anchorSe Optional anchor {@link StructElement}
     * @param ownerIddoc IDDOC of the owner document (either docstruct or parent metadata)
     * @param sortFields
     * @param truncateLength
     * @param locale a {@link java.util.Locale} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @should use default value of no value found
     */
    public boolean populate(StructElement se, StructElement anchorSe, String ownerIddoc, List<StringPair> sortFields,
            Map<String, Set<String>> searchTerms, int truncateLength, Locale locale) throws IndexUnreachableException, PresentationException {
        if (se == null) {
            return false;
        }

        // Skip topstruct-only parameters, if this is not a topstruct or anchor/group
        if (topstructOnly && !se.isWork() && !se.isAnchor() && !se.isGroup()) {
            return false;
        }

        this.ownerStructElementIddoc = ownerIddoc;
        ownerDocstrctType = se.getDocStructType();

        if (StringUtils.isNotEmpty(citationTemplate)) {
            ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
            if (adb != null && adb.isRecordLoaded()) {
                // Retrieve citation processor from ViewManager, if available
                if (adb.getViewManager().getCitationProcessorWrapper() == null) {
                    adb.getViewManager().setCitationProcessorWrapper(new CitationProcessorWrapper());
                }
                setCitationProcessorWrapper(adb.getViewManager().getCitationProcessorWrapper());
            } else {
                // Create a new processor for situations where no record is loaded, but a metadata view with citations is requested
                setCitationProcessorWrapper(new CitationProcessorWrapper());
            }
        }

        // Grouped metadata
        if (group) {
            if (se.getMetadataFields().get(label) == null && parentMetadata == null) {
                // If there is no plain value in the docstruct/event doc or this is a child metadata, then there shouldn't be a metadata Solr doc.
                // In this case save time by skipping this field.
                return false;
            }
            return populateGroup(se, ownerIddoc, sortFields, searchTerms, truncateLength, locale);
        }

        // Regular, atomic metadata
        boolean found = false;
        for (MetadataParameter param : params) {

            int count = 0;
            int indexOfParam = params.indexOf(param);
            // logger.trace("{} ({})", param.toString(), indexOfParam); //NOSONAR Debug
            List<String> values = null;
            if (MetadataParameterType.TOPSTRUCTFIELD.equals(param.getType()) && se.getTopStruct() != null) {
                // Use topstruct value, if the parameter has the type "topstructfield"
                values = getMetadata(se.getTopStruct().getMetadataFields(), param.getKey(), locale);
            } else if (MetadataParameterType.ANCHORFIELD.equals(param.getType())) {
                // Use anchor value, if the parameter has the type "anchorfield"
                if (anchorSe != null) {
                    values = getMetadata(anchorSe.getTopStruct().getMetadataFields(), param.getKey(), locale);
                } else {
                    // Add empty parameter if there is no anchor
                    setParamValue(0, getParams().indexOf(param), Collections.singletonList(""), null, null, null, null, locale);
                    continue;
                }
            } else {
                // Own values
                values = getMetadata(se.getMetadataFields(), param.getKey(), locale);
            }

            if (values == null && se.getTopStruct() != null && param.isTopstructValueFallback()) {
                // Topstruct values as a fallback
                values = getMetadata(se.getTopStruct().getMetadataFields(), param.getKey(), locale);
            }
            if (values != null) {
                if (MetadataParameterType.CITEPROC.equals(param.getType())) {
                    // logger.trace(param.getKey() + ":" + values.get(0)); //NOSONAR Debug
                    // Use all available values for citation
                    found = true;
                    // Apply replace rules
                    if (!param.getReplaceRules().isEmpty()) {
                        List<String> moddedValues = new ArrayList<>(values.size());
                        for (String value : values) {
                            moddedValues.add(MetadataTools.applyReplaceRules(value, param.getReplaceRules(), se.getPi()));
                        }
                        values = moddedValues;
                    }
                    setParamValue(0, indexOfParam, values, param.getKey(), null, null, null, locale);
                } else {
                    for (String val : values) {
                        // logger.trace("{}: {}", param.getKey(), val); //NOSONAR Debug
                        if (count >= number && number != -1) {
                            break;
                        }
                        found = true;
                        String value = val;
                        // Apply replace rules
                        if (!param.getReplaceRules().isEmpty()) {
                            value = MetadataTools.applyReplaceRules(value, param.getReplaceRules(), se.getPi());
                        }
                        // If a conditional query is configured, check for match first
                        if (StringUtils.isNotEmpty(param.getCondition())) {
                            String query = param.getCondition().replace("{0}", value);
                            if (DataManager.getInstance().getSearchIndex().getHitCount(query) == 0) {
                                continue;
                            }
                        }

                        // Truncate long values
                        if (truncateLength > 0 && value.length() > truncateLength) {
                            value = new StringBuilder(value.substring(0, truncateLength - 3)).append("...").toString();
                        }
                        // Add highlighting
                        if (searchTerms != null) {
                            if (searchTerms.get(getLabel()) != null) {
                                value = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(getLabel()));
                            } else if (getLabel().startsWith("MD_SHELFMARK") && searchTerms.get("MD_SHELFMARKSEARCH") != null) {
                                value = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get("MD_SHELFMARKSEARCH"));
                            }
                            if (searchTerms.get(SolrConstants.DEFAULT) != null) {
                                value = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(SolrConstants.DEFAULT));
                            }
                        }

                        setParamValue(count, indexOfParam, Collections.singletonList(value), param.getKey(), null, null, null, locale);
                        count++;
                    }
                }
            }
            if (values == null && param.getDefaultValue() != null) {
                // logger.trace("No value found for {} (index {}), using default value '{}'", //NOSONAR Debug
                // param.getKey(), indexOfParam, param.getDefaultValue()); //NOSONAR Debug
                setParamValue(0, indexOfParam, Collections.singletonList(param.getDefaultValue()), param.getKey(), null, null, null, locale);
                found = true;
                count++;
            }
            if (param.getType().equals(MetadataParameterType.LINK_MAPS) && found) {
                for (MetadataValue mdValue : this.getValues()) {
                    if (mdValue.getParamValues().size() < 2) {
                        mdValue.getParamValues().add(new ArrayList<>());
                        mdValue.getParamValues().get(0).add("");
                        mdValue.getParamValues().add(2, new ArrayList<>());
                        mdValue.getParamValues().get(2).add(0, param.getKey());
                    } else {
                        mdValue.getParamValues().get(2).add(0, param.getKey());
                    }
                }

                // logger.debug("populate theme: type="+param.getType() + " key=" +param.getKey() + "  count="+count ); //NOSONAR Debug
                found = true;
                // setParamValue(count, getParams().indexOf(param), param.getKey()); //NOSONAR Debug
                count++;
            }
        }

        return found;
    }

    /**
     *
     * @param se {@link StructElement}
     * @param ownerIddoc Owner IDDOC (either docstruct or parent metadata)
     * @param sortFields Optional field/order pairs for sorting
     * @param searchTerms
     * @param truncateLength
     * @param locale
     * @return true if successful; false otherwise
     * @throws IndexUnreachableException
     */
    boolean populateGroup(StructElement se, String ownerIddoc, List<StringPair> sortFields, Map<String, Set<String>> searchTerms, int truncateLength,
            Locale locale) throws IndexUnreachableException {
        if (ownerIddoc == null) {
            return false;
        }

        boolean found = false;
        try {
            SolrDocumentList groupedMdList = MetadataTools.getGroupedMetadata(ownerIddoc, '+' + SolrConstants.LABEL + ":" + label, sortFields);
            if (groupedMdList == null || groupedMdList.isEmpty()) {
                return false;
            }
            /**
             * Load data of related documents if any params are of type "related". Otherwise generate an empty RelationshipMetadataContainer
             */
            RelationshipMetadataContainer relatedDocuments = new RelationshipMetadataContainer(Collections.emptyList(), Collections.emptyMap());
            if (hasRelationshipMetadata()) {
                relatedDocuments = RelationshipMetadataContainer.loadRelationships(new ComplexMetadataContainer(groupedMdList));
            }
            int count = 0;
            for (SolrDocument doc : groupedMdList) {
                String metadataDocIddoc = null;
                Map<String, List<String>> groupFieldMap = new HashMap<>();
                // Collect values for all fields in this metadata doc
                for (String fieldName : doc.getFieldNames()) {
                    List<String> vals = groupFieldMap.get(fieldName);
                    if (vals == null) {
                        vals = new ArrayList<>();
                        groupFieldMap.put(fieldName, vals);
                    }
                    // logger.trace(fieldName + ":" + doc.getFieldValue(fieldName).toString()); //NOSONAR Debug
                    if (doc.getFieldValue(fieldName) instanceof String value) {
                        vals.add(value);
                    } else if (doc.getFieldValue(fieldName) instanceof Collection) {
                        vals.addAll(SolrTools.getMetadataValues(doc, fieldName));
                    }
                    // Collect IDDOC value for use as owner IDDOC for child metadata
                    if (fieldName.equals(SolrConstants.IDDOC)) {
                        metadataDocIddoc = (String) doc.getFieldValue(fieldName);
                    }
                }
                String groupType = null;
                if (groupFieldMap.containsKey(SolrConstants.METADATATYPE) && !groupFieldMap.get(SolrConstants.METADATATYPE).isEmpty()) {
                    groupType = groupFieldMap.get(SolrConstants.METADATATYPE).get(0);
                }
                // Populate params for which metadata values have been found
                for (int i = 0; i < params.size(); ++i) {
                    MetadataParameter param = params.get(i);
                    // logger.trace("param: {}", param.getKey()); //NOSONAR Debug

                    if (groupFieldMap.get(param.getKey()) != null) {
                        found = true;
                        Map<String, String> options = new HashMap<>();
                        List<String> paramValues = new ArrayList<>(groupFieldMap.get(param.getKey()).size());
                        for (String value : groupFieldMap.get(param.getKey())) {
                            // If a conditional query is configured, check for match first
                            if (StringUtils.isNotEmpty(param.getCondition())) {
                                String query = param.getCondition().replace("{0}", value);
                                if (DataManager.getInstance().getSearchIndex().getHitCount(query) == 0) {
                                    continue;
                                }
                                logger.trace("conditional value added: {}", value);
                            }

                            // Truncate long values
                            if (truncateLength > 0 && value.length() > truncateLength) {
                                value = new StringBuilder(value.substring(0, truncateLength - 3)).append("...").toString();
                            }
                            // Add highlighting
                            if (searchTerms != null) {
                                if (searchTerms.get(getLabel()) != null) {
                                    value = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(getLabel()));
                                } else if (getLabel().startsWith("MD_SHELFMARK") && searchTerms.get("MD_SHELFMARKSEARCH") != null) {
                                    value = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get("MD_SHELFMARKSEARCH"));
                                }
                                if (searchTerms.get(SolrConstants.DEFAULT) != null) {
                                    value = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(SolrConstants.DEFAULT));
                                }
                            }

                            paramValues.add(value);
                        }
                        if (param.getKey().startsWith(NormDataImporter.FIELD_URI) && doc.getFieldValue(FIELD_NORM_TYPE) != null) {
                            options.put(FIELD_NORM_TYPE, SolrTools.getSingleFieldStringValue(doc, FIELD_NORM_TYPE));
                        }
                        setParamValue(count, i, paramValues, relatedDocuments, param.getKey(), null, options, groupType, locale);
                    } else if (param.getDefaultValue() != null) {
                        logger.debug("No value found for {}, using default value", param.getKey());
                        setParamValue(0, i, Collections.singletonList(param.getDefaultValue()), relatedDocuments, param.getKey(), null, null,
                                groupType, locale);
                        found = true;
                    } else {
                        setParamValue(count, i, Collections.singletonList(""), relatedDocuments, null, null, null, groupType, locale);
                    }
                }
                // Set value IDDOC
                if (metadataDocIddoc != null && values.size() > count) {
                    MetadataValue val = values.get(count);
                    val.setIddoc(metadataDocIddoc);
                    val.setOwnerIddoc(ownerIddoc);

                    if (!getChildMetadata().isEmpty()) {
                        for (Metadata child : getChildMetadata()) {
                            // logger.trace("populating child metadata: {}", child.getLabel()); //NOSONAR Debug
                            child.populate(se, metadataDocIddoc, sortFields, locale);
                        }
                    }
                }

                count++;
            }
            // logger.trace("GROUP QUERY END"); //NOSONAR Debug
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
        }

        return found;
    }

    private boolean hasRelationshipMetadata() {
        return this.params.stream().map(param -> param.getType()).anyMatch(type -> type == MetadataParameterType.RELATEDFIELD);
    }

    /**
     * Return all values from the given map for either the given key, or - preferably - the given key suffixed by "_LANG_{locale.language}", i.e. the
     * language specific values for that key ( = metadata field) The return value may be null if neither the key nor the suffix key is in the map
     *
     * @param metadataMap
     * @param key
     * @param locale
     * @return List<String>
     */
    private static List<String> getMetadata(Map<String, List<String>> metadataMap, String key, Locale locale) {
        List<String> mdValues = null;
        if (locale != null) {
            String langKey = key + "_LANG_" + locale.getLanguage().toUpperCase();
            mdValues = metadataMap.get(langKey);
        }
        if (mdValues == null) {
            mdValues = metadataMap.get(key);
        }
        if (mdValues == null) {
            String langKey = key + "_LANG_" + IPolyglott.getDefaultLocale().getLanguage().toUpperCase();
            mdValues = metadataMap.get(langKey);
        }
        return mdValues;
    }

    /**
     * Converts aggregated person/corporation metadata to just the displayable name.
     *
     * @param aggregatedMetadata a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getPersonDisplayName(String aggregatedMetadata) {
        if (aggregatedMetadata.contains(";")) {
            StringBuilder sb = new StringBuilder();
            String[] split = aggregatedMetadata.split(";");
            if (split.length != 0) {
                sb.append(split[0]);
                if (split.length > 1 && StringUtils.isNotEmpty(split[1])) {
                    sb.append(", " + split[1]);
                }
            }
            return sb.toString();
        }

        return aggregatedMetadata;
    }

    /**
     * <p>
     * Getter for the field <code>number</code>.
     * </p>
     *
     * @return a int.
     */
    public int getNumber() {
        return number;
    }

    /**
     * @param number the number to set
     * @return this
     */
    public Metadata setNumber(int number) {
        this.number = number;
        return this;
    }

    /**
     * <p>
     * isGroup.
     * </p>
     *
     * @return the group
     */
    public boolean isGroup() {
        return group;
    }

    /**
     * @param group the group to set
     * @return this
     */
    public Metadata setGroup(boolean group) {
        this.group = group;
        return this;
    }

    /**
     * @return the singleString
     */
    public boolean isSingleString() {
        return singleString;
    }

    /**
     * @param singleString the singleString to set
     * @return this
     */
    public Metadata setSingleString(boolean singleString) {
        this.singleString = singleString;
        return this;
    }

    /**
     * @return the hideIfOnlyMetadataField
     */
    public boolean isHideIfOnlyMetadataField() {
        return hideIfOnlyMetadataField;
    }

    /**
     * @param hideIfOnlyMetadataField the hideIfOnlyMetadataField to set
     * @return this
     */
    public Metadata setHideIfOnlyMetadataField(boolean hideIfOnlyMetadataField) {
        this.hideIfOnlyMetadataField = hideIfOnlyMetadataField;
        return this;
    }

    /**
     * @return the topstructOnly
     */
    public boolean isTopstructOnly() {
        return topstructOnly;
    }

    /**
     * @param topstructOnly the topstructOnly to set
     * @return this
     */
    public Metadata setTopstructOnly(boolean topstructOnly) {
        this.topstructOnly = topstructOnly;
        return this;
    }

    /**
     * @return the labelField
     */
    public String getLabelField() {
        return labelField;
    }

    /**
     * @param labelField the labelField to set
     * @return this
     */
    public Metadata setLabelField(String labelField) {
        this.labelField = labelField;
        return this;
    }

    /**
     * @return the separator
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * @param separator the separator to set
     * @return this
     */
    public Metadata setSeparator(String separator) {
        this.separator = separator;
        return this;
    }

    /**
     * @return the ownerDocstrctType
     */
    public String getOwnerDocstrctType() {
        return ownerDocstrctType;
    }

    /**
     * @param ownerDocstrctType the ownerDocstrctType to set
     * @return this
     */
    public Metadata setOwnerDocstrctType(String ownerDocstrctType) {
        this.ownerDocstrctType = ownerDocstrctType;
        return this;
    }

    /**
     * @return the citationTemplate
     */
    public String getCitationTemplate() {
        return citationTemplate;
    }

    /**
     * @param citationTemplate the citationTemplate to set
     * @return this
     */
    public Metadata setCitationTemplate(String citationTemplate) {
        this.citationTemplate = citationTemplate;
        return this;
    }

    /**
     * @return the citationProcessorWrapper
     */
    public CitationProcessorWrapper getCitationProcessorWrapper() {
        return citationProcessorWrapper;
    }

    /**
     * @param citationProcessorWrapper the citationProcessorWrapper to set
     */
    public void setCitationProcessorWrapper(CitationProcessorWrapper citationProcessorWrapper) {
        this.citationProcessorWrapper = citationProcessorWrapper;
    }

    /**
     * @return the parentMetadata
     */
    public Metadata getParentMetadata() {
        return parentMetadata;
    }

    /**
     * @param parentMetadata the parentMetadata to set
     */
    public void setParentMetadata(Metadata parentMetadata) {
        this.parentMetadata = parentMetadata;
    }

    /**
     *
     * @return true if childMetadata not empty; false otherwise
     */
    public boolean isHasChildren() {
        return !childMetadata.isEmpty();
    }

    /**
     * @return the childMetadata
     */
    public List<Metadata> getChildMetadata() {
        return childMetadata;
    }

    /**
     * @return the indentation
     */
    public int getIndentation() {
        return indentation;
    }

    /**
     * @param indentation the indentation to set
     * @return this
     *
     */
    public Metadata setIndentation(int indentation) {
        this.indentation = indentation;
        return this;
    }

    /**
     * Returns a metadata list that contains the fields of the given metadata list minus any language-specific fields that do not match the given
     * language.
     *
     * @param metadataList a {@link java.util.List} object.
     * @param language a {@link java.lang.String} object.
     * @param field
     * @return Metadata list without any fields with non-matching language; original list if no language is given
     * @should return language-specific version of a field
     * @should return generic version if no language specific version is found
     * @should preserve metadata field order
     * @should filter by desired field name correctly
     */
    public static List<Metadata> filterMetadata(List<Metadata> metadataList, String language, String field) {
        // logger.trace("filterMetadataByLanguage: {}", recordLanguage); //NOSONAR Debug
        if (language == null || metadataList == null || metadataList.isEmpty()) {
            return metadataList;
        }

        List<Metadata> ret = new ArrayList<>(metadataList);
        Set<String> addedLanguageSpecificFields = new HashSet<>();
        Set<Metadata> toRemove = new HashSet<>();
        String languageCode = language.toUpperCase();
        for (Metadata md : metadataList) {
            if (StringUtils.isBlank(md.getLabel())) {
                continue;
            }
            if (md.getLabel().contains(SolrConstants.MIDFIX_LANG)) {
                String lang = md.getLabel().substring(md.getLabel().length() - 2);
                String rawFieldName = md.getLabel().substring(0, md.getLabel().length() - 8);
                // Mark wrong field names for removal
                if (field != null && !field.equals(rawFieldName)) {
                    toRemove.add(md);
                }
                if (languageCode.equals(lang)) {
                    addedLanguageSpecificFields.add(rawFieldName);
                } else {
                    // Mark wrong language versions for removal
                    toRemove.add(md);
                }
            } else {
                // Mark wrong non-language version field names for removal
                if (field != null && !field.equals(md.getLabel())) {
                    toRemove.add(md);
                }
            }
        }
        // Mark non-language versions for removal, if a language-specific version has been found
        for (Metadata md : ret) {
            if (addedLanguageSpecificFields.contains(md.getLabel())) {
                toRemove.add(md);
            }
        }
        ret.removeAll(toRemove);

        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (!values.isEmpty()) {
            return "Label: " + label + " MasterValue: " + masterValue + " paramValues: " + values.get(0).getParamValues() + " ### ";

        }
        return "Label: " + label + " MasterValue: " + masterValue + " ### ";
    }

    public String getCombinedValue(String separator) {
        return this.getValues().stream().map(MetadataValue::getCombinedValue).collect(Collectors.joining(separator));
    }
}
