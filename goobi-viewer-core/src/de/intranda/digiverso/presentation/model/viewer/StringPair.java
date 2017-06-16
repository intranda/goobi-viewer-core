/**
 * This file is part of the Goobi Viewer - a content presentation and management application for digitized objects.
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
import java.util.Comparator;

public class StringPair implements Comparable<StringPair>, Serializable {

    private static final long serialVersionUID = 2587406123069125825L;

    private String one;
    private String two;

    public StringPair(String one, String two) {
        if (one == null || two == null) {
            throw new IllegalArgumentException("one and two must be not null.");
        }
        this.one = one;
        this.two = two;
    }

    /**
     * Compare by translation.
     */
    @Override
    public int compareTo(StringPair o) {
        return this.getTwo().toLowerCase().compareTo(o.getTwo().toLowerCase());
    }

    /**
     * @return the one
     */
    public String getOne() {
        return one;
    }

    /**
     * @param one the one to set
     */
    public void setOne(String one) {
        this.one = one;
    }

    /**
     * @return the two
     */
    public String getTwo() {
        return two;
    }

    /**
     * @param two the two to set
     */
    public void setTwo(String two) {
        this.two = two;
    }

    @Override
    public String toString() {
        return one + ":" + two;
    }

    public static class OneComparator implements Comparator<StringPair>, Serializable {

        private static final long serialVersionUID = -5579914817514299754L;

        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(StringPair o1, StringPair o2) {
            return o1.getOne().compareTo(o2.getOne());
        }
    }

    public static class TwoComparator implements Comparator<StringPair>, Serializable {

        private static final long serialVersionUID = 3377396736263291749L;

        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(StringPair o1, StringPair o2) {
            return o1.getTwo().compareTo(o2.getTwo());
        }
    }
}
