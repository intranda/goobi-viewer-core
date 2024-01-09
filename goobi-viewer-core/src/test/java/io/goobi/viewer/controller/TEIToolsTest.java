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
package io.goobi.viewer.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

public class TEIToolsTest extends AbstractTest {

    /**
     * @see TEITools#getTeiFulltext(String)
     * @verifies extract fulltext correctly
     */
    @Test
    public void getTeiFulltext_shouldExtractFulltextCorrectly() throws Exception {
        Path path = Paths.get("src/test/resources/data/viewer/tei/DE_2013_Riedel_PolitikUndCo_241__248/DE_2013_Riedel_PolitikUndCo_241__248_eng.xml");
        Assertions.assertTrue(Files.isRegularFile(path));
        String tei = FileTools.getStringFromFile(path.toFile(), StringTools.DEFAULT_ENCODING);
        Assertions.assertFalse(StringUtils.isEmpty(tei));
        Assertions.assertTrue(tei.contains("<note>"));
        String text = TEITools.getTeiFulltext(tei);
        Assertions.assertFalse(StringUtils.isEmpty(text));
        Assertions.assertFalse(text.contains("<note>"));
    }

}
