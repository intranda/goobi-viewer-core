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
package io.goobi.viewer.model.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import io.goobi.viewer.api.rest.v1.ApiUrls;

/**
 * Unit tests for {@link AltoAnnotationBuilder} granularity modes.
 *
 * <p>These tests use a real ALTO fixture file and require no database or Solr connection.
 */
class AltoAnnotationBuilderTest {

    // Path relative to the maven project root (src/test/…)
    private static final Path ALTO_FIXTURE =
            Paths.get("src/test/resources/data/viewer/data/1/alto/PPN517154005/00000001.xml");

    // Known word count and line count in that fixture (verified by inspection):
    //   grep -oP 'CONTENT="[^"]*"' 00000001.xml | wc -l  → 67 words
    //   grep -oP '<TextLine'          00000001.xml | wc -l  → 11 lines
    private static final int EXPECTED_WORD_COUNT = 67;
    private static final int EXPECTED_LINE_COUNT = 11;

    private static final String PI = "PPN517154005";
    private static final int PAGE_NO = 1;

    private AltoAnnotationBuilder oaBuilder;
    private Canvas2 canvas;
    private AltoDocument alto;

    @BeforeEach
    void setUp() throws Exception {
        assertTrue(ALTO_FIXTURE.toFile().exists(), "ALTO test fixture missing: " + ALTO_FIXTURE);
        alto = AltoDocument.getDocumentFromFile(ALTO_FIXTURE.toFile());
        assertNotNull(alto.getFirstPage(), "AltoDocument must have at least one page");

        oaBuilder = new AltoAnnotationBuilder(new ApiUrls(""), "oa");

        // Minimal canvas — real dimensions not needed for annotation-count assertions
        canvas = new Canvas2(java.net.URI.create("http://example.com/canvas/1"));
        canvas.setWidth(3108);
        canvas.setHeight(4180);
    }

    /**
     * @verifies return one annotation per word when granularity is WORD
     * @see AltoAnnotationBuilder#createAnnotations
     */
    @Test
    void createAnnotations_wordGranularity_shouldReturnOneAnnotationPerWord() {
        List<AbstractAnnotation> annos = oaBuilder.createAnnotations(
                alto.getFirstPage(), PI, PAGE_NO, canvas,
                AltoAnnotationBuilder.Granularity.WORD, false);

        assertEquals(EXPECTED_WORD_COUNT, annos.size(),
                "WORD granularity must produce one annotation per ALTO String element");
    }

    /**
     * @verifies return one annotation per line when granularity is LINE
     * @see AltoAnnotationBuilder#createAnnotations
     */
    @Test
    void createAnnotations_lineGranularity_shouldReturnOneAnnotationPerLine() {
        List<AbstractAnnotation> annos = oaBuilder.createAnnotations(
                alto.getFirstPage(), PI, PAGE_NO, canvas,
                AltoAnnotationBuilder.Granularity.LINE, false);

        assertEquals(EXPECTED_LINE_COUNT, annos.size(),
                "LINE granularity must produce one annotation per ALTO TextLine element");
    }

    /**
     * @verifies produce more annotations for WORD than for LINE granularity
     * @see AltoAnnotationBuilder#createAnnotations
     */
    @Test
    void createAnnotations_wordGranularity_shouldProduceMoreAnnotationsThanLineGranularity() {
        List<AbstractAnnotation> wordAnnos = oaBuilder.createAnnotations(
                alto.getFirstPage(), PI, PAGE_NO, canvas,
                AltoAnnotationBuilder.Granularity.WORD, false);
        List<AbstractAnnotation> lineAnnos = oaBuilder.createAnnotations(
                alto.getFirstPage(), PI, PAGE_NO, canvas,
                AltoAnnotationBuilder.Granularity.LINE, false);

        assertTrue(wordAnnos.size() > lineAnnos.size(),
                "WORD granularity (" + wordAnnos.size() + ") must exceed LINE granularity ("
                        + lineAnnos.size() + ")");
    }
}
