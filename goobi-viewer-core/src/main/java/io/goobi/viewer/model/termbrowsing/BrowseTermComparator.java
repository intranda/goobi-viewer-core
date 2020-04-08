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
package io.goobi.viewer.model.termbrowsing;

import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.AlphanumCollatorComparator;

/**
 * Custom string comparator for browsing terms (case-insensitive, ignores brackets, natural sorting).
 */
public class BrowseTermComparator implements Comparator<BrowseTerm>, Serializable {

    private static final long serialVersionUID = 8047374873015931547L;

    private static final Logger logger = LoggerFactory.getLogger(BrowseTermComparator.class);

    private final Locale locale;
    private AlphanumCollatorComparator comparator;

    public BrowseTermComparator(Locale locale) {
        if (locale != null) {
            this.locale = locale;
        } else {
            this.locale = Locale.GERMAN;
        }
        comparator = new AlphanumCollatorComparator(Collator.getInstance(this.locale));
    }

    /**
     * 
     * @should compare correctly
     * @should use sort term if provided
     * @should use translated term if provided
     */
    @Override
    public int compare(BrowseTerm o1, BrowseTerm o2) {
        BrowseTerm o1a = o1;
        BrowseTerm o2a = o2;

        String relevantString1 = o1a.getTerm();
        if (StringUtils.isNotEmpty(relevantString1)) {
            if (o1a.getSortTerm() != null) {
                // sort term
                relevantString1 = o1a.getSortTerm().toLowerCase();
            } else if (o1a.getTranslations() != null && o1a.getTranslations().getValue(locale) != null
                    && o1a.getTranslations().getValue(locale).isPresent()) {
                // translated term
                relevantString1 = o1a.getTranslations().getValue(locale).get();
            } else {
                // raw term
                relevantString1 = relevantString1.toLowerCase();
            }
        }
        // Remove the first character, if not alphanumeric
        if (relevantString1.length() > 1 && !StringUtils.isAlphanumeric(relevantString1.substring(0, 1))) {
            relevantString1 = relevantString1.substring(1);
        }

        String relevantString2 = o2a.getTerm();
        if (StringUtils.isNotEmpty(relevantString2)) {
            if (o2a.getSortTerm() != null) {
                relevantString2 = o2a.getSortTerm().toLowerCase();
            } else if (o2a.getTranslations() != null && o2a.getTranslations().getValue(locale) != null
                    && o2a.getTranslations().getValue(locale).isPresent()) {
                relevantString2 = o2a.getTranslations().getValue(locale).get();
            } else {
                relevantString2 = relevantString2.toLowerCase();
            }
        }
        // Remove the first character, if not alphanumeric
        if (relevantString2.length() > 1 && !StringUtils.isAlphanumeric(relevantString2.substring(0, 1))) {
            relevantString2 = relevantString2.substring(1);
        }

        // logger.trace("Comparing '{}' to '{}' ({})", relevantString1, relevantString2, locale);
        return comparator.compare(relevantString1, relevantString2);
    }
}
