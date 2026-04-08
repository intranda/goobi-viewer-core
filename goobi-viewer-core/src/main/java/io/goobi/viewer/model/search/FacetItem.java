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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.util.ClientUtils;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.FacetSorting.SortingMap;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Represents a single facet value with its display label, hit count, and selection state.
 */
public class FacetItem implements Serializable, IFacetItem {

    private static final long serialVersionUID = 5033196184122928247L;

    private static final Logger logger = LogManager.getLogger(FacetItem.class);

    private FacetType type = FacetType.STANDARD;
    private String field;
    private String value;
    private String value2;
    private String link;
    private String label;
    private long count;
    private boolean group;
    private final boolean hierarchial;

    /**
     * Constructor that doesn't parse the link; for testing purposes.
     *
     * @param hierarchical true if this is a hierarchical facet
     */
    FacetItem(boolean hierarchical) {
        this.hierarchial = hierarchical;
    }

    /**
     * Creates a new FacetItem for active facets received via the URL. The Solr query is split into individual field/value.
     *
     * @param link Solr field:value query string to parse
     * @param hierarchical true if this is a hierarchical facet
     * @should split field and value correctly
     * @should split field and value range correctly
     */
    public FacetItem(String link, boolean hierarchical) {
        this(link, null, hierarchical);
    }

    /**
     * Creates a new FacetItem for active facets received via the URL. The Solr query is split into individual field/value.
     *
     * @param link Solr field:value query string to parse
     * @param label display label; null to use the value as label
     * @param hierarchical true if this is a hierarchical facet
     * @should split field and value correctly
     * @should split field and value range correctly
     * @should set label to value if no label value given
     */
    public FacetItem(String link, String label, boolean hierarchical) {
        this.label = label;
        this.hierarchial = hierarchical;
        setLink(link.trim());
    }

    /**
     * 
     * @param count Solr facet count object to build the item from
     */
    public FacetItem(Count count) {
        this(count.getFacetField().getName(), count.getFacetField().getName() + ":" + count.getName(), count.getName(), count.getCount(), false);
    }

    /**
     * Internal constructor.
     * 
     * @param field Solr field name for this facet
     * @param link {@link String}
     * @param label {@link String}
     * @param count {@link Integer}
     * @param hierarchical true if this is a hierarchical facet
     */
    private FacetItem(String field, String link, String label, long count, boolean hierarchical) {
        this.field = field;
        this.label = label;
        this.count = count;
        this.hierarchial = hierarchical;
        setLink(link.trim());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + ((link == null) ? 0 : link.hashCode());
        return result;
    }

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
        FacetItem other = (FacetItem) obj;
        if (field == null) {
            if (other.field != null) {
                return false;
            }
        } else if (!field.equals(other.field)) {
            return false;
        }
        if (link == null) {
            if (other.link != null) {
                return false;
            }
        } else if (!link.equals(other.link)) {
            return false;
        }
        return true;
    }

    /**
     * Extracts field name and value(s) from the given link string.
     *
     * @should set label to value if label empty
     * @should removed wildcard from label
     */
    void parseLink() {
        if (link == null) {
            return;
        }

        int colonIndex = link.indexOf(':');
        if (colonIndex == -1) {
            return;
        }

        this.field = link.substring(0, colonIndex);

        String fullValue = link.substring(colonIndex + 1);
        if (fullValue.startsWith("[") && fullValue.endsWith("]") && fullValue.contains(" TO ")) {
            this.value = fullValue.substring(1, fullValue.indexOf(" TO "));
            this.value2 = fullValue.substring(fullValue.indexOf(" TO ") + 4, fullValue.length() - 1);
        } else {
            value = fullValue;
        }
        if (StringUtils.isEmpty(label)) {
            label = value;
            if (label.endsWith("*")) {
                label = label.substring(0, label.length() - 1);
            }
        }
    }

    /**
     * Constructs facet items from the list of given field:value combinations. Always sorted by the label translation.
     *
     * @param existingFacetsItems List of previously generated items (e.g. via other result groups) to combine with new items
     * @param field Facet field
     * @param values Map containing facet values and their counts
     * @param hierarchical true if facet field is hierarchical; false otherwise
     * @param groupToLength If value is greater than 0, facet values will be grouped together by if they contain equal characters at {0-groupByLength}
     * @param labelMap Optional map for storing alternate labels for later use by the client
     * @return {@link java.util.ArrayList} of {@link io.goobi.viewer.model.search.FacetItem}
     * @should add priority values first
     * @should set label from separate field if configured and found
     * @should group values by starting character correctly
     * @should augment existing items with new values
     * @should prefer existing items
     */
    public static List<IFacetItem> generateFilterLinkList(List<IFacetItem> existingFacetsItems, String field, SortingMap<String, Long> values,
            boolean hierarchical, int groupToLength, Map<String, String> labelMap) {
        // logger.trace("generateFilterLinkList: {}", field); //NOSONAR Debug
        List<String> priorityValues = DataManager.getInstance().getConfiguration().getPriorityValuesForFacetField(field);
        Map<String, FacetItem> priorityValueMap = new HashMap<>(priorityValues.size());

        // If a separate label field configured for fieldName, load all values and add them to labelMap
        String labelField = DataManager.getInstance().getConfiguration().getLabelFieldForFacetField(field);
        if (labelField != null && labelMap != null) {
            try {
                labelMap.putAll(DataManager.getInstance()
                        .getSearchIndex()
                        .getLabelValuesForFacetField(field, labelField, values.getMap().keySet()));
            } catch (PresentationException e) {
                logger.debug(e.getMessage());
            } catch (IndexUnreachableException e) {
                logger.error(e.getMessage(), e);
            }
        }

        List<IFacetItem> retList = new ArrayList<>();
        Map<String, FacetItem> existingItems = new HashMap<>();
        // Add supplied existing items; copy first to avoid ConcurrentModificationException
        // if the shared list is modified by another thread while iterating (refs #1234)
        if (existingFacetsItems != null) {
            List<IFacetItem> existingItemsCopy = new ArrayList<>(existingFacetsItems);
            for (IFacetItem item : existingItemsCopy) {
                if (item instanceof FacetItem facetItem) {
                    values.getMap().compute(item.getValue(), (k, v) -> v == null ? item.getCount() : v + item.getCount());
                }
            }
        }

        for (Entry<String, Long> entry : values.getMap().entrySet()) {
            // Skip reversed values
            if (entry.getKey().charAt(0) == 1) {
                continue;
            }
            String useValue;
            String label;
            if (groupToLength > 0 && entry.getKey().length() > groupToLength) {
                label = entry.getKey().substring(0, groupToLength).toUpperCase();
                useValue = label + "*";
            } else {
                useValue = entry.getKey();
                label = useValue;
            }

            String key = field + ":" + useValue;
            if (existingItems.containsKey(key)) {
                existingItems.get(key).increaseCount(entry.getValue());
            } else {
                if (labelMap != null && labelMap.containsKey(key)) {
                    label = labelMap.get(key);
                    logger.trace("using label from map: {}", label);
                }

                if (StringUtils.isEmpty(field)) {
                    label = new StringBuilder(useValue).append(SolrConstants.SUFFIX_DD).toString();
                }
                String linkValue = useValue;
                if (field.endsWith(SolrConstants.SUFFIX_UNTOKENIZED)) {
                    linkValue = '"' + linkValue + '"';
                }
                String link = StringUtils.isNotEmpty(field) ? new StringBuilder(field).append(':').append(linkValue).toString() : linkValue;

                FacetItem facetItem =
                        new FacetItem(field, link, StringTools.intern(label), entry.getValue(),
                                hierarchical);
                if (!priorityValues.isEmpty() && priorityValues.contains(useValue)) {
                    priorityValueMap.put(useValue, facetItem);
                } else {
                    retList.add(facetItem);
                }

                existingItems.put(key, facetItem);
            }
        }
        //        String comparator = DataManager.getInstance().getConfiguration().getSortOrder(SearchHelper.defacetifyField(field));
        //        sortItems(field, locale, retList, comparator);
        // Add priority values at the beginning
        if (!priorityValueMap.isEmpty()) {
            List<IFacetItem> regularValues = new ArrayList<>(retList);
            retList.clear();
            for (String val : priorityValues) {
                if (priorityValueMap.containsKey(val)) {
                    retList.add(priorityValueMap.get(val));
                }
            }
            retList.addAll(regularValues);
        }
        // logger.debug("filters: {}", retList.size()); //NOSONAR Debug
        return retList;
    }

    /**
     * Constructs a list of FilterLink objects for faceting. Optionally sorted by the raw values.
     *
     * @param field Solr facet field name
     * @param values map of facet values to their document counts
     * @param sort if true, sort items by value before returning
     * @param reverseOrder if true, reverse the sort order
     * @param hierarchical true if facet field is hierarchical
     * @return a {@link java.util.List} object.
     * @should sort items correctly
     */
    public static List<IFacetItem> generateFacetItems(String field, Map<String, Long> values, boolean sort, boolean reverseOrder,
            boolean hierarchical) {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (values == null) {
            throw new IllegalArgumentException("values may not be null");
        }

        List<IFacetItem> retList = new ArrayList<>(values.keySet().size());

        boolean numeric = true;
        for (String s : values.keySet()) {
            try {
                Long.valueOf(s);
            } catch (NumberFormatException e) {
                numeric = false;
                break;
            }
        }
        List<String> keys = new ArrayList<>(values.keySet());
        if (sort) {
            if (numeric) {
                List<Long> numberKeys = new ArrayList<>();
                for (String s : keys) {
                    numberKeys.add(Long.valueOf(s));
                }
                Collections.sort(numberKeys);
                if (reverseOrder) {
                    Collections.reverse(numberKeys);
                }
                keys.clear();
                for (Long l : numberKeys) {
                    keys.add(String.valueOf(l));
                }
            } else {
                Collections.sort(keys);
                if (reverseOrder) {
                    Collections.reverse(keys);
                }
            }
        }

        for (Object value : keys) {
            String label = String.valueOf(value);
            if (StringUtils.isEmpty(field)) {
                label += SolrConstants.SUFFIX_DD;
            }
            String link = StringUtils.isNotEmpty(field) ? field + ":" + ClientUtils.escapeQueryChars(String.valueOf(value)) : String.valueOf(value);
            retList.add(new FacetItem(field, link, label, values.get(String.valueOf(value)),
                    hierarchical));
        }

        return retList;
    }

    /**
     * Returns field:value (with the value escaped for the Solr query).
     *
     * @should construct link correctly
     * @should escape values containing whitespaces
     * @should construct hierarchical link correctly
     * @should construct range link correctly
     * @should construct polygon link correctly
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getQueryEscapedLink() {
        String f = SearchHelper.facetifyField(this.field);
        String escapedValue = getEscapedValue(value);
        if (hierarchial) {
            return new StringBuilder("(").append(f)
                    .append(':')
                    .append(escapedValue)
                    .append(" OR ")
                    .append(f)
                    .append(':')
                    .append(escapedValue)
                    .append(".*)")
                    .toString();
        }
        if (StringUtils.isEmpty(value2)) {
            return new StringBuilder(f).append(':').append(escapedValue).toString();
        }
        String escapedValue2 = getEscapedValue(value2);
        return new StringBuilder(f).append(":[").append(escapedValue).append(" TO ").append(escapedValue2).append(']').toString();
    }

    /**
     *
     * @param value Value to escape
     * @return Escaped value
     * @should escape value correctly
     * @should add quotation marks if value contains space
     * @should preserve leading and trailing quotation marks
     * @should preserve wildcard
     */
    static String getEscapedValue(final String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }

        String val = value;
        boolean addLeftTruncation = false;
        boolean addRightTruncation = false;
        if (val.charAt(0) == '*') {
            addLeftTruncation = true;
            val = val.substring(1);

        }
        if (val.endsWith("*")) {
            addRightTruncation = true;
            val = val.substring(0, val.length() - 1);
        }

        // If the entire value consisted of wildcards, return them as-is without escaping
        if (val.isEmpty()) {
            return (addLeftTruncation ? "*" : "") + (addRightTruncation ? "*" : "");
        }

        String escapedValue = null;
        if (val.charAt(0) == '"' && val.charAt(val.length() - 1) == '"' && val.length() > 2) {
            escapedValue = '"' + ClientUtils.escapeQueryChars(val.substring(1, val.length() - 1)) + '"';
        } else {
            escapedValue = ClientUtils.escapeQueryChars(val);
        }
        // Add quotation marks if spaces are contained
        if (escapedValue.contains(" ") && !escapedValue.startsWith("\"") && !escapedValue.endsWith("\"")) {
            escapedValue = '"' + escapedValue + '"';
        }

        if (addLeftTruncation) {
            escapedValue = '*' + escapedValue;
        }
        if (addRightTruncation) {
            escapedValue += '*';
        }

        return escapedValue;
    }

    /**
     * Link after slash/backslash replacements for partner collection, static drill-down components and topic browsing (HU Berlin).
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getEscapedLink() {
        return BeanUtils.escapeCriticalUrlChracters(link);
    }

    /**
     * URL escaped link for using in search drill-downs.
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getUrlEscapedLink() {
        String ret = getEscapedLink();
        try {
            return URLEncoder.encode(ret, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return ret;
        }
    }

    
    @Override
    public FacetType getType() {
        return type;
    }

    
    public void setType(FacetType type) {
        this.type = type;
    }

    /**
     * Getter for the field <code>field</code>.
     *

     */
    @Override
    public String getField() {
        return field;
    }

    /**
     * Setter for the field <code>field</code>.
     *
     * @param field the Solr field name this facet item belongs to
     */
    @Override
    public void setField(String field) {
        this.field = field;
    }

    /**
     * getFullValue.
     *
     * @return Range of value - value2; just value if value2 empty
     * @should build full value correctly
     */
    public String getFullValue() {
        StringBuilder sb = new StringBuilder(value);
        if (StringUtils.isNotEmpty(value2)) {
            sb.append(" - ").append(value2);
        }

        return sb.toString();
    }

    /**
     * Getter for the field <code>value</code>.
     *

     */
    @Override
    public String getValue() {
        return value;
    }

    /**
     * Setter for the field <code>value</code>.
     *
     * @param value the facet value string
     */
    @Override
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Getter for the field <code>value2</code>.
     *

     */
    @Override
    public String getValue2() {
        return value2;
    }

    /**
     * Setter for the field <code>value2</code>.
     *
     * @param value2 the upper bound value for range facets
     */
    @Override
    public void setValue2(String value2) {
        this.value2 = value2;
    }

    /**
     * Getter for the field <code>link</code>.
     *

     */
    @Override
    public String getLink() {
        return link;
    }

    /**
     * Setter for the field <code>link</code>.
     *
     * @param link the colon-separated field:value string used as the facet link
     */
    @Override
    public void setLink(String link) {
        // TODO move logic out of the setter
        int colonIndex = link.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException(new StringBuilder().append("Field and value are not colon-separated: ").append(link).toString());
        }
        this.link = link;
        if (this.link.endsWith(";;")) {
            this.link = this.link.substring(0, this.link.length() - 2);
        }
        parseLink();
    }

    /**
     * Getter for the field <code>label</code>.
     *

     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Setter for the field <code>label</code>.
     *
     * @param label the display label for this facet item
     * @return this
     */
    @Override
    public FacetItem setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Getter for the field <code>translatedLabel</code>.
     *

     */
    @Override
    public String getTranslatedLabel() {
        if (DataManager.getInstance().getConfiguration().isTranslateFacetFieldLabels(field)) {
            return ViewerResourceBundle.getTranslation(label, BeanUtils.getLocale());
        }

        return label;
    }

    /**
     * Dummy setter to fulfill the interface contract.
     *
     * @param translatedLabel ignored; translations are computed dynamically from {@link #label}
     */
    @Override
    public void setTranslatedLabel(String translatedLabel) {
        // Do nothing
    }

    /**
     * Getter for the field <code>count</code>.
     *

     */
    @Override
    public long getCount() {
        return count;
    }

    /**
     * Setter for the field <code>count</code>.
     *
     * @param count the number of search results matching this facet value
     * @return this
     */
    @Override
    public FacetItem setCount(long count) {
        this.count = count;
        return this;
    }

    public void increaseCount(long amount) {
        this.count += amount;
    }

    
    @Override
    public boolean isGroup() {
        return group;
    }

    
    @Override
    public IFacetItem setGroup(boolean group) {
        this.group = group;
        return this;
    }

    /**
     * isHierarchial.
     *

     */
    @Override
    public boolean isHierarchial() {
        return hierarchial;
    }

    @Override
    public boolean isBooleanType() {
        return DataManager.getInstance().getConfiguration().getBooleanFacetFields().contains(field);
    }

    public String toString() {
        return field + ":" + value + " - " + value2;
    }

    public enum FacetType {
        STANDARD,
        BOOLEAN,
        GEO,
        HIERARCHICAL,
        RANGE;
    }
}
