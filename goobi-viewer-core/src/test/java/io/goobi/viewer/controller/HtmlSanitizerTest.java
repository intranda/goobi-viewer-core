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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies return null when input is null
     */
    @Test
    void cleanRichText_shouldReturnNullWhenInputIsNull() {
        assertNull(HtmlSanitizer.cleanRichText(null));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies return empty string when input is empty
     */
    @Test
    void cleanRichText_shouldReturnEmptyStringWhenInputIsEmpty() {
        assertEquals("", HtmlSanitizer.cleanRichText(""));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies remove script tags
     */
    @Test
    void cleanRichText_shouldRemoveScriptTags() {
        String result = HtmlSanitizer.cleanRichText("<p>hello</p><script>alert(1)</script>");
        assertFalse(result.toLowerCase().contains("<script"));
        assertTrue(result.contains("hello"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies remove img onerror attribute
     */
    @Test
    void cleanRichText_shouldRemoveImgOnerrorAttribute() {
        String result = HtmlSanitizer.cleanRichText("<img src=\"x\" onerror=\"alert(1)\">");
        assertFalse(result.toLowerCase().contains("onerror"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies remove svg onload attribute
     */
    @Test
    void cleanRichText_shouldRemoveSvgOnloadAttribute() {
        String result = HtmlSanitizer.cleanRichText("<svg onload=\"alert(1)\"></svg>");
        assertFalse(result.toLowerCase().contains("onload"));
        assertFalse(result.toLowerCase().contains("<svg"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies remove javascript URI from anchor href
     */
    @Test
    void cleanRichText_shouldRemoveJavascriptUriFromAnchorHref() {
        String result = HtmlSanitizer.cleanRichText("<a href=\"javascript:alert(1)\">click</a>");
        assertFalse(result.toLowerCase().contains("javascript:"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies remove javascript URI with leading whitespace
     */
    @Test
    void cleanRichText_shouldRemoveJavascriptUriWithLeadingWhitespace() {
        String result = HtmlSanitizer.cleanRichText("<a href=\"\t javascript:alert(1)\">x</a>");
        assertFalse(result.toLowerCase().contains("javascript:"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies remove iframe tags
     */
    @Test
    void cleanRichText_shouldRemoveIframeTags() {
        String result = HtmlSanitizer.cleanRichText("<iframe src=\"https://evil\"></iframe>");
        assertFalse(result.toLowerCase().contains("<iframe"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies remove details ontoggle attribute
     */
    @Test
    void cleanRichText_shouldRemoveDetailsOntoggleAttribute() {
        String result = HtmlSanitizer.cleanRichText("<details ontoggle=\"alert(1)\" open>x</details>");
        assertFalse(result.toLowerCase().contains("ontoggle"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies remove formaction bypass on button inside form
     */
    @Test
    void cleanRichText_shouldRemoveFormactionBypassOnButtonInsideForm() {
        String result = HtmlSanitizer.cleanRichText(
                "<form action=\"x\"><button formaction=\"javascript:alert(1)\">click</button></form>");
        assertFalse(result.toLowerCase().contains("formaction"));
        assertFalse(result.toLowerCase().contains("<form"));
        assertFalse(result.toLowerCase().contains("<button"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies remove base href javascript URI
     */
    @Test
    void cleanRichText_shouldRemoveBaseHrefJavascriptUri() {
        String result = HtmlSanitizer.cleanRichText("<base href=\"javascript:alert(1)\">");
        assertFalse(result.toLowerCase().contains("<base"));
        assertFalse(result.toLowerCase().contains("javascript:"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve allowlisted structural tags
     */
    @Test
    void cleanRichText_shouldPreserveAllowlistedStructuralTags() {
        String result = HtmlSanitizer.cleanRichText("<p>foo <strong>bar</strong> <em>baz</em></p>");
        assertTrue(result.contains("<p>"));
        assertTrue(result.contains("<strong>"));
        assertTrue(result.contains("<em>"));
        assertTrue(result.contains("foo"));
        assertTrue(result.contains("bar"));
        assertTrue(result.contains("baz"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve figure and figcaption tags
     */
    @Test
    void cleanRichText_shouldPreserveFigureAndFigcaptionTags() {
        String result = HtmlSanitizer.cleanRichText(
                "<figure><img src=\"http://x/y.png\" alt=\"x\"><figcaption>caption</figcaption></figure>");
        assertTrue(result.contains("<figure>"));
        assertTrue(result.contains("<figcaption>"));
        assertTrue(result.contains("caption"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve http and https anchor href
     */
    @Test
    void cleanRichText_shouldPreserveHttpAndHttpsAnchorHref() {
        String httpsResult = HtmlSanitizer.cleanRichText("<a href=\"https://example.com\">x</a>");
        assertTrue(httpsResult.contains("href=\"https://example.com\""));
        String httpResult = HtmlSanitizer.cleanRichText("<a href=\"http://example.com\">x</a>");
        assertTrue(httpResult.contains("href=\"http://example.com\""));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve target and rel on anchor
     */
    @Test
    void cleanRichText_shouldPreserveTargetAndRelOnAnchor() {
        String result = HtmlSanitizer.cleanRichText(
                "<a href=\"https://example.com\" target=\"_blank\" rel=\"noopener\">x</a>");
        assertTrue(result.contains("target=\"_blank\""));
        assertTrue(result.contains("rel=\"noopener\""));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve table markup
     */
    @Test
    void cleanRichText_shouldPreserveTableMarkup() {
        String result = HtmlSanitizer.cleanRichText("<table><tr><td>x</td></tr></table>");
        assertTrue(result.contains("<table>"));
        assertTrue(result.contains("<tr>"));
        assertTrue(result.contains("<td>"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies remove data URI from img src
     */
    @Test
    void cleanRichText_shouldRemoveDataUriFromImgSrc() {
        String result = HtmlSanitizer.cleanRichText(
                "<img src=\"data:image/svg+xml;base64,PHN2Zy8+\">");
        assertFalse(result.toLowerCase().contains("data:"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve newlines without collapsing whitespace
     */
    @Test
    void cleanRichText_shouldPreserveNewlinesWithoutCollapsingWhitespace() {
        String result = HtmlSanitizer.cleanRichText("<p>line1\nline2</p>");
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains("\n"));
    }

    /**
     * @see HtmlSanitizer#isCleanRichText(String)
     * @verifies return true for null input
     */
    @Test
    void isCleanRichText_shouldReturnTrueForNullInput() {
        assertTrue(HtmlSanitizer.isCleanRichText(null));
    }

    /**
     * @see HtmlSanitizer#isCleanRichText(String)
     * @verifies return true for empty input
     */
    @Test
    void isCleanRichText_shouldReturnTrueForEmptyInput() {
        assertTrue(HtmlSanitizer.isCleanRichText(""));
    }

    /**
     * @see HtmlSanitizer#isCleanRichText(String)
     * @verifies return true for legitimate markup
     */
    @Test
    void isCleanRichText_shouldReturnTrueForLegitimateMarkup() {
        assertTrue(HtmlSanitizer.isCleanRichText("<p>hello <strong>world</strong></p>"));
    }

    /**
     * @see HtmlSanitizer#isCleanRichText(String)
     * @verifies return false for script injection
     */
    @Test
    void isCleanRichText_shouldReturnFalseForScriptInjection() {
        assertFalse(HtmlSanitizer.isCleanRichText("<p>hi</p><script>alert(1)</script>"));
    }

    /**
     * @see HtmlSanitizer#cleanComment(String)
     * @verifies return null when input is null
     */
    @Test
    void cleanComment_shouldReturnNullWhenInputIsNull() {
        assertNull(HtmlSanitizer.cleanComment(null));
    }

    /**
     * @see HtmlSanitizer#cleanComment(String)
     * @verifies remove script tags
     */
    @Test
    void cleanComment_shouldRemoveScriptTags() {
        String result = HtmlSanitizer.cleanComment("hello<script>alert(1)</script>");
        assertFalse(result.toLowerCase().contains("<script"));
        assertTrue(result.contains("hello"));
    }

    /**
     * @see HtmlSanitizer#cleanComment(String)
     * @verifies remove img tags
     */
    @Test
    void cleanComment_shouldRemoveImgTags() {
        String result = HtmlSanitizer.cleanComment("<img src=\"http://x/y.png\" alt=\"x\">");
        assertFalse(result.toLowerCase().contains("<img"));
    }

    /**
     * @see HtmlSanitizer#cleanComment(String)
     * @verifies remove table tags
     */
    @Test
    void cleanComment_shouldRemoveTableTags() {
        String result = HtmlSanitizer.cleanComment("<table><tr><td>x</td></tr></table>");
        assertFalse(result.toLowerCase().contains("<table"));
        assertFalse(result.toLowerCase().contains("<td"));
    }

    /**
     * @see HtmlSanitizer#cleanComment(String)
     * @verifies preserve basic inline formatting tags
     */
    @Test
    void cleanComment_shouldPreserveBasicInlineFormattingTags() {
        String result = HtmlSanitizer.cleanComment(
                "<p>foo <strong>bar</strong> <em>baz</em></p>");
        assertTrue(result.contains("<strong>"));
        assertTrue(result.contains("<em>"));
        assertTrue(result.contains("foo"));
    }

    /**
     * @see HtmlSanitizer#cleanComment(String)
     * @verifies preserve safe anchor tags
     */
    @Test
    void cleanComment_shouldPreserveSafeAnchorTags() {
        String result = HtmlSanitizer.cleanComment("<a href=\"https://example.com\">link</a>");
        assertTrue(result.contains("href=\"https://example.com\""));
    }

    /**
     * @see HtmlSanitizer#cleanComment(String)
     * @verifies remove javascript URI from anchor href
     */
    @Test
    void cleanComment_shouldRemoveJavascriptUriFromAnchorHref() {
        String result = HtmlSanitizer.cleanComment("<a href=\"javascript:alert(1)\">x</a>");
        assertFalse(result.toLowerCase().contains("javascript:"));
    }

    /**
     * @see HtmlSanitizer#cleanComment(String)
     * @verifies preserve plain text line breaks as br tags
     */
    @Test
    void cleanComment_shouldPreservePlainTextLineBreaksAsBrTags() {
        String result = HtmlSanitizer.cleanComment("line1\nline2\nline3");
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains("line3"));
        // Two newlines in input → two <br> in output
        int brCount = result.toLowerCase().split("<br").length - 1;
        assertEquals(2, brCount);
    }

    /**
     * @see HtmlSanitizer#cleanComment(String)
     * @verifies normalize windows line endings to br tags without stray carriage return
     */
    @Test
    void cleanComment_shouldNormalizeWindowsLineEndingsToBrTagsWithoutStrayCarriageReturn() {
        String result = HtmlSanitizer.cleanComment("line1\r\nline2");
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.toLowerCase().contains("<br"));
        assertFalse(result.contains("\r"));
    }

    /**
     * @see HtmlSanitizer#isCleanComment(String)
     * @verifies return true for null input
     */
    @Test
    void isCleanComment_shouldReturnTrueForNullInput() {
        assertTrue(HtmlSanitizer.isCleanComment(null));
    }

    /**
     * @see HtmlSanitizer#isCleanComment(String)
     * @verifies return true for plain text with newlines
     */
    @Test
    void isCleanComment_shouldReturnTrueForPlainTextWithNewlines() {
        assertTrue(HtmlSanitizer.isCleanComment("hello\nworld"));
    }

    /**
     * @see HtmlSanitizer#isCleanComment(String)
     * @verifies return false for script injection
     */
    @Test
    void isCleanComment_shouldReturnFalseForScriptInjection() {
        assertFalse(HtmlSanitizer.isCleanComment("hi<script>alert(1)</script>"));
    }

    /**
     * @see HtmlSanitizer#isCleanComment(String)
     * @verifies return false for img tag in comment
     */
    @Test
    void isCleanComment_shouldReturnFalseForImgTagInComment() {
        assertFalse(HtmlSanitizer.isCleanComment("hello<img src=\"x\">"));
    }
}
