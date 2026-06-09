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
package io.goobi.viewer.model.cms.pages.content.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.cms.pages.content.CMSContent;

class CMSStatisticsFilterContentTest {

    /**
     * @see CMSStatisticsFilterContent#copy()
     * @verifies produce an independent copy with same filter query
     * @verifies copy logarithmic scale flag
     */
    @Test
    void copy_shouldProduceAnIndependentCopyWithSameFilterQueryAndLogarithmicScale() {
        CMSStatisticsFilterContent orig = new CMSStatisticsFilterContent();
        orig.setFilterQuery("DC:zeitschriften");
        orig.setLogarithmicScale(true);

        CMSContent copy = orig.copy();

        assertNotSame(orig, copy);
        assertEquals("DC:zeitschriften", ((CMSStatisticsFilterContent) copy).getFilterQuery());
        assertTrue(((CMSStatisticsFilterContent) copy).isLogarithmicScale());
        // Mutate the copy and verify the original is unaffected — guards against shared-state copies.
        ((CMSStatisticsFilterContent) copy).setFilterQuery("DC:other");
        ((CMSStatisticsFilterContent) copy).setLogarithmicScale(false);
        assertEquals("DC:zeitschriften", orig.getFilterQuery());
        assertTrue(orig.isLogarithmicScale());
    }

    /**
     * @see CMSStatisticsFilterContent#isEmpty()
     * @verifies return false even with blank filter query
     */
    @Test
    void isEmpty_shouldReturnFalseEvenWithBlankFilterQuery() {
        CMSStatisticsFilterContent c = new CMSStatisticsFilterContent();
        // Blank filter is a valid "no constraint" state — the content must still render its editor, so it is not empty.
        assertFalse(c.isEmpty());
    }

    @Test
    void getBackendComponentName_shouldReturnStatisticsFilter() {
        assertEquals("statisticsFilter", new CMSStatisticsFilterContent().getBackendComponentName());
    }
}
