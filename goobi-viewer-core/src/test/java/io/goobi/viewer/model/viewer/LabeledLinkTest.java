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
package io.goobi.viewer.model.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LabeledLinkTest {

    /**
     * @see LabeledLink#equals(Object)
     * @verifies return true for links with the same url regardless of name
     */
    @Test
    void equals_shouldReturnTrueForLinksWithTheSameUrlRegardlessOfName() {
        LabeledLink a = new LabeledLink("Title A", "/viewer/record/PI1/", 0);
        LabeledLink b = new LabeledLink("Title B", "/viewer/record/PI1/", 0);
        assertEquals(a, b);
    }

    /**
     * @see LabeledLink#equals(Object)
     * @verifies return false for links with different urls
     */
    @Test
    void equals_shouldReturnFalseForLinksWithDifferentUrls() {
        LabeledLink a = new LabeledLink("Title", "/viewer/record/PI1/", 0);
        LabeledLink b = new LabeledLink("Title", "/viewer/record/PI2/", 0);
        assertNotEquals(a, b);
    }

    /**
     * @see LabeledLink#equals(Object)
     * @verifies be consistent with hashCode
     */
    @Test
    void equals_shouldBeConsistentWithHashCode() {
        LabeledLink a = new LabeledLink("Title A", "/viewer/record/PI1/", 0);
        LabeledLink b = new LabeledLink("Title B", "/viewer/record/PI1/", 0);
        // Contract: a.equals(b) implies a.hashCode() == b.hashCode()
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }

    /**
     * @see LabeledLink#equals(Object)
     * @verifies handle null url correctly
     */
    @Test
    void equals_shouldHandleNullUrlCorrectly() {
        LabeledLink a = new LabeledLink("Title", null, 0);
        LabeledLink b = new LabeledLink("Title", null, 0);
        LabeledLink c = new LabeledLink("Title", "/viewer/record/PI1/", 0);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertFalse(a.equals(null));
    }
}
