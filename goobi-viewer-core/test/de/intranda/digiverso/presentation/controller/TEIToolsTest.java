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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.ocr.tei.convert.TeiToHtmlConvert;

public class TEIToolsTest {

    /**
     * @see TEITools#convertDocxToTei(Path)
     * @verifies convert docx to tei correctly
     */
    @Test
    public void convertDocxToTei_shouldConvertDocxToTeiCorrectly() throws Exception {
        String result = TEITools.convertDocxToTei(Paths.get("resources/test/data/example.docx"));
        Assert.assertTrue(StringUtils.isNotEmpty(result));
//        System.out.println(result);
        System.out.println(new TeiToHtmlConvert().convert(result));
    }
}