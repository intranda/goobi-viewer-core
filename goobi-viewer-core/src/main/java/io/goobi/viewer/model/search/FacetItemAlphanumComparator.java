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
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.sorting.AlphanumCollatorComparator;
import io.goobi.viewer.controller.sorting.AlphanumComparator;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Custom string comparator for browsing terms (case-insensitive, ignores brackets, natural sorting).
 * 
 * @deprecated use {@link AlphanumComparator} instead with constructor
 * 
 *             <pre>{@code new AlphanumComparator<IFacetItem>(true, locale,
                        i -> i.getTranslatedLabel() != null ? i.getTranslatedLabel() : i.getLabel())}</pre>
 */
@Deprecated(since = "24.10")
public class FacetItemAlphanumComparator implements Comparator<IFacetItem>, Serializable {

    private static final long serialVersionUID = 8047374873015931547L;

    private static final Logger logger = LogManager.getLogger(FacetItemAlphanumComparator.class);

    private final Locale locale;
    private transient AlphanumCollatorComparator comparator;

    public FacetItemAlphanumComparator(Locale locale) {
        if (locale != null) {
            this.locale = locale;
        } else {
            this.locale = ViewerResourceBundle.getDefaultLocale();
        }
        try {
            comparator = new AlphanumCollatorComparator(new RuleBasedCollator("< a< b< c< d"));
        } catch (ParseException e) {
            logger.error(e.getMessage());
            comparator = new AlphanumCollatorComparator(null);
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
    public int compare(IFacetItem o1, IFacetItem o2) {
        String relevantString1 = o1.getTranslatedLabel() != null ? o1.getTranslatedLabel() : o1.getLabel();
        String relevantString2 = o2.getTranslatedLabel() != null ? o2.getTranslatedLabel() : o2.getLabel();
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

        return comparator.compare(relevantString1, relevantString2);
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
