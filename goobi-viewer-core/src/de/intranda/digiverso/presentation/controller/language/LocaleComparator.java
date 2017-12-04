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
package de.intranda.digiverso.presentation.controller.language;

import java.util.Comparator;
import java.util.Locale;

public class LocaleComparator implements Comparator<Locale> {

    private final Locale primaryLocale;
    private final Locale secondaryLocale;

    /**
     *
     * @param locale
     */
    public LocaleComparator(Locale locale) {
        this.primaryLocale = locale;
        this.secondaryLocale = Locale.ENGLISH;
    }

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Locale l1, Locale l2) {
        if (l1 == null && l2 == null) {
            return 0;
        } else if (l1 == null) {
            return 1;
        } else if (l2 == null) {
            return 2;
        }
        int i1 = l1.getLanguage().equals(primaryLocale.getLanguage()) ? 1 : (l1.getLanguage().equals(secondaryLocale.getLanguage()) ? 2 : 3);
        int i2 = l2.getLanguage().equals(primaryLocale.getLanguage()) ? 1 : (l2.getLanguage().equals(secondaryLocale.getLanguage()) ? 2 : 3);

        return Integer.compare(i1, i2);
    }

}
