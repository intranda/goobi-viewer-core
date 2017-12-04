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
package de.intranda.digiverso.presentation.controller;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.TeiToHtmlConverter.ConverterMode;

public class TeiToHtmlConverterTest {

    /**
     * @see TeiToHtmlConverter#populateHierarchyLevels(String)
     * @verifies map hierarchy levels correctly
     */
    @Test
    public void populateHierarchyLevels_shouldMapHierarchyLevelsCorrectly() throws Exception {
        final String text = "<div><h1>H1<h1><div><p>some text</p><div><h3>H3<h3><div><h4>H4</h4></div></div></div></div>";
        TeiToHtmlConverter converter = new TeiToHtmlConverter(ConverterMode.resource);
        converter.populateHierarchyLevels(text);

        for (int i = 0; i <= 14; ++i) {
            Assert.assertEquals(1, converter.getHierarchyLevel(i));
        }
        for (int i = 15; i <= 35; ++i) {
            Assert.assertEquals(2, converter.getHierarchyLevel(i));
        }
        for (int i = 36; i <= 50; ++i) {
            Assert.assertEquals(3, converter.getHierarchyLevel(i));
        }
        for (int i = 51; i <= 72; ++i) {
            Assert.assertEquals(4, converter.getHierarchyLevel(i));
        }
        Assert.assertEquals(3, converter.getHierarchyLevel(73));
        Assert.assertEquals(3, converter.getHierarchyLevel(78));
        Assert.assertEquals(2, converter.getHierarchyLevel(79));
        Assert.assertEquals(2, converter.getHierarchyLevel(84));
        Assert.assertEquals(1, converter.getHierarchyLevel(85));
        Assert.assertEquals(1, converter.getHierarchyLevel(90));
    }
}