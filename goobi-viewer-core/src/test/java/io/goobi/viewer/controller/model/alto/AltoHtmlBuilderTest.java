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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.model.alto.AltoHtmlBuilder.AnnotationStyle;

class AltoHtmlBuilderTest {

    private String alto;

    @BeforeEach
    void setUp() throws Exception {
        File file = new File("src/test/resources/data/alto_layouttags.xml");
        Assertions.assertTrue(file.isFile());
        alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
    }

    private String build() throws Exception {
        // The label->markup mapping is supplied explicitly (as it would be from config_viewer.xml); the sample
        // fixture uses the gr_* labels. No NamedEntityEnricher here, to avoid its DataManager dependency.
        Map<String, String> wordTags = Map.of(
                "gr_unterstrichen", "alto-tag--underline",
                "gr_durchgestrichen", "alto-tag--strikethrough");
        Map<String, AnnotationStyle> annotationStyles = new LinkedHashMap<>();
        annotationStyles.put("gr_jacob_grimm", new AnnotationStyle("alto-annotation--jacob-grimm", "Jacob Grimm"));
        annotationStyles.put("gr_jacob_oder_wilhelm_grimm",
                new AnnotationStyle("alto-annotation--jacob-oder-wilhelm-grimm", "Jacob oder Wilhelm Grimm"));
        annotationStyles.put("gr_wilhelm_grimm", new AnnotationStyle("alto-annotation--wilhelm-grimm", "Wilhelm Grimm"));
        return new AltoHtmlBuilder("gr_printed", annotationStyles, new LayoutTagEnricher(wordTags))
                .build(alto, StringTools.DEFAULT_ENCODING);
    }

    /**
     * @see AltoHtmlBuilder#build(String, String)
     * @verifies separate handwritten margin annotations from the main text
     */
    @Test
    void build_shouldSeparateHandwrittenMarginAnnotationsFromTheMainText() throws Exception {
        String html = build();
        Assertions.assertTrue(html.contains("<div class=\"alto-fulltext\">"), html);
        Assertions.assertTrue(html.contains("alto-fulltext__margin--left"), html);
        // The "cf. Adelung" note is a left-margin annotation.
        int mainIndex = html.indexOf("alto-fulltext__main");
        int adelungIndex = html.indexOf("Adelung");
        Assertions.assertTrue(adelungIndex > -1, "margin note missing: " + html);
        Assertions.assertTrue(mainIndex > -1, html);
        Assertions.assertTrue(adelungIndex < mainIndex, "margin note should render before the main column");
        // Its block carries a handwriting-author annotation class.
        Assertions.assertTrue(html.contains("alto-annotation--jacob-oder-wilhelm-grimm"), html);
    }

    /**
     * The footnote callout "1.)" sits in the gutter at the column edge and must remain in the main flow.
     *
     * @see AltoHtmlBuilder#build(String, String)
     * @verifies keep footnote callouts in the main text
     */
    @Test
    void build_shouldKeepFootnoteCalloutsInTheMainText() throws Exception {
        String html = build();
        int mainIndex = html.indexOf("alto-fulltext__main");
        int calloutIndex = html.indexOf(">1.)</p>");
        Assertions.assertTrue(calloutIndex > -1, "footnote callout missing: " + html);
        Assertions.assertTrue(calloutIndex > mainIndex, "footnote callout should render inside the main column");
    }

    /**
     * @see AltoHtmlBuilder#build(String, String)
     * @verifies mark up underlined and struck through words
     */
    @Test
    void build_shouldMarkUpUnderlinedAndStruckThroughWords() throws Exception {
        String html = build();
        Assertions.assertTrue(html.contains("alto-tag--underline"), html);
    }

}
