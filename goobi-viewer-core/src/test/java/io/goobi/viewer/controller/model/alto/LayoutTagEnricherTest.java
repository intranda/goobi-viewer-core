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

    /** Explicit test mapping — the enricher has no built-in labels. */
    private final LayoutTagEnricher enricher = new LayoutTagEnricher(Map.of(
            "gr_unterstrichen", "underline",
            "gr_durchgestrichen", "strikethrough"), null);

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
     * @verifies derive the css class from the semantic type
     */
    @Test
    void enrich_shouldDeriveTheCssClassFromTheSemanticType() {
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
        LayoutTagEnricher strikeOnly = new LayoutTagEnricher(Map.of("gr_durchgestrichen", "strikethrough"), null);
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
     * @verifies append the configured style class after the derived class
     */
    @Test
    void enrich_shouldAppendTheConfiguredStyleClassAfterTheDerivedClass() {
        LayoutTagEnricher withStyleClass = new LayoutTagEnricher(Map.of("gr_unterstrichen", "underline"),
                Map.of("gr_unterstrichen", "my-vocab-class"));
        Assertions.assertEquals("<span class=\"alto-tag alto-tag--underline my-vocab-class\">buͦch</span>",
                withStyleClass.enrich("buͦch", word("s-143")));
    }

    /**
     * @see LayoutTagEnricher#enrich(String,LineElement)
     * @verifies apply style class only mappings without a type
     */
    @Test
    void enrich_shouldApplyStyleClassOnlyMappingsWithoutAType() {
        LayoutTagEnricher styleClassOnly = new LayoutTagEnricher(null, Map.of("gr_unterstrichen", "fulltext-underline"));
        Assertions.assertEquals("<span class=\"alto-tag fulltext-underline\">buͦch</span>",
                styleClassOnly.enrich("buͦch", word("s-143")));
    }

    /**
     * Legacy configurations map the derived class directly as styleClass; the span must not carry it twice.
     *
     * @see LayoutTagEnricher#enrich(String,LineElement)
     * @verifies not duplicate a class mapped by both type and style class
     */
    @Test
    void enrich_shouldNotDuplicateAClassMappedByBothTypeAndStyleClass() {
        LayoutTagEnricher redundant = new LayoutTagEnricher(Map.of("gr_unterstrichen", "underline"),
                Map.of("gr_unterstrichen", "alto-tag--underline"));
        Assertions.assertEquals("<span class=\"alto-tag alto-tag--underline\">buͦch</span>",
                redundant.enrich("buͦch", word("s-143")));
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
