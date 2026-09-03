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
package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

class CollectionBrowseBeanTest extends AbstractTest {

    /**
     * @see CollectionBrowseBean#getCollectionHierarchy(String,String)
     * @verifies return slash-separated ancestor chain for dot-delimited collection name
     */
    @Test
    void getCollectionHierarchy_shouldReturnSlashSeparatedAncestorChainForDotDelimitedCollectionName() {
        CollectionBrowseBean bb = new CollectionBrowseBean();
        assertEquals("foo", bb.getCollectionHierarchy("x", "foo"));
        assertEquals("foo / foo.bar", bb.getCollectionHierarchy("x", "foo.bar"));
    }

    /**
     * @see CollectionBrowseBean#getDynamicCollectionView(String)
     * @verifies return null for blank name
     */
    @Test
    void getDynamicCollectionView_shouldReturnNullForBlankName() {
        CollectionBrowseBean bb = new CollectionBrowseBean();
        assertNull(bb.getDynamicCollectionView(null));
        assertNull(bb.getDynamicCollectionView(" "));
    }
}
