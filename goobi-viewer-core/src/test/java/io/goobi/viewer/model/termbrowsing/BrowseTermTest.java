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
package io.goobi.viewer.model.termbrowsing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BrowseTermTest {

    /**
     * @see BrowseTerm#addToHitCount(int)
     * @verifies add to hit count correctly
     */
    @Test
    void addToHitCount_shouldAddToHitCountCorrectly() throws Exception {
        BrowseTerm browseTerm = new BrowseTerm("foo", null, null);
        Assertions.assertEquals(0, browseTerm.getHitCount());

        browseTerm.addToHitCount(1);
        Assertions.assertEquals(1, browseTerm.getHitCount());

        browseTerm.addToHitCount(2);
        Assertions.assertEquals(3, browseTerm.getHitCount());
    }
}
