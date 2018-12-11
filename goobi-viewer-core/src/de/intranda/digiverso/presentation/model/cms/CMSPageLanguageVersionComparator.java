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
package de.intranda.digiverso.presentation.model.cms;

import java.util.Comparator;
import java.util.Locale;

/**
 * Sorts {@link CMSPageLanguageVersion}s so that the version for the given locale is at the beginning of the Collection, followed by
 * the given defaultLocale, followed by the English and German locale, followed by all others in alphabetical order. The global language version is always the last element of the Collection
 * Language Versions that don't exist are ignored silently, as are the passed local and defaultLocale if they are null
 * 
 * @author Florian Alpers
 *
 */
public class CMSPageLanguageVersionComparator implements Comparator<CMSPageLanguageVersion> {

    private final Locale locale;
    private final Locale defaultLocale;
    /**
     * @param locale            The preferred locale, ignored if null
     * @param defaultLocale     The default locale, ignored if null
     */
    public CMSPageLanguageVersionComparator(Locale locale, Locale defaultLocale) {
        super();
        this.locale = locale;
        this.defaultLocale = defaultLocale;
    }
    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(CMSPageLanguageVersion l1, CMSPageLanguageVersion l2) {
        {
            if(l1.getLanguage().equals(CMSPage.GLOBAL_LANGUAGE)) {
                return 1;
            } else if(l2.getLanguage().equals(CMSPage.GLOBAL_LANGUAGE)) {
                return -1;
            } else if(locale != null && l1.getLanguage().equals(locale.toString())) {
                return -1;
            } else if(locale != null && l2.getLanguage().equals(locale.toString())) {
                return 1;
            } else if(defaultLocale != null && l1.getLanguage().equals(defaultLocale.toString())) {
                return -1;
            } else if(defaultLocale != null && l2.getLanguage().equals(defaultLocale.toString())) {
                return 1;
            } else if(l1.getLanguage().equals(Locale.ENGLISH.toString())) {
                return -1;
            } else if(l2.getLanguage().equals(Locale.ENGLISH.toString())) {
                return 1;
            } else if(l1.getLanguage().equals(Locale.GERMAN.toString())) {
                return -1;
            } else if(l2.getLanguage().equals(Locale.GERMAN.toString())) {
                return 1;
            } else {
                return l1.getLanguage().compareTo(l2.getLanguage());
            }
        }
    }
    
    
}
