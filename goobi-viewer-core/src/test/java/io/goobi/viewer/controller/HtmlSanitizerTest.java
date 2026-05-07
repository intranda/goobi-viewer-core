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
     * @verifies preserve root relative anchor href
     */
    @Test
    void cleanRichText_shouldPreserveRootRelativeAnchorHref() {
        // Regression: with the default Jsoup safelist (no preserveRelativeLinks) and an empty
        // baseUri, root-relative hrefs were resolved to "" and then dropped by the protocol
        // allowlist, leaving CMS-internal links like "/viewer/image/..." stripped to bare anchors.
        String result = HtmlSanitizer.cleanRichText(
                "<a class=\"link\" href=\"/viewer/image/10089470_1919/1/LOG_0003/\""
                        + " target=\"_blank\" rel=\"noopener noreferrer\">1919</a>");
        assertTrue(result.contains("href=\"/viewer/image/10089470_1919/1/LOG_0003/\""),
                "root-relative href must survive sanitization, got: " + result);
        assertTrue(result.contains("target=\"_blank\""));
        assertTrue(result.contains("1919"));
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve path relative anchor href
     */
    @Test
    void cleanRichText_shouldPreservePathRelativeAnchorHref() {
        // Path-relative hrefs (no leading slash) must also survive sanitization.
        String result = HtmlSanitizer.cleanRichText(
                "<a href=\"viewer/image/10089470_1919/1/LOG_0003/\">1919</a>");
        assertTrue(result.contains("href=\"viewer/image/10089470_1919/1/LOG_0003/\""),
                "path-relative href must survive sanitization, got: " + result);
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies still remove javascript URI when relative links are preserved
     */
    @Test
    void cleanRichText_shouldStillRemoveJavascriptUriWhenRelativeLinksArePreserved() {
        // Security regression guard: enabling preserveRelativeLinks must NOT weaken the
        // protocol allowlist for absolute URIs. javascript: still has a scheme and must be
        // dropped — preserveRelativeLinks only affects strings without a resolvable scheme.
        String result = HtmlSanitizer.cleanRichText(
                "<a href=\"javascript:alert(1)\">click</a>"
                        + "<a href=\"/safe/relative\">ok</a>");
        assertFalse(result.toLowerCase().contains("javascript:"),
                "javascript: URI must still be stripped, got: " + result);
        assertTrue(result.contains("href=\"/safe/relative\""),
                "relative href must survive in the same document, got: " + result);
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
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve class attribute on any element
     */
    @Test
    void cleanRichText_shouldPreserveClassAttributeOnAnyElement() {
        // CMS authors use class attributes as CSS hooks; the rich-text profile must keep
        // them on arbitrary tags (not only the few Jsoup happens to allow by default).
        String result = HtmlSanitizer.cleanRichText(
                "<div class=\"intro\"><p class=\"lead\">hello</p></div>");
        assertTrue(result.contains("class=\"intro\""), "class on div lost: " + result);
        assertTrue(result.contains("class=\"lead\""), "class on p lost: " + result);
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve id attribute on any element
     */
    @Test
    void cleanRichText_shouldPreserveIdAttributeOnAnyElement() {
        // id is required as the target of in-page anchor hrefs (#fragment) — without it,
        // Bootstrap tabs / TOC links would link to nothing after sanitization.
        String result = HtmlSanitizer.cleanRichText("<div id=\"uebersicht\">x</div>");
        assertTrue(result.contains("id=\"uebersicht\""), "id stripped: " + result);
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve role attribute on any element
     */
    @Test
    void cleanRichText_shouldPreserveRoleAttributeOnAnyElement() {
        String result = HtmlSanitizer.cleanRichText(
                "<ul role=\"tablist\"><li role=\"presentation\">x</li></ul>");
        assertTrue(result.contains("role=\"tablist\""), "role on ul stripped: " + result);
        assertTrue(result.contains("role=\"presentation\""), "role on li stripped: " + result);
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve aria attributes on any element
     */
    @Test
    void cleanRichText_shouldPreserveAriaAttributesOnAnyElement() {
        // aria-* is an open-ended family; the override in buildRichTextSafelist must let any
        // aria-prefixed attribute pass — not just an enumerated subset. <span> is used
        // because it is in the allowlist; <button> would be stripped as a tag.
        String result = HtmlSanitizer.cleanRichText(
                "<span aria-controls=\"panel\" aria-expanded=\"false\""
                        + " aria-label=\"open\" aria-describedby=\"desc\">x</span>");
        assertTrue(result.contains("aria-controls=\"panel\""), "aria-controls stripped: " + result);
        assertTrue(result.contains("aria-expanded=\"false\""), "aria-expanded stripped: " + result);
        assertTrue(result.contains("aria-label=\"open\""), "aria-label stripped: " + result);
        assertTrue(result.contains("aria-describedby=\"desc\""), "aria-describedby stripped: " + result);
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve data attributes on any element
     */
    @Test
    void cleanRichText_shouldPreserveDataAttributesOnAnyElement() {
        // data-* is also open-ended; required for Bootstrap data-toggle / data-target hooks
        // and arbitrary custom JS bindings that CMS authors add to rich-text components.
        String result = HtmlSanitizer.cleanRichText(
                "<a href=\"#x\" data-toggle=\"tab\" data-target=\"#x\""
                        + " data-bs-toggle=\"modal\">x</a>");
        assertTrue(result.contains("data-toggle=\"tab\""), "data-toggle stripped: " + result);
        assertTrue(result.contains("data-target=\"#x\""), "data-target stripped: " + result);
        assertTrue(result.contains("data-bs-toggle=\"modal\""), "data-bs-toggle stripped: " + result);
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies preserve bootstrap tab navigation markup
     */
    @Test
    void cleanRichText_shouldPreserveBootstrapTabNavigationMarkup() {
        // End-to-end regression: the actual CMS author markup (calendar widget tabs) must
        // round-trip the sanitizer without losing class / role / aria / data hooks, otherwise
        // the tab JS no longer works after save.
        String input = "<ul class=\"nav nav-tabs\" role=\"tablist\">"
                + "<li class=\"active\" role=\"presentation\">"
                + "<a class=\"nav-link active\" role=\"tab\" href=\"#uebersicht\""
                + " aria-controls=\"uebersicht\" data-toggle=\"tab\">Jahresübersicht</a>"
                + "</li>"
                + "<li role=\"presentation\">"
                + "<a class=\"nav-link\" role=\"tab\" href=\"#titel\""
                + " aria-controls=\"Titel\" data-toggle=\"tab\">Titelübersicht</a>"
                + "</li>"
                + "</ul>";
        String result = HtmlSanitizer.cleanRichText(input);
        assertTrue(result.contains("class=\"nav nav-tabs\""), "ul class lost: " + result);
        assertTrue(result.contains("role=\"tablist\""), "ul role lost: " + result);
        assertTrue(result.contains("class=\"active\""), "li class lost: " + result);
        assertTrue(result.contains("class=\"nav-link active\""), "first a class lost: " + result);
        assertTrue(result.contains("role=\"tab\""), "a role lost: " + result);
        assertTrue(result.contains("aria-controls=\"uebersicht\""), "aria-controls lost: " + result);
        assertTrue(result.contains("data-toggle=\"tab\""), "data-toggle lost: " + result);
        assertTrue(result.contains("href=\"#uebersicht\""), "fragment href lost: " + result);
        assertTrue(result.contains("href=\"#titel\""), "second fragment href lost: " + result);
    }

    /**
     * @see HtmlSanitizer#cleanRichText(String)
     * @verifies still remove onclick when class is allowed
     */
    @Test
    void cleanRichText_shouldStillRemoveOnclickWhenClassIsAllowed() {
        // Security regression guard: opening up class/role/aria/data must not accidentally
        // open up event-handler attributes like onclick / onmouseover. The Safelist override
        // only relaxes aria-*/data-*, never on*-prefixed attributes.
        String result = HtmlSanitizer.cleanRichText(
                "<div class=\"x\" onclick=\"alert(1)\" onmouseover=\"alert(2)\">x</div>");
        assertTrue(result.contains("class=\"x\""), "class must survive: " + result);
        assertFalse(result.toLowerCase().contains("onclick"),
                "onclick must still be stripped: " + result);
        assertFalse(result.toLowerCase().contains("onmouseover"),
                "onmouseover must still be stripped: " + result);
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
     * @see HtmlSanitizer#isCleanRichText(String)
     * @verifies return true for relative anchor href
     */
    @Test
    void isCleanRichText_shouldReturnTrueForRelativeAnchorHref() {
        // CMS rich-text content commonly contains internal relative links; they must be
        // accepted as clean so the save-pipeline does not flag them as suspect.
        assertTrue(HtmlSanitizer.isCleanRichText(
                "<p><a href=\"/viewer/image/10089470_1919/1/LOG_0003/\">1919</a></p>"));
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
     * @verifies preserve relative anchor href
     */
    @Test
    void cleanComment_shouldPreserveRelativeAnchorHref() {
        // Same regression as cleanRichText: relative hrefs in user comments must not be
        // dropped by the protocol allowlist when no baseUri is provided.
        String result = HtmlSanitizer.cleanComment(
                "<a href=\"/viewer/image/10089470_1919/1/LOG_0003/\">1919</a>");
        assertTrue(result.contains("href=\"/viewer/image/10089470_1919/1/LOG_0003/\""),
                "root-relative href must survive comment sanitization, got: " + result);
    }

    /**
     * @see HtmlSanitizer#cleanComment(String)
     * @verifies still remove javascript URI when relative links are preserved
     */
    @Test
    void cleanComment_shouldStillRemoveJavascriptUriWhenRelativeLinksArePreserved() {
        String result = HtmlSanitizer.cleanComment(
                "<a href=\"javascript:alert(1)\">x</a><a href=\"/ok\">y</a>");
        assertFalse(result.toLowerCase().contains("javascript:"),
                "javascript: URI must still be stripped from comments, got: " + result);
        assertTrue(result.contains("href=\"/ok\""),
                "relative href must survive in the same comment, got: " + result);
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
     * @see HtmlSanitizer#cleanCommentPlainText(String)
     * @verifies return null when input is null
     */
    @Test
    void cleanCommentPlainText_shouldReturnNullWhenInputIsNull() {
        assertNull(HtmlSanitizer.cleanCommentPlainText(null));
    }

    /**
     * @see HtmlSanitizer#cleanCommentPlainText(String)
     * @verifies return empty string when input is empty
     */
    @Test
    void cleanCommentPlainText_shouldReturnEmptyStringWhenInputIsEmpty() {
        assertEquals("", HtmlSanitizer.cleanCommentPlainText(""));
    }

    /**
     * @see HtmlSanitizer#cleanCommentPlainText(String)
     * @verifies preserve plain text newlines verbatim
     */
    @Test
    void cleanCommentPlainText_shouldPreservePlainTextNewlinesVerbatim() {
        // Plain text input must round-trip unchanged: no <br> injection, newlines preserved.
        String result = HtmlSanitizer.cleanCommentPlainText("A bird in the hand is worth\ntwo in the bush.");
        assertEquals("A bird in the hand is worth\ntwo in the bush.", result);
    }

    /**
     * @see HtmlSanitizer#cleanCommentPlainText(String)
     * @verifies strip all html tags
     */
    @Test
    void cleanCommentPlainText_shouldStripAllHtmlTags() {
        // Even allowlisted-by-cleanComment inline tags are removed by the plain-text profile.
        String result = HtmlSanitizer.cleanCommentPlainText("<p>foo <strong>bar</strong> <em>baz</em></p>");
        assertFalse(result.toLowerCase().contains("<p"));
        assertFalse(result.toLowerCase().contains("<strong"));
        assertFalse(result.toLowerCase().contains("<em"));
        assertTrue(result.contains("foo"));
        assertTrue(result.contains("bar"));
        assertTrue(result.contains("baz"));
    }

    /**
     * @see HtmlSanitizer#cleanCommentPlainText(String)
     * @verifies remove script tags and content
     */
    @Test
    void cleanCommentPlainText_shouldRemoveScriptTagsAndContent() {
        String result = HtmlSanitizer.cleanCommentPlainText("hello<script>alert(1)</script>world");
        assertFalse(result.toLowerCase().contains("<script"));
        assertFalse(result.contains("alert(1)"));
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("world"));
    }

    /**
     * @see HtmlSanitizer#cleanCommentPlainText(String)
     * @verifies strip br tags injected by attackers
     */
    @Test
    void cleanCommentPlainText_shouldStripBrTagsInjectedByAttackers() {
        // No <br> may survive — neither user-supplied nor as a byproduct of sanitization.
        String result = HtmlSanitizer.cleanCommentPlainText("line1<br>line2");
        assertFalse(result.toLowerCase().contains("<br"));
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
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

    /**
     * @see HtmlSanitizer#isCleanComment(String)
     * @verifies return true for relative anchor href
     */
    @Test
    void isCleanComment_shouldReturnTrueForRelativeAnchorHref() {
        assertTrue(HtmlSanitizer.isCleanComment(
                "<a href=\"/viewer/image/10089470_1919/1/LOG_0003/\">1919</a>"));
    }
}
