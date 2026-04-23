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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.apache.commons.lang3.IntegerRange;

import io.goobi.viewer.solr.SolrTools;

class StringToolsTest {

    /**
     * @see StringTools#escapeHtmlChars(String)
     * @verifies replace angle brackets, quotes, and ampersands with HTML entities
     */
    @Test
    void escapeHtmlChars_shouldReplaceAngleBracketsQuotesAndAmpersandsWithHTMLEntities() throws Exception {
        assertEquals("&lt;i&gt;&quot;A&amp;B&quot;&lt;/i&gt;", StringTools.escapeHtmlChars("<i>\"A&B\"</i>"));
    }

    /**
     * @see StringTools#replaceCharacters(String,String[],String[])
     * @verifies replace characters correctly
     */
    @Test
    void replaceCharacters_shouldReplaceCharactersCorrectly() throws Exception {
        assertEquals("|-|3110",
                StringTools.replaceCharacters("Hello", new String[] { "H", "e", "l", "o" }, new String[] { "|-|", "3", "1", "0" }));
    }

    /**
     * @see StringTools#removeDiacriticalMarks(String)
     * @verifies strip accents from umlauts and accented characters while preserving eszett and base letters
     */
    @Test
    void removeDiacriticalMarks_shouldStripAccentsFromUmlautsAndAccentedCharactersWhilePreservingEszettAndBaseLetters() throws Exception {
        assertEquals("aaaaoooouuuueeeeßn", StringTools.removeDiacriticalMarks("äáàâöóòôüúùûëéèêßñ"));
    }

    /**
     * @see StringTools#removeLineBreaks(String,String)
     * @verifies strip CRLF sequences and join text with given replacement
     */
    @Test
    void removeLineBreaks_shouldStripCRLFSequencesAndJoinTextWithGivenReplacement() throws Exception {
        assertEquals("foobar", StringTools.removeLineBreaks("foo\r\nbar", ""));
    }

    /**
     * @see StringTools#removeLineBreaks(String,String)
     * @verifies replace all HTML br tag variants with the given replacement string
     */
    @Test
    void removeLineBreaks_shouldReplaceAllHTMLBrTagVariantsWithTheGivenReplacementString() throws Exception {
        assertEquals("foo bar", StringTools.removeLineBreaks("foo<br>bar", " "));
        assertEquals("foo bar", StringTools.removeLineBreaks("foo<br/>bar", " "));
        assertEquals("foo bar", StringTools.removeLineBreaks("foo<br />bar", " "));
    }

    /**
     * @see StringTools#stripJS(String)
     * @verifies remove script tags, self-closing scripts, and SVG event handler elements regardless of case
     */
    @Test
    void stripJS_shouldRemoveScriptTagsSelfClosingScriptsAndSVGEventHandlerElementsRegardlessOfCase() throws Exception {
        assertEquals("foo  bar", StringTools.stripJS("foo <script type=\"javascript\">\nfunction f {\n alert();\n}\n</script> bar"));
        assertEquals("foo  bar", StringTools.stripJS("foo <SCRIPT>\nfunction f {\n alert();\n}\n</ScRiPt> bar"));
        assertEquals("foo  bar", StringTools.stripJS("foo <SCRIPT src=\"http://dangerousscript.js\"/> bar"));
        assertEquals("foo  bar", StringTools.stripJS("foo <svG onLoad=alert(\"Hello_XSS_World\")></svG> bar"));
        assertEquals("foo  bar", StringTools.stripJS("foo <svG onLoad=alert(\"Hello_XSS_World\")/> bar"));
    }

    /**
     * @see StringTools#stripPatternBreakingChars(String)
     * @verifies replace tabs and line break characters with underscores
     */
    @Test
    void stripPatternBreakingChars_shouldReplaceTabsAndLineBreakCharactersWithUnderscores() throws Exception {
        assertEquals("foo_bar__", StringTools.stripPatternBreakingChars("foo\tbar\r\n"));
    }

    /**
     * @verifies return expected value for given input
     * @see StringTools#escapeQuotes(String)
     */
    @Test
    void escapeQuotes_shouldReturnExpectedValueForGivenInput() {
        String original = "Das ist ein 'String' mit \"Quotes\".";
        String reference = "Das ist ein \\'String\\' mit \\\"Quotes\\\".";

        String escaped = StringTools.escapeQuotes(original);
        assertEquals(reference, escaped);

        escaped = StringTools.escapeQuotes(reference);
        assertEquals(reference, escaped);
    }

    /**
     * @see StringTools#isImageUrl(String)
     * @verifies return true for image urls
     */
    @Test
    void isImageUrl_shouldReturnTrueForImageUrls() throws Exception {
        assertTrue(StringTools.isImageUrl("https://example.com/default.jpg"));
        assertTrue(StringTools.isImageUrl("https://example.com/MASTER.TIFF"));
    }

    /**
     * @verifies move leading digits in CSS class names to the end in both selectors and class attributes
     */
    @Test
    void renameIncompatibleCSSClasses_shouldMoveLeadingDigitsInCSSClassNamesToTheEndInBothSelectorsAndClassAttributes() throws Exception {
        Path file = Paths.get("src/test/resources/data/text_example_bad_classes.htm");
        assertTrue(Files.isRegularFile(file));

        String html = FileTools.getStringFromFile(file.toFile(), StringTools.DEFAULT_ENCODING);
        Assertions.assertNotNull(html);
        assertTrue(html.contains(".20Formatvorlage"));
        assertTrue(html.contains("class=\"20Formatvorlage"));

        html = StringTools.renameIncompatibleCSSClasses(html);
        assertFalse(html.contains(".20Formatvorlage"));
        assertFalse(html.contains("class=\"20Formatvorlage"));
        assertTrue(html.contains(".Formatvorlage20"));
        assertTrue(html.contains("class=\"Formatvorlage20"));
    }

    /**
     * @see StringTools#getHierarchyForCollection(String,String)
     * @verifies build cumulative hierarchy list from separator-delimited collection string
     */
    @Test
    void getHierarchyForCollection_shouldBuildCumulativeHierarchyListFromSeparatorDelimitedCollectionString() throws Exception {
        List<String> result = StringTools.getHierarchyForCollection("a.b.c.d", ".");
        assertEquals(4, result.size());
        assertEquals("a", result.get(0));
        assertEquals("a.b", result.get(1));
        assertEquals("a.b.c", result.get(2));
        assertEquals("a.b.c.d", result.get(3));
    }

    /**
     * @see StringTools#getHierarchyForCollection(String,String)
     * @verifies return single-element list when collection string has no separator
     */
    @Test
    void getHierarchyForCollection_shouldReturnSingleElementListWhenCollectionStringHasNoSeparator() throws Exception {
        List<String> result = StringTools.getHierarchyForCollection("a", ".");
        assertEquals(1, result.size());
        assertEquals("a", result.get(0));
    }

    /**
     * @see StringTools#normalizeWebAnnotationCoordinates(String)
     * @verifies convert xywh fragment to absolute x 1 y 1 x 2 y 2 coordinate format
     */
    @Test
    void normalizeWebAnnotationCoordinates_shouldConvertXywhFragmentToAbsoluteX1Y1X2Y2CoordinateFormat() throws Exception {
        assertEquals("1, 2, 4, 6", StringTools.normalizeWebAnnotationCoordinates("xywh=1, 2, 3, 4"));
    }

    /**
     * @see StringTools#normalizeWebAnnotationCoordinates(String)
     * @verifies preserve legacy coordinates
     */
    @Test
    void normalizeWebAnnotationCoordinates_shouldPreserveLegacyCoordinates() throws Exception {
        assertEquals("1, 2, 3, 4", StringTools.normalizeWebAnnotationCoordinates("1, 2, 3, 4"));
    }

    /**
     * @see StringTools#generateHash(String)
     * @verifies return SHA-256 hex digest for given input string
     */
    @Test
    void generateHash_shouldReturnSHA256HexDigestForGivenInputString() throws Exception {
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", StringTools.generateHash("test"));
    }

    /**
     * @see StringTools#checkValueEmptyOrInverted(String)
     * @verifies return true if value null or empty
     */
    @Test
    void checkValueEmptyOrInverted_shouldReturnTrueIfValueNullOrEmpty() throws Exception {
        assertTrue(StringTools.checkValueEmptyOrInverted(null));
        assertTrue(StringTools.checkValueEmptyOrInverted(""));
    }

    /**
     * @see StringTools#checkValueEmptyOrInverted(String)
     * @verifies return true if value starts with 0x1
     */
    @Test
    void checkValueEmptyOrInverted_shouldReturnTrueIfValueStartsWith0x1() throws Exception {
        assertTrue(StringTools.checkValueEmptyOrInverted("oof"));
    }

    /**
     * @see StringTools#checkValueEmptyOrInverted(String)
     * @verifies return true if value starts with 1
     */
    @Test
    void checkValueEmptyOrInverted_shouldReturnTrueIfValueStartsWith1() throws Exception {
        assertTrue(StringTools.checkValueEmptyOrInverted("#1;oof"));
    }

    /**
     * @see StringTools#checkValueEmptyOrInverted(String)
     * @verifies return false otherwise
     */
    @Test
    void checkValueEmptyOrInverted_shouldReturnFalseOtherwise() throws Exception {
        assertFalse(StringTools.checkValueEmptyOrInverted("foo"));
    }

    /**
     * @see StringTools#filterStringsViaRegex(List,String)
     * @verifies return all matching keys
     */
    @Test
    void filterStringsViaRegex_shouldReturnAllMatchingKeys() throws Exception {
        String[] keys = new String[] { "foo", "bar", "key0", "key1", "key2" };
        List<String> result = StringTools.filterStringsViaRegex(Arrays.asList(keys), "key[0-9]+");
        assertEquals(3, result.size());
        assertEquals("key0", result.get(0));
        assertEquals("key1", result.get(1));
        assertEquals("key2", result.get(2));
    }

    /**
     * @see StringTools#isStringUrlEncoded(String,String)
     * @verifies return true if string contains url encoded characters
     */
    @Test
    void isStringUrlEncoded_shouldReturnTrueIfStringContainsUrlEncodedCharacters() throws Exception {
        assertTrue(StringTools.isStringUrlEncoded("%28foo%29", StringTools.DEFAULT_ENCODING));
    }

    /**
     * @see StringTools#isStringUrlEncoded(String,String)
     * @verifies return false if string not encoded
     */
    @Test
    void isStringUrlEncoded_shouldReturnFalseIfStringNotEncoded() throws Exception {
        assertFalse(StringTools.isStringUrlEncoded("(foo)", StringTools.DEFAULT_ENCODING));
    }

    /**
     * @see StringTools#isStringUrlEncoded(String,String)
     * @verifies return false if string contains literal percent sign
     */
    @Test
    void isStringUrlEncoded_shouldReturnFalseIfStringContainsLiteralPercentSign() throws Exception {
        assertFalse(StringTools.isStringUrlEncoded("%", StringTools.DEFAULT_ENCODING));
    }

    /**
     * @see StringTools#escapeCriticalUrlChracters(String,boolean)
     * @verifies replace +, /, backslash, pipe, and ? with Unicode escape sequences and decode percent-encoding when flagged
     */
    @Test
    void escapeCriticalUrlChracters_shouldReplacePlusSlashBackslashPipeAndQuestionMarkWithUnicodeEscapeSequencesAndDecodePercentEncodingWhenFlagged() throws Exception {
        assertEquals("U002BAU002FU005CU007CU003FZ", StringTools.escapeCriticalUrlChracters("+A/\\|?Z", false));
        assertEquals("U007C", StringTools.escapeCriticalUrlChracters("%7C", true));
    }

    /**
     * @see StringTools#unescapeCriticalUrlChracters(String)
     * @verifies restore original characters from Unicode escape sequences
     */
    @Test
    void unescapeCriticalUrlChracters_shouldRestoreOriginalCharactersFromUnicodeEscapeSequences() throws Exception {
        assertEquals("+A/\\|?Z", StringTools.unescapeCriticalUrlChracters("U002BAU002FU005CU007CU003FZ"));
    }

    /**
     * @verifies return expected value
     * @see StringTools#sortByList(Object, Object, List)
     */
    @Test
    void sortByList_shouldReturnExpectedValue() {
        List<String> sorting = List.of("c", "d", "e", "f", "g", "a", "h");

        List<String> s1 = List.of("a", "b", "c", "d");
        List<String> s1Sorted = new ArrayList<>(s1);
        s1Sorted.sort((k, l) -> StringTools.sortByList(k, l, sorting));
        assertEquals("c", s1Sorted.get(0));
        assertEquals("d", s1Sorted.get(1));
        assertEquals("a", s1Sorted.get(2));
        assertEquals("b", s1Sorted.get(3));
    }

    /**
     * @verifies remove script tags and data attributes from html
     */
    @Test
    void scenario_shouldRemoveScriptTagsAndDataAttributesWhenCleaningHtmlWithJsoup() {

        String html =
                "<p><script>alert('SPAM')</script><span data-sheets-value=\"{\"1\":2,\"2\":\"Kremer, Boris, and Alex Reding. My home is my castle : exposition d’art contemporain, du 1er juin au 27 octobre 2006, Galerie l’Indépendance - Parc Heintz] = from 1 June to 27 October 2006. Luxembourg: Dexia-BIL, 2006. Print.\"}\" data-sheets-userformat=\"{\"2\":15107,\"3\":{\"1\":0},\"4\":{\"1\":2,\"2\":16777215},\"11\":4,\"12\":0,\"14\":{\"1\":2,\"2\":3815994},\"15\":\"\\\"Source Sans Pro\\\", \\\"Helvetica Neue\\\", Helvetica, Arial, sans-serif\",\"16\":11}\">Kremer, Boris, and Alex Reding. <em>My home is my castle : exposition d’art contemporain, du 1er juin au 27 octobre 2006, Galerie l’Indépendance - Parc Heintz</em>. Luxembourg: Dexia-BIL, 2006. Print.</span></p>";
        String expectHtmlCleanted =
                "<p><span>Kremer, Boris, and Alex Reding. <em>My home is my castle : exposition d’art contemporain, du 1er juin au 27 octobre 2006, Galerie l’Indépendance - Parc Heintz</em>. Luxembourg: Dexia-BIL, 2006. Print.</span></p>";

        String htmlCleaned = Jsoup.clean(html, Safelist.relaxed());

        assertEquals(expectHtmlCleanted, htmlCleaned);

    }

    /**
     * @verifies throw IllegalArgumentException if s is null
     */
    @Test
    void findBestMatch_shouldThrowIllegalArgumentExceptionIfSIsNull() throws Exception {
        Exception e = Assertions.assertThrows(IllegalArgumentException.class, () -> StringTools.findBestMatch(null, Collections.emptyList(), "en"));
        assertEquals("s may not be null", e.getMessage());
    }

    /**
     * @see StringTools#findBestMatch(String,List<String>,Locale)
     * @verifies throw IllegalArgumentException if candidates is null
     */
    @Test
    void findBestMatch_shouldThrowIllegalArgumentExceptionIfCandidatesIsNull() throws Exception {
        Exception e = Assertions.assertThrows(IllegalArgumentException.class, () -> StringTools.findBestMatch("foo", null, "en"));
        assertEquals("candidates may not be null", e.getMessage());
    }

    /**
     * @see StringTools#findBestMatch(String,List<String>,Locale)
     * @verifies return best match
     */
    @Test
    void findBestMatch_shouldReturnBestMatch() throws Exception {
        assertEquals("amet, sit, ipsum!",
                StringTools.findBestMatch("Lorem ipsum dolor sit amet", Arrays.asList("foo", "foo lorem", "amet, sit, ipsum!"), "en"));
    }

    /**
     * @verifies return null if no matches found
     */
    @Test
    void findBestMatch_shouldReturnNullIfNoMatchesFound() throws Exception {
        Assertions.assertNull(StringTools.findBestMatch("Lorem ipsum dolor sit amet", Arrays.asList(";)", "", "zyx"), "en"));
    }

    /**
     * @verifies return abcde for given input
     * @see StringTools#truncateText(String, int)
     */
    @Test
    void truncateText_shouldReturnAbcdeForGivenInput() {
        Assertions.assertEquals("abcde", StringTools.truncateText("abcde", 5));
        Assertions.assertEquals("a...", StringTools.truncateText("abcde", 4));
        Assertions.assertEquals("abc...", StringTools.truncateText("abc def gh", 6));
        Assertions.assertEquals("abc def...", StringTools.truncateText("abc def ghij", 10));
    }
    
    /**
     * @see StringTools#replaceLast(String, String, String)
     * @verifies return original text if target not found
     */
    @Test
    void replaceLast_shouldReturnOriginalTextIfTargetNotFound() {
        assertEquals("lorem ipsum dolor sit amet", StringTools.replaceLast("lorem ipsum dolor sit amet", "foo", "bar"));
    }
    
    /**
     * @see StringTools#replaceLast(String, String, String)
     * @verifies replace the last occurrence of target substring with replacement
     */
    @Test
    void replaceLast_shouldReplaceTheLastOccurrenceOfTargetSubstringWithReplacement() {
        assertEquals("lorem ipsum dolor stand amet", StringTools.replaceLast("lorem ipsum dolor sit amet", "sit", "stand"));
    }

    /**
     * @see StringTools#sanitizeFilenameToAscii(String)
     * @verifies return null for null input
     */
    @Test
    void sanitizeFilenameToAscii_shouldReturnNullForNullInput() {
        Assertions.assertNull(StringTools.sanitizeFilenameToAscii(null));
    }

    /**
     * @see StringTools#sanitizeFilenameToAscii(String)
     * @verifies preserve ascii filenames unchanged
     */
    @Test
    void sanitizeFilenameToAscii_shouldPreserveAsciiFilenamesUnchanged() {
        assertEquals("Screenshot 2026-02-18 ZLB.png", StringTools.sanitizeFilenameToAscii("Screenshot 2026-02-18 ZLB.png"));
    }

    /**
     * @see StringTools#sanitizeFilenameToAscii(String)
     * @verifies replace en dash with hyphen
     */
    @Test
    void sanitizeFilenameToAscii_shouldReplaceEnDashWithHyphen() {
        // En-dash (U+2013) in filename causes Tomcat to reject the Content-Location header
        assertEquals("Crawl Workflows - ZLB.png", StringTools.sanitizeFilenameToAscii("Crawl Workflows \u2013 ZLB.png"));
    }

    /**
     * @see StringTools#sanitizeFilenameToAscii(String)
     * @verifies strip combining diacritical marks
     */
    @Test
    void sanitizeFilenameToAscii_shouldStripCombiningDiacriticalMarks() {
        assertEquals("Uber die Alpen.png", StringTools.sanitizeFilenameToAscii("\u00dcber die Alpen.png"));
    }

    /**
     * @see StringTools#sanitizeFilenameToAscii(String)
     * @verifies collapse consecutive hyphens
     */
    @Test
    void sanitizeFilenameToAscii_shouldCollapseConsecutiveHyphens() {
        // Two non-ASCII characters adjacent produce two hyphens which are collapsed to one
        assertEquals("a-b.png", StringTools.sanitizeFilenameToAscii("a\u2013\u2014b.png"));
    }

    /**
     * @see StringTools#parseIntRange(String)
     * @verifies return range from 0 to n for integer input
     */
    @Test
    void parseIntRange_shouldReturnRangeFrom0ToNForIntegerInput() {
        assertEquals(IntegerRange.of(0, 5), StringTools.parseIntRange("5"));
    }

    /**
     * @see StringTools#parseIntRange(String)
     * @verifies return correct range for closed interval notation
     */
    @Test
    void parseIntRange_shouldReturnCorrectRangeForClosedIntervalNotation() {
        assertEquals(IntegerRange.of(1, 5), StringTools.parseIntRange("[1,5]"));
    }

    /**
     * @see StringTools#parseIntRange(String)
     * @verifies return correct range for open interval notation
     */
    @Test
    void parseIntRange_shouldReturnCorrectRangeForOpenIntervalNotation() {
        assertEquals(IntegerRange.of(2, 4), StringTools.parseIntRange("(1,5)"));
    }

    /**
     * @see StringTools#parseIntRange(String)
     * @verifies return correct range for half open interval notation
     */
    @Test
    void parseIntRange_shouldReturnCorrectRangeForHalfOpenIntervalNotation() {
        assertEquals(IntegerRange.of(1, 4), StringTools.parseIntRange("[1,5)"));
        assertEquals(IntegerRange.of(2, 5), StringTools.parseIntRange("(1,5]"));
    }

    /**
     * @see StringTools#parseIntRange(String)
     * @verifies throw IllegalArgumentException for invalid input
     */
    @Test
    void parseIntRange_shouldThrowIllegalArgumentExceptionForInvalidInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> StringTools.parseIntRange("abc"));
    }
}
