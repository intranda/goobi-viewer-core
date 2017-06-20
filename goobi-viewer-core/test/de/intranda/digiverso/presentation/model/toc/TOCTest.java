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
package de.intranda.digiverso.presentation.model.toc;

import org.junit.Assert;
import org.junit.Test;

public class TOCTest {

    /**
     * @see TOC#getNumPages()
     * @verifies calculate number correctly
     */
    @Test
    public void getNumPages_shouldCalculateNumberCorrectly() throws Exception {
        TOC toc = new TOC();
        toc.setTotalTocSize(70);
        Assert.assertEquals(7, toc.getNumPages());
        toc.setTotalTocSize(77);
        Assert.assertEquals(8, toc.getNumPages());
    }
}