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
 *   <li>{@link #cleanCommentPlainText(String)} — for consumers that must produce plain
 *       text (no HTML markup) such as IIIF Search hit selectors or other non-HTML JSON
 *       payloads. Strips all tags and preserves plain-text newlines verbatim (no
 *       {@code <br>} injection).</li>
 * </ul>
 *
 * <p>
 * Replaces the regex-based {@link StringTools#stripJS(String)} which only removed {@code
 * <script>} and {@code <svg>} blocks and was bypassable through any other XSS vector
 * (event-handler attributes, {@code javascript:} URIs, etc.).
 * </p>
 */
public final class HtmlSanitizer {

    /**
     * Synthetic base URI used when resolving relative hrefs during sanitization. Jsoup's cleaner
     * resolves every URL attribute against the document base URI before applying the protocol
     * allowlist; with an empty base, relative URLs resolve to {@code ""} and are then dropped
     * because the empty string does not match any allowed scheme. Pairing a non-empty base URI
     * with {@link Safelist#preserveRelativeLinks(boolean)} keeps the original (relative) form
     * in the output while still letting the protocol filter block {@code javascript:},
     * {@code data:} and other absolute-URL attacks. The {@code .invalid} TLD is reserved by
     * RFC 2606 and never collides with real content.
     */
    private static final String SANITIZER_BASE_URI = "https://placeholder.invalid/";

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
     * @should preserve root relative anchor href
     * @should preserve path relative anchor href
     * @should still remove javascript URI when relative links are preserved
     */
    public static String cleanRichText(String input) {
        if (input == null) {
            return null;
        }
        if (input.isEmpty()) {
            return input;
        }
        // Use prettyPrint(false) so whitespace and newlines inside elements are not collapsed.
        // SANITIZER_BASE_URI is required for preserveRelativeLinks(true) to keep relative hrefs
        // such as "/viewer/image/..." (the placeholder is never written to output).
        return Jsoup.clean(input, SANITIZER_BASE_URI, buildRichTextSafelist(),
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
     * @should return true for relative anchor href
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
     * @should preserve relative anchor href
     * @should still remove javascript URI when relative links are preserved
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
        // SANITIZER_BASE_URI: see field javadoc — required so relative hrefs survive
        // preserveRelativeLinks(true) without weakening the protocol filter.
        return Jsoup.clean(preprocessPlainTextNewlines(input), SANITIZER_BASE_URI, buildCommentSafelist(),
                new Document.OutputSettings().prettyPrint(false));
    }

    /**
     * Sanitize user-authored content for consumers that must emit plain text (no HTML markup),
     * for example IIIF Search hit selectors whose {@code prefix}/{@code suffix} fields are
     * plain text per the W3C Web Annotation spec, or any other non-HTML JSON payload that may
     * be rendered downstream.
     *
     * <p>
     * Differs from {@link #cleanComment(String)} in two ways: the deny-by-default
     * {@code Safelist.none()} strips ALL tags (including the inline formatting that
     * {@code cleanComment} preserves), and plain-text newlines are kept as {@code \n} rather
     * than rewritten to {@code <br>\n}. Output therefore round-trips a plain-text input
     * unchanged. {@code prettyPrint(false)} prevents Jsoup from collapsing whitespace in
     * surviving text nodes.
     * </p>
     *
     * @param input raw string; may be {@code null}
     * @return plain-text string with all HTML tags removed and {@code \n} preserved;
     *         {@code null} if input was {@code null}
     * @should return null when input is null
     * @should return empty string when input is empty
     * @should preserve plain text newlines verbatim
     * @should strip all html tags
     * @should remove script tags and content
     * @should strip br tags injected by attackers
     */
    public static String cleanCommentPlainText(String input) {
        if (input == null) {
            return null;
        }
        if (input.isEmpty()) {
            return input;
        }
        // Safelist.none() strips every tag; prettyPrint(false) keeps existing \n in text nodes.
        // Deliberately skip the cleanComment newline-to-<br> preprocessing so the IIIF Search
        // selectors and other plain-text consumers receive verbatim newlines.
        return Jsoup.clean(input, "", Safelist.none(),
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
     * @should return true for relative anchor href
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
        //   - preserveRelativeLinks(true): keep CMS-internal hrefs like "/viewer/image/..."
        //     intact. Without this, Jsoup resolves relative hrefs against the (empty) baseUri
        //     to "" and the protocol allowlist then drops them, leaving anchors stripped of
        //     their href. The protocol allowlist still applies to absolute URIs, so
        //     javascript: / data: stay blocked.
        // We intentionally do NOT add data: scheme — would allow data:image/svg+xml XSS.
        return Safelist.relaxed()
                .addTags("figure", "figcaption")
                .addAttributes("a", "target", "rel")
                .preserveRelativeLinks(true);
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
        // preserveRelativeLinks(true): same rationale as in buildRichTextSafelist() —
        // viewer-internal links in comments must survive sanitization; the protocol allowlist
        // continues to block javascript:/data: because those are absolute URIs with a scheme.
        return new Safelist()
                .addTags("p", "br", "b", "i", "em", "strong", "u", "span", "a")
                .addAttributes("a", "href", "title", "target", "rel")
                .addProtocols("a", "href", "http", "https", "mailto")
                .preserveRelativeLinks(true);
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
