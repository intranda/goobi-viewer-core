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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.model.alto.AltoLayoutAnnotationBuilder.BlockAnnotation;
import io.goobi.viewer.controller.model.alto.AltoLayoutAnnotationBuilder.LayoutAnnotations;
import io.goobi.viewer.controller.model.alto.AltoLayoutAnnotationBuilder.TaggedElement;

class AltoLayoutAnnotationBuilderTest {

    /** Explicit test mappings — there are no built-in defaults (LABELs are collection-specific). */
    private static final Map<String, AltoHtmlBuilder.AnnotationStyle> STYLES = Map.of(
            "gr_jacob_grimm", new AltoHtmlBuilder.AnnotationStyle("alto-annotation--jacob-grimm", "Jacob Grimm"),
            "gr_wilhelm_grimm", new AltoHtmlBuilder.AnnotationStyle("alto-annotation--wilhelm-grimm", "Wilhelm Grimm"),
            "gr_jacob_oder_wilhelm_grimm", new AltoHtmlBuilder.AnnotationStyle("alto-annotation--jow-grimm", "Jacob oder Wilhelm Grimm"));

    private static final Map<String, String> TYPES = Map.of(
            "gr_unterstrichen", "underline",
            "gr_durchgestrichen", "strikethrough");

    private static final Map<String, String> WORD_STYLES = Map.of(
            "gr_unterstrichen", "test-underline",
            "gr_durchgestrichen", "test-strike");

    private String alto;

    @BeforeEach
    void setUp() throws Exception {
        File file = new File("src/test/resources/data/alto_layouttags.xml");
        Assertions.assertTrue(file.isFile());
        alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
    }

    private LayoutAnnotations build() throws Exception {
        return new AltoLayoutAnnotationBuilder("gr_printed", STYLES, TYPES, WORD_STYLES).build(alto, StringTools.DEFAULT_ENCODING);
    }

    private static Optional<BlockAnnotation> block(LayoutAnnotations result, String id) {
        return result.getBlocks().stream().filter(b -> id.equals(b.getId())).findFirst();
    }

    /**
     * @see AltoLayoutAnnotationBuilder#build(String, String)
     * @verifies derive the print column from the printed content
     */
    @Test
    void build_shouldDeriveThePrintColumnFromThePrintedContent() throws Exception {
        LayoutAnnotations result = build();
        Assertions.assertNotNull(result.getPrintColumn());
        // The gr_printed main block starts around HPOS 686; its declared rect is degenerate (HPOS=0 WIDTH=0).
        Assertions.assertTrue(result.getPrintColumn().getX() > 650);
    }

    /**
     * @see AltoLayoutAnnotationBuilder#build(String, String)
     * @verifies classify handwritten side notes as left margin
     */
    @Test
    void build_shouldClassifyHandwrittenSideNotesAsLeftMargin() throws Exception {
        LayoutAnnotations result = build();
        for (String id : new String[] { "b-3", "b-4", "b-6", "b-7" }) {
            BlockAnnotation b = block(result, id).orElse(null);
            Assertions.assertNotNull(b, id + " should be listed");
            Assertions.assertEquals("left", b.getMargin(), id);
        }
        Assertions.assertEquals("gr_jacob_oder_wilhelm_grimm", block(result, "b-3").orElseThrow().getAuthor());
        Assertions.assertEquals("gr_jacob_grimm", block(result, "b-6").orElseThrow().getAuthor());
        Assertions.assertEquals("Jacob Grimm", block(result, "b-6").orElseThrow().getAuthorName());
        Assertions.assertTrue(block(result, "b-3").orElseThrow().getText().contains("Adelung"));
    }

    /**
     * @see AltoLayoutAnnotationBuilder#build(String, String)
     * @verifies use the configured annotation styles as author authority
     */
    @Test
    void build_shouldUseTheConfiguredAnnotationStylesAsAuthorAuthority() throws Exception {
        Map<String, AltoHtmlBuilder.AnnotationStyle> custom =
                Map.of("gr_wilhelm_grimm", new AltoHtmlBuilder.AnnotationStyle("custom-wilhelm", "Wilhelm (konfiguriert)"));
        LayoutAnnotations result = new AltoLayoutAnnotationBuilder(null, custom, null, null).build(alto, StringTools.DEFAULT_ENCODING);
        // Only the configured label counts as handwriting now: b-2 (Wilhelm) stays, Jacob blocks disappear.
        Assertions.assertTrue(block(result, "b-2").isPresent());
        Assertions.assertEquals("Wilhelm (konfiguriert)", block(result, "b-2").orElseThrow().getAuthorName());
        Assertions.assertTrue(block(result, "b-6").isEmpty());
    }

    /**
     * @see AltoLayoutAnnotationBuilder#build(String, String)
     * @verifies keep footnote callouts inline and skip printed blocks
     */
    @Test
    void build_shouldKeepFootnoteCalloutsInlineAndSkipPrintedBlocks() throws Exception {
        LayoutAnnotations result = build();
        BlockAnnotation callout = block(result, "b-5").orElse(null);
        Assertions.assertNotNull(callout);
        Assertions.assertEquals("gr_jacob_grimm", callout.getAuthor());
        Assertions.assertNull(callout.getMargin(), "gutter callout must stay inline");
        BlockAnnotation header = block(result, "b-2").orElse(null);
        Assertions.assertNotNull(header);
        Assertions.assertEquals("gr_wilhelm_grimm", header.getAuthor());
        Assertions.assertNull(header.getMargin());
        Assertions.assertTrue(block(result, "b-1").isEmpty(), "printed-only block must not be listed");
    }

    /**
     * @see AltoLayoutAnnotationBuilder#build(String, String)
     * @verifies list words carrying layout tags with their bounds
     */
    @Test
    void build_shouldListWordsCarryingLayoutTagsWithTheirBounds() throws Exception {
        LayoutAnnotations result = build();
        Assertions.assertEquals(14, result.getWords().size());
        TaggedElement word = result.getWords().stream().filter(w -> "s-143".equals(w.getId())).findFirst().orElse(null);
        Assertions.assertNotNull(word);
        Assertions.assertEquals(List.of("gr_unterstrichen"), word.getTags());
        Assertions.assertEquals(List.of("underline"), word.getTypes());
        Assertions.assertEquals(List.of("test-underline"), word.getStyleClasses());
        Assertions.assertEquals(1077, word.getRect().getX());
        Assertions.assertEquals(1994, word.getRect().getY());
        Assertions.assertEquals(84, word.getRect().getW());
        Assertions.assertEquals(66, word.getRect().getH());
        // s-151 carries both word-level tags and must map to both types.
        TaggedElement both = result.getWords().stream().filter(w -> "s-151".equals(w.getId())).findFirst().orElseThrow();
        Assertions.assertEquals(List.of("underline", "strikethrough"), both.getTypes());
        Assertions.assertEquals(List.of("test-underline", "test-strike"), both.getStyleClasses());
    }

    /**
     * @see AltoLayoutAnnotationBuilder#build(String, String)
     * @verifies list drawn lines with their tags
     */
    @Test
    void build_shouldListDrawnLinesWithTheirTags() throws Exception {
        LayoutAnnotations result = build();
        Assertions.assertEquals(10, result.getLines().size());
        TaggedElement line = result.getLines().stream().filter(l -> "g-1".equals(l.getId())).findFirst().orElse(null);
        Assertions.assertNotNull(line);
        Assertions.assertEquals(List.of("gr_unterstrichen"), line.getTags());
        Assertions.assertEquals(List.of("underline"), line.getTypes());
        Assertions.assertEquals(List.of("test-underline"), line.getStyleClasses());
        Assertions.assertEquals(867, line.getRect().getX());
        Assertions.assertEquals(2105, line.getRect().getY());
    }

    /**
     * A foreign customer vocabulary: everything the immersive client needs (styleClass, types) comes from the
     * configured mappings, no built-in label knowledge required.
     *
     * @see AltoLayoutAnnotationBuilder#build(String, String)
     * @verifies deliver semantic types and style classes for configured mappings
     */
    @Test
    void build_shouldDeliverSemanticTypesAndStyleClassesForConfiguredMappings() throws Exception {
        Map<String, AltoHtmlBuilder.AnnotationStyle> customStyles =
                Map.of("gr_wilhelm_grimm", new AltoHtmlBuilder.AnnotationStyle("kunde--hand-a", "Hand A"));
        Map<String, String> customTypes = Map.of("gr_unterstrichen", "mark");
        LayoutAnnotations result = new AltoLayoutAnnotationBuilder(null, customStyles, customTypes, null).build(alto, StringTools.DEFAULT_ENCODING);
        BlockAnnotation blockAnnotation = block(result, "b-2").orElseThrow();
        Assertions.assertEquals("kunde--hand-a", blockAnnotation.getStyleClass());
        Assertions.assertEquals("Hand A", blockAnnotation.getAuthorName());
        TaggedElement word = result.getWords().stream().filter(w -> "s-143".equals(w.getId())).findFirst().orElseThrow();
        Assertions.assertEquals(List.of("mark"), word.getTypes());
        // Labels without a configured type deliver no types at all (client renders nothing for them).
        TaggedElement both = result.getWords().stream().filter(w -> "s-151".equals(w.getId())).findFirst().orElseThrow();
        Assertions.assertEquals(List.of("mark"), both.getTypes());
    }

    /**
     * There are no built-in label mappings: without configuration the result is empty, even for a fully
     * layout-tagged document.
     *
     * @see AltoLayoutAnnotationBuilder#build(String, String)
     * @verifies return an empty result without configured mappings
     */
    @Test
    void build_shouldReturnAnEmptyResultWithoutConfiguredMappings() throws Exception {
        LayoutAnnotations result = new AltoLayoutAnnotationBuilder().build(alto, StringTools.DEFAULT_ENCODING);
        Assertions.assertTrue(result.getBlocks().isEmpty());
        // words/lines still list raw layout tags, but carry no renderable types
        Assertions.assertTrue(result.getWords().stream().allMatch(w -> w.getTypes().isEmpty()));
        Assertions.assertTrue(result.getLines().stream().allMatch(l -> l.getTypes().isEmpty()));
    }
}
