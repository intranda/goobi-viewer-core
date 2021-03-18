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
package io.goobi.viewer.model.metadata;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.normdataimporter.model.MarcRecord;
import de.intranda.digiverso.normdataimporter.model.NormData;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrConstants.MetadataGroupType;
import io.goobi.viewer.controller.SolrSearchIndex;
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
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Metadata field configuration.
 */
public class Metadata implements Serializable {

    private static final long serialVersionUID = 5671775647919258310L;

    private static final Logger logger = LoggerFactory.getLogger(Metadata.class);

    /** ID of the owning StructElement. Used for constructing unique value IDs, where required. */
    private String ownerId;
    /** Label from messages.properties. */
    private final String label;
    /** Value from messages.properties (with placeholders) */
    private final String masterValue;
    private final int type;
    private final int number;
    private final List<MetadataValue> values = new ArrayList<>();
    private final List<MetadataParameter> params = new ArrayList<>();
    private final boolean group;
    private String ownerDocstrctType;
    private String citationTemplate;
    private CitationProcessorWrapper citationProcessorWrapper;

    /**
     * <p>
     * Constructor for Metadata.
     * </p>
     */
    public Metadata() {
        this.ownerId = "";
        this.label = "";
        this.masterValue = "";
        this.type = 0;
        this.number = -1;
        this.group = false;
    }

    /**
     * <p>
     * Constructor for Metadata.
     * </p>
     *
     * @param ownerId
     * @param label a {@link java.lang.String} object.
     * @param masterValue a {@link java.lang.String} object.
     * @param paramValue a {@link java.lang.String} object.
     */
    public Metadata(String ownerId, String label, String masterValue, String paramValue) {
        this.ownerId = ownerId;
        this.label = label;
        this.masterValue = masterValue;
        values.add(new MetadataValue(ownerId + "_" + 0, masterValue));
        if (paramValue != null) {
            values.get(0).getParamValues().add(new ArrayList<>());
            values.get(0).getParamValues().get(0).add(paramValue);
        }
        this.type = 0;
        this.number = -1;
        this.group = false;
    }

    /**
     * <p>
     * Constructor for Metadata.
     * </p>
     *
     * @param label a {@link java.lang.String} object.
     * @param masterValue a {@link java.lang.String} object.
     * @param param a {@link io.goobi.viewer.model.metadata.MetadataParameter} object.
     * @param paramValue a {@link java.lang.String} object.
     * @param locale
     */
    public Metadata(String ownerId, String label, String masterValue, MetadataParameter param, String paramValue, Locale locale) {
        this.ownerId = ownerId;
        this.label = label;
        this.masterValue = masterValue;
        params.add(param);
        values.add(new MetadataValue(ownerId + "_" + 0, masterValue));
        if (paramValue != null) {
            setParamValue(0, 0, Collections.singletonList(paramValue), label, null, null, null, locale);
            //            values.get(0).getParamValues().add(new ArrayList<>());
            //            values.get(0).getParamValues().get(0).add(paramValue);
        }
        this.type = 0;
        this.number = -1;
        this.group = false;
    }

    /**
     * <p>
     * Constructor for Metadata.
     * </p>
     *
     * @param label a {@link java.lang.String} object.
     * @param masterValue a {@link java.lang.String} object.
     * @param type a int.
     * @param params a {@link java.util.List} object.
     * @param group a boolean.
     */
    public Metadata(String label, String masterValue, int type, List<MetadataParameter> params, boolean group) {
        this.label = label;
        this.masterValue = masterValue;
        this.type = type;
        this.params.addAll(params);
        this.group = group;
        this.number = -1;
    }

    /**
     * <p>
     * Constructor for Metadata.
     * </p>
     *
     * @param label a {@link java.lang.String} object.
     * @param masterValue a {@link java.lang.String} object.
     * @param type a int.
     * @param params a {@link java.util.List} object.
     * @param group a boolean.
     * @param number a int.
     */
    public Metadata(String label, String masterValue, int type, List<MetadataParameter> params, boolean group, int number) {
        this.label = label;
        this.masterValue = masterValue;
        this.type = type;
        this.params.addAll(params);
        this.group = group;
        this.number = number;
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
        if (type != other.type) {
            return false;
        }
        return true;
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
     */
    public String getMasterValue() {
        if (StringUtils.isEmpty(masterValue)) {
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
     * <p>
     * setParamValue.
     * </p>
     *
     * @param valueIndex a int.
     * @param paramIndex a int.
     * @param inValues List with values
     * @param label a {@link java.lang.String} object.
     * @param url a {@link java.lang.String} object.
     * @param options a {@link java.util.Map} object.
     * @param groupType value of METADATATYPE, if available
     * @param locale a {@link java.util.Locale} object.
     * @return this
     * @should add multivalued param values correctly
     * @should set group type correctly
     */
    public void setParamValue(int valueIndex, int paramIndex, List<String> inValues, String label, String url, Map<String, String> options,
            String groupType, Locale locale) {
        // logger.trace("setParamValue: {}", label);
        if (inValues == null || inValues.isEmpty()) {
            return;
        }
        if (paramIndex >= params.size()) {
            logger.warn("No params defined");
            return;
        }

        // Adopt indexes to list sizes, if necessary
        while (values.size() - 1 < valueIndex) {
            values.add(new MetadataValue(ownerId + "_" + valueIndex, masterValue));
        }
        MetadataValue mdValue = values.get(valueIndex);
        mdValue.setGroupType(groupType);
        mdValue.setDocstrct(ownerDocstrctType);
        if (StringUtils.isNotEmpty(citationTemplate) && citationProcessorWrapper != null) {
            try {
                mdValue.setCitationProcessor(citationProcessorWrapper.getCitationProcessor(citationTemplate));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            mdValue.setCitationItemDataProvider(citationProcessorWrapper.getCitationItemDataProvider());
        }

        MetadataParameter param = params.get(paramIndex);
        for (String value : inValues) {
            if (param.getType() == null) {
                continue;
            }
            value = value.trim();
            switch (param.getType()) {
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
                            String[] valueSplit = value.split("[,]");
                            if (valueSplit.length > 1) {
                                value = valueSplit[1].trim() + "_" + valueSplit[0].trim();
                            }
                        }
                    }
                    value = value.trim();
                    value = value.replace("<", "");
                    value = value.replace(">", "");
                    value = value.replace(" ", "_");
                    // logger.debug("WIKIPEDIA: " + value + " paramIndex: " + paramIndex);
                    break;
                case TRANSLATEDFIELD:
                    // Values that are message keys
                    value = ViewerResourceBundle.getTranslation(value, locale);
                    // value = StringEscapeUtils.escapeHtml4(value);
                    // convert line breaks back to HTML
                    value = value.replace("&lt;br /&gt;", "<br />");
                    break;
                case UNESCAPEDFIELD:
                    // convert line breaks back to HTML
                    value = value.replace("&lt;br /&gt;", "<br />");
                    break;
                case URLESCAPEDFIELD:
                    // escape reserved URL characters
                    value = BeanUtils.escapeCriticalUrlChracters(value);
                    break;
                case HIERARCHICALFIELD:
                // create a link for reach hierarchy level
                {
                    NavigationHelper nh = BeanUtils.getNavigationHelper();
                    value = buildHierarchicalValue(label, value, locale, nh != null ? nh.getApplicationUrl() : null);
                }
                    break;
                case MILLISFIELD:
                    // Create formatted date-time from millis
                    LocalDateTime ldt = DateTools.getLocalDateTimeFromMillis(Long.valueOf(value), false);
                    value = DateTools.formatDate(ldt, locale);
                    break;
                case NORMDATAURI:
                    if (StringUtils.isNotEmpty(value)) {
                        NavigationHelper nh = BeanUtils.getNavigationHelper();
                        String normDataType = MetadataGroupType.OTHER.name();
                        // Use the last part of NORM_URI_* field name as the normdata type
                        if (param.getKey() != null) {
                            if (param.getKey().startsWith("NORM_URI_")) {
                                // Determine norm data set type from the URI field name
                                normDataType = param.getKey().replace("NORM_URI_", "");
                            } else if (param.getKey().equals("NORM_URI")) {
                                if (options != null && options.get("NORM_TYPE") != null) {
                                    // Try local NORM_TYPE value, if given
                                    normDataType = MetadataTools.findMetadataGroupType(options.get("NORM_TYPE"));
                                } else {
                                    // Fetch MARCXML record and determine norm data set type from gndspec field 075$b
                                    MarcRecord marcRecord = NormDataImporter.getSingleMarcRecord(value);
                                    if (marcRecord != null && !marcRecord.getNormDataList().isEmpty()) {
                                        for (NormData normData : marcRecord.getNormDataList()) {
                                            if ("NORM_TYPE".equals(normData.getKey())) {
                                                String val = normData.getValues().get(0).getText();
                                                normDataType = MetadataTools.findMetadataGroupType(val);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Popup button
                        String html = ViewerResourceBundle.getTranslation("NORMDATA_BUTTON", locale);
                        html = html.replace("{0}", nh.getApplicationUrl())
                                .replace("{1}", BeanUtils.escapeCriticalUrlChracters(value))
                                .replace("{2}", normDataType == null ? MetadataGroupType.OTHER.name() : normDataType)
                                .replace("{3}", nh.getLocaleString())
                                .replace("{4}", ViewerResourceBundle.getTranslation("normdataExpand", locale))
                                .replace("{5}", ViewerResourceBundle.getTranslation("normdataPopoverCloseAll", locale));
                        value = html;
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
                                                .append(':')
                                                .append(value)
                                                .append("/1/-/-/-/")
                                                .toString())
                                .replace("{1}", ViewerResourceBundle.getTranslation("search", locale));
                        value = html;
                    }
                    break;
                case CITEPROC:
                    // Use original param index to retrieve the correct destination
                    String citationKey = param.getDestination();
                    if (StringUtils.isNotEmpty(citationKey)) {
                        List<String> values = mdValue.getCitationValues().get(citationKey);
                        if (values == null) {
                            values = new ArrayList<>();
                            mdValue.getCitationValues().put(citationKey, values);
                        }
                        values.add(value);
                    }
                    value = MetadataParameterType.CITEPROC.getKey();
                    break;
                default:
                    // Values containing random HTML-like elements (e.g. 'V<a>e') will break the table, therefore escape the string
                    value = StringEscapeUtils.escapeHtml4(value);
                    // convert line breaks back to HTML
                    value = value.replace("&lt;br /&gt;", "<br />");
            }
            value = value.replace("'", "&#39;");
            value = SearchHelper.replaceHighlightingPlaceholders(value);

            if (paramIndex >= 0) {
                while (mdValue.getParamLabels().size() <= paramIndex) {
                    mdValue.getParamLabels().add("");
                }
                mdValue.getParamLabels().set(paramIndex, label);
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
        }
    }

    /**
     * 
     * @param field Index field
     * @param value Field value
     * @param locale Optional locale for value translation
     * @param applicationUrl Application root URL for hyperlinks; only the values will be included if url is null
     * @return
     * @should build value correctly
     */
    static String buildHierarchicalValue(String field, String value, Locale locale, String applicationUrl) {
        String[] valueSplit = value.split("[.]");
        StringBuilder sbFullValue = new StringBuilder();
        StringBuilder sbHierarchy = new StringBuilder();
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
                sbFullValue.append("<a href=\"").append(applicationUrl).append(PageType.browse.getName()).append("/-/1/-/");
                if (field != null) {
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
     * <p>
     * hasParam.
     * </p>
     *
     * @param paramName a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasParam(String paramName) {
        if (params != null) {
            for (MetadataParameter param : params) {
                if (param.getKey().equals(paramName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks whether any parameter values are set. 'empty' seems to be a reserved word in JSF, so use 'blank'.
     *
     * @return true if all paramValues are empty or blank; false otherwise.
     * @should return true if all paramValues are empty
     * @should return false if at least one paramValue is not empty
     */
    public boolean isBlank() {
        if (values == null || values.isEmpty()) {
            return true;
        }

        for (MetadataValue value : values) {
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
     * Populates the parameters of the given metadata with values from the given StructElement.
     *
     * @param locale a {@link java.util.Locale} object.
     * @param se a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @should use default value of no value found
     */
    public boolean populate(StructElement se, Locale locale) throws IndexUnreachableException, PresentationException {
        if (se == null) {
            return false;
        }
        ownerId = String.valueOf(se.getLuceneId());
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
            return populateGroup(se, locale);
        }

        // Regular, atomic metadata
        boolean found = false;
        for (MetadataParameter param : params) {
            // Skip topstruct-only parameters, if this is not a topstruct or anchr/group
            if (param.isTopstructOnly() && !se.isWork() && !se.isAnchor() && !se.isGroup()) {
                continue;
            }

            int count = 0;
            int indexOfParam = params.indexOf(param);
            // logger.trace("{} ({})", param.toString(), indexOfParam);
            List<String> values = null;
            if (MetadataParameterType.TOPSTRUCTFIELD.equals(param.getType()) && se.getTopStruct() != null) {
                // Topstruct values as the first choice
                values = getMetadata(se.getTopStruct().getMetadataFields(), param.getKey(), locale);
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
                    // logger.trace(param.getKey() + ":" + values.get(0));
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
                    for (String value : values) {
                        // logger.trace("{}: {}", param.getKey(), mdValue);
                        if (count >= number && number != -1) {
                            break;
                        }
                        found = true;
                        // Apply replace rules
                        if (!param.getReplaceRules().isEmpty()) {
                            value = MetadataTools.applyReplaceRules(value, param.getReplaceRules(), se.getPi());
                        }
                        setParamValue(count, indexOfParam, Collections.singletonList(value), param.getKey(), null, null, null, locale);
                        count++;
                    }
                }
            }
            if (values == null && param.getDefaultValue() != null) {
                // logger.trace("No value found for {} (index {}), using default value '{}'", param.getKey(), indexOfParam, param.getDefaultValue());
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

                // logger.debug("populate theme: type="+param.getType() + " key=" +param.getKey() + "  count="+count );
                found = true;
                // setParamValue(count, getParams().indexOf(param), param.getKey());
                count++;
            }
        }

        return found;

    }

    /**
     * 
     * @param se
     * @param locale
     * @return
     * @throws IndexUnreachableException
     */
    boolean populateGroup(StructElement se, Locale locale) throws IndexUnreachableException {
        boolean found = false;

        // Metadata grouped in an own Solr document
        if (se.getMetadataFields().get(label) == null) {
            // If there is no plain value in the docstruct doc, then there shouldn't be a metadata Solr doc. In this case save time by skipping this field.
            return false;
        }
        if (se.getMetadataFields().get(SolrConstants.IDDOC) == null || se.getMetadataFields().get(SolrConstants.IDDOC).isEmpty()) {
            return false;
        }
        String iddoc = se.getMetadataFields().get(SolrConstants.IDDOC).get(0);
        try {
            SolrDocumentList groupedMdList = MetadataTools.getGroupedMetadata(iddoc, '+' + SolrConstants.LABEL + ":" + label);
            int count = 0;
            for (SolrDocument doc : groupedMdList) {
                Map<String, List<String>> groupFieldMap = new HashMap<>();
                // Collect values for all fields in this metadata doc
                for (String fieldName : doc.getFieldNames()) {
                    List<String> values = groupFieldMap.get(fieldName);
                    if (values == null) {
                        values = new ArrayList<>();
                        groupFieldMap.put(fieldName, values);
                    }
                    // logger.trace(fieldName + ":" + doc.getFieldValue(fieldName).toString());
                    if (doc.getFieldValue(fieldName) instanceof String) {
                        String value = (String) doc.getFieldValue(fieldName);
                        values.add(value);
                    } else if (doc.getFieldValue(fieldName) instanceof Collection) {
                        values.addAll(SolrSearchIndex.getMetadataValues(doc, fieldName));

                    }
                }
                String groupType = null;
                if (groupFieldMap.containsKey(SolrConstants.METADATATYPE) && !groupFieldMap.get(SolrConstants.METADATATYPE).isEmpty()) {
                    groupType = groupFieldMap.get(SolrConstants.METADATATYPE).get(0);
                }
                // Populate params for which metadata values have been found
                for (int i = 0; i < params.size(); ++i) {
                    MetadataParameter param = params.get(i);
                    // logger.trace("param: {}", param.getKey());

                    // Skip topstruct-only parameters, if this is not a topstruct or anchor/group
                    if (param.isTopstructOnly() && !se.isWork() && !se.isAnchor() && !se.isGroup()) {
                        continue;
                    }
                    if (groupFieldMap.get(param.getKey()) != null) {
                        found = true;
                        Map<String, String> options = new HashMap<>();
                        StringBuilder sbValue = new StringBuilder();
                        List<String> values = new ArrayList<>(groupFieldMap.get(param.getKey()).size());
                        for (String value : groupFieldMap.get(param.getKey())) {
                            if (sbValue.length() == 0) {
                                sbValue.append(value);
                            }
                            values.add(value);
                        }
                        String paramValue = sbValue.toString();
                        if (param.getKey().startsWith(NormDataImporter.FIELD_URI)) {
                            if (doc.getFieldValue("NORM_TYPE") != null) {
                                options.put("NORM_TYPE", SolrSearchIndex.getSingleFieldStringValue(doc, "NORM_TYPE"));
                            }
                        }
                        setParamValue(count, i, values, param.getKey(), null, options, groupType, locale);
                    } else if (param.getDefaultValue() != null) {
                        logger.debug("No value found for {}, using default value", param.getKey());
                        setParamValue(0, i, Collections.singletonList(param.getDefaultValue()), param.getKey(), null, null, groupType, locale);
                        found = true;
                    } else {
                        setParamValue(count, i, Collections.singletonList(""), null, null, null, groupType, locale);
                    }
                }
                count++;
            }
            // logger.trace("GROUP QUERY END");
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        }

        return found;
    }

    /**
     * Return all values from the given map for either the given key, or - preferably - the given key suffixed by "_LANG_{locale.language}", i.e. the
     * language specific values for that key ( = metadata field) The return value may be null if neither the key nor the suffix key is in the map
     * 
     * @param metadataMap
     * @param key
     * @param locale
     * @return
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
     * @return the ownerDocstrct
     */
    public String getOwnerDocstrct() {
        return ownerDocstrctType;
    }

    /**
     * @param ownerDocstrct the ownerDocstrct to set
     */
    public void setOwnerDocstrct(String ownerDocstrct) {
        this.ownerDocstrctType = ownerDocstrct;
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
        // logger.trace("filterMetadataByLanguage: {}", recordLanguage);
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
            if (md.getLabel().contains(SolrConstants._LANG_)) {
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
        if (values != null) {
            return "Label: " + label + " MasterValue: " + masterValue + " paramValues: " + values.get(0).getParamValues() + " ### ";

        }
        return "Label: " + label + " MasterValue: " + masterValue + " ### ";
    }
}
