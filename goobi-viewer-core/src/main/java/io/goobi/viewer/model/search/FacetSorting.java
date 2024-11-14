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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import io.goobi.viewer.controller.sorting.AlphabeticComparator;
import io.goobi.viewer.controller.sorting.AlphanumComparator;
import io.goobi.viewer.controller.sorting.NumericComparator;

/**
 * Class to create maps for facet values and their respective counts which are automatically sorted according to a given sort order. The maps are
 * returned as a SortedMap class to signal that the map takes care of sorting its members
 */
public final class FacetSorting {

    /**
     * Utility class. No instantiation necessary
     */
    private FacetSorting() {

    }

    /**
     * Creates a SortingMap on the basis of the given map and with the given sortOrder. Possible values for sortOrder are
     * <ul>
     * <li>numerical</li>
     * <li>alphabetical</li>
     * <li>alphanumerical</li>
     * <li>alphabetical_raw</li>
     * </ul>
     * Each value can be suffixed by '_asc' or '_desc' to disignate that the sorting should be done ascending or descending, respectively. Default
     * order is ascending alphabetical and alphanumerical orderings may use message keys to determine order and thus take longer to evaluate; use
     * alphabetical_raw for a alphabetical ordering by the values alone, without considering message keys. Passing any other value for sortOrder keeps
     * the original ordering of the values as they are added to the map.
     * 
     * 
     * @param map Map containing values to sort
     * @param sortOrder sorting order
     * @return a SortingMap, which automatically orders entries as they are added to the map
     */
    public static final SortingMap<String, Long> getSortingMap(Map<String, Long> map, String sortOrder) {
        SortingMap<String, Long> sortingMap = getSortingMap("", sortOrder, null);
        map.entrySet().forEach(entry -> sortingMap.getMap().put(entry.getKey(), entry.getValue()));
        return sortingMap;
    }

    /**
     * Creates a SortingMap on the basis of the given field and locale and the given sortOrder. Possible values for sortOrder are
     * <ul>
     * <li>numerical</li>
     * <li>alphabetical</li>
     * <li>alphanumerical</li>
     * <li>alphabetical_raw</li>
     * </ul>
     * Each value can be suffixed by '_asc' or '_desc' to disignate that the sorting should be done ascending or descending, respectively. Default
     * order is ascending alphabetical and alphanumerical orderings may use message keys to determine order and thus take longer to evaluate; use
     * alphabetical_raw for a alphabetical ordering by the values alone, without considering message keys. Passing any other value for sortOrder keeps
     * the original ordering of the values as they are added to the map.
     * 
     * 
     * @param field The solr-field which values are to be sorted. Affects which configuration options are used for ordering
     * @param sortOrder sorting order
     * @param locale the locale to use if translated values are to be used for ordering
     * @return a SortingMap, which automatically orders entries as they are added to the map
     */
    public static SortingMap<String, Long> getSortingMap(String field, String sortOrder, Locale locale) {
        return new SortingMap<>(getMap(field, sortOrder, locale));
    }

    private static Map<String, Long> getMap(String field, String sortOrder, Locale locale) {
        switch (sortOrder) {
            case "numerical":
            case "numerical_asc":
                return new TreeMap<>(new NumericComparator<String>(Function.identity()));
            case "numerical_desc":
                return new TreeMap<>(new NumericComparator<String>(false, Function.identity()));
            case "alphabetical":
            case "alphabetical_asc":
                return new TreeMap<>(new AlphabeticComparator<String>(field, locale, Function.identity()));
            case "alphabetical_desc":
                return new TreeMap<>(new AlphabeticComparator<String>(field, locale, false, Function.identity()));
            case "alphabetical_raw":
            case "alphabetical_raw_asc":
                return new TreeMap<>();
            case "alphabetical_raw_desc":
                return new TreeMap<>((a, b) -> b.compareTo(a));
            case "alphanumerical":
            case "natural":
            case "natural_asc":
                return new TreeMap<>(new AlphanumComparator<String>(field, locale, Function.identity()));
            case "alphanumerical_desc":
            case "natural_desc":
                return new TreeMap<>(new AlphanumComparator<String>(field, locale, false, Function.identity()));
            case "count":
            default:
                return new LinkedHashMap<>();
        }
    }

    /**
     * A map container indicating the the contained map is created using one of the getSortedMap methods
     * 
     * @param <K> key
     * @param <V> value
     */
    public static final class SortingMap<K, V> {

        private final Map<K, V> map;

        private SortingMap(Map<K, V> map) {
            this.map = map;
        }

        /**
         * get the underlying map
         * 
         * @return a map
         */
        public Map<K, V> getMap() {
            return map;
        }

        /**
         * Add an entry to the underlying map
         * 
         * @param a key
         * @param l value
         */
        public void put(K a, V l) {
            this.map.put(a, l);
        }
    }

}
