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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

public class FacetItem implements Comparable<FacetItem>, Serializable {

    private static final long serialVersionUID = 5033196184122928247L;

    private static final Logger logger = LoggerFactory.getLogger(FacetItem.class);

    private static final Comparator<FacetItem> NUMERIC_COMPARATOR = new FacetItem.NumericComparator();
    private static final Comparator<FacetItem> ALPHABETIC_COMPARATOR = new FacetItem.AlphabeticComparator();

    //    private static AlphanumCollatorComparator comparator = new AlphanumCollatorComparator(null);

    private String field;
    private String value;
    private String value2;
    private String link;
    private String label;
    private String translatedLabel;
    private long count;
    private final boolean hierarchial;

    /**
     * Constructor for active facets received via the URL. The Solr query is split into individual field/value.
     *
     * @param link
     * @param hierarchical
     * @should split field and value correctly
     * @should split field and value range correctly
     */
    public FacetItem(String link, boolean hierarchical) {
        int colonIndex = link.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException(new StringBuilder().append("Field and value are not colon-separated: ").append(link).toString());
        }
        this.link = link;
        this.hierarchial = hierarchical;
        parseLink(link);
    }

    /**
     *
     * @param link {@link String}
     * @param label {@link String}
     * @param translatedLabel {@link String}
     * @param count {@link Integer}
     * @param hierarchical
     */
    private FacetItem(String field, String link, String label, String translatedLabel, long count, boolean hierarchical) {
        this.field = field;
        this.link = link.trim();
        parseLink(link);
        this.label = label;
        this.translatedLabel = translatedLabel;
        this.count = count;
        this.hierarchial = hierarchical;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + ((link == null) ? 0 : link.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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

    @Override
    public int compareTo(FacetItem facetItem) {
        //                return facetItem.getTranslatedLabel().toLowerCase().compareTo(translatedLabel.toLowerCase());
        return count > facetItem.getCount() ? +1 : count < facetItem.getCount() ? -1 : 0;
    }

    /**
     * Extracts field name and value(s) from the given link string.
     * 
     * @param link
     */
    void parseLink(String link) {
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
    }

    /**
     * Constructs Lucene queries for the drill-down. Always sorted by the label translation.
     *
     * @return {@link ArrayList} of {@link FacetItem}
     * @should add priority values first
     */
    public static List<FacetItem> generateFilterLinkList(String field, Map<String, Long> values, boolean hierarchical, Locale locale) {
        List<FacetItem> retList = new ArrayList<>();
        List<String> priorityValues = DataManager.getInstance().getConfiguration().getPriorityValuesForDrillDownField(field);
        Map<String, FacetItem> priorityValueMap = new HashMap<>(priorityValues.size());
        for (String value : values.keySet()) {
            // Skip reversed values
            if (value.charAt(0) == 1) {
                continue;
            }
            String label = value;
            if (StringUtils.isEmpty(field)) {
                label = new StringBuilder(value).append(SolrConstants._DRILLDOWN_SUFFIX).toString();
            }
            String linkValue = value;
            if (field.endsWith(SolrConstants._UNTOKENIZED)) {
                linkValue = new StringBuilder("\"").append(linkValue).append('"').toString();
            }
            String link = StringUtils.isNotEmpty(field) ? new StringBuilder(field).append(':').append(linkValue).toString() : linkValue;
            FacetItem facetItem =
                    new FacetItem(field, link, Helper.intern(label), Helper.getTranslation(label, locale), values.get(value), hierarchical);
            if (!priorityValues.isEmpty() && priorityValues.contains(value)) {
                priorityValueMap.put(value, facetItem);
            } else {
                retList.add(facetItem);
            }
        }
        switch (DataManager.getInstance().getConfiguration().getSortOrder(SearchHelper.defacetifyField(field))) {
            case "numerical":
            case "numerical_asc":
                Collections.sort(retList, FacetItem.NUMERIC_COMPARATOR);
                break;
            case "numerical_desc":
                Collections.sort(retList, FacetItem.NUMERIC_COMPARATOR);
                Collections.reverse(retList);
                break;
            case "alphabetical":
            case "alphabetical_asc":
                Collections.sort(retList, FacetItem.ALPHABETIC_COMPARATOR);
                break;
            case "alphabetical_desc":
                Collections.sort(retList, FacetItem.ALPHABETIC_COMPARATOR);
                Collections.reverse(retList);
                break;
            default:
                Collections.sort(retList); // sort by count
                Collections.reverse(retList);

        }
        // Add priority values at the beginning
        if (!priorityValueMap.isEmpty()) {
            List<FacetItem> regularValues = new ArrayList<>(retList);
            retList.clear();
            for (String val : priorityValues) {
                if (priorityValueMap.containsKey(val)) {
                    retList.add(priorityValueMap.get(val));
                }
            }
            retList.addAll(regularValues);
        }
        // logger.debug("filters: " + retList.size());
        return retList;
    }

    /**
     * Constructs a list of FilterLink objects for the drill-down. Optionally sorted by the raw values.
     *
     * @param field
     * @param values
     * @param sort
     * @param reverseOrder
     * @param hierarchical
     * @param locale
     * @return
     * @should sort items correctly
     */
    public static List<FacetItem> generateFacetItems(String field, Map<String, Long> values, boolean sort, boolean reverseOrder, boolean hierarchical,
            Locale locale) {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (values == null) {
            throw new IllegalArgumentException("values may not be null");
        }

        List<FacetItem> retList = new ArrayList<>(values.keySet().size());

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
                label += SolrConstants._DRILLDOWN_SUFFIX;
            }
            String link = StringUtils.isNotEmpty(field) ? field + ":" + ClientUtils.escapeQueryChars(String.valueOf(value)) : String.valueOf(value);
            retList.add(new FacetItem(field, link, label, Helper.getTranslation(label, locale), values.get(String.valueOf(value)), hierarchical));
        }

        // logger.debug("filters: " + retList.size());
        return retList;
    }

    /**
     * Returns field:value (with the value escaped for the Solr query).
     *
     * @return
     * @should construct link correctly
     * @should escape values containing whitespaces
     * @should construct hierarchical link correctly
     * @should construct range link correctly
     */
    public String getQueryEscapedLink() {
        String escapedValue = getEscapedValue(value);
        if (hierarchial) {
            return new StringBuilder("(").append(field)
                    .append(':')
                    .append(escapedValue)
                    .append(" OR ")
                    .append(field)
                    .append(':')
                    .append(escapedValue)
                    .append(".*)")
                    .toString();
        }
        if (value2 == null) {
            logger.debug("value2 is null");
            return new StringBuilder(field).append(':').append(escapedValue).toString();
        }
        String escapedValue2 = getEscapedValue(value2);
        return new StringBuilder(field).append(":[").append(escapedValue).append(" TO ").append(escapedValue2).append(']').toString();
    }

    /**
     * 
     * @param value
     * @return
     */
    static String getEscapedValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }

        String escapedValue = ClientUtils.escapeQueryChars(value);
        if (escapedValue.contains(" ") && !escapedValue.startsWith("\"") && !escapedValue.endsWith("\"")) {
            escapedValue = '"' + escapedValue + '"';
        }

        return escapedValue;
    }

    /**
     * Link after slash/backslash replacements for partner collection, static drill-down components and topic browsing (HU Berlin).
     *
     * @return
     */
    public String getEscapedLink() {
        return BeanUtils.escapeCriticalUrlChracters(link);
    }

    /**
     * URL escaped link for using in search drill-downs.
     *
     * @return
     */
    public String getUrlEscapedLink() {
        String ret = getEscapedLink();
        try {
            return URLEncoder.encode(ret, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return ret;
        }
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @param field the field to set
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
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
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the value2
     */
    public String getValue2() {
        return value2;
    }

    /**
     * @param value2 the value2 to set
     */
    public void setValue2(String value2) {
        this.value2 = value2;
    }

    /**
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
        parseLink(link);
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the translatedLabel
     */
    public String getTranslatedLabel() {
        return translatedLabel;
    }

    /**
     * @param translatedLabel the translatedLabel to set
     */
    public void setTranslatedLabel(String translatedLabel) {
        this.translatedLabel = translatedLabel;
    }

    /**
     * @return the count
     */
    public long getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * @return the hierarchial
     */
    public boolean isHierarchial() {
        return hierarchial;
    }

    public static class AlphabeticComparator implements Comparator<FacetItem> {

        @Override
        public int compare(FacetItem o1, FacetItem o2) {
            int ret = o1.getLabel().compareTo(o2.getLabel());
            return ret;
        }

    }

    public static class NumericComparator implements Comparator<FacetItem> {

        @Override
        public int compare(FacetItem o1, FacetItem o2) {
            try {
                int i1 = Integer.parseInt(o1.getLabel());
                int i2 = Integer.parseInt(o2.getLabel());
                return Integer.compare(i1, i2);
            } catch (NumberFormatException e) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        }

    }
}
