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
package io.goobi.viewer.model.iiif.search.parser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Range;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.intranda.digiverso.ocr.alto.model.structureclasses.Line;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;

/**
 * @author florian
 *
 */
class AltoSearchParserTest {

    Path altoFile = Paths.get("src/test/resources/data/sample_alto.xml");
    AltoDocument doc;
    AltoSearchParser parser = new AltoSearchParser();

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        doc = AltoDocument.getDocumentFromFile(altoFile.toFile());
        //        System.out.println(doc.getContent());
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    void testFindWordMatches() {
        String query = "diese* schönste*";
        String regex = AltoSearchParser.getQueryRegex(query);
        List<Word> words =
                doc.getFirstPage().getAllWordsAsList().stream().filter(l -> l instanceof Word).map(l -> (Word) l).collect(Collectors.toList());
        Assertions.assertFalse(words == null || words.isEmpty(), "No words found in " + altoFile);
        Assertions.assertNotNull(parser, "AltoSearchParser is not initialized");
        List<List<Word>> hits = parser.findWordMatches(words, regex);

        //        for (List<Word> hit : hits) {
        //            System.out.println("Found hit");
        //            System.out.println("\tmatch = " + hit.stream().map(Word::getSubsContent).collect(Collectors.joining(" ")));
        //            for (Word hitWord : hit) {
        //                System.out.println("\tWord " + hitWord.getId() + ": " + hitWord.getContent());
        //            }
        //        }

        Assertions.assertEquals(6, hits.size());
        Assertions.assertEquals(1, hits.get(0).size());
        Assertions.assertEquals(2, hits.get(1).size());
    }

    @Test
    void testFindLineMatches() {
        String query = "diese* schönste*";
        String regex = AltoSearchParser.getQueryRegex(query);
        List<Line> lines =
                doc.getFirstPage().getAllLinesAsList().stream().filter(l -> l instanceof Line).map(l -> (Line) l).collect(Collectors.toList());
        Map<Range<Integer>, List<Line>> hits = parser.findLineMatches(lines, regex);
        String text = parser.getText(lines);
        for (Range<Integer> position : hits.keySet()) {
            List<Line> containingLines = hits.get(position);
            String match = text.substring(position.getMinimum(), position.getMaximum() + 1);
            //            System.out.println("Found hit");
            //            System.out.println("\tmatch = " + match);
            //            for (Line line : containingLines) {
            //                System.out.println("\tLine " + line.getId() + ": " + line.getContent());
            //            }
        }

        Assertions.assertEquals(6, hits.size());
    }
}
