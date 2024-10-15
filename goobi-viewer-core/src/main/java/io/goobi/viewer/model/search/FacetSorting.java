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
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.AlphanumCollatorComparator;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.messages.ViewerResourceBundle;

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

    public static class AlphabeticComparator implements Comparator<String> {

        private final int reverse;
        private final boolean translate;
        private final Collator col;
        private final Locale locale;

        public AlphabeticComparator(String field, Locale locale) {
            this(field, locale, true);
        }

        public AlphabeticComparator(String field, Locale locale, boolean asc) {
            this.translate = DataManager.getInstance().getConfiguration().isTranslateFacetFieldLabels(field);
            col = Collator.getInstance();
            col.setStrength(Collator.PRIMARY);
            this.reverse = asc ? 1 : -1;
            if (locale != null) {
                this.locale = locale;
            } else {
                this.locale = ViewerResourceBundle.getDefaultLocale();
            }
        }

        private String getTranslatedLabel(String label) {
            if (translate) {
                return ViewerResourceBundle.getTranslation(label, this.locale);
            }
            return label;
        }

        @Override
        public int compare(String o1, String o2) {
            String label1 = getTranslatedLabel(o1) != null ? getTranslatedLabel(o1) : o1;
            String label2 = getTranslatedLabel(o2) != null ? getTranslatedLabel(o2) : o2;

            return this.reverse * col.compare(label1, label2);
        }

    }

    public static class NumericComparator implements Comparator<String> {

        private final int reverse;

        public NumericComparator() {
            this(true);
        }

        public NumericComparator(boolean asc) {
            this.reverse = asc ? 1 : -1;
        }

        @Override
        public int compare(String o1, String o2) {
            try {
                int i1 = Integer.parseInt(o1);
                int i2 = Integer.parseInt(o2);
                return this.reverse * Integer.compare(i1, i2);
            } catch (NumberFormatException e) {
                return this.reverse * o1.compareTo(o2);
            }
        }

    }

    public static class AlphanumComparator implements Comparator<String>, Serializable {

        private static final long serialVersionUID = 8047374873015931547L;

        private final Locale locale;
        private final AlphanumCollatorComparator comparator;
        private final boolean translate;
        private final int reverse;

        public AlphanumComparator(String field, Locale locale) {
            this(field, locale, true);
        }

        public AlphanumComparator(String field, Locale locale, boolean asc) {
            if (locale != null) {
                this.locale = locale;
            } else {
                this.locale = ViewerResourceBundle.getDefaultLocale();
            }
            this.comparator = buildComparator();
            this.translate = DataManager.getInstance().getConfiguration().isTranslateFacetFieldLabels(field);
            this.reverse = asc ? 1 : -1;
        }

        private String getTranslatedLabel(String label) {
            if (translate) {
                return ViewerResourceBundle.getTranslation(label, this.locale);
            }
            return label;
        }

        private static AlphanumCollatorComparator buildComparator() {
            try {
                return new AlphanumCollatorComparator(new RuleBasedCollator("< a< b< c< d"));
            } catch (ParseException e) {
                return new AlphanumCollatorComparator(null);
            }
        }

        /**
         *
         * @should compare correctly
         * @should use sort term if provided
         * @should use translated term if provided
         * @should sort accented vowels after plain vowels
         */
        @Override
        public int compare(String o1, String o2) {
            String relevantString1 = getTranslatedLabel(o1) != null ? getTranslatedLabel(o1) : o1;
            String relevantString2 = getTranslatedLabel(o2) != null ? getTranslatedLabel(o2) : o2;
            // logger.trace("Comparing '{}' to '{}' ({})", relevantString1, relevantString2, locale); //NOSONAR Debug

            // If one of the strings starts with a non-alphanumerical character and the other doesn't, always sort the alphanumerical string first
            boolean string1Alphanum = true;
            boolean string2Alphanum = true;
            if (relevantString1.length() > 0 && !Character.isDigit(relevantString1.charAt(0)) && !Character.isLetter(relevantString1.charAt(0))) {
                string1Alphanum = false;
            }
            if (StringUtils.isNotEmpty(relevantString2) && !Character.isDigit(relevantString2.charAt(0))
                    && !Character.isLetter(relevantString2.charAt(0))) {
                string2Alphanum = false;
            }
            if (string1Alphanum && !string2Alphanum) {
                return -1;
            }
            if (!string1Alphanum && string2Alphanum) {
                return 1;
            }
            // Sort digits after letters
            if (Character.isDigit(relevantString1.charAt(0)) && StringUtils.isNotEmpty(relevantString2)
                    && Character.isLetter(relevantString2.charAt(0))) {
                return 1;
            }
            if (Character.isLetter(relevantString1.charAt(0)) && StringUtils.isNotEmpty(relevantString2)
                    && Character.isDigit(relevantString2.charAt(0))) {
                return -1;
            }

            return reverse * comparator.compare(relevantString1, relevantString2);
        }

        /**
         *
         * @param s String to normalize
         * @param ignoreChars Optional string containing leading characters to remove from the string
         * @return Cleaned-up string for comparison
         * @should use ignoreChars if provided
         * @should remove first char if non alphanum if ignoreChars not provided
         */
        public static String normalizeString(final String s, String ignoreChars) {
            if (s == null) {
                return null;
            }

            String ret = s;
            if (StringUtils.isNotEmpty(ignoreChars)) {
                // Remove leading chars if they are among ignored chars
                while (ret.length() > 1 && ignoreChars.contains(ret.substring(0, 1))) {
                    ret = ret.substring(1);
                }
            } else {
                // Remove the first character, if not alphanumeric
                if (ret.length() > 1 && !StringUtils.isAlphanumeric(ret.substring(0, 1))) {
                    ret = ret.substring(1);
                }
            }

            return ret;
        }
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
    public static SortingMap<String, Long> getSortingMap(Map<String, Long> map, String sortOrder) {
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
                return new TreeMap<>(new NumericComparator());
            case "numerical_desc":
                return new TreeMap<>(new NumericComparator(false));
            case "alphabetical":
            case "alphabetical_asc":
                return new TreeMap<>(new AlphabeticComparator(field, locale));
            case "alphabetical_desc":
                return new TreeMap<>(new AlphabeticComparator(field, locale, false));
            case "alphabetical_raw":
            case "alphabetical_raw_asc":
                return new TreeMap<>();
            case "alphabetical_raw_desc":
                return new TreeMap<>((a, b) -> b.compareTo(a));
            case "alphanumerical":
            case "natural":
            case "natural_asc":
                return new TreeMap<>(new AlphanumComparator(field, locale));
            case "alphanumerical_desc":
            case "natural_desc":
                return new TreeMap<>(new AlphanumComparator(field, locale, false));
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
    public static class SortingMap<K, V> {

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
