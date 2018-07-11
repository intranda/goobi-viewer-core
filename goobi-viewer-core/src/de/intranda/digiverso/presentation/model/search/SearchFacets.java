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
package de.intranda.digiverso.presentation.model.search;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

/**
 * Current faceting settings for a search.
 */
public class SearchFacets {

    private static final Logger logger = LoggerFactory.getLogger(SearchFacets.class);

    /** Available regular facets for the current search result. */
    private final Map<String, List<FacetItem>> availableFacets = new LinkedHashMap<>();
    /** Currently applied facets. */
    private final List<FacetItem> currentFacets = new ArrayList<>();

    private final Map<String, Boolean> drillDownExpanded = new HashMap<>();

    private final Map<String, String> minValues = new HashMap<>();

    private final Map<String, String> maxValues = new HashMap<>();

    private final Map<String, List<Integer>> valueRanges = new HashMap<>();

    private String tempValue;

    public void resetAvailableFacets() {
        logger.trace("resetAvailableFacets");
        availableFacets.clear();
        drillDownExpanded.clear();
    }

    public void resetCurrentFacets() {
        resetCurrentFacetString();
    }

    public void resetSliderRange() {
        logger.trace("resetSliderRange");
        minValues.clear();
        maxValues.clear();
        valueRanges.clear();
    }

    /**
     * Generates a list containing filter queries for the selected regular and hierarchical facets.
     * 
     * @param advancedSearchGroupOperator
     * @return
     */
    public List<String> generateFacetFilterQueries(int advancedSearchGroupOperator, boolean includeRangeFacets) {
        List<String> ret = new ArrayList<>(2);

        // Add hierarchical facets
        String hierarchicalQuery = generateHierarchicalFacetFilterQuery(advancedSearchGroupOperator);
        if (StringUtils.isNotEmpty(hierarchicalQuery)) {
            ret.add(hierarchicalQuery);
        }

        // Add regular facets
        String regularQuery = generateFacetFilterQuery(includeRangeFacets);
        if (StringUtils.isNotEmpty(regularQuery)) {
            ret.add(regularQuery);
        }

        return ret;
    }

    /**
     * Generates a filter query for the selected hierarchical facets.
     * 
     * @param advancedSearchGroupOperator
     * @return
     * @should generate query correctly
     * @should return null if facet list is empty
     */
    String generateHierarchicalFacetFilterQuery(int advancedSearchGroupOperator) {
        if (!currentFacets.isEmpty()) {
            StringBuilder sbQuery = new StringBuilder();
            int count = 0;
            for (FacetItem facetItem : currentFacets) {
                if (!facetItem.isHierarchial()) {
                    continue;
                }
                if (count > 0) {
                    if (advancedSearchGroupOperator == 1) {
                        sbQuery.append(" OR ");
                    } else {
                        sbQuery.append(" AND ");
                    }
                }
                String field = SearchHelper.facetifyField(facetItem.getField());
                sbQuery.append('(')
                        .append(field)
                        .append(':')
                        .append("\"" + facetItem.getValue() + "\"")
                        .append(" OR ")
                        .append(field)
                        .append(':')
                        .append(facetItem.getValue())
                        .append(".*)");
                count++;
            }

            return sbQuery.toString();
        }

        return null;
    }

    /**
     * Generates a filter query for the selected non-hierarchical facets.
     * 
     * @param includeRangeFacets
     * @return
     * @should generate query correctly
     * @should return null if facet list is empty
     * @should skip range facet fields if so requested
     */
    String generateFacetFilterQuery(boolean includeRangeFacets) {
        if (!currentFacets.isEmpty()) {
            StringBuilder sbQuery = new StringBuilder();
            if (sbQuery.length() > 0) {
                sbQuery.insert(0, '(');
                sbQuery.append(')');
            }
            for (FacetItem facetItem : currentFacets) {
                if (facetItem.isHierarchial()) {
                    continue;
                }
                if (!includeRangeFacets && DataManager.getInstance().getConfiguration().getRangeFacetFields().contains(facetItem.getField())) {
                    continue;
                }
                if (sbQuery.length() > 0) {
                    sbQuery.append(" AND ");
                }
                sbQuery.append(facetItem.getQueryEscapedLink());
                logger.trace("Added facet: {}", facetItem.getQueryEscapedLink());
            }

            return sbQuery.toString();
        }

        return null;
    }

    /**
     * Returns the first FacetItem objects in <code>currentFacets</code> where the field name matches the given field name.
     *
     * @param field The field name to match.
     * @return
     */
    public FacetItem getCurrentFacetForField(String field) {
        List<FacetItem> ret = getCurrentFacetsForField(field);
        if (!ret.isEmpty()) {
            return ret.get(0);
        }

        return null;
    }

    /**
     * Returns a list of FacetItem objects in <code>currentFacets</code> where the field name matches the given field name.
     *
     * @param field The field name to match.
     * @return
     */
    public List<FacetItem> getCurrentFacetsForField(String field) {
        List<FacetItem> ret = new ArrayList<>();

        for (FacetItem facet : currentFacets) {
            if (facet.getField().equals(field)) {
                ret.add(facet);
            }
        }

        return ret;
    }

    /**
     * Checks whether the given facet is currently in use.
     *
     * @param facet The facet to check.
     * @return
     */
    public boolean isFacetCurrentlyUsed(FacetItem facet) {
        for (FacetItem fi : getCurrentFacetsForField(facet.getField())) {
            if (fi.getLink().equals(facet.getLink())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the size of the full element list of the facet for the given field.
     */
    public int getAvailableFacetsListSizeForField(String field) {
        if (availableFacets.get(field) != null) {
            return availableFacets.get(field).size();
        }

        return 0;
    }

    /**
     *
     * @return Size of <code>currentFacets</code>.
     */
    public int getCurrentFacetsSizeForField(String field) {
        return getCurrentFacetsForField(field).size();
    }

    /**
     * Returns a collapsed sublist of the available facet elements for the given field.
     *
     * @param field
     * @return
     * @should return full DC facet list if expanded
     * @should return full DC facet list if list size less than default
     * @should return reduced DC facet list if list size larger than default
     * @should return full facet list if expanded
     * @should return full facet list if list size less than default
     * @should return reduced facet list if list size larger than default
     * @should not contain currently used facets
     */
    public List<FacetItem> getLimitedFacetListForField(String field) {
        List<FacetItem> facetItems = availableFacets.get(field);
        if (facetItems != null) {
            // Remove currently used facets
            facetItems.removeAll(currentFacets);
            int initial = DataManager.getInstance().getConfiguration().getInitialDrillDownElementNumber(field);
            if (!isDrillDownExpanded(field) && initial != -1 && facetItems.size() > initial) {
                return facetItems.subList(0, initial);
            }
            logger.trace("facet items {}: {}", field, facetItems.size());
            return facetItems;
        }

        return null;
    }

    /**
     * If the drill-down for given field is expanded, return the size of the facet, otherwise the initial (collapsed) number of elements as
     * configured.
     *
     * @return
     * @should return full facet size if expanded
     * @should return default if collapsed
     */
    public int getDrillDownElementDisplayNumber(String field) {
        if (isDrillDownExpanded(field) && availableFacets.get(field) != null) {
            return availableFacets.get(field).size();
        }
        return DataManager.getInstance().getConfiguration().getInitialDrillDownElementNumber(field);
    }

    /**
     * Sets the expanded flag to <code>true</code> for the given drill-down field.
     *
     * @param field
     */
    public void expandDrillDown(String field) {
        logger.trace("expandDrillDown: {}", field);
        drillDownExpanded.put(field, true);
    }

    /**
     * Sets the expanded flag to <code>false</code> for the given drill-down field.
     *
     * @param field
     */
    public void collapseDrillDown(String field) {
        logger.trace("collapseDrillDown: {}", field);
        drillDownExpanded.put(field, false);
    }

    /**
     * Returns true if the "(more)" link is to be displayed for a drill-down box. This is the case if the facet has more elements than the initial
     * number of displayed elements and the facet hasn't been manually expanded yet.
     *
     * @param field
     * @return
     * @should return true if DC facet collapsed and has more elements than default
     * @should return true if facet collapsed and has more elements than default
     * @should return false if DC facet expanded
     * @should return false if facet expanded
     * @should return false if DC facet smaller than default
     * @should return false if facet smaller than default
     */
    public boolean isDisplayDrillDownExpandLink(String field) {
        List<FacetItem> facetItems = availableFacets.get(field);
        int expandSize = DataManager.getInstance().getConfiguration().getInitialDrillDownElementNumber(field);
        if (facetItems != null && !isDrillDownExpanded(field) && expandSize > 0 && facetItems.size() > expandSize) {
            return true;
        }

        return false;
    }

    /**
     *
     * @param field
     * @return
     */
    public boolean isDisplayDrillDownCollapseLink(String field) {
        return isDrillDownExpanded(field);
    }

    /**
     * Returns a URL encoded SSV string of facet fields and values from the elements in currentFacets (hyphen if empty).
     *
     * @return SSV string of facet queries or "-" if empty
     * @should contain queries from all FacetItems
     * @should return hyphen if currentFacets empty
     */
    public String getCurrentFacetString() {
        String ret = generateFacetPrefix(currentFacets, true);
        if (StringUtils.isEmpty(ret)) {
            ret = "-";
        }
        try {
            return URLEncoder.encode(ret, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return ret;
        }
    }

    public String getCurrentFacetString(boolean urlEncode) {
        String ret = generateFacetPrefix(currentFacets, true);
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
     * @return the currentCollection
     */
    @Deprecated
    public String getCurrentHierarchicalFacetString() {
        return "-";
    }

    /**
     * @return the currentCollection
     */
    @Deprecated
    public String getCurrentCollection() {
        return "-";
    }

    /**
     * Receives an SSV string of facet fields and values (FIELD1:value1;FIELD2:value2;FIELD3:value3) and generates new Elements for currentFacets.
     *
     * @param currentFacetString
     * @should create FacetItems from all links
     * @should decode slashes and backslashes
     * @should reset slider range if no slider field among current facets
     * @should not reset slider range if slider field among current facets
     */
    public void setCurrentFacetString(String currentFacetString) {
        logger.trace("setCurrentFacetString: {}", currentFacetString);
        parseFacetString(currentFacetString, currentFacets);
    }

    /**
     * Receives an SSV string of facet fields and values (FIELD1:value1;FIELD2:value2;FIELD3:value3) and generates new Elements for
     * currentHierarchicalFacets.
     *
     * @param currentFacetString
     */
    @Deprecated
    public void setCurrentHierarchicalFacetString(String currentHierarchicalFacetString) {
    }

    /**
     * @param currentCollection the currentCollection to set
     */
    @Deprecated
    public void setCurrentCollection(String currentCollection) {
        setCurrentHierarchicalFacetString(currentCollection);
    }

    /**
     * 
     * @param facetString
     * @param facetItems
     * @should fill list correctly
     * @should empty list before filling
     * @should add DC field prefix if no field name is given
     * @should set hierarchical status correctly
     */
    static void parseFacetString(String facetString, List<FacetItem> facetItems) {
        if (facetItems == null) {
            facetItems = new ArrayList<>();
        } else {
            facetItems.clear();
        }
        if (StringUtils.isNotEmpty(facetString) && !"-".equals(facetString)) {
            try {
                facetString = URLDecoder.decode(facetString, "utf-8");
                facetString = BeanUtils.unescapeCriticalUrlChracters(facetString);
            } catch (UnsupportedEncodingException e) {
            }
            String[] facetStringSplit = facetString.split(";;");
            for (String facetLink : facetStringSplit) {
                if (StringUtils.isNotEmpty(facetLink)) {
                    if (!facetLink.contains(":")) {
                        facetLink = new StringBuilder(SolrConstants.DC).append(':').append(facetLink).toString();
                    }
                    facetItems.add(new FacetItem(facetLink, isFieldHierarchical(facetLink.substring(0, facetLink.indexOf(":")))));
                }
            }
        }
    }

    /**
     * 
     * @param field
     * @return true if field is hierarchical; false otherwise
     */
    static boolean isFieldHierarchical(String field) {
        logger.trace("isFieldHierarchical: {} ? {}", field,
                DataManager.getInstance().getConfiguration().getHierarchicalDrillDownFields().contains(field));
        return DataManager.getInstance().getConfiguration().getHierarchicalDrillDownFields().contains(field);
    }

    /**
     * Updates existing facet item for the given field with a new value. If no item for that field yet exist, a new one is added.
     * 
     * @param field
     * @param hierarchical
     */
    public String updateFacetItem(String field, boolean hierarchical) {
        updateFacetItem(field, tempValue, currentFacets, hierarchical);

        return "pretty:search6"; // TODO advanced search
    }

    /**
     * Updates existing facet item for the given field with a new value. If no item for that field yet exist, a new one is added.
     * 
     * @param field
     * @param updateValue
     * @param facetItems
     * @param hierarchical
     * @should update facet item correctly
     * @should add new item correctly
     */
    static void updateFacetItem(String field, String updateValue, List<FacetItem> facetItems, boolean hierarchical) {
        if (facetItems == null) {
            facetItems = new ArrayList<>();
        }

        if (StringUtils.isNotEmpty(updateValue) && !"-".equals(updateValue)) {
            try {
                updateValue = URLDecoder.decode(updateValue, "utf-8");
                updateValue = BeanUtils.unescapeCriticalUrlChracters(updateValue);
            } catch (UnsupportedEncodingException e) {
            }

            FacetItem fieldItem = null;
            for (FacetItem item : facetItems) {
                if (item.getField().equals(field)) {
                    fieldItem = item;
                    break;
                }
            }
            if (fieldItem == null) {
                fieldItem = new FacetItem(field + ":" + updateValue, hierarchical);
                facetItems.add(fieldItem);
            }
            fieldItem.setLink(field + ":" + updateValue);
            logger.trace("Facet item updated: {}", fieldItem.getLink());
        }
    }

    /**
     * 
     * @param field
     * @return Current min value, if facet in use; otherwise absolute min value for that field
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getCurrentMinRangeValue(String field) throws PresentationException, IndexUnreachableException {
        for (FacetItem item : currentFacets) {
            if (item.getField().equals(field)) {
                logger.trace("currentMinRangeValue: {}", item.getValue());
                return item.getValue();
            }
        }

        return getAbsoluteMinRangeValue(field);
    }

    /**
     * 
     * @param field
     * @return Current max value, if facet in use; otherwise absolute max value for that field
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getCurrentMaxRangeValue(String field) throws PresentationException, IndexUnreachableException {
        for (FacetItem item : currentFacets) {
            if (item.getField().equals(field)) {
                if (item.getValue2() != null) {
                    logger.trace("currentMaxRangeValue: {}", item.getValue());
                    return item.getValue2();
                }
            }
        }

        return getAbsoluteMaxRangeValue(field);
    }

    /**
     * Returns the minimum value for the given field available in the search index.
     * 
     * @param field
     * @return Smallest available value
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String getAbsoluteMinRangeValue(String field) throws PresentationException, IndexUnreachableException {
        if (!minValues.containsKey(field)) {
            //            populateAbsoluteMinMaxValuesForField(field);
            return "0";
        }
        return minValues.get(field);
    }

    /**
     * Returns the maximum value for the given field available in the search index.
     * 
     * @param field
     * @return Largest available value
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String getAbsoluteMaxRangeValue(String field) throws PresentationException, IndexUnreachableException {
        if (!maxValues.containsKey(field)) {
            //            populateAbsoluteMinMaxValuesForField(field);
            return "0";
        }
        return maxValues.get(field);
    }

    /**
     * Returns a sorted list of all available values for the given field among available facet values.
     * 
     * @param field
     * @return sorted list of all values for the given field among available facet values
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public List<Integer> getValueRange(String field) throws PresentationException, IndexUnreachableException {
        if (!maxValues.containsKey(field)) {
            //            populateAbsoluteMinMaxValuesForField(field);
            return Collections.emptyList();
        }
        return valueRanges.get(field);
    }

    /**
     * Adds the min and max values from the search index for the given field to the bottomValues map. Min and max values are determined via an
     * alphanumeric comparator.
     * 
     * @param field
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should populate values correctly
     * @should add all values to list
     */
    void populateAbsoluteMinMaxValuesForField(String field, List<String> stringValues) throws PresentationException, IndexUnreachableException {
        if (field == null) {
            return;
        }

        if (!SolrConstants._CALENDAR_YEAR.equals(field) && !field.startsWith("MDNUM_")) {
            logger.info("{} is not an integer type field, cannot use with a range query");
            return;
        }

        List<Integer> intValues = null;
        if (stringValues != null) {
            intValues = new ArrayList<>(stringValues.size());
            for (String s : stringValues) {
                if (s == null) {
                    continue;
                }
                intValues.add(Integer.valueOf(s));
            }
        } else {
            logger.trace("No facets found for field {}", field);
            intValues = Collections.emptyList();
        }
        if (!intValues.isEmpty()) {
            Collections.sort(intValues);
            // Collections.sort(values, new AlphanumCollatorComparator(Collator.getInstance()));
            valueRanges.put(field, intValues);
            minValues.put(field, String.valueOf(intValues.get(0)));
            maxValues.put(field, String.valueOf(intValues.get(intValues.size() - 1)));
            logger.trace("Absolute range for field {}: {} - {}", field, minValues.get(field), maxValues.get(field));
        }
    }

    /**
     * 
     */
    public void resetCurrentFacetString() {
        logger.trace("resetCurrentFacetString");
        setCurrentFacetString("-");
    }

    /**
     * Returns a URL encoded value returned by generateFacetPrefix() for regular facets.
     *
     * @return
     */
    public String getCurrentFacetStringPrefix() {
        try {
            return URLEncoder.encode(generateFacetPrefix(currentFacets, true), SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return generateFacetPrefix(currentFacets, true);
        }
    }

    /**
     * Returns a URL encoded value returned by generateFacetPrefix() for hierarchical facets.
     *
     * @return
     */
    @Deprecated
    public String getCurrentHierarchicalFacetPrefix() {
        return "";
    }

    /**
     * Generates an SSV string of facet fields and values from the elements in the given List<FacetString> (empty string if empty).
     *
     * @param facetItems
     * @param escapeSlashes If true, slashes and backslashes are replaced with URL-compatible replacement strings
     * @return
     * @should encode slashed and backslashes
     */
    static String generateFacetPrefix(List<FacetItem> facetItems, boolean escapeSlashes) {
        if (facetItems == null) {
            throw new IllegalArgumentException("facetItems may not be null");
        }
        StringBuilder sb = new StringBuilder();
        for (FacetItem facetItem : facetItems) {
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
     * 
     * @param facetQuery
     * @param
     * @return
     * @should remove facet correctly
     * @should remove facet containing reserved chars
     */
    public String removeFacetAction(final String facetQuery, final String ret) {
        logger.trace("removeFacetAction: {}", facetQuery);
        String currentFacetString = generateFacetPrefix(currentFacets, false);
        if (currentFacetString.contains(facetQuery)) {
            currentFacetString = currentFacetString.replaceAll("(" + Pattern.quote(facetQuery) + ")(?=;|(?=/))", "").replace(";;;;", ";;");
            setCurrentFacetString(currentFacetString);
        }

        return ret;
    }

    /**
     * Returns true if the value for the given field type in <code>drillDownExpanded</code> has been previously set to true.
     *
     * @param field
     * @return
     * @should return false if value null
     * @should return true if value true
     */
    public boolean isDrillDownExpanded(String field) {
        return drillDownExpanded.get(field) != null && drillDownExpanded.get(field);
    }

    /**
     * 
     * @param field
     * @return
     */
    public boolean isDrillDownCollapsed(String field) {
        return !isDrillDownExpanded(field);
    }

    /**
     * 
     * @return
     * @should return all facet items in correct order
     */
    public Map<String, List<FacetItem>> getAllAvailableFacets() {
        Map<String, List<FacetItem>> ret = new LinkedHashMap<>();

        for (String field : DataManager.getInstance().getConfiguration().getAllDrillDownFields()) {
            if (availableFacets.containsKey(field)) {
                ret.put(field, availableFacets.get(field));
            }
        }

        return ret;
    }

    /**
     * @return the availableFacets
     */
    public Map<String, List<FacetItem>> getAvailableFacets() {
        return availableFacets;
    }

    /**
     * @return the currentFacets
     */
    public List<FacetItem> getCurrentFacets() {
        return currentFacets;
    }

    /**
     * @return the tempValue
     */
    public String getTempValue() {
        return tempValue;
    }

    /**
     * @param tempValue the tempValue to set
     */
    public void setTempValue(String tempValue) {
        this.tempValue = tempValue;
    }

    /**
     * Returns true if the given <code>field</code> is language-specific to a different language than the given <code>language</code>.
     * 
     * @param field
     * @param language
     * @return
     * @should return true if language code different
     * @should return false if language code same
     * @should return false if no language code
     */
    public boolean isHasWrongLanguageCode(String field, String language) {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (language == null) {
            throw new IllegalArgumentException("language may not be null");
        }
        if (field.contains(SolrConstants._LANG_) && !field.endsWith(SolrConstants._LANG_ + language.toUpperCase())) {
            return true;
        }

        return false;
    }
}