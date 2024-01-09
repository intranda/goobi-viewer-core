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
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringToolsTest {

    /**
     * @see StringTools#escapeHtmlChars(String)
     * @verifies escape all characters correctly
     */
    @Test
    void escapeHtmlChars_shouldEscapeAllCharactersCorrectly() throws Exception {
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
     * @verifies remove diacritical marks correctly
     */
    @Test
    void removeDiacriticalMarks_shouldRemoveDiacriticalMarksCorrectly() throws Exception {
        assertEquals("aaaaoooouuuueeeeßn", StringTools.removeDiacriticalMarks("äáàâöóòôüúùûëéèêßñ"));
    }

    /**
     * @see StringTools#removeLineBreaks(String,String)
     * @verifies remove line breaks correctly
     */
    @Test
    void removeLineBreaks_shouldRemoveLineBreaksCorrectly() throws Exception {
        assertEquals("foobar", StringTools.removeLineBreaks("foo\r\nbar", ""));
    }

    /**
     * @see StringTools#removeLineBreaks(String,String)
     * @verifies remove html line breaks correctly
     */
    @Test
    void removeLineBreaks_shouldRemoveHtmlLineBreaksCorrectly() throws Exception {
        assertEquals("foo bar", StringTools.removeLineBreaks("foo<br>bar", " "));
        assertEquals("foo bar", StringTools.removeLineBreaks("foo<br/>bar", " "));
        assertEquals("foo bar", StringTools.removeLineBreaks("foo<br />bar", " "));
    }

    /**
     * @see StringTools#stripJS(String)
     * @verifies remove JS blocks correctly
     */
    @Test
    void stripJS_shouldRemoveJSBlocksCorrectly() throws Exception {
        assertEquals("foo  bar", StringTools.stripJS("foo <script type=\"javascript\">\nfunction f {\n alert();\n}\n</script> bar"));
        assertEquals("foo  bar", StringTools.stripJS("foo <SCRIPT>\nfunction f {\n alert();\n}\n</ScRiPt> bar"));
        assertEquals("foo  bar", StringTools.stripJS("foo <SCRIPT src=\"http://dangerousscript.js\"/> bar"));
        assertEquals("foo  bar", StringTools.stripJS("foo <svG onLoad=alert(\"Hello_XSS_World\")></svG> bar"));
        assertEquals("foo  bar", StringTools.stripJS("foo <svG onLoad=alert(\"Hello_XSS_World\")/> bar"));
    }

    /**
     * @see StringTools#stripPatternBreakingChars(String)
     * @verifies remove chars correctly
     */
    @Test
    void stripPatternBreakingChars_shouldRemoveCharsCorrectly() throws Exception {
        assertEquals("foo_bar__", StringTools.stripPatternBreakingChars("foo\tbar\r\n"));
    }

    @Test
    void testEscapeQuotes() {
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
     * @see StringTools#renameIncompatibleCSSClasses(String)
     * @verifies rename classes correctly
     */
    @Test
    void renameIncompatibleCSSClasses_shouldRenameClassesCorrectly() throws Exception {
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
     * @verifies create list correctly
     */
    @Test
    void getHierarchyForCollection_shouldCreateListCorrectly() throws Exception {
        List<String> result = StringTools.getHierarchyForCollection("a.b.c.d", ".");
        assertEquals(4, result.size());
        assertEquals("a", result.get(0));
        assertEquals("a.b", result.get(1));
        assertEquals("a.b.c", result.get(2));
        assertEquals("a.b.c.d", result.get(3));
    }

    /**
     * @see StringTools#getHierarchyForCollection(String,String)
     * @verifies return single value correctly
     */
    @Test
    void getHierarchyForCollection_shouldReturnSingleValueCorrectly() throws Exception {
        List<String> result = StringTools.getHierarchyForCollection("a", ".");
        assertEquals(1, result.size());
        assertEquals("a", result.get(0));
    }

    /**
     * @see StringTools#normalizeWebAnnotationCoordinates(String)
     * @verifies normalize coordinates correctly
     */
    @Test
    void normalizeWebAnnotationCoordinates_shouldNormalizeCoordinatesCorrectly() throws Exception {
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
     * @verifies hash string correctly
     */
    @Test
    void generateHash_shouldHashStringCorrectly() throws Exception {
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
     * @verifies return true if value starts with #1;
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
     * @see StringTools#escapeCriticalUrlChracters(String,boolean)
     * @verifies replace characters correctly
     */
    @Test
    void escapeCriticalUrlChracters_shouldReplaceCharactersCorrectly() throws Exception {
        assertEquals("U002BAU002FU005CU007CU003FZ", StringTools.escapeCriticalUrlChracters("+A/\\|?Z", false));
        assertEquals("U007C", StringTools.escapeCriticalUrlChracters("%7C", true));
    }

    /**
     * @see StringTools#unescapeCriticalUrlChracters(String)
     * @verifies replace characters correctly
     */
    @Test
    void unescapeCriticalUrlChracters_shouldReplaceCharactersCorrectly() throws Exception {
        assertEquals("+A/\\|?Z", StringTools.unescapeCriticalUrlChracters("U002BAU002FU005CU007CU003FZ"));
    }

    @Test
    void testSortByList() {
        List<String> sorting = List.of("c", "d", "e", "f", "g", "a", "h");

        List<String> s1 = List.of("a", "b", "c", "d");
        List<String> s1Sorted = new ArrayList<>(s1);
        s1Sorted.sort((k, l) -> StringTools.sortByList(k, l, sorting));
        assertEquals("c", s1Sorted.get(0));
        assertEquals("d", s1Sorted.get(1));
        assertEquals("a", s1Sorted.get(2));
        assertEquals("b", s1Sorted.get(3));
    }
    
    @Test
    void testCleanHtml() {
        
        String html = "<p><script>alert('SPAM')</script><span data-sheets-value=\"{\"1\":2,\"2\":\"Kremer, Boris, and Alex Reding. My home is my castle : exposition d’art contemporain, du 1er juin au 27 octobre 2006, Galerie l’Indépendance - Parc Heintz] = from 1 June to 27 October 2006. Luxembourg: Dexia-BIL, 2006. Print.\"}\" data-sheets-userformat=\"{\"2\":15107,\"3\":{\"1\":0},\"4\":{\"1\":2,\"2\":16777215},\"11\":4,\"12\":0,\"14\":{\"1\":2,\"2\":3815994},\"15\":\"\\\"Source Sans Pro\\\", \\\"Helvetica Neue\\\", Helvetica, Arial, sans-serif\",\"16\":11}\">Kremer, Boris, and Alex Reding. <em>My home is my castle : exposition d’art contemporain, du 1er juin au 27 octobre 2006, Galerie l’Indépendance - Parc Heintz</em>. Luxembourg: Dexia-BIL, 2006. Print.</span></p>";
        String expectHtmlCleanted = "<p><span>Kremer, Boris, and Alex Reding. <em>My home is my castle : exposition d’art contemporain, du 1er juin au 27 octobre 2006, Galerie l’Indépendance - Parc Heintz</em>. Luxembourg: Dexia-BIL, 2006. Print.</span></p>";

            
        String htmlCleaned = Jsoup.clean(html, Safelist.relaxed());
        
        assertEquals(expectHtmlCleanted, htmlCleaned);
        
    }
}
