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

public class FacetSorting {

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
            } else {
                return label;
            }
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

    public class AlphanumComparator implements Comparator<String>, Serializable {

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
            } else {
                return label;
            }
        }

        private AlphanumCollatorComparator buildComparator() {
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
            if (relevantString2.length() > 0 && !Character.isDigit(relevantString2.charAt(0)) && !Character.isLetter(relevantString2.charAt(0))) {
                string2Alphanum = false;
            }
            if (string1Alphanum && !string2Alphanum) {
                return -1;
            }
            if (!string1Alphanum && string2Alphanum) {
                return 1;
            }
            // Sort digits after letters
            if (Character.isDigit(relevantString1.charAt(0)) && Character.isLetter(relevantString2.charAt(0))) {
                return 1;
            }
            if (Character.isLetter(relevantString1.charAt(0)) && Character.isDigit(relevantString2.charAt(0))) {
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

    public Map<String, Long> getSortingMap(String field, String sortOrder, Locale locale) {
        switch (sortOrder) {
            case "numerical":
            case "numerical_asc":
                return new TreeMap<String, Long>(new NumericComparator());
            case "numerical_desc":
                return new TreeMap<String, Long>(new NumericComparator(false));
            case "alphabetical":
            case "alphabetical_asc":
                return new TreeMap<String, Long>(new AlphabeticComparator(field, locale));
            case "alphabetical_desc":
                return new TreeMap<String, Long>(new AlphabeticComparator(field, locale, false));
            case "alphabetical_raw":
            case "alphabetical_raw_asc":
                return new TreeMap<String, Long>();
            case "alphabetical_raw_desc":
                return new TreeMap<String, Long>((a, b) -> b.compareTo(a));
            case "alphanumerical":
            case "natural":
            case "natural_asc":
                return new TreeMap<String, Long>(new AlphanumComparator(field, locale));
            case "alphanumerical_desc":
            case "natural_desc":
                return new TreeMap<String, Long>(new AlphanumComparator(field, locale, false));
            case "count":
            default:
                return new LinkedHashMap<>();
        }
    }

}
