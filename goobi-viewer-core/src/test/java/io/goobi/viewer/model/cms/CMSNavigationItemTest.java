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
package io.goobi.viewer.model.cms;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CMSNavigationItemTest {

    /**
     * @see CMSNavigationItem#compareTo(CMSNavigationItem)
     * @verifies not overflow for distant order values
     */
    @Test
    void compareTo_shouldNotOverflowForDistantOrderValues() {
        CMSNavigationItem low = new CMSNavigationItem();
        low.setOrder(Integer.MIN_VALUE + 1);
        CMSNavigationItem high = new CMSNavigationItem();
        high.setOrder(Integer.MAX_VALUE);

        assertTrue(low.compareTo(high) < 0);
        assertTrue(high.compareTo(low) > 0);
    }
}
