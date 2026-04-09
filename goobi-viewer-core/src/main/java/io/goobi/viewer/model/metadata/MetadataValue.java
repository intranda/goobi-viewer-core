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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.undercouch.citeproc.CSL;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.citation.Citation;
import io.goobi.viewer.model.citation.CitationDataProvider;
import io.goobi.viewer.model.citation.CitationTools;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.variables.VariableReplacer;

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
    /** Local copies of child metadata configurations containing only values for this particular instance. */
    private final List<Metadata> childMetadata = new ArrayList<>();
    private final Set<String> accessConditions = new HashSet<>();
    /** Unique ID for citation item generation. */
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
    private final VariableReplacer replacer = new VariableReplacer(DataManager.getInstance().getConfiguration());

    /**
     * Package-private constructor.
     *
     * @param id unique identifier for citation item generation
     * @param masterValue master value template string for display formatting
     * @param label metadata field label key
     */
    MetadataValue(String id, String masterValue, String label) {
        this.id = id;
        this.masterValue = masterValue;
        this.label = label;
    }

    /**
     *
     * @param index zero-based parameter index to check
     * @return true if value at index blank; false otherwise
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
        for (int i = 0; i < paramValues.size(); ++i) {
            if (StringUtils.isNotBlank(getComboValueShort(i))) {
                return false;
            }
        }

        return true;
    }

    public String getDisplayParamValue(int index) {
        return replacer.replaceFirst(getComboValueShort(index));
    }

    /**
     * getComboValueShort.
     *
     * @param index zero-based parameter index to retrieve
     * @should construct param correctly
     * @should construct multivalued param correctly
     * @should not add prefix if first param
     * @should return empty string if value index larger than number of values
     * @should return empty string if value is empty
     * @should not add empty prefix
     * @should not add empty suffix
     * @should add separator between values if no prefix used
     * @should use master value fragment correctly
     * @return the formatted combined metadata value for the given parameter index
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

            // logger.trace("param value: {}", paramValue); //NOSONAR Debug
            if (MetadataParameterType.CITEPROC.getKey().equals(paramValue)) {
                // logger.trace("CitePROC value: {}", index); //NOSONAR Debug
                if (citationProcessor == null) {
                    return "No citation processor";
                }

                if (citationString == null) {
                    citationString = new Citation(id, citationProcessor, citationItemDataProvider,
                            CitationTools.getCSLTypeForDocstrct(docstrct, topstruct),
                            citationValues).getCitationString("text");
                }
                return citationString;
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
                // logger.trace("master fragment: {}", masterFragment); //NOSONAR Debug
            }
            // Only add prefix if the total parameter value lengths is > 0 so far
            if (addPrefix && paramPrefixes.size() > index && StringUtils.isNotEmpty(paramPrefixes.get(index))) {
                sb.append(paramPrefixes.get(index));
            } else if (!sb.isEmpty()) {
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
     * getParamLabelWithColon.
     *
     * @param index zero-based parameter index to look up
     * @return the translated label for the given parameter index with an appended colon, or an empty string if the index is out of range
     */
    public String getParamLabelWithColon(int index) {
        // logger.trace("getParamLabelWithColon: {}", index); //NOSONAR Debug
        if (paramLabels.size() > index && paramLabels.get(index) != null) {
            // logger.trace(ViewerResourceBundle.getTranslation(paramLabels.get(index), null) + ": "); //NOSONAR Debug
            return ViewerResourceBundle.getTranslation(paramLabels.get(index), null) + ": ";
        }
        return "";
    }

    /**
     * Getter for the field <code>paramLabels</code>.
     *
     * @return the list of label keys for each metadata parameter
     */
    public List<String> getParamLabels() {
        return paramLabels;
    }

    /**
     * Getter for the field <code>paramValues</code>.
     *
     * @return the list of value lists for each metadata parameter
     */
    public List<List<String>> getParamValues() {
        return paramValues;
    }

    /**
     * Getter for the field <code>paramMasterValueFragments</code>.
     *
     * @return the list of master value template fragments associated with each metadata parameter
     */
    public List<String> getParamMasterValueFragments() {
        return paramMasterValueFragments;
    }

    /**
     * Getter for the field <code>paramPrefixes</code>.
     *
     * @return the list of prefix strings prepended to each parameter value during rendering
     */
    public List<String> getParamPrefixes() {
        return paramPrefixes;
    }

    /**
     * Getter for the field <code>paramSuffixes</code>.
     *
     * @return the list of suffix strings appended to each parameter value during rendering
     */
    public List<String> getParamSuffixes() {
        return paramSuffixes;
    }

    /**
     * Getter for the field <code>paramUrls</code>.
     *
     * @return the list of URLs associated with each metadata parameter value
     */
    public List<String> getParamUrls() {
        return paramUrls;
    }

    /**
     * Getter for the field <code>normDataUrls</code>.
     *
     * @return the map of norm data type keys to their resolved URLs
     */
    public Map<String, String> getNormDataUrls() {
        return normDataUrls;
    }

    /**
     * getNormDataUrlKeys.
     *
     * @return a list of norm data URL keys stored in this metadata value
     */
    public List<String> getNormDataUrlKeys() {
        if (!normDataUrls.isEmpty()) {
            return new ArrayList<>(normDataUrls.keySet());
        }

        return Collections.emptyList();
    }

    /**
     * getNormDataUrl.
     *
     * @param key norm data type identifier key
     * @return Not URL-encoded norm data URL
     */
    public String getNormDataUrl(String key) {
        return getNormDataUrl(key, false);
    }

    /**
     * getNormDataUrl.
     *
     * @param key norm data type identifier key
     * @param urlEncode true to return a URL-encoded value
     * @return if urlEncode=true, then URL-encoded norm data URL; otherwise not encoded norm data URL
     */
    public String getNormDataUrl(String key, boolean urlEncode) {
        if (urlEncode) {
            return BeanUtils.escapeCriticalUrlChracters(normDataUrls.get(key));
        }

        return normDataUrls.get(key);
    }

    
    public Map<String, List<String>> getCitationValues() {
        return citationValues;
    }

    
    public List<Metadata> getChildMetadata() {
        return childMetadata;
    }

    /**
     * 
     * @return true if thids.accessConditions not empty; false otherwise
     * @should return false if accessConditions empty
     * @should return false if only value is open access
     * @should return false if random values contained
     * @should return true if metadata access restricted condition contained
     */
    public boolean isAccessRestricted() {
        // logger.trace("access conditions for {}: {}", label, !this.accessConditions.isEmpty());
        return this.accessConditions.contains(StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED);
    }

    
    public Set<String> getAccessConditions() {
        return accessConditions;
    }

    /**
     * hasParamValue.
     *
     * @param paramLabel label key identifying the parameter
     * @return true if this metadata value has a value for the parameter identified by the given label, false otherwise
     */
    public boolean hasParamValue(String paramLabel) {
        int index = paramLabels.indexOf(paramLabel);
        return index > -1 && index < paramValues.size();
    }

    /**
     * getParamValue.
     *
     * @param paramLabel label key identifying the parameter
     * @return the first value for the parameter identified by the given label, or an empty string if not found
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
     * @param paramLabel label key identifying the parameter
     * @return List of parameter values for the given paramLabel
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
        // logger.trace("applyHighlightingToParamValue: {} ({})", paramIndex, searchTerms); //NOSONAR Debug

        List<String> values = paramValues.get(paramIndex);
        for (int i = 0; i < values.size(); ++i) {
            String value = values.get(i);
            String newValue = SearchHelper.replaceHighlightingPlaceholders(SearchHelper.applyHighlightingToPhrase(value, searchTerms));
            if (!newValue.equals(value)) {
                values.set(paramIndex, newValue);
            }
        }
    }

    
    public String getIddoc() {
        return iddoc;
    }

    
    public void setIddoc(String iddoc) {
        this.iddoc = iddoc;
    }

    
    public String getOwnerIddoc() {
        return ownerIddoc;
    }

    
    public void setOwnerIddoc(String ownerIddoc) {
        this.ownerIddoc = ownerIddoc;
    }

    /**
     * 
     * @return Display value for the current locale
     */
    public String getDisplayValue() {
        return getDisplayValue(IPolyglott.getCurrentLocale());
    }

    /**
     *
     * @param includeLabels if true, prepend parameter labels to each value
     * @return Display value for the current locale
     */
    public String getDisplayValue(boolean includeLabels) {
        return getDisplayValue(IPolyglott.getCurrentLocale(), includeLabels);
    }

    /**
     *
     * @param locale locale for translation lookup
     * @return Display value for the given locale
     */
    public String getDisplayValue(Locale locale) {
        return getDisplayValue(locale, false);
    }

    /**
     *
     * @param locale locale for translation lookup
     * @param includeLabels if true, prepend parameter labels to each value
     * @return Display value for the given locale
     */
    public String getDisplayValue(Locale locale, boolean includeLabels) {
        String[] comboValues = IntStream.range(0, paramValues.size()).mapToObj(ind -> {
            String l = includeLabels ? getParamLabelWithColon(ind) : "";
            String v = getComboValueShort(ind);
            return includeLabels ? List.of(l, v) : List.of(v);
        })
                .flatMap(List::stream)
                .toArray(String[]::new);

        String displayValue = ViewerResourceBundle.getTranslationWithParameters(getMasterValue(), locale, true, comboValues);
        displayValue = replacer.replaceFirst(displayValue);
        return displayValue;
    }

    /**
     * Getter for the field <code>masterValue</code>.
     *
     * @return the template string into which parameter values are substituted during rendering, defaulting to "{0}" if empty
     */
    public String getMasterValue() {
        if (StringUtils.isEmpty(masterValue)) {
            return "{0}";
        }

        return masterValue;
    }

    /**
     * Setter for the field <code>masterValue</code>.
     *
     * @param masterValue the template string into which parameter values are substituted during rendering
     */
    public void setMasterValue(String masterValue) {
        this.masterValue = masterValue;
    }

    /**
     * getGroupTypeForUrl.
     *
     * @return the METADATATYPE value of the owning grouped metadata structure element, or "-" if not set
     */
    public String getGroupTypeForUrl() {
        if (StringUtils.isEmpty(groupType)) {
            return "-";
        }
        return groupType;
    }

    /**
     * Setter for the field <code>groupType</code>.
     *
     * @param groupType the METADATATYPE value of the owning grouped metadata structure element
     * @return this
     */
    public MetadataValue setGroupType(String groupType) {
        this.groupType = groupType;
        return this;
    }

    
    public String getDocstrct() {
        return docstrct;
    }

    /**
     * @param docstrct the document structure type of the owning structure element
     * @return this
     */
    public MetadataValue setDocstrct(String docstrct) {
        this.docstrct = docstrct;
        return this;
    }

    
    public String getTopstruct() {
        return topstruct;
    }

    
    public void setTopstruct(String topstruct) {
        this.topstruct = topstruct;
    }

    
    public String getLabel() {
        return label;
    }

    /**
     * @param label the display label for this metadata value
     * @return this
     */
    public MetadataValue setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * @param citationProcessor the configured CSL citation processor used to format this value as a citation
     * @return this
     */
    public MetadataValue setCitationProcessor(CSL citationProcessor) {
        this.citationProcessor = citationProcessor;
        return this;
    }

    /**
     * @param citationItemDataProvider the data provider that supplies citation item data to the CSL processor
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
