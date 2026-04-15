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
package io.goobi.viewer.model.search;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import jakarta.servlet.http.HttpServletRequest;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.collections.CMSCollection;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Current faceting settings for a search.
 */
public class SearchFacets implements Serializable {

    private static final long serialVersionUID = -7170821006287251119L;

    private static final Logger logger = LogManager.getLogger(SearchFacets.class);

    private final transient Object lock = new Object();

    /** Available regular facets for the current search result. ConcurrentHashMap allows concurrent reads and fine-grained write locks,
     *  replacing the single-mutex synchronizedMap bottleneck. Display order is controlled by getAllFacetFields(), not map insertion order. */
    private final Map<String, List<IFacetItem>> availableFacets = new ConcurrentHashMap<>();
    /** Currently applied facets. */
    private final List<IFacetItem> activeFacets = new ArrayList<>();

    private final Map<String, Boolean> facetsExpanded = new HashMap<>();

    private final Map<String, String> minValues = new HashMap<>();
    private final Map<String, String> maxValues = new HashMap<>();

    private final Map<String, SortedMap<Integer, Long>> valueRanges = new HashMap<>();
    /** Map storing labels from separate label fields that were already retrieved from the index. */
    private final Map<String, String> labelMap = new HashMap<>();

    private String tempValue;

    /**
     * resetAvailableFacets.
     */
    public void resetAvailableFacets() {
        logger.trace("resetAvailableFacets");
        availableFacets.clear();
        facetsExpanded.clear();
    }

    /**
     * resetCurrentFacets.
     *
     * @should reset facets correctly
     */
    public void resetActiveFacets() {
        resetActiveFacetString();
    }

    /**
     * resetSliderRange.
     */
    public void resetSliderRange() {
        logger.trace("resetSliderRange");
        minValues.clear();
        maxValues.clear();
        valueRanges.clear();
    }

    /**
     * Generates a list containing filter queries for the selected regular and hierarchical facets.
     *
     * @param includeRangeFacets if true, range facet fields are included in the output
     * @return a list of Solr filter query strings for the currently active facets
     */
    public List<String> generateFacetFilterQueries(boolean includeRangeFacets) {
        List<String> ret = new ArrayList<>(2);

        // Add hierarchical facets
        String hierarchicalQuery = generateHierarchicalFacetFilterQuery();
        if (StringUtils.isNotEmpty(hierarchicalQuery)) {
            ret.add(hierarchicalQuery);
        }

        // Add regular facets
        List<String> queries = generateSimpleFacetFilterQueries(includeRangeFacets);
        ret.addAll(queries);

        return ret;
    }

    /**
     * Generates a filter query for the selected hierarchical facets.
     *
     * @return Generated Solr query
     * @should generate query correctly
     * @should return null if facet list is empty
     */
    String generateHierarchicalFacetFilterQuery() {
        if (activeFacets.isEmpty()) {
            return null;
        }

        StringBuilder sbQuery = new StringBuilder();
        int count = 0;
        for (IFacetItem facetItem : getActiveFacetsCopy()) {
            if (!facetItem.isHierarchial()) {
                continue;
            }
            if (count > 0) {
                sbQuery.append(SolrConstants.SOLR_QUERY_AND);
            }
            String field = SearchHelper.facetifyField(facetItem.getField());
            sbQuery.append('(')
                    .append(field)
                    .append(':')
                    .append("\"" + facetItem.getValue() + "\"")
                    .append(SolrConstants.SOLR_QUERY_OR)
                    .append(field)
                    .append(':')
                    .append(facetItem.getValue())
                    .append(".*)");
            count++;
        }

        return sbQuery.toString();
    }

    /**
     * Generates a filter query for the selected non-hierarchical facets.
     *
     * @param includeRangeFacets if true, range facet fields are included in the query
     * @return List of generated Solr queries
     * @should generate queries correctly
     * @should return empty list if facet list empty
     * @should skip range facet fields if so requested
     * @should skip subelement fields
     * @should skip hierarchical fields
     * @should combine facet queries if field name same
     */
    List<String> generateSimpleFacetFilterQueries(boolean includeRangeFacets) {
        if (activeFacets.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> ret = new ArrayList<>();
        Map<String, StringBuilder> queries = LinkedHashMap.newLinkedHashMap(activeFacets.size());

        for (IFacetItem facetItem : getActiveFacetsCopy()) {
            if (facetItem.isHierarchial() || facetItem.getField().equals(SolrConstants.DOCSTRCT_SUB)
                    || (!includeRangeFacets && DataManager.getInstance().getConfiguration().getRangeFacetFields().contains(facetItem.getField()))) {
                continue;
            }
            StringBuilder sbQuery = queries.computeIfAbsent(facetItem.getField(), k -> new StringBuilder());
            if (!sbQuery.isEmpty()) {
                if ("OR".equalsIgnoreCase(DataManager.getInstance().getConfiguration().getMultiValueOperatorForField(facetItem.getField()))) {
                    sbQuery.append(' ');
                } else {
                    sbQuery.append(SolrConstants.SOLR_QUERY_AND);
                }
            }
            sbQuery.append(facetItem.getQueryEscapedLink());
        }

        for (Entry<String, StringBuilder> entry : queries.entrySet()) {
            ret.add(entry.getValue().toString());
            logger.trace("Added facet: {}", entry.getValue());
        }

        return ret;
    }

    /**
     * Generates a filter query for the selected subelement facets.
     *
     * @return Generated Solr query
     * @should generate query correctly
     */
    String generateSubElementFacetFilterQuery() {
        StringBuilder sbQuery = new StringBuilder();

        if (!activeFacets.isEmpty()) {
            for (IFacetItem facetItem : getActiveFacetsCopy()) {
                if (facetItem.getField().equals(SolrConstants.DOCSTRCT_SUB)) {
                    if (sbQuery.length() > 0) {
                        sbQuery.append(" AND ");
                    }
                    sbQuery.append(facetItem.getQueryEscapedLink());
                    logger.trace("Added subelement facet: {}", facetItem.getQueryEscapedLink());
                }
            }
        }

        return sbQuery.toString();
    }

    /**
     * Returns a list of FacetItem objects in <code>activeFacets</code> where the field name matches the given field name.
     *
     * @param field Solr facet field name to match against active facets
     * @return a list of active facet items that belong to the given Solr field
     * @should return correct items
     */
    public List<IFacetItem> getActiveFacetsForField(String field) {
        List<IFacetItem> ret = new ArrayList<>();
        for (IFacetItem facet : getActiveFacetsCopy()) {
            if (facet.getField().equals(field)) {
                ret.add(facet);
            }
        }

        return ret;
    }

    /**
     *
     * @param link facet query link string to look up in the active facets
     * @return true if given link is part of the active facet string; false otherwise
     */
    public boolean isFacetStringCurrentlyUsed(String link) {
        try {
            return isFacetCurrentlyUsed(new FacetItem(link, false));
        } catch (IllegalArgumentException e) {
            // Log link in quotes so empty strings are visible, and include the request URL for context
            HttpServletRequest req = BeanUtils.getRequest();
            String requestUrl = req != null ? (req.getRequestURL().toString()
                    + (req.getQueryString() != null ? "?" + req.getQueryString() : "")) : "unknown";
            logger.warn("Field and value are not colon-separated: '{}'; requestUrl='{}'", link, requestUrl);
            return false;
        }
    }

    /**
     * Checks whether the given facet is currently in use.
     *
     * @param facet facet item to check for active use
     * @return true if the given facet item is currently active (i.e. selected by the user), false otherwise
     * @should return correct value
     */
    public boolean isFacetCurrentlyUsed(IFacetItem facet) {
        for (IFacetItem fi : getActiveFacetsForField(facet.getField())) {
            if (fi.getLink().equals(facet.getLink())) {
                return true;
            }
        }

        return false;
    }

    /**
     * isFacetListSizeSufficient.
     *
     * @param field Solr facet field name to check
     * @return true if the facet list for the given field has enough elements to be shown (more than one,
     *         or more than zero for DOCSTRCT_SUB), false otherwise
     */
    public boolean isFacetListSizeSufficient(String field) {
        // logger.trace("isFacetListSizeSufficient: {}", field); //NOSONAR Debug
        if (availableFacets.get(field) != null) {
            if (SolrConstants.DOCSTRCT_SUB.equals(field)) {
                return getAvailableFacetsListSizeForField(field) > 0;
            }
            return getAvailableFacetsListSizeForField(field) > 1;
        }

        return false;
    }

    /**
     * Returns the size of the full element list of the facet for the given field.
     *
     * @param field Solr facet field name to look up
     * @return a int.
     */
    public int getAvailableFacetsListSizeForField(String field) {
        // Store in local variable to avoid TOCTOU race on the synchronized map:
        // a concurrent clear() between the null-check and the second get() would cause NPE.
        List<IFacetItem> facetItems = availableFacets.get(field);
        if (facetItems != null) {
            return facetItems.size();
        }

        return 0;
    }

    /**
     * getActiveFacetsSizeForField.
     *
     * @param field Solr facet field name to count active facets for
     * @return Size of <code>activeFacets</code>.
     */
    public int getActiveFacetsSizeForField(String field) {
        return getActiveFacetsForField(field).size();
    }

    /**
     * Returns a collapsed sublist of the available facet elements for the given field.
     *
     * @param field Solr facet field name whose items to retrieve
     * @return a list of available facet items for the given field, trimmed to the configured display limit unless expanded
     * @should return full DC facet list if expanded
     * @should return full DC facet list if list size less than default
     * @should return reduced DC facet list if list size larger than default
     * @should return full facet list if expanded
     * @should return full facet list if list size less than default
     * @should return reduced facet list if list size larger than default
     * @should not contain currently used facets
     */
    public List<IFacetItem> getLimitedFacetListForField(String field) {
        return getAvailableFacetsForField(field, true);
    }

    /**
     *
     * @param field Solr facet field name to look up in the available facets map
     * @param excludeSelected If true, selected facets will be removed from the list
     * @return List<IFacetItem>
     */
    public List<IFacetItem> getAvailableFacetsForField(String field, boolean excludeSelected) {
        // logger.trace("getAvailableFacetsForField: {}", field); //NOSONAR Debug
        List<IFacetItem> facetItems = availableFacets.get(field);
        if (facetItems == null) {
            return Collections.emptyList();
        }

        // Remove currently used facets (unless boolean type)
        if (excludeSelected) {
            facetItems.removeAll(activeFacets);
        }

        // Trim to initial number
        int initialNumber = DataManager.getInstance().getConfiguration().getInitialFacetElementNumber(field);
        if (!isFacetExpanded(field) && initialNumber != -1 && facetItems.size() > initialNumber) {
            // Return a defensive copy instead of a subList view to prevent ConcurrentModificationException
            // when this session-scoped bean is accessed concurrently by multiple requests (e.g., AJAX).
            return new ArrayList<>(facetItems.subList(0, initialNumber));
        }

        return facetItems;
    }

    /**
     *
     * @param field Solr facet field name to search for
     * @param value facet value to match within the given field
     * @return Specific facet item for the given field and value; null if none found
     * @should return null if field or value null
     * @should return correct facet item
     */
    public IFacetItem getFacet(String field, String value) {
        if (field == null || value == null) {
            return null;
        }

        List<IFacetItem> facetItems = availableFacets.get(field);
        if (facetItems != null) {
            for (IFacetItem item : facetItems) {
                if (value.equals(item.getValue())) {
                    return item;
                }
            }
        }

        return null;
    }

    /**
     * Checks whether there are still selectable values across all available facet fields.
     * 
     * @return true if any available facet field has at least one unselected value; false otherwise
     * @should return true if a facet field has selectable values
     * @should return false of no selectable values found
     * @should return false if only range facets available
     */
    public boolean isUnselectedValuesAvailable() {
        // ConcurrentHashMap.keySet() is thread-safe; no external synchronization needed
        List<String> availableFacetFields = new ArrayList<>(availableFacets.keySet());
        for (String field : availableFacetFields) {
            if (!getAvailableFacetsForField(field, true).isEmpty()
                    && !DataManager.getInstance().getConfiguration().getRangeFacetFields().contains(field)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 
     * @return true if any configured range facet field has a value range in the current search result; false otherwise
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return correct value
     */
    public boolean isHasRangeFacets() throws PresentationException, IndexUnreachableException {
        for (String rangeField : DataManager.getInstance().getConfiguration().getRangeFacetFields()) {
            // Use Objects.equals for null-safe comparison in case getAbsoluteMinRangeValue/getAbsoluteMaxRangeValue
            // return null due to a concurrent map clear between containsKey() and get().
            if (!Objects.equals(getAbsoluteMinRangeValue(rangeField), getAbsoluteMaxRangeValue(rangeField))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sets the expanded flag to <code>true</code> for the given facet field.
     *
     * @param field Solr facet field name to expand
     */
    public void expandFacet(String field) {
        logger.trace("expandFacet: {}", field);
        facetsExpanded.put(field, true);
    }

    /**
     * Sets the expanded flag to <code>false</code> for the given facet field.
     *
     * @param field Solr facet field name to collapse
     */
    public void collapseFacet(String field) {
        logger.trace("collapseFacet: {}", field);
        facetsExpanded.put(field, false);
    }

    /**
     * Returns true if the "(more)" link is to be displayed for a facet box. This is the case if the facet has more elements than the initial number
     * of displayed elements and the facet hasn't been manually expanded yet.
     *
     * @param field Solr facet field name to check expansion state for
     * @should return true if DC facet collapsed and has more elements than default
     * @should return true if facet collapsed and has more elements than default
     * @should return false if DC facet expanded
     * @should return false if facet expanded
     * @should return false if DC facet smaller than default
     * @should return false if facet smaller than default
     * @return true if the facet is collapsed and has more elements than the configured initial display count, false otherwise
     */
    public boolean isDisplayFacetExpandLink(String field) {
        List<IFacetItem> facetItems = availableFacets.get(field);
        int expandSize = DataManager.getInstance().getConfiguration().getInitialFacetElementNumber(field);

        return facetItems != null && !isFacetExpanded(field) && expandSize > 0 && facetItems.size() > expandSize;
    }

    /**
     * isDisplayFacetCollapseLink.
     *
     * @param field Solr facet field name to check expansion state for
     * @return true if the facet for the given field is currently expanded and a collapse link should be shown, false otherwise
     */
    public boolean isDisplayFacetCollapseLink(String field) {
        return isFacetExpanded(field);
    }

    /**
     * Returns a URL encoded SSV string of facet fields and values from the elements in <code>activeFacets</code> (hyphen if empty).
     *
     * @return SSV string of facet queries or "-" if empty
     * @should contain queries from all FacetItems
     * @should return hyphen if currentFacets empty
     */
    public String getActiveFacetString() {
        String ret = generateFacetPrefix(getActiveFacetsCopy(), null, true);
        if (StringUtils.isEmpty(ret)) {
            ret = "-";
        }
        try {
            return URLEncoder.encode(ret, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return ret;
        }
    }

    /**
     * Receives an SSV string of facet fields and values (FIELD1:value1;FIELD2:value2;FIELD3:value3) and generates new Elements for currentFacets.
     *
     * @param activeFacetString SSV-encoded string of active facet field:value pairs
     * @should create FacetItems from all links
     * @should decode slashes and backslashes
     * @should reset slider range if no slider field among current facets
     * @should not reset slider range if slider field among current facets
     */
    public void setActiveFacetString(String activeFacetString) {
        synchronized (lock) {
            parseFacetString(activeFacetString, activeFacets, labelMap);
        }
    }

    /**
     * Constructs a list of facet items out of the given facet string.
     *
     * @param facetString String containing field:value pairs
     * @param facetItems List of facet items to which to add the parsed items
     * @param labelMap Optional map containing labels for a field:value pair if the facet field uses separate labels
     * @should fill list correctly
     * @should empty list before filling
     * @should add DC field prefix if no field name is given
     * @should set hierarchical status correctly
     * @should use label from labelMap if available
     * @should parse wildcard facets correctly
     * @should create multiple items from multiple instances of same field
     * @should skip value pairs if field or value missing
     * @should skip facet links with leading semicolon caused by triple separators in URL
     */
    static void parseFacetString(final String facetString, final List<IFacetItem> facetItems, final Map<String, String> labelMap) {
        if (facetItems == null) {
            throw new IllegalArgumentException("facetItems may not be null");
        }
        facetItems.clear();
        if (StringUtils.isEmpty(facetString) || "-".equals(facetString)) {
            return;
        }

        String useFacetString = facetString;
        try {
            useFacetString = URLDecoder.decode(useFacetString, StandardCharsets.UTF_8.name());
            useFacetString = StringTools.unescapeCriticalUrlChracters(useFacetString);
            useFacetString = URLDecoder.decode(useFacetString, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            //
        }

        String[] facetStringSplit = useFacetString.split(";;");
        for (final String fl : facetStringSplit) {
            String facetLink = fl != null ? fl.trim() : "";
            // Skip empty, undefined, or structurally invalid links. Also skip links with a
            // leading ';', which occur when a bot-crawled URL contains triple separators
            // (';;;') — splitting on ';;' leaves one leftover ';' at the start of the next
            // token, e.g. ';MD_GENRE_LANG_EN:value'. Passing such a field name to Solr
            // causes an "undefined field ;MD_GENRE_LANG_EN" error.
            if ("".equals(facetLink) || "undefined".equals(facetLink) || facetLink.startsWith(":") || facetLink.startsWith(";")
                    || facetLink.endsWith(":")) {
                logger.warn("Invalid facet, skipping: {}", facetLink);
                continue;
            }

            if (!facetLink.contains(":")) {
                facetLink = new StringBuilder(SolrConstants.DC).append(':').append(facetLink).toString();
            }
            String facetField = facetLink.substring(0, facetLink.indexOf(":"));
            if (DataManager.getInstance().getConfiguration().getGeoFacetFields().contains(facetField)) {
                GeoFacetItem item = new GeoFacetItem(facetField);
                item.setValue(facetLink.substring(facetLink.indexOf(":") + 1));
                facetItems.add(item);
            } else {
                // If there is a cached pre-generated label for this facet link (separate label field), use it so that there's no empty label
                String label = labelMap != null && labelMap.containsKey(facetLink) ? labelMap.get(facetLink) : null;
                facetItems.add(
                        new FacetItem(facetLink, label, isFieldHierarchical(facetLink.substring(0, facetLink.indexOf(":")))));
            }
        }
    }

    /**
     *
     * @param field Solr facet field name to test for hierarchical configuration
     * @return true if field is hierarchical; false otherwise
     */
    static boolean isFieldHierarchical(String field) {
        //        logger.trace("isFieldHierarchical: {} ? {}", field, //NOSONAR Debug
        return DataManager.getInstance().getConfiguration().getHierarchicalFacetFields().contains(field);
    }

    /**
     * Updates existing facet item for the given field with a new value. If no item for that field yet exist, a new one is added.
     *
     * @param field Solr facet field name whose item to update
     * @param hierarchical if true, the facet item is marked as hierarchical
     * @return the JSF navigation outcome after updating the facet (e.g. "pretty:search6")
     */
    public String updateFacetItem(String field, boolean hierarchical) {
        // Synchronize on lock (same as other write operations) to protect activeFacets.
        synchronized (lock) {
            updateFacetItem(field, tempValue, activeFacets, hierarchical);
        }
        return "pretty:search6"; // TODO advanced search
    }

    /**
     * Updates existing facet item for the given field with a new value. If no item for that field yet exist, a new one is added.
     *
     * @param field Solr facet field name whose item is to be updated
     * @param updateValue new facet value to set for the given field
     * @param facetItems list of active facet items to update in place
     * @param hierarchical if true, the new facet item is marked as hierarchical
     * @should update facet item correctly
     * @should add new item correctly
     */
    static void updateFacetItem(String field, final String updateValue, final List<IFacetItem> facetItems, boolean hierarchical) {
        if (StringUtils.isEmpty(updateValue) || "-".equals(updateValue)) {
            return;
        }
        if (facetItems == null) {
            throw new IllegalArgumentException("facetItems may no be null");
        }

        String useUpdateValue = updateValue;
        try {
            useUpdateValue = URLDecoder.decode(useUpdateValue, "utf-8");
            useUpdateValue = StringTools.unescapeCriticalUrlChracters(useUpdateValue);
        } catch (UnsupportedEncodingException e) {
            //
        }

        IFacetItem fieldItem = null;
        for (IFacetItem item : facetItems) {
            if (item.getField().equals(field)) {
                fieldItem = item;
                break;
            }
        }
        if (fieldItem == null) {
            List<String> geoFacetFields = DataManager.getInstance().getConfiguration().getGeoFacetFields();
            if (!geoFacetFields.isEmpty() && geoFacetFields.get(0).equals(field)) {
                fieldItem = new GeoFacetItem(field);
                fieldItem.setValue(useUpdateValue);
            } else {
                fieldItem = new FacetItem(field + ":" + useUpdateValue, hierarchical);
            }
            facetItems.add(fieldItem);
        }
        fieldItem.setLink(field + ":" + useUpdateValue);
        logger.trace("Facet item updated: {}", fieldItem.getLink());
    }

    /**
     * getHierarchicalFacets.
     *
     * @param facetString SSV-encoded string of active facet field:value pairs
     * @param facetFields list of hierarchical facet field names to extract values for
     * @return a list of hierarchical facet values extracted from the facet string for the given fields
     */
    public static List<String> getHierarchicalFacets(String facetString, List<String> facetFields) {
        List<String> facets = Arrays.asList(StringUtils.split(facetString, ";;"));
        List<String> values = new ArrayList<>();

        for (String facetField : facetFields) {
            String matchingFacet = facets.stream()
                    .filter(facet -> facet.replace(SolrConstants.SUFFIX_UNTOKENIZED, "").startsWith(facetField + ":"))
                    .findFirst()
                    .orElse("");
            if (StringUtils.isNotBlank(matchingFacet)) {
                int separatorIndex = matchingFacet.indexOf(":");
                if (separatorIndex > 0 && separatorIndex < matchingFacet.length() - 1) {
                    String value = matchingFacet.substring(separatorIndex + 1);
                    values.add(value);
                }
            }
        }
        return values;
    }

    /**
     * splitHierarchicalFacet.
     *
     * @param facet hierarchical facet value string to split into path segments
     * @return a list of hierarchical path segment strings from the root level down to the given facet value
     */
    public static List<String> splitHierarchicalFacet(final String facet) {
        List<String> facets = new ArrayList<>();
        String f = facet;
        while (f.contains(".")) {
            facets.add(f);
            f = f.substring(0, f.lastIndexOf("."));
        }
        if (StringUtils.isNotBlank(f)) {
            facets.add(f);
        }
        Collections.reverse(facets);
        return facets;
    }

    /**
     * getCurrentMinRangeValue.
     *
     * @param field Solr range facet field name to look up
     * @return Current min value, if facet in use; otherwise absolute min value for that field
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getCurrentMinRangeValue(String field) throws PresentationException, IndexUnreachableException {
        synchronized (lock) {
            for (IFacetItem item : getActiveFacetsCopy()) {
                if (item.getField().equals(field)) {
                    logger.trace("currentMinRangeValue: {}", item.getValue());
                    return item.getValue();
                }
            }
        }

        return getAbsoluteMinRangeValue(field);
    }

    /**
     * getCurrentMaxRangeValue.
     *
     * @param field Solr range facet field name to look up
     * @return Current max value, if facet in use; otherwise absolute max value for that field
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getCurrentMaxRangeValue(String field) throws PresentationException, IndexUnreachableException {
        synchronized (lock) {
            for (IFacetItem item : getActiveFacetsCopy()) {
                if (item.getField().equals(field) && item.getValue2() != null) {
                    logger.trace("currentMaxRangeValue: {}", item.getValue());
                    return item.getValue2();
                }
            }

            return getAbsoluteMaxRangeValue(field);
        }
    }

    /**
     * Returns the minimum value for the given field available in the search index.
     *
     * @param field Solr range facet field name to look up
     * @return Smallest available value
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getAbsoluteMinRangeValue(String field) throws PresentationException, IndexUnreachableException {
        // Use a single get() call instead of containsKey() + get() to avoid a race condition
        // where another thread clears the map (resetSliderRange) between the two calls,
        // causing get() to return null even though containsKey() returned true.
        String value = minValues.get(field);
        return value != null ? value : "0";
    }

    /**
     * Returns the maximum value for the given field available in the search index.
     *
     * @param field Solr range facet field name to look up
     * @return Largest available value
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getAbsoluteMaxRangeValue(String field) throws PresentationException, IndexUnreachableException {
        // Use a single get() call instead of containsKey() + get() to avoid a race condition
        // where another thread clears the map (resetSliderRange) between the two calls,
        // causing get() to return null even though containsKey() returned true.
        String value = maxValues.get(field);
        return value != null ? value : "0";
    }

    /**
     * Returns a sorted list of all available values for the given field among available facet values.
     *
     * @param field Solr range facet field name to look up
     * @return sorted list of all values for the given field among available facet values
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<Integer> getValueRange(String field) throws PresentationException, IndexUnreachableException {
        if (!maxValues.containsKey(field)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(valueRanges.get(field).keySet());
    }

    /**
     *
     * @param field Solr range facet field name whose value range to serialize
     * @return {@link String}
     */
    public String getValueRangeAsJsonMap(String field) {
        if (!maxValues.containsKey(field)) {
            return "[]";
        }
        return new JSONObject(valueRanges.get(field)).toString();
    }

    /**
     *
     * @param field Solr range facet field name to check for an active range constraint
     * @return true if active range for field is currently smaller than the absolute range; false otherwise
     */
    public boolean isRangeFacetActive(String field) {
        try {
            List<Integer> range = getValueRange(field);
            if (!range.isEmpty()) {
                return Integer.parseInt(getCurrentMinRangeValue(field)) > range.get(0)
                        || Integer.parseInt(getCurrentMaxRangeValue(field)) < range.get(range.size() - 1);
            }
        } catch (PresentationException | IndexUnreachableException | NullPointerException | NumberFormatException e) {
            logger.warn("Unable to parse range values of range slider for field {}: {}", field, e.toString());
        }
        return false;
    }

    /**
     * Adds the min and max values from the search index for the given field to the bottomValues map. Min and max values are determined via an
     * alphanumeric comparator.
     *
     * @param field Solr range facet field name for which to compute min and max values
     * @param counts sorted map of facet values and their document counts from the index
     * @should populate values correctly
     * @should add all values to list
     * @should use configured min max values correctly
     */
    void populateAbsoluteMinMaxValuesForField(String field, SortedMap<String, Long> counts) {
        if (field == null) {
            return;
        }

        if (!SolrConstants.CALENDAR_YEAR.equals(field) && !field.startsWith(SolrConstants.PREFIX_MDNUM)) {
            logger.info("{} is not an integer type field, cannot use with a range query", field);
            return;
        }

        SortedMap<Integer, Long> intValues = new TreeMap<>();
        if (counts != null) {
            for (Entry<String, Long> e : counts.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) {
                    continue;
                }
                // Only add values inside the min/max range for the field, if any configured
                int keyValue = Integer.parseInt(e.getKey());
                if (keyValue >= getRangeFacetMinValue(field) && keyValue <= getRangeFacetMaxValue(field)) {
                    intValues.put(keyValue, e.getValue());
                }
            }
        } else {
            logger.trace("No facets found for field {}", field);
        }
        if (!intValues.isEmpty()) {
            valueRanges.put(field, intValues);
            minValues.put(field, String.valueOf(intValues.firstKey()));
            maxValues.put(field, String.valueOf(intValues.lastKey()));
            logger.trace("Absolute range for field {}: {} - {}", field, intValues.firstKey(), intValues.lastKey());
        }
    }

    /**
     * resetActiveFacetString.
     */
    public void resetActiveFacetString() {
        logger.trace("resetActivetFacetString");
        setActiveFacetString("-");
    }

    /**
     * Returns a URL encoded value returned by generateFacetPrefix() for regular facets. Returns an empty string instead a hyphen if empty.
     *
     * @return the URL-encoded active facet string prefix for non-hierarchical facets
     */
    public String getActiveFacetStringPrefix() {
        return getActiveFacetStringPrefix(null, true);
    }

    /**
     * 
     * @param omitField Field name to not include in the string
     * @return {@link java.lang.String} object.
     */
    public String getActiveFacetStringPrefix(String omitField) {
        return getActiveFacetStringPrefix(Collections.singletonList(omitField), true);
    }

    /**
     * Returns the value returned by generateFacetPrefix() for regular facets. Returns an empty string instead a hyphen if empty.
     *
     * @param omitFields Field names to omit from the facet string
     * @param urlEncode if true, the resulting string is URL-encoded
     * @return URL part for currently selected facets; empty string if empty
     */
    public String getActiveFacetStringPrefix(List<String> omitFields, boolean urlEncode) {
        // logger.trace("getActiveFacetStringPrefix"); //NOSONAR Debug
        if (urlEncode) {
            try {
                return URLEncoder.encode(generateFacetPrefix(getActiveFacetsCopy(), omitFields, true), SearchBean.URL_ENCODING);
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
        }

        return generateFacetPrefix(activeFacets, omitFields, true);
    }

    /**
     * Generates an SSV string of facet fields and values from the elements in the given List<FacetString> (empty string if empty).
     *
     * @param facetItems list of active facet items to serialize into the prefix string
     * @param omitFields Field names to omit from the facet string
     * @param escapeSlashes If true, slashes and backslashes are replaced with URL-compatible replacement strings
     * @return Generated prefix
     * @should encode slashed and backslashes
     */
    static String generateFacetPrefix(List<IFacetItem> facetItems, List<String> omitFields, boolean escapeSlashes) {
        if (facetItems == null) {
            throw new IllegalArgumentException("facetItems may not be null");
        }
        StringBuilder sb = new StringBuilder();
        for (IFacetItem facetItem : facetItems) {
            if (omitFields != null && omitFields.contains(facetItem.getField())) {
                continue;
            }
            if (escapeSlashes) {
                sb.append(BeanUtils.escapeCriticalUrlChracters(facetItem.getLink()));
            } else {
                sb.append(facetItem.getLink());
            }
            sb.append(";;");
        }

        return sb.toString();
    }

    /**
     * removeFacetAction.
     *
     * @param facetQuery facet query string to remove from active facets
     * @should remove facet correctly
     * @should remove facet containing reserved chars
     * @should sanitize triple semicolons to double after removal
     * @param ret navigation outcome string to return after removal
     * @return the navigation outcome string after removing the facet
     */
    public String removeFacetAction(final String facetQuery, final String ret) {
        logger.trace("removeFacetAction: {}", facetQuery);
        String currentFacetString = generateFacetPrefix(getActiveFacetsCopy(), null, false);
        if (currentFacetString.contains(facetQuery)) {
            // After removing a facet, ';;;;' can appear where the removed entry was flanked by
            // ';;' separators. A facet value ending with ';' can additionally produce ';;;'.
            // Both patterns are collapsed back to the standard ';;' separator.
            currentFacetString = currentFacetString.replaceAll("(" + Pattern.quote(facetQuery) + ")(?=;|(?=/))", "")
                    .replace(";;;;", ";;")
                    .replace(";;;", ";;");
            setActiveFacetString(currentFacetString);
        }

        return ret;
    }

    /**
     * Returns true if the value for the given field type in <code>facetsExpanded</code> has been previously set to true.
     *
     * @param field Solr facet field name to check
     * @should return false if value null
     * @should return true if value true
     * @return true if the facet for the given field has been explicitly expanded, false otherwise
     */
    public boolean isFacetExpanded(String field) {
        return facetsExpanded.get(field) != null && facetsExpanded.get(field);
    }

    /**
     * Getter for unit tests.
     * 

     */
    Map<String, String> getMinValues() {
        return minValues;
    }

    /**
     * Getter for unit tests.
     * 

     */
    Map<String, String> getMaxValues() {
        return maxValues;
    }

    /**
     * isFacetCollapsed.
     *
     * @param field Solr facet field name to check
     * @return true if the facet for the given field is not expanded (i.e. collapsed), false otherwise
     */
    public boolean isFacetCollapsed(String field) {
        return !isFacetExpanded(field);
    }

    /**
     * 
     * @return All facet field names of the type "range"
     */
    public List<String> getAllRangeFacetFields() {
        return DataManager.getInstance().getConfiguration().getRangeFacetFields();
    }

    /**
     *
     * @param field Solr range facet field name whose visualization style to retrieve
     * @return Visualization style for the given range field
     */
    public String getRangeFacetStyle(String field) {
        return DataManager.getInstance().getConfiguration().getFacetFieldStyle(field);
    }

    /**
     *
     * @param field Solr range facet field name whose configured minimum to retrieve
     * @return Configured min value for the given field
     */
    public int getRangeFacetMinValue(String field) {
        return DataManager.getInstance().getConfiguration().getRangeFacetFieldMinValue(field);
    }

    /**
     *
     * @param field Solr range facet field name whose configured maximum to retrieve
     * @return Configured max value for the given field
     */
    public int getRangeFacetMaxValue(String field) {
        return DataManager.getInstance().getConfiguration().getRangeFacetFieldMaxValue(field);
    }

    /**
     * Returns configured facet fields of regular and hierarchical type only.
     *
     * @return a map of facet field name to facet items for all regular and hierarchical facet fields
     * @should return all facet items in correct order
     */
    public Map<String, List<IFacetItem>> getAllAvailableFacets() {
        return getAvailableFacets(Arrays.asList("", "boolean", "hierarchical"));
    }

    public boolean hasAvailableFacets() {
        return !this.activeFacets.isEmpty()
                || this.getAvailableFacets().values().stream().flatMap(List::stream).anyMatch(facetItem -> facetItem.getCount() > 1);
    }

    /**
     *
     * @param types list of facet field type strings to include; null means all types
     * @return Map<String, List<IFacetItem>>
     */
    Map<String, List<IFacetItem>> getAvailableFacets(List<String> types) {
        Map<String, List<IFacetItem>> ret = new LinkedHashMap<>();

        List<String> allFacetFields = DataManager.getInstance().getConfiguration().getAllFacetFields();
        for (String field : allFacetFields) {
            if (availableFacets.get(field) != null && !DataManager.getInstance().getConfiguration().isFacetFieldSkipInWidget(field)
                    && (types == null || types.contains(DataManager.getInstance().getConfiguration().getFacetFieldType(field)))) {
                ret.put(field, availableFacets.get(field));
            }
        }

        synchronized (lock) {
            //add current facets which have no hits. This may happen due to geomap faceting
            for (IFacetItem currentItem : getActiveFacetsCopy()) {
                String fieldType = DataManager.getInstance().getConfiguration().getFacetFieldType(currentItem.getField());
                //don't include geo and range facets in list, since they have their own widgets
                //Is this code still relevant then? Aren't all other facets included in allFacetFields and availableFacets?
                if (!"geo".equals(fieldType) && !"range".equals(fieldType)) {
                    // Make a copy of the list to avoid concurrent modification
                    List<IFacetItem> availableFacetItems = new ArrayList<>(ret.computeIfAbsent(currentItem.getField(), x -> new ArrayList<>()));
                    if (!availableFacetItems.contains(currentItem)) {
                        availableFacetItems.add(currentItem);
                        ret.put(currentItem.getField(), availableFacetItems);
                    }
                }
            }

            return ret;
        }
    }

    /**
     * getConfiguredSubelementFacetFields.
     *
     * @return Configured subelement fields names only
     */
    public List<String> getConfiguredSubelementFacetFields() {
        List<String> ret = new ArrayList<>();

        for (String field : DataManager.getInstance().getConfiguration().getAllFacetFields()) {
            if (SolrConstants.DOCSTRCT_SUB.equals(field)) {
                ret.add(SearchHelper.facetifyField(field));
            }
        }

        return ret;
    }

    /**
     * Getter for the field <code>availableFacets</code>.
     *
     * @return the map of Solr field names to their available facet items
     */
    public Map<String, List<IFacetItem>> getAvailableFacets() {
        return availableFacets;
    }

    /**
     * Returns a snapshot copy of the currently active facet filters.
     *
     * <p>Returns a defensive copy so that callers (e.g. JSF {@code c:forEach} templates) cannot
     * receive a {@link java.util.ConcurrentModificationException} if a concurrent request modifies
     * the list. Uses the same {@code lock} object as all write operations so that the copy is taken
     * atomically with respect to any concurrent {@link #setActiveFacetString} or
     * {@link #setGeoFacetFeature} call.
     *
     * @return a new, mutable {@link ArrayList} snapshot of the active facet filters; never {@code null}
     */
    public List<IFacetItem> getActiveFacets() {
        // Use the same lock as write operations (parseFacetString, setGeoFacetting, etc.)
        // to prevent ConcurrentModificationException when c:forEach iterates while a concurrent
        // request modifies activeFacets. Previously this method used synchronized(this) which
        // is a different monitor than the lock used by writes, providing no real protection.
        synchronized (lock) {
            return new ArrayList<>(activeFacets);
        }
    }

    /**
     * Get a shallow copy of <code>activeFacets</code>.
     *
     * @return a new ArrayList containing all activeFacets
     * @deprecated Use {@link #getActiveFacets()} which now already returns a defensive copy.
     */
    @Deprecated(since = "26.04", forRemoval = true)
    public List<IFacetItem> getActiveFacetsCopy() {
        return getActiveFacets();
    }

    /**
     * Removes all given facet items from <code>activeFacets</code> under the correct lock.
     *
     * @param facetsToRemove collection of facet items to remove; must not be {@code null}
     */
    public void removeActiveFacets(java.util.Collection<? extends IFacetItem> facetsToRemove) {
        synchronized (lock) {
            activeFacets.removeAll(facetsToRemove);
        }
    }

    /**
     * Returns whether <code>activeFacets</code> is empty without allocating a defensive copy.
     *
     * @return {@code true} if no active facets are set; {@code false} otherwise
     */
    public boolean isActiveFacetsEmpty() {
        synchronized (lock) {
            return activeFacets.isEmpty();
        }
    }

    /**
     * Getter for the field <code>tempValue</code>.
     *
     * @return the temporary value held during facet editing before it is applied
     */
    public String getTempValue() {
        return tempValue;
    }

    /**
     * Setter for the field <code>tempValue</code>.
     *
     * @param tempValue a temporary value held during facet editing before it is applied
     */
    public void setTempValue(String tempValue) {
        this.tempValue = tempValue;
    }

    /**
     * Returns true if the given <code>field</code> is language-specific to a different language than the given <code>language</code>.
     *
     * @param field Solr field name to test for a language code suffix
     * @param language BCP 47 language code to compare against the field suffix
     * @should return true if language code different
     * @should return false if language code same
     * @should return false if no language code
     * @should return false if language code different but active facet selected
     * @return true if the field has a language code suffix that does not match the given language and
     *         no active facet is selected for it, false otherwise
     */
    public boolean isHasWrongLanguageCode(String field, String language) {
        if (SolrTools.isHasWrongLanguageCode(field, language)) {
            // Even if the language code is wrong, keep the field visible if a facet value is actively selected
            return getActiveFacetsForField(field).isEmpty();
        }
        return false;
    }

    /**
     * getFacetValue.
     *
     * @param field Solr facet field name whose active value to retrieve
     * @return the active facet value for the given field, or an empty string if no active facet exists for that field
     */
    public String getFacetValue(String field) {
        return getActiveFacets().stream().filter(facet -> facet.getField().equals(field)).map(SearchFacets::getFacetName).findFirst().orElse("");
    }

    /**
     * getFacetDescription.
     *
     * @param field Solr facet field name whose active description to retrieve
     * @return the CMS collection description for the active facet of the given field, or an empty string if not found
     */
    public String getFacetDescription(String field) {
        return getActiveFacets().stream()
                .filter(facet -> facet.getField().equals(field))
                .map(SearchFacets::getFacetDescription)
                .findFirst()
                .orElse("");
    }

    /**
     * getFirstHierarchicalFacetValue.
     *
     * @return the value of the first active hierarchical facet, or an empty string if none exist
     */
    public String getFirstHierarchicalFacetValue() {
        return getActiveFacets().stream().filter(IFacetItem::isHierarchial).map(SearchFacets::getFacetName).findFirst().orElse("");
    }

    /**
     * getFirstHierarchicalFacetDescription.
     *
     * @param field Solr facet field name (unused; description taken from first hierarchical active facet)
     * @return the CMS collection description for the first active hierarchical facet, or an empty string if none exist
     */
    public String getFirstHierarchicalFacetDescription(String field) {
        return getActiveFacets().stream().filter(IFacetItem::isHierarchial).map(SearchFacets::getFacetDescription).findFirst().orElse("");
    }

    /**
     * @param facet active facet item whose CMS collection description to retrieve
     * @return {@link String}
     */
    private static String getFacetDescription(IFacetItem facet) {
        String desc = "";
        try {
            CMSCollection cmsCollection = DataManager.getInstance().getDao().getCMSCollection(facet.getField(), facet.getValue());
            if (cmsCollection != null) {
                desc = cmsCollection.getDescription();
            }
        } catch (DAOException e) {
            logger.trace("Error retrieving cmsCollection from DAO");
        }
        return desc;
    }

    /**
     * @param facet active facet item whose value to return
     * @return Value of the given facet
     */
    private static String getFacetName(IFacetItem facet) {
        if (facet == null) {
            return "";
        }

        return facet.getValue();
    }

    
    public Map<String, String> getLabelMap() {
        return labelMap;
    }

    
    public GeoFacetItem getGeoFacetting() {
        synchronized (lock) {
            List<String> geoFacetFields = DataManager.getInstance().getConfiguration().getGeoFacetFields();
            return getActiveFacetsCopy()
                    .stream()
                    .filter(GeoFacetItem.class::isInstance)
                    .map(GeoFacetItem.class::cast)
                    .findAny()
                    .orElse(new GeoFacetItem(!geoFacetFields.isEmpty() ? geoFacetFields.get(0) : null));
        }
    }

    /**
     * Sets the feature of the geoFacettingfield to to given feature. A new GeoFacetItem is added to currentFacets if none exists yet
     *
     * @param feature GeoJSON feature string defining the geographic filter area
     */
    public void setGeoFacetFeature(String feature) {
        GeoFacetItem item = getGeoFacetting();
        List<String> geoFacetFields = DataManager.getInstance().getConfiguration().getGeoFacetFields();
        item.setField(!geoFacetFields.isEmpty() ? geoFacetFields.get(0) : null);
        synchronized (lock) {
            if (StringUtils.isBlank(feature)) {
                this.activeFacets.remove(item);
            } else {
                item.setFeature(feature);
                if (!this.activeFacets.contains(item)) {
                    this.activeFacets.add(item);
                }
            }
        }
    }

    public String getGeoFacetFeature() {
        if (this.getGeoFacetting().isActive()) {
            return this.getGeoFacetting().getFeature();
        }
        return "";
    }

    public int getActiveFacetsSize() {
        return this.getAllAvailableFacets()
                .keySet()
                .stream()
                .mapToInt(this::getActiveFacetsSizeForField)
                .sum();
    }
}
