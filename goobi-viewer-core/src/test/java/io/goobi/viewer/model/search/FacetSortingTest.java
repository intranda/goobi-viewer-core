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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.search.FacetSorting.AlphabeticComparator;
import io.goobi.viewer.model.search.FacetSorting.NumericComparator;

class FacetSortingTest extends AbstractTest {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Nested
    class AlphabeticComparatorTest extends AbstractTest {

        /**
         * @see CountComparator#compare(IFacetItem,IFacetItem)
         * @verifies compare correctly
         */
        @Test
        void compare_shouldCompareCorrectly() {
            Assertions.assertEquals(1, new AlphabeticComparator("MD_FOO", null).compare("b", "a"));
            Assertions.assertEquals(-1, new AlphabeticComparator("MD_FOO", null).compare("a", "b"));
            Assertions.assertEquals(0, new AlphabeticComparator("MD_FOO", null).compare("ä", "a"));
        }
    }

    @Nested
    class NumericComparatorTest extends AbstractTest {

        /**
         * @see CountComparator#compare(IFacetItem,IFacetItem)
         * @verifies compare correctly
         */
        @Test
        void compare_shouldCompareCorrectly() {
            Assertions.assertEquals(1, new NumericComparator().compare("2", "1"));
            Assertions.assertEquals(-1, new NumericComparator().compare("1", "2"));
            Assertions.assertEquals(0, new NumericComparator().compare("1", "1"));
            Assertions.assertEquals(-1, new NumericComparator(false).compare("2", "1"));
            Assertions.assertEquals(1, new NumericComparator(false).compare("1", "2"));
        }
    }

}
