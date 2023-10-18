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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.undercouch.citeproc.CSL;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.citation.Citation;
import io.goobi.viewer.model.citation.CitationDataProvider;
import io.goobi.viewer.model.citation.CitationTools;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.model.search.SearchHelper;

/**
 * Wrapper class for metadata parameter value groups, so that JSF can iterate through them properly.
 */
public class MetadataValue implements Serializable {

    private static final long serialVersionUID = -3162322038017977356L;

    private static final Logger logger = LogManager.getLogger(MetadataValue.class);

    static final String MASTERVALUE_NULL = "MASTERVALUE_NULL";

    private final List<String> paramLabels = new ArrayList<>();
    /**
     * List of lists with parameter values. The top list represents the different parameters, with each containing one or more values for that
     * parameter.
     */
    private final List<List<String>> paramValues = new ArrayList<>();
    private final List<String> paramMasterValueFragments = new ArrayList<>();
    private final List<String> paramPrefixes = new ArrayList<>();
    private final List<String> paramSuffixes = new ArrayList<>();
    private final List<String> paramUrls = new ArrayList<>();
    private final Map<String, String> normDataUrls = new HashMap<>();
    private final Map<String, List<String>> citationValues = new HashMap<>();
    private final List<MetadataValue> childValues = new ArrayList<>();
    /** Unique ID for citation item generation */
    private String id;
    /** IDDOC of the grouped metadata Solr doc. */
    private String iddoc;
    private String ownerIddoc;
    private String masterValue;
    private String groupType;
    private String docstrct = null;
    private String topstruct = null;
    private String label;
    private transient CSL citationProcessor = null;
    private transient CitationDataProvider citationItemDataProvider = null;
    private String citationString = null;

    /**
     * Package-private constructor.
     *
     * @param id
     * @param masterValue
     * @param label
     */
    MetadataValue(String id, String masterValue, String label) {
        this.id = id;
        this.masterValue = masterValue;
        this.label = label;
    }

    /**
     *
     * @param index
     * @return
     */
    public boolean isParamValueBlank(int index) {
        return StringUtils.isBlank(getComboValueShort(index));
    }

    /**
     *
     * @return true if all of the param values are empty or blank; false otherwise
     * @should return true if all param values blank
     * @should return false if any param value not blank
     */
    public boolean isAllParamValuesBlank() {
        for (int i = 0; i < paramValues.size(); ++i)
            if (StringUtils.isNotBlank(getComboValueShort(i))) {
                return false;
            }

        return true;
    }

    /**
     * <p>
     * getComboValueShort.
     * </p>
     *
     * @param index a int.
     * @should construct param correctly
     * @should construct multivalued param correctly
     * @should not add prefix if first param
     * @should return empty string if value index larger than number of values
     * @should return empty string if value is empty
     * @should not add empty prefix
     * @should not add empty suffix
     * @should add separator between values if no prefix used
     * @should use master value fragment correctly
     * @return a {@link java.lang.String} object.
     */
    public String getComboValueShort(int index) {
        if (paramValues.size() <= index || paramValues.get(index) == null || paramValues.get(index).isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String paramValue : paramValues.get(index)) {
            if (StringUtils.isEmpty(paramValue)) {
                continue;
            }

            // logger.trace("param value: {}", paramValue);

            if (MetadataParameterType.CITEPROC.getKey().equals(paramValue)) {
                // logger.trace("CitePROC value: {}", index);
                if (citationProcessor == null) {
                    return "No citation processor";
                }
                try {
                    if (citationString == null) {
                        citationString = new Citation(id, citationProcessor, citationItemDataProvider,
                                CitationTools.getCSLTypeForDocstrct(docstrct, topstruct),
                                citationValues).getCitationString("text");
                    }
                    return citationString;
                } catch (IOException e) {
                    logger.error(e.getMessage());
                    return e.getMessage();
                }
            }

            boolean addPrefix = true;
            boolean addSuffix = true;
            if (index == 0) {
                addPrefix = false;
            }

            String masterFragment = "{0}";
            // Configured master value fragment overrides prefix/suffix
            if (paramMasterValueFragments.size() > index && StringUtils.isNotEmpty(paramMasterValueFragments.get(index))) {
                addPrefix = false;
                addSuffix = false;
                masterFragment = ViewerResourceBundle.getTranslation(paramMasterValueFragments.get(index), null);
                // logger.trace("master fragment: {}", masterFragment);
            }
            // Only add prefix if the total parameter value lengths is > 0 so far
            if (addPrefix && paramPrefixes.size() > index && StringUtils.isNotEmpty(paramPrefixes.get(index))) {
                sb.append(paramPrefixes.get(index));
            } else if (sb.length() > 0) {
                // Use separator between values if no prefix is used
                sb.append(", ");
            }
            if (paramUrls.size() > index && StringUtils.isNotEmpty(paramUrls.get(index))) {
                StringBuilder sbUrl = new StringBuilder();
                sbUrl.append("<a href=\"").append(paramUrls.get(index)).append("\">").append(paramValue).append("</a>");
                masterFragment = masterFragment.replace("{0}", sbUrl.toString());
            } else {
                masterFragment = masterFragment.replace("{0}", paramValue);
            }
            sb.append(masterFragment);
            if (addSuffix && paramSuffixes.size() > index && StringUtils.isNotEmpty(paramSuffixes.get(index))) {
                sb.append(paramSuffixes.get(index));
            }
        }

        return sb.toString();
    }

    /**
     * <p>
     * getParamLabelWithColon.
     * </p>
     *
     * @return the paramLabels
     * @param index a int.
     */
    public String getParamLabelWithColon(int index) {
        // logger.trace("getParamLabelWithColon: {}", index);
        if (paramLabels.size() > index && paramLabels.get(index) != null) {
            // logger.trace(ViewerResourceBundle.getTranslation(paramLabels.get(index), null) + ": ");
            return ViewerResourceBundle.getTranslation(paramLabels.get(index), null) + ": ";
        }
        return "";
    }

    /**
     * <p>
     * Getter for the field <code>paramLabels</code>.
     * </p>
     *
     * @return the paramLabels
     */
    public List<String> getParamLabels() {
        return paramLabels;
    }

    /**
     * <p>
     * Getter for the field <code>paramValues</code>.
     * </p>
     *
     * @return the paramValues
     */
    public List<List<String>> getParamValues() {
        return paramValues;
    }

    /**
     * <p>
     * Getter for the field <code>paramMasterValueFragments</code>.
     * </p>
     *
     * @return the paramMasterValueFragments
     */
    public List<String> getParamMasterValueFragments() {
        return paramMasterValueFragments;
    }

    /**
     * <p>
     * Getter for the field <code>paramPrefixes</code>.
     * </p>
     *
     * @return the paramPrefixes
     */
    public List<String> getParamPrefixes() {
        return paramPrefixes;
    }

    /**
     * <p>
     * Getter for the field <code>paramSuffixes</code>.
     * </p>
     *
     * @return the paramSuffixes
     */
    public List<String> getParamSuffixes() {
        return paramSuffixes;
    }

    /**
     * <p>
     * Getter for the field <code>paramUrls</code>.
     * </p>
     *
     * @return the paramUrls
     */
    public List<String> getParamUrls() {
        return paramUrls;
    }

    /**
     * <p>
     * Getter for the field <code>normDataUrls</code>.
     * </p>
     *
     * @return the normDataUrls
     */
    public Map<String, String> getNormDataUrls() {
        return normDataUrls;
    }

    /**
     * <p>
     * getNormDataUrlKeys.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getNormDataUrlKeys() {
        if (!normDataUrls.isEmpty()) {
            return new ArrayList<>(normDataUrls.keySet());
        }

        return null;
    }

    /**
     * <p>
     * getNormDataUrl.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     * @return Not URL-encoded norm data URL
     */
    public String getNormDataUrl(String key) {
        return getNormDataUrl(key, false);
    }

    /**
     * <p>
     * getNormDataUrl.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     * @param urlEncode a boolean.
     * @return if urlEncode=true, then URL-encoded norm data URL; otherwise not encoded norm data URL
     */
    public String getNormDataUrl(String key, boolean urlEncode) {
        if (urlEncode) {
            return BeanUtils.escapeCriticalUrlChracters(normDataUrls.get(key));
        }

        return normDataUrls.get(key);
    }

    /**
     * @return the citationValues
     */
    public Map<String, List<String>> getCitationValues() {
        return citationValues;
    }

    /**
     * @return the childValues
     */
    public List<MetadataValue> getChildValues() {
        return childValues;
    }

    /**
     * <p>
     * hasParamValue.
     * </p>
     *
     * @param paramLabel a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasParamValue(String paramLabel) {
        int index = paramLabels.indexOf(paramLabel);
        if (index > -1 && index < paramValues.size()) {
            return true;
        }
        return false;
    }

    /**
     * <p>
     * getParamValue.
     * </p>
     *
     * @param paramLabel a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getParamValue(String paramLabel) {
        List<String> values = getParamValues(paramLabel);
        if (!values.isEmpty()) {
            return values.get(0);
        }

        return "";
    }

    /**
     * 
     * @param paramLabel
     * @return
     */
    public List<String> getParamValues(String paramLabel) {
        int index = paramLabels.indexOf(paramLabel);
        if (index > -1 && index < paramValues.size()) {
            return paramValues.get(index);
        }
        return Collections.emptyList();
    }

    /**
     * Applies (full HTML) search hit value highlighting to all values for the given parameter index.
     *
     * @param paramIndex Metadata parameter index
     * @param searchTerms Set of search terms
     * @should apply highlighting correctly
     */
    public void applyHighlightingToParamValue(int paramIndex, Set<String> searchTerms) {
        if (paramValues.size() <= paramIndex || paramValues.get(paramIndex) == null) {
            return;
        }
        if (searchTerms == null || searchTerms.isEmpty()) {
            return;
        }
        logger.trace("applyHighlightingToParamValue: {}", paramIndex, searchTerms);

        List<String> values = paramValues.get(paramIndex);
        for (int i = 0; i < values.size(); ++i) {
            String value = values.get(i);
            String newValue = SearchHelper.replaceHighlightingPlaceholders(SearchHelper.applyHighlightingToPhrase(value, searchTerms));
            if (!newValue.equals(value)) {
                values.set(paramIndex, newValue);
            }
        }
    }

    /**
     * @return the iddoc
     */
    public String getIddoc() {
        return iddoc;
    }

    /**
     * @param iddoc the iddoc to set
     */
    public void setIddoc(String iddoc) {
        this.iddoc = iddoc;
    }

    /**
     * @return the ownerIddoc
     */
    public String getOwnerIddoc() {
        return ownerIddoc;
    }

    /**
     * @param ownerIddoc the ownerIddoc to set
     */
    public void setOwnerIddoc(String ownerIddoc) {
        this.ownerIddoc = ownerIddoc;
    }

    public String getDisplayValue(Locale locale) {
        String[] comboValues = IntStream.range(0, paramValues.size()).mapToObj(this::getComboValueShort).toArray(String[]::new);
        return ViewerResourceBundle.getTranslationWithParameters(getMasterValue(), locale, comboValues);
    }
    
    /**
     * <p>
     * Getter for the field <code>masterValue</code>.
     * </p>
     *
     * @return the masterValue
     */
    public String getMasterValue() {
        if (StringUtils.isEmpty(masterValue)) {
            return "{0}";
        }

        return masterValue;
    }

    /**
     * <p>
     * Setter for the field <code>masterValue</code>.
     * </p>
     *
     * @param masterValue the masterValue to set
     */
    public void setMasterValue(String masterValue) {
        this.masterValue = masterValue;
    }

    /**
     * <p>
     * getGroupTypeForUrl.
     * </p>
     *
     * @return the groupType
     */
    public String getGroupTypeForUrl() {
        if (StringUtils.isEmpty(groupType)) {
            return "-";
        }
        return groupType;
    }

    /**
     * <p>
     * Setter for the field <code>groupType</code>.
     * </p>
     *
     * @param groupType the groupType to set
     * @return this
     */
    public MetadataValue setGroupType(String groupType) {
        this.groupType = groupType;
        return this;
    }

    /**
     * @return the docstrct
     */
    public String getDocstrct() {
        return docstrct;
    }

    /**
     * @param docstrct the docstrct to set
     * @return this
     */
    public MetadataValue setDocstrct(String docstrct) {
        this.docstrct = docstrct;
        return this;
    }

    /**
     * @return the topstruct
     */
    public String getTopstruct() {
        return topstruct;
    }

    /**
     * @param topstruct the topstruct to set
     */
    public void setTopstruct(String topstruct) {
        this.topstruct = topstruct;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     * @return this
     */
    public MetadataValue setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * @param citationStyle the citationStyle to set
     * @return this
     */
    public MetadataValue setCitationProcessor(CSL citationProcessor) {
        this.citationProcessor = citationProcessor;
        return this;
    }

    /**
     * @param citationItemDataProvider the citationItemDataProvider to set
     * @return this
     */
    public MetadataValue setCitationItemDataProvider(CitationDataProvider citationItemDataProvider) {
        this.citationItemDataProvider = citationItemDataProvider;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (List<String> params : this.paramValues) {
            for (String s : params) {
                sb.append("ParamValue_").append(count).append(": ").append(s).append(' ');
                count++;
            }
        }
        return sb.toString();
    }

    public String getCombinedValue() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramValues.size(); i++) {
            sb.append(getComboValueShort(i));
        }
        return sb.toString();
    }
}
