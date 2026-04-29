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

// Added Jsoup imports for the cleanRichText/isCleanRichText implementation (Task 1).
// Jsoup is encapsulated here — callers must never import org.jsoup.* directly.
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * <p>
 * HTML sanitizer for user-generated content. Uses Jsoup with explicit allowlists to neutralize
 * cross-site-scripting payloads (script tags, event handler attributes, javascript: URIs, etc.)
 * while preserving safe markup produced by the CMS rich-text editor or annotation/comment
 * authors. The Jsoup dependency is fully encapsulated — callers never import {@code
 * org.jsoup.*}.
 * </p>
 *
 * <p>
 * Two profiles are exposed, each with a sanitizing variant and a read-only validation variant
 * that share the same allowlist definition:
 * </p>
 * <ul>
 *   <li>{@link #cleanRichText(String)} / {@link #isCleanRichText(String)} — for TinyMCE-style
 *       rich-text editor output (CMS htmltext components, license placeholder descriptions).
 *       Allows the structural and inline tag set including tables, headings, lists, links,
 *       images and figure/figcaption.</li>
 *   <li>{@link #cleanComment(String)} / {@link #isCleanComment(String)} — for short
 *       user-authored snippets (comments, annotation bodies). Allows only minimal inline
 *       formatting; preserves plain-text line breaks by converting them to {@code <br>}
 *       before sanitization.</li>
 * </ul>
 *
 * <p>
 * Replaces the regex-based {@link StringTools#stripJS(String)} which only removed {@code
 * <script>} and {@code <svg>} blocks and was bypassable through any other XSS vector
 * (event-handler attributes, {@code javascript:} URIs, etc.).
 * </p>
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
        // Utility class
    }

    /**
     * Sanitize rich-text HTML produced by TinyMCE-style editors. Allows the typical structural
     * and inline tag set, with explicit URL-scheme allowlist for anchors and images. Output
     * is generated with {@code prettyPrint=false} so byte-equality round-trips are preserved
     * for non-pathological input (no whitespace collapsing).
     *
     * @param input raw HTML string from a CMS rich-text editor; may be {@code null}
     * @return sanitized HTML containing only allowlisted tags and attributes; {@code null} if
     *         input was {@code null}
     * @should return null when input is null
     * @should return empty string when input is empty
     * @should remove script tags
     * @should remove img onerror attribute
     * @should remove svg onload attribute
     * @should remove javascript URI from anchor href
     * @should remove javascript URI with leading whitespace
     * @should remove iframe tags
     * @should remove details ontoggle attribute
     * @should remove formaction bypass on button inside form
     * @should remove base href javascript URI
     * @should preserve allowlisted structural tags
     * @should preserve figure and figcaption tags
     * @should preserve http and https anchor href
     * @should preserve target and rel on anchor
     * @should preserve table markup
     * @should remove data URI from img src
     * @should preserve newlines without collapsing whitespace
     */
    public static String cleanRichText(String input) {
        if (input == null) {
            return null;
        }
        if (input.isEmpty()) {
            return input;
        }
        // Use prettyPrint(false) so whitespace and newlines inside elements are not collapsed
        return Jsoup.clean(input, "", buildRichTextSafelist(),
                new Document.OutputSettings().prettyPrint(false));
    }

    /**
     * Validate whether the given input would survive {@link #cleanRichText(String)} unchanged
     * (modulo Jsoup's internal parser representation). Use this for warn-or-clean detection
     * branches, instead of comparing input to {@code cleanRichText(input)} — the latter would
     * trigger on harmless attribute reordering or tag-case normalization.
     *
     * @param input HTML string to validate; may be {@code null}
     * @return {@code true} if input contains only allowlisted tags and attributes (or is
     *         {@code null}/empty); {@code false} otherwise
     * @should return true for null input
     * @should return true for empty input
     * @should return true for legitimate markup
     * @should return false for script injection
     */
    public static boolean isCleanRichText(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        return Jsoup.isValid(input, buildRichTextSafelist());
    }

    /**
     * Sanitize short user-authored snippets such as comments and annotation bodies. Allows
     * only minimal inline formatting; rejects images, tables, headings and any block-level
     * structural markup. Plain-text line breaks ({@code \n}) are converted to {@code <br>}
     * before sanitization so they survive Jsoup's whitespace collapsing.
     *
     * @param input raw string; may be {@code null}
     * @return sanitized string with allowlisted inline tags only; {@code null} if input was
     *         {@code null}
     * @should return null when input is null
     * @should remove script tags
     * @should remove img tags
     * @should remove table tags
     * @should preserve basic inline formatting tags
     * @should preserve safe anchor tags
     * @should remove javascript URI from anchor href
     * @should preserve plain text line breaks as br tags
     */
    public static String cleanComment(String input) {
        if (input == null) {
            return null;
        }
        if (input.isEmpty()) {
            return input;
        }
        // Convert plain-text newlines to <br> before sanitization to preserve line breaks,
        // then sanitize using the comment-profile safelist (no images, tables, headings).
        return Jsoup.clean(preprocessPlainTextNewlines(input), "", buildCommentSafelist(),
                new Document.OutputSettings().prettyPrint(false));
    }

    /**
     * Validate whether the given input would survive {@link #cleanComment(String)} unchanged.
     * Plain-text line breaks are first converted to {@code <br>} (matching the sanitize path)
     * so a multi-line plain-text comment is considered clean.
     *
     * @param input string to validate; may be {@code null}
     * @return {@code true} if input contains only allowlisted tags (or is {@code null}/empty);
     *         {@code false} otherwise
     * @should return true for null input
     * @should return true for plain text with newlines
     * @should return false for script injection
     * @should return false for img tag in comment
     */
    public static boolean isCleanComment(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        // Apply the same newline-preprocessing as cleanComment so plain-text inputs with \n
        // are not incorrectly reported as dirty.
        return Jsoup.isValid(preprocessPlainTextNewlines(input), buildCommentSafelist());
    }

    /**
     * Build a fresh {@code Safelist} for the rich-text profile. Returned per call to prevent
     * shared-mutable-state issues — Jsoup's {@code Safelist} is mutable and we never want a
     * caller (or a forgotten earlier modification) to alter the project's HTML allowlist.
     */
    private static Safelist buildRichTextSafelist() {
        // Safelist.relaxed() already permits a/href with http, https, ftp, mailto. We add:
        //   - figure/figcaption: TinyMCE default markup for captioned images
        //   - target, rel on a: required for target="_blank" rel="noopener" patterns
        // We intentionally do NOT add data: scheme — would allow data:image/svg+xml XSS.
        return Safelist.relaxed()
                .addTags("figure", "figcaption")
                .addAttributes("a", "target", "rel");
    }

    /**
     * Build a fresh {@code Safelist} for the comment profile. Returned per call (defensive
     * copy) — see rationale on {@link #buildRichTextSafelist()}.
     *
     * <p>
     * Starts from an empty {@link Safelist} (deny-by-default) rather than a built-in preset,
     * because the comment profile is stricter than any preset Jsoup ships with. Allows only
     * minimal inline tags; deliberately excludes images, tables, and headings.
     * </p>
     *
     * <p>
     * <b>Do not widen this allowlist without a security review.</b> The current set is the
     * intersection of "what comment authors actually use" and "what cannot be abused" —
     * adding {@code img}, {@code style}, {@code data:} scheme, or block-level structural tags
     * reopens XSS or layout-injection vectors.
     * </p>
     */
    private static Safelist buildCommentSafelist() {
        return new Safelist()
                .addTags("p", "br", "b", "i", "em", "strong", "u", "span", "a")
                .addAttributes("a", "href", "title", "target", "rel")
                .addProtocols("a", "href", "http", "https", "mailto");
    }

    /**
     * Convert plain-text line breaks to {@code <br>} so they survive Jsoup's whitespace
     * collapsing. Windows line endings ({@code \r\n}) are first normalized to {@code \n} so
     * the {@code \r} does not leak through as stray whitespace. The original newline
     * character is preserved after the tag for source readability.
     */
    private static String preprocessPlainTextNewlines(String s) {
        return s.replace("\r\n", "\n").replace("\n", "<br>\n");
    }
}
