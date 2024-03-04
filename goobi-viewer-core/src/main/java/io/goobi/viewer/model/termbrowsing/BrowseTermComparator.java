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
package io.goobi.viewer.model.termbrowsing;

import java.io.Serializable;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.AlphanumCollatorComparator;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Custom string comparator for browsing terms (case-insensitive, ignores brackets, natural sorting).
 */
public class BrowseTermComparator implements Comparator<BrowseTerm>, Serializable {

    private static final long serialVersionUID = 8047374873015931547L;

    private static final Logger logger = LogManager.getLogger(BrowseTermComparator.class);

    private final Locale locale;
    private AlphanumCollatorComparator comparator;

    public BrowseTermComparator(Locale locale) {
        if (locale != null) {
            this.locale = locale;
        } else {
            this.locale = ViewerResourceBundle.getDefaultLocale();
        }
        // comparator = new AlphanumCollatorComparator(Collator.getInstance(this.locale));
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
    public int compare(BrowseTerm o1, BrowseTerm o2) {
        BrowseTerm o1a = o1;
        BrowseTerm o2a = o2;

        String relevantString1 = o1a.getTerm();
        if (StringUtils.isNotEmpty(relevantString1)) {
            Optional<String> translationValue = o1a.getTranslations() != null ? o1a.getTranslations().getValue(locale) : Optional.empty();
            if (o1a.getSortTerm() != null) {
                // sort term
                relevantString1 = o1a.getSortTerm().toLowerCase();
            } else if (translationValue.isPresent()) {
                // translated term
                relevantString1 = translationValue.get();
            } else {
                // raw term
                relevantString1 = relevantString1.toLowerCase();
            }
        }
        relevantString1 = normalizeString(relevantString1, DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars());

        String relevantString2 = o2a.getTerm();
        if (StringUtils.isNotEmpty(relevantString2)) {
            Optional<String> translationValue = o2a.getTranslations() != null ? o2a.getTranslations().getValue(locale) : Optional.empty();
            if (o2a.getSortTerm() != null) {
                relevantString2 = o2a.getSortTerm().toLowerCase();
            } else if (translationValue.isPresent()) {
                relevantString2 = translationValue.get();
            } else {
                relevantString2 = relevantString2.toLowerCase();
            }
        }
        relevantString2 = normalizeString(relevantString2, DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars());

        // logger.trace("Comparing '{}' to '{}' ({})", relevantString1, relevantString2, locale);
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
    public static String normalizeString(final String str, String ignoreChars) {
        if (str == null) {
            return null;
        }

        String s = str;
        if (StringUtils.isNotEmpty(ignoreChars)) {
            // Remove leading chars if they are among ignored chars
            while (s.length() > 1 && ignoreChars.contains(s.substring(0, 1))) {
                s = s.substring(1);
            }
        } else {
            // Remove the first character, if not alphanumeric
            if (s.length() > 1 && !StringUtils.isAlphanumeric(s.substring(0, 1))) {
                s = s.substring(1);
            }
        }

        return s;
    }
}
