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

package io.goobi.viewer.controller.model.alto;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.LineElement;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;

class LayoutTagEnricherTest {

    private final LayoutTagEnricher enricher = new LayoutTagEnricher();

    private Map<String, LineElement> wordsById;

    @BeforeEach
    void setUp() throws Exception {
        File file = new File("src/test/resources/data/alto_layouttags.xml");
        Assertions.assertTrue(file.isFile());
        AltoDocument doc = AltoDocument.getDocumentFromFile(file);
        wordsById = new HashMap<>();
        for (LineElement word : doc.getFirstPage().getAllWordsAsList()) {
            wordsById.put(word.getId(), word);
        }
    }

    private LineElement word(String id) {
        LineElement word = wordsById.get(id);
        Assertions.assertNotNull(word, "fixture should contain word " + id);
        return word;
    }

    /**
     * @see LayoutTagEnricher#enrich(String,LineElement)
     * @verifies wrap underlined words in an underline span
     */
    @Test
    void enrich_shouldWrapUnderlinedWordsInAnUnderlineSpan() {
        Assertions.assertEquals("<span class=\"alto-tag alto-tag--underline\">buͦch</span>", enricher.enrich("buͦch", word("s-143")));
    }

    /**
     * The fixture has no strike-only word, so a strike-only mapping proves the class selection is per-label.
     *
     * @see LayoutTagEnricher#enrich(String,LineElement)
     * @verifies wrap struck through words in a strikethrough span
     */
    @Test
    void enrich_shouldWrapStruckThroughWordsInAStrikethroughSpan() {
        LayoutTagEnricher strikeOnly =
                new LayoutTagEnricher(Map.of(LayoutTagEnricher.LABEL_STRIKETHROUGH, LayoutTagEnricher.CLASS_STRIKETHROUGH));
        Assertions.assertEquals("<span class=\"alto-tag alto-tag--strikethrough\">Dageminde</span>", strikeOnly.enrich("Dageminde", word("s-151")));
    }

    /**
     * @see LayoutTagEnricher#enrich(String,LineElement)
     * @verifies combine both classes on a single span
     */
    @Test
    void enrich_shouldCombineBothClassesOnASingleSpan() {
        Assertions.assertEquals("<span class=\"alto-tag alto-tag--underline alto-tag--strikethrough\">Dageminde</span>",
                enricher.enrich("Dageminde", word("s-151")));
    }

    /**
     * @see LayoutTagEnricher#enrich(String,LineElement)
     * @verifies leave untagged words unchanged
     */
    @Test
    void enrich_shouldLeaveUntaggedWordsUnchanged() {
        Assertions.assertEquals("Diʒ", enricher.enrich("Diʒ", word("s-1")));
    }

}
