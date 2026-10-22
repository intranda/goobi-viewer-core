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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.Tag;
import de.intranda.digiverso.ocr.alto.utils.AltoCoords;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.model.ner.ElementReference;
import io.goobi.viewer.api.rest.model.ner.NERTag;
import io.goobi.viewer.api.rest.model.ner.TagCount;
import io.goobi.viewer.controller.model.alto.AltoHtmlBuilder.AnnotationStyle;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import jakarta.servlet.http.HttpServletRequest;

class ALTOToolsTest extends AbstractTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        AbstractTest.setUpClass();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @verifies return expected value for given input
     */
    @Test
    void rotate_shouldReturnExpectedValueForGivenInput() {
        Rectangle rect = new Rectangle(589, 502, 948 - 589, 654 - 502);
        Dimension canvasSize = new Dimension(1792, 2747);
        Rectangle expectedRotatedRect_270 = new Rectangle(502, 844, 654 - 502, 1203 - 844);
        rect = ALTOTools.rotate(rect, 270, canvasSize);
        assertEquals(expectedRotatedRect_270, rect);

    }

    /**
     * @verifies return non empty collection for given input
     */
    @Test
    void getWordCoords_shouldReturnNonEmptyCollectionForGivenInput() throws IOException {
        File testFile = new File("src/test/resources/data/sample_alto.xml");
        int rotation = 0;

        String altoString = FileUtils.readFileToString(testFile, StringTools.DEFAULT_ENCODING);
        List<String> coords = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, Collections.singleton("hinauf"), rotation);
        Assertions.assertFalse(coords.isEmpty());
        Assertions.assertEquals("1133,2549,1263,2584", coords.get(0));

        Set<String> terms = new LinkedHashSet<>();
        terms.add("Santa");
        terms.add("Monica.");
        terms.add("puh");
        coords = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, terms, rotation);
        Assertions.assertEquals(2, coords.size());
        Assertions.assertEquals("1032,2248,1136,2280", coords.get(0));

        terms = new LinkedHashSet<>();
        terms.add("Santa Monica.");
        coords = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, terms, rotation);
        Assertions.assertTrue(coords.size() == 2);
        Assertions.assertEquals("1032,2248,1136,2280", coords.get(0));

        terms = new LinkedHashSet<>();
        terms.add("Santa Monica. puh");
        coords = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, terms, rotation);
        Assertions.assertTrue(coords.size() == 0);
    }

    /**
     * @verifies match hyphenated words
     */
    @Test
    void getWordCoords_shouldMatchHyphenatedWords() throws Exception {
        File file = new File("src/test/resources/data/viewer/alto/0230L.xml");
        Assertions.assertTrue(file.isFile());
        String altoString = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        List<String> result = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, Collections.singleton("wappen"), 0);
        Assertions.assertFalse(result.isEmpty());
    }

    /**
     * @verifies match phrases
     */
    @Test
    void getWordCoords_shouldMatchPhrases() throws Exception {
        File file = new File("src/test/resources/data/sample_alto.xml");
        Assertions.assertTrue(file.isFile());
        String altoString = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        {
            List<String> result =
                    ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, Collections.singleton("ein neues doppelpyramidenrätsel"), 0);
            Assertions.assertEquals(3, result.size());
            Assertions.assertEquals("843,478,904,509", result.get(0));
            Assertions.assertEquals("923,489,1021,509", result.get(1));
            Assertions.assertEquals("1039,477,1482,516", result.get(2));
        }
        {
            List<String> result =
                    ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING,
                            Collections.singletonMap("ein neues dreifachpyramidenrätsel", null).keySet(), 0);
            Assertions.assertEquals(0, result.size());
        }
    }

    /**
     * @verifies match diacritics via base letter
     */
    @Test
    void getWordCoords_shouldMatchDiacriticsViaBaseLetter() throws Exception {
        File file = new File("src/test/resources/data/sample_alto.xml");
        Assertions.assertTrue(file.isFile());
        String altoString = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);

        List<String> result = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, Collections.singleton("doppelpyramidenratsel"), 0);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("1039,477,1482,516", result.get(0));
    }

    /**
     * @see ALTOTools#getNERTags(String,String,NERTag.Type)
     * @verifies add identifier to tag count
     */
    @Test
    void getNERTags_shouldAddIdentifierToTagCount() throws Exception {
        File file = new File("src/test/resources/data/viewer/data/1/alto/PPN648829383/00000014.xml");
        Assertions.assertTrue(file.isFile());
        String text = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        Assertions.assertNotNull(text);
        List<TagCount> tags = ALTOTools.getNERTags(text, StringTools.DEFAULT_ENCODING, NERTag.Type.LOCATION);
        Assertions.assertFalse(tags.isEmpty());
        // Tags are out of order
        for (TagCount tag : tags) {
            if ("4043271-3".equals(tag.getIdentifier())) {
                return;
            }
        }
        Assertions.fail("Identifier not found");
    }

    /**
     * Regression guard: filtering NER tags by a concrete type must not throw when the ALTO
     * document contains a {@code NamedEntityTag} without a {@code TYPE} attribute (i.e.
     * {@code tag.getType()} is null). Previously {@code NERTag.Type.matches(null)} threw a
     * NullPointerException, which surfaced as an HTTP 500 in the NER faceting widget as soon
     * as any entity type filter was applied (the unfiltered case short-circuited via
     * {@code type == null} and never called {@code matches}).
     *
     * @see ALTOTools#getNERTags(String, String, NERTag.Type)
     * @verifies not fail on named entity tag without type when filtering by type
     */
    @Test
    void getNERTags_shouldNotFailOnTypelessTagWhenFilteringByType() throws Exception {
        File file = new File("src/test/resources/data/alto_ner_typeless.xml");
        Assertions.assertTrue(file.isFile());
        String text = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        Assertions.assertNotNull(text);
        List<TagCount> tags = ALTOTools.getNERTags(text, StringTools.DEFAULT_ENCODING, NERTag.Type.PERSON);
        // Only the typed PERSON tag is returned; the type-less tag is skipped, not an exception.
        Assertions.assertEquals(1, tags.size(), tags.toString());
        Assertions.assertEquals("TEUBNER", tags.get(0).getValue());
    }

    /**
     * @verifies return non empty text from valid alto file
     */
    @Test
    void alto2Txt_shouldReturnNonEmptyTextFromValidAltoFile() throws Exception {
        File file = new File("src/test/resources/data/viewer/alto/LIWZ_1877_01_05_001.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        Assertions.assertNotNull(alto);
        String text = ALTOTools.alto2Txt(alto, StringTools.DEFAULT_ENCODING, false);
        Assertions.assertNotNull(text);
        Assertions.assertTrue(text.length() > 100);
    }

    /**
     * @verifies join hyphenated words split across line breaks into complete words
     */
    @Test
    void alto2Txt_shouldJoinHyphenatedWordsSplitAcrossLineBreaksIntoCompleteWords() throws Exception {
        File file = new File("src/test/resources/data/viewer/alto/0230L.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        Assertions.assertNotNull(alto);
        String text = ALTOTools.alto2Txt(alto, StringTools.DEFAULT_ENCODING, true);
        Assertions.assertNotNull(text);
        Assertions.assertTrue(text.contains("Wappen"));
    }

    /**
     * @verifies return non-empty text from valid ALTO file
     */
    @Test
    void getFulltext_shouldReturnNonEmptyTextFromValidALTOFile() throws Exception {
        File file = new File("src/test/resources/data/viewer/data/1/alto/00000010.xml");
        Assertions.assertTrue(file.isFile());
        String text = ALTOTools.getFulltext(file.toPath(), StringTools.DEFAULT_ENCODING);
        Assertions.assertNotNull(text);
        Assertions.assertTrue(text.length() > 100);
    }

    /**
     * @see ALTOTools#getFulltextHtml(String,String,Locale)
     * @verifies include authority data URI attributes in output
     */
    @Test
    void getFulltextHtml_shouldIncludeAuthorityDataURIAttributesInOutput() throws Exception {
        File file = new File("src/test/resources/data/viewer/data/1/alto/PPN648829383/00000014.xml");
        Assertions.assertTrue(file.isFile());
        // Named-entity button markup lives on the HTML display path (getFulltextHtml), not the plain-text path.
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        String text = ALTOTools.getFulltextHtml(alto, StringTools.DEFAULT_ENCODING, null);
        Assertions.assertNotNull(text);
        Assertions.assertTrue(text.contains("data-entity-authority-data-uri="));
    }

    /**
     * The plain-text extraction path must not carry any HTML markup (no NER buttons, no layout spans).
     *
     * @see ALTOTools#getFulltext(String,String,boolean)
     * @verifies return plain text without markup
     */
    @Test
    void getFulltext_shouldReturnPlainTextWithoutMarkup() throws Exception {
        File file = new File("src/test/resources/data/viewer/data/1/alto/PPN648829383/00000014.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        String text = ALTOTools.getFulltext(alto, StringTools.DEFAULT_ENCODING, false);
        Assertions.assertNotNull(text);
        Assertions.assertFalse(text.contains("<button"), text);
        Assertions.assertFalse(text.contains("view-fulltext__entity-action-button"), text);
        Assertions.assertFalse(text.contains("<span"), text);
    }

    /**
     * Regression guard for the dead MAX_ENRICHMENTS=1 limit in NamedEntityEnricher that
     * previously emitted only the first named-entity button per page-render and silently
     * downgraded every subsequent reference to plain text. The fixture references multiple
     * NER tags on distinct words, so the enriched output must contain at least one button
     * per referenced word.
     *
     * @see NamedEntityEnricher#enrich(String, de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.LineElement)
     * @verifies enrich every word that references a named entity tag
     */
    @Test
    void enrich_shouldEnrichEveryWordThatReferencesANamedEntityTag() throws Exception {
        File file = new File("src/test/resources/data/viewer/data/1/alto/PPN648829383/00000014.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        String text = ALTOTools.getFulltextHtml(alto, StringTools.DEFAULT_ENCODING, null);
        Assertions.assertNotNull(text);
        int buttonCount = StringUtils.countMatches(text, "view-fulltext__entity-action-button");
        Assertions.assertTrue(buttonCount >= 2,
                "Expected at least 2 enriched entity buttons (regression for MAX_ENRICHMENTS=1 limit), found " + buttonCount);
    }

    /**
     * Regression guard for the label-duplication bug: a named entity that spans multiple ALTO
     * String words (e.g. the person "B. G. TEUBNER" split into "B.", "G.", "TEUBNER", all with
     * TAGREFS="Tag0" and a shared LABEL) must render one inline button per word using each
     * word's own content, not one button per word repeating the full entity label.
     *
     * @see NamedEntityEnricher#enrich(String, de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.LineElement)
     * @verifies use the word content as button text
     */
    @Test
    void enrich_shouldUseTheWordContentAsButtonText() throws Exception {
        File file = new File("src/test/resources/data/alto_ner_multiword.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        String text = ALTOTools.getFulltextHtml(alto, StringTools.DEFAULT_ENCODING, null);
        Assertions.assertNotNull(text);

        // Each word of the multi-word entity becomes its own button showing its own content.
        Assertions.assertTrue(text.contains(">B.</button>"), text);
        Assertions.assertTrue(text.contains(">G.</button>"), text);
        Assertions.assertTrue(text.contains(">TEUBNER</button>"), text);
        // The full entity label must not be repeated as button text.
        Assertions.assertFalse(text.contains(">B. G. TEUBNER</button>"), text);

        // Three words of the person entity plus the single-word location entity.
        int buttonCount = StringUtils.countMatches(text, "view-fulltext__entity-action-button");
        Assertions.assertEquals(4, buttonCount, "Expected 4 word-level entity buttons, found " + buttonCount + ": " + text);
    }

    /**
     * Regression guard: words referenced only by layout tags (e.g. gr_unterstrichen) must not be turned into
     * named-entity buttons. They receive layout markup (a span) instead.
     *
     * @see NamedEntityEnricher#enrich(String, de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.LineElement)
     * @verifies not enrich words referenced only by layout tags
     */
    @Test
    void enrich_shouldNotEnrichWordsReferencedOnlyByLayoutTags() throws Exception {
        File file = new File("src/test/resources/data/alto_layouttags.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        String text = ALTOTools.getFulltextHtml(alto, StringTools.DEFAULT_ENCODING, null);
        Assertions.assertNotNull(text);
        // The fixture carries only layout tags (no NamedEntityTag), so no entity buttons must be produced ...
        Assertions.assertFalse(text.contains("view-fulltext__entity-action-button"), text);
        // ... but the underlined words must still receive layout markup (style class comes from the test config's
        // altoLayoutTags mapping: gr_unterstrichen -> test-underline).
        Assertions.assertTrue(text.contains("test-underline"), text);
    }

    /**
     * @see ALTOTools#getLayoutTags(String,String,String,Collection)
     * @verifies return layout tags with references
     * @verifies filter by label
     */
    @Test
    void getLayoutTags_shouldReturnLayoutTagsWithReferencesAndFilterByLabel() throws Exception {
        File file = new File("src/test/resources/data/alto_layouttags.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);

        List<TagCount> all = ALTOTools.getLayoutTags(alto, StringTools.DEFAULT_ENCODING, null, Collections.emptySet());
        Assertions.assertNotNull(all);
        Set<String> values = new LinkedHashSet<>();
        for (TagCount tc : all) {
            values.add(tc.getValue());
            // each label aggregates its occurrences as references with coordinates + content
            Assertions.assertFalse(tc.getReferences().isEmpty());
            Assertions.assertEquals(tc.getReferences().size(), tc.getCounter().intValue());
        }
        Assertions.assertTrue(values.contains("gr_jacob_grimm"), values.toString());
        Assertions.assertTrue(values.contains("gr_unterstrichen"), values.toString());
        Assertions.assertTrue(values.contains("gr_wilhelm_grimm"), values.toString());
        // one distinct TagCount per label (occurrences merged), not one per occurrence
        Assertions.assertEquals(values.size(), all.size());

        // Label filter returns only the requested label
        List<TagCount> underlinedOnly = ALTOTools.getLayoutTags(alto, StringTools.DEFAULT_ENCODING, "gr_unterstrichen", Collections.emptySet());
        Assertions.assertEquals(1, underlinedOnly.size());
        Assertions.assertEquals("gr_unterstrichen", underlinedOnly.get(0).getValue());
        Assertions.assertTrue(underlinedOnly.get(0).getCounter() > 1);
    }

    /**
     * A word tag marks a drawn line: the ALTO carries the same LABEL on the mark itself
     * ({@code <GraphicalElement>}) and on every word the mark runs across, so only the marks are occurrences.
     *
     * @see ALTOTools#getLayoutTags(String,String,String,Collection)
     * @verifies keep only graphical references for word tags
     * @verifies keep references of word tags without graphical reference
     * @verifies not filter labels that are no word tags
     */
    @Test
    void getLayoutTags_shouldKeepOnlyMarksForWordTags() throws Exception {
        File file = new File("src/test/resources/data/alto_layouttags.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        Set<String> wordTagLabels = Set.of("gr_unterstrichen", "gr_durchgestrichen");

        // The fixture attaches gr_unterstrichen to 10 GraphicalElements and to the 14 words they run across.
        List<TagCount> unfiltered = ALTOTools.getLayoutTags(alto, StringTools.DEFAULT_ENCODING, "gr_unterstrichen", Collections.emptySet());
        Assertions.assertEquals(24, unfiltered.get(0).getCounter().intValue());

        List<TagCount> underlines = ALTOTools.getLayoutTags(alto, StringTools.DEFAULT_ENCODING, "gr_unterstrichen", wordTagLabels);
        Assertions.assertEquals(1, underlines.size());
        Assertions.assertEquals(10, underlines.get(0).getCounter().intValue());
        for (ElementReference reference : underlines.get(0).getReferences()) {
            Assertions.assertEquals("GraphicalElement", reference.getElementType(), reference.getId());
        }

        // gr_durchgestrichen is a word tag too, but the fixture records no mark for it: its word reference remains.
        List<TagCount> struck = ALTOTools.getLayoutTags(alto, StringTools.DEFAULT_ENCODING, "gr_durchgestrichen", wordTagLabels);
        Assertions.assertEquals(1, struck.size());
        Assertions.assertEquals(1, struck.get(0).getCounter().intValue());
        Assertions.assertEquals("s-151", struck.get(0).getReferences().get(0).getId());
        Assertions.assertEquals("String", struck.get(0).getReferences().get(0).getElementType());

        // Annotation tags are untouched by the filter.
        List<TagCount> jacob = ALTOTools.getLayoutTags(alto, StringTools.DEFAULT_ENCODING, "gr_jacob_grimm", wordTagLabels);
        Assertions.assertEquals(1, jacob.size());
        Assertions.assertEquals(7, jacob.get(0).getCounter().intValue());
    }

    /**
     * @verifies find fuzzy terms
     */
    @Test
    void getMatchALTOWord_shouldFindFuzzyTerms() {
        String[] searchTerms = new String[] { "Steigbügel~1", "Halter~1" };
        {
            Word word = new Word("Steigbugle", new AltoCoords(10, 10, 12, 12));
            assertEquals(1, ALTOTools.getMatchALTOWord(word, searchTerms));
        }
        {
            Word word = new Word("Ich war beim Steigbügel Häälter", new AltoCoords(10, 10, 12, 12));
            assertEquals(2, ALTOTools.getMatchALTOWord(word, searchTerms));
        }
    }

    /**
     * @see ALTOTools#getFulltext(String,String,boolean)
     * @verifies return non-empty text from valid ALTO string
     */
    @Test
    void getFulltext_shouldReturnNonEmptyTextFromValidALTOString() throws Exception {
        // Read sample ALTO XML and pass it as a string to the 3-arg getFulltext method
        File file = new File("src/test/resources/data/sample_alto.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        Assertions.assertNotNull(alto);
        String text = ALTOTools.getFulltext(alto, StringTools.DEFAULT_ENCODING, false);
        Assertions.assertNotNull(text);
        Assertions.assertFalse(text.isEmpty());
    }

    /**
     * @verifies return collection with 3 elements
     */
    @Test
    void getWordCoords_shouldReturnCollectionWith3Elements() throws ViewerConfigurationException, IOException {

        File testFile = new File("src/test/resources/data/sample_alto.xml");
        String altoString = FileUtils.readFileToString(testFile, StringTools.DEFAULT_ENCODING);

        Set<String> words = Set.of("Büschel Früchten");
        List<String> hits = ALTOTools.getWordCoords(altoString, "utf-8", words, 2, 0);
        assertEquals(3, hits.size());
        List<String> hits2 = ALTOTools.getWordCoords(altoString, "utf-8", words, 0, 0);
        assertEquals(0, hits2.size());
    }

    /**
     * @verifies scale coords when actual image size differs from ALTO page size
     */
    @Test
    void getWordCoords_shouldScaleCoordsToActualImageSize() throws IOException {
        File testFile = new File("src/test/resources/data/sample_alto.xml");
        // ALTO page size: WIDTH=2172 HEIGHT=2960
        String altoString = FileUtils.readFileToString(testFile, StringTools.DEFAULT_ENCODING);
        // Actual image is half the ALTO page size → all coords should be halved
        Dimension imageSize = new Dimension(1086, 1480);
        List<String> coords = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING,
                Collections.singleton("hinauf"), 0, 0, imageSize);
        Assertions.assertFalse(coords.isEmpty());
        // Original: 1133,2549,1263,2584 → scaled ×0.5: 566,1274,631,1292
        Assertions.assertEquals("566,1274,631,1292", coords.get(0));
    }

    /**
     * @see ALTOTools#translateAnnotationTagNames(Map,Locale)
     * @verifies translate names that match a message key
     */
    @Test
    void translateAnnotationTagNames_shouldTranslateNamesThatMatchAMessageKey() {
        Map<String, AnnotationStyle> tags = Map.of("hand_x", new AnnotationStyle("alto-annotation--x", "MD_AUTHOR"));
        Map<String, AnnotationStyle> ret = ALTOTools.translateAnnotationTagNames(tags, Locale.GERMAN);
        Assertions.assertEquals("Autor", ret.get("hand_x").name());
        Assertions.assertEquals("alto-annotation--x", ret.get("hand_x").styleClass());
    }

    /**
     * @see ALTOTools#translateAnnotationTagNames(Map,Locale)
     * @verifies keep literal names without a message key
     */
    @Test
    void translateAnnotationTagNames_shouldKeepLiteralNamesWithoutAMessageKey() {
        Map<String, AnnotationStyle> tags = Map.of(
                "hand_a", new AnnotationStyle("alto-annotation--a", "Andere Handschrift"),
                "hand_b", new AnnotationStyle("alto-annotation--b", "Einweisungszeichen"));
        Map<String, AnnotationStyle> ret = ALTOTools.translateAnnotationTagNames(tags, Locale.GERMAN);
        Assertions.assertEquals("Andere Handschrift", ret.get("hand_a").name());
        Assertions.assertEquals("Einweisungszeichen", ret.get("hand_b").name());
    }

    /**
     * @see ALTOTools#translateAnnotationTagNames(Map,Locale)
     * @verifies pass null names through unchanged
     */
    @Test
    void translateAnnotationTagNames_shouldPassNullNamesThroughUnchanged() {
        Map<String, AnnotationStyle> tags = Map.of("hand_x", new AnnotationStyle("alto-annotation--x", null));
        Map<String, AnnotationStyle> ret = ALTOTools.translateAnnotationTagNames(tags, Locale.GERMAN);
        Assertions.assertNull(ret.get("hand_x").name());
    }

    /**
     * @see ALTOTools#getLayoutTags(String,String,String,Collection)
     * @verifies ignore layout tags without label
     */
    @Test
    void getLayoutTags_shouldIgnoreLayoutTagsWithoutLabel() throws Exception {
        File file = new File("src/test/resources/data/alto_layouttags.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING)
                .replace("<LayoutTag ID=\"RT-2\" LABEL=\"gr_unterstrichen\"/>", "<LayoutTag ID=\"RT-2\"/>");
        Assertions.assertTrue(alto.contains("<LayoutTag ID=\"RT-2\"/>"));

        List<TagCount> all = ALTOTools.getLayoutTags(alto, StringTools.DEFAULT_ENCODING, null, Collections.emptySet());
        for (TagCount tc : all) {
            Assertions.assertTrue(StringUtils.isNotEmpty(tc.getValue()), "tag without label must be skipped: " + all);
        }
        // RT-10 and RT-11 still carry the label
        Assertions.assertTrue(all.stream().anyMatch(tc -> "gr_unterstrichen".equals(tc.getValue())), all.toString());
    }

    /**
     * @see ALTOTools#getFulltextHtml(String,String,Locale)
     * @verifies translate annotation names for the given locale
     */
    @Test
    void getFulltextHtml_shouldTranslateAnnotationNamesForTheGivenLocale() throws Exception {
        File file = new File("src/test/resources/data/alto_layouttags.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        // config_viewer.test.xml maps gr_jacob_oder_wilhelm_grimm to the message key ADDRESS
        String german = ALTOTools.getFulltextHtml(alto, StringTools.DEFAULT_ENCODING, Locale.GERMAN);
        Assertions.assertTrue(german.contains("title=\"Adresse\""), german);
        String english = ALTOTools.getFulltextHtml(alto, StringTools.DEFAULT_ENCODING, Locale.ENGLISH);
        Assertions.assertTrue(english.contains("title=\"Address\""), english);
    }

    /**
     * @see ALTOTools#getFulltextHtml(String,String,Locale)
     * @verifies escape entity attribute values
     */
    @Test
    void getFulltextHtml_shouldEscapeEntityAttributeValues() throws Exception {
        File file = new File("src/test/resources/data/alto_ner_multiword.xml");
        Assertions.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING)
                .replace("URI=\"https://d-nb.info/gnd/118756559\"", "URI=\"https://d-nb.info/gnd/118756559&quot;x\"");
        Assertions.assertTrue(alto.contains("&quot;x"));
        String text = ALTOTools.getFulltextHtml(alto, StringTools.DEFAULT_ENCODING, null);
        Assertions.assertNotNull(text);
        Assertions.assertTrue(text.contains("118756559&quot;x"), text);
        Assertions.assertFalse(text.contains("118756559\"x"), text);
    }
}
