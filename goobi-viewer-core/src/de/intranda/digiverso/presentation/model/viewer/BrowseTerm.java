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
package de.intranda.digiverso.presentation.model.viewer;

import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.AlphanumCollatorComparator;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

public class BrowseTerm implements Serializable {

    private static final long serialVersionUID = -55691065713339706L;

    private static final Logger logger = LoggerFactory.getLogger(BrowseTerm.class);

    private final String term;
    private final String sortTerm;
    private long hitCount = 0;
    private final Set<String> piList = ConcurrentHashMap.newKeySet();

    /**
     * Constructor that sets <code>hitCount</code> to 1.
     *
     * @param term
     * @param sortTerm
     */
    public BrowseTerm(String term, String sortTerm) {
        this.term = term;
        this.sortTerm = sortTerm;
        this.hitCount = 1;
    }

    public BrowseTerm(String term, String sortTerm, long hitCount) {
        this.term = term;
        this.sortTerm = sortTerm;
        this.hitCount = hitCount;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (term == null ? 0 : term.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
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
        BrowseTerm other = (BrowseTerm) obj;
        if (term == null) {
            if (other.term != null) {
                return false;
            }
        } else if (!term.equals(other.term)) {
            return false;
        }
        return true;
    }

    /**
     * @return the term
     */
    public String getTerm() {
        return term;
    }

    /**
     * @return the sortTerm
     */
    public String getSortTerm() {
        return sortTerm;
    }

    /**
     * Custom string comparator for browsing terms (case-insensitive, ignores brackets, natural sorting).
     */
    public static class BrowseTermRawComparator implements Comparator<BrowseTerm>, Serializable {

        private static final long serialVersionUID = 8047374873015931547L;

        private Locale locale;
        private AlphanumCollatorComparator comparator;

        public BrowseTermRawComparator() {
            NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
            if (navigationHelper != null) {
                locale = navigationHelper.getLocale();
                //                logger.debug("Sorting locale: " + locale.getLanguage());
            }
            if (locale == null) {
                locale = Locale.GERMAN;
            }
            comparator = new AlphanumCollatorComparator(Collator.getInstance(locale));
        }

        @Override
        public int compare(BrowseTerm o1, BrowseTerm o2) {
            BrowseTerm o1a = o1;
            BrowseTerm o2a = o2;

            String relevantString1 = o1a.getTerm();
            if (StringUtils.isNotEmpty(relevantString1)) {
                if (o1a.getSortTerm() == null) {
                    relevantString1 = relevantString1.toLowerCase();
                    // Remove the first character, if not alphanumeric
                    if (relevantString1.length() > 1 && !StringUtils.isAlphanumeric(relevantString1.substring(0, 1))) {
                        relevantString1 = relevantString1.substring(1);
                    }
                } else {
                    relevantString1 = o1a.getSortTerm().toLowerCase();
                }
            }
            String relevantString2 = o2a.getTerm();
            if (StringUtils.isNotEmpty(relevantString2)) {
                if (o2a.getSortTerm() == null) {
                    relevantString2 = relevantString2.toLowerCase();
                    // Remove the first character, if not alphanumeric
                    if (relevantString2.length() > 1 && !StringUtils.isAlphanumeric(relevantString2.substring(0, 1))) {
                        relevantString2 = relevantString2.substring(1);
                    }
                } else {
                    relevantString2 = o2a.getSortTerm().toLowerCase();
                }
            }
            return comparator.compare(relevantString1, relevantString2);
        }
    }

    /**
     * Custom string comparator for browser term translations (case-insensitive, natural sorting).
     */
    public static class BrowseTermTranslatedComparator implements Comparator<BrowseTerm>, Serializable {

        private static final long serialVersionUID = 8047374873015931547L;

        private Locale locale;
        private AlphanumCollatorComparator comparator;

        public BrowseTermTranslatedComparator() {
            NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
            if (navigationHelper != null) {
                locale = navigationHelper.getLocale();
                //                logger.debug("Sorting locale: " + locale.getLanguage());
            }
            if (locale == null) {
                locale = Locale.GERMAN;
            }
            comparator = new AlphanumCollatorComparator(Collator.getInstance(locale));
        }

        @Override
        public int compare(BrowseTerm o1, BrowseTerm o2) {
            BrowseTerm o1a = o1;
            BrowseTerm o2a = o2;

            String relevantString1 = Helper.getTranslation(o1a.getTerm(), locale);
            if (StringUtils.isNotEmpty(relevantString1)) {
                if (o1a.getSortTerm() == null) {
                    relevantString1 = relevantString1.toLowerCase();
                } else {
                    relevantString1 = o1a.getSortTerm().toLowerCase();
                }
            }
            String relevantString2 = Helper.getTranslation(o2a.getTerm(), locale);
            if (StringUtils.isNotEmpty(relevantString2)) {
                if (o2a.getSortTerm() == null) {
                    relevantString2 = relevantString2.toLowerCase();
                } else {
                    relevantString2 = o2a.getSortTerm().toLowerCase();
                }
            }
            return comparator.compare(relevantString1, relevantString2);
        }
    }

    public void addToHitCount(int num) {
        hitCount += num;
    }

    /**
     * @return the hitCount
     */
    public long getHitCount() {
        return hitCount;
    }
    
    /**
     * @return the piList
     */
    public Set<String> getPiList() {
        return piList;
    }
    
    
}
