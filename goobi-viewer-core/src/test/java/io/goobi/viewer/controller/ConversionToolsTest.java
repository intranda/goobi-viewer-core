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
package io.goobi.viewer.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.controller.ConversionTools;

public class ConversionToolsTest {

    /**
     * @see ConversionTools#convertFileToHtml(Path)
     * @verifies convert docx file correctly
     */
    @Test
    @Deprecated
    public void convertFileToHtml_shouldConvertDocxFileCorrectly() throws Exception {
        Path rtfFile = Paths.get("src/test/resources/data/text_example.docx");
        Assert.assertTrue(Files.isRegularFile(rtfFile));
        String html = ConversionTools.convertFileToHtml(rtfFile);
        Assert.assertNotNull(html);
        // FileTools.getFileFromString(html, "src/test/resources/data/example (tika docx).htm", Helper.DEFAULT_ENCODING, false);
    }

    /**
     * @see ConversionTools#convertFileToHtml(Path)
     * @verifies convert rtf file correctly
     */
    @Test
    @Deprecated
    public void convertFileToHtml_shouldConvertRtfFileCorrectly() throws Exception {
        Path rtfFile = Paths.get("src/test/resources/data/text_example.rtf");
        Assert.assertTrue(Files.isRegularFile(rtfFile));
        String html = ConversionTools.convertFileToHtml(rtfFile);
        Assert.assertNotNull(html);
        // FileTools.getFileFromString(html, "src/test/resources/data/example (tika rtf).htm", Helper.DEFAULT_ENCODING, false);
    }

    /**
     * @see ConversionTools#convertDocxToHtml(Path)
     * @verifies convert docx correctly
     */
    @Test
    @Deprecated
    public void convertDocxToHtml_shouldConvertDocxCorrectly() throws Exception {
        String html = ConversionTools.convertDocxToHtml(Paths.get("src/test/resources/data/text_example.docx"));
        Assert.assertNotNull(html);
        // FileTools.getFileFromString(html, "src/test/resources/data/example (docx4j).htm", Helper.DEFAULT_ENCODING, false);
    }
}