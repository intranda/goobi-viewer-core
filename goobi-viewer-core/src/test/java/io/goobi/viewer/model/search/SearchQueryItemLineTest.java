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
import org.junit.jupiter.api.Test;

class SearchQueryItemLineTest {

    /**
     * @see SearchQueryItemLine#toggleValue(String)
     * @verifies set values correctly
     */
    @Test
    void toggleValue_shouldSetValuesCorrectly() {
        // Toggling a value that is not present should add it to the values list
        SearchQueryItemLine line = new SearchQueryItemLine();
        Assertions.assertFalse(line.isValueSet("foo"));
        line.toggleValue("foo");
        Assertions.assertTrue(line.isValueSet("foo"));
    }

    /**
     * @see SearchQueryItemLine#toggleValue(String)
     * @verifies unset values correctly
     */
    @Test
    void toggleValue_shouldUnsetValuesCorrectly() {
        // Toggling an already-present value should remove it from the values list
        SearchQueryItemLine line = new SearchQueryItemLine();
        line.toggleValue("foo");
        Assertions.assertTrue(line.isValueSet("foo"));
        line.toggleValue("foo");
        Assertions.assertFalse(line.isValueSet("foo"));
    }
}
