package de.intranda.digiverso.presentation.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

public class StringToolsTest {

    /**
     * @see StringTools#escapeHtmlChars(String)
     * @verifies escape all characters correctly
     */
    @Test
    public void escapeHtmlChars_shouldEscapeAllCharactersCorrectly() throws Exception {
        Assert.assertEquals("&lt;i&gt;&quot;A&amp;B&quot;&lt;/i&gt;", StringTools.escapeHtmlChars("<i>\"A&B\"</i>"));
    }

    /**
     * @see StringTools#removeDiacriticalMarks(String)
     * @verifies remove diacritical marks correctly
     */
    @Test
    public void removeDiacriticalMarks_shouldRemoveDiacriticalMarksCorrectly() throws Exception {
        Assert.assertEquals("aaaaoooouuuueeeeßn", StringTools.removeDiacriticalMarks("äáàâöóòôüúùûëéèêßñ"));
    }

    /**
     * @see StringTools#removeLineBreaks(String,String)
     * @verifies remove line breaks correctly
     */
    @Test
    public void removeLineBreaks_shouldRemoveLineBreaksCorrectly() throws Exception {
        Assert.assertEquals("foobar", StringTools.removeLineBreaks("foo\r\nbar", ""));
    }

    /**
     * @see StringTools#removeLineBreaks(String,String)
     * @verifies remove html line breaks correctly
     */
    @Test
    public void removeLineBreaks_shouldRemoveHtmlLineBreaksCorrectly() throws Exception {
        Assert.assertEquals("foo bar", StringTools.removeLineBreaks("foo<br>bar", " "));
        Assert.assertEquals("foo bar", StringTools.removeLineBreaks("foo<br/>bar", " "));
        Assert.assertEquals("foo bar", StringTools.removeLineBreaks("foo<br />bar", " "));
    }

    /**
     * @see StringTools#stripJS(String)
     * @verifies remove JS blocks correctly
     */
    @Test
    public void stripJS_shouldRemoveJSBlocksCorrectly() throws Exception {
        Assert.assertEquals("foo  bar", StringTools.stripJS("foo <script type=\"javascript\">\nfunction f {\n alert();\n}\n</script> bar"));
        Assert.assertEquals("foo  bar", StringTools.stripJS("foo <SCRIPT>\nfunction f {\n alert();\n}\n</ScRiPt> bar"));
    }

    @Test
    public void testEscapeQuotes() {
        String original = "Das ist ein 'String' mit \"Quotes\".";
        String reference = "Das ist ein \\'String\\' mit \\\"Quotes\\\".";

        String escaped = StringTools.escapeQuotes(original);
        Assert.assertEquals(reference, escaped);

        escaped = StringTools.escapeQuotes(reference);
        Assert.assertEquals(reference, escaped);
    }

    /**
     * @see StringTools#isImageUrl(String)
     * @verifies return true for image urls
     */
    @Test
    public void isImageUrl_shouldReturnTrueForImageUrls() throws Exception {
        Assert.assertTrue(StringTools.isImageUrl("https://example.com/default.jpg"));
        Assert.assertTrue(StringTools.isImageUrl("https://example.com/MASTER.TIFF"));
    }

    /**
     * @see StringTools#convertFileToHtml(Path)
     * @verifies convert docx file correctly
     */
    @Test
    public void convertFileToHtml_shouldConvertDocxFileCorrectly() throws Exception {
        Path rtfFile = Paths.get("resources/test/data/example.docx");
        Assert.assertTrue(Files.isRegularFile(rtfFile));
        String html = StringTools.convertFileToHtml(rtfFile);
        Assert.assertNotNull(html);
        //                FileTools.getFileFromString(html, "resources/test/data/433 SR (docx).htm", Helper.DEFAULT_ENCODING, false);
    }

    /**
     * @see StringTools#convertFileToHtml(Path)
     * @verifies convert rtf file correctly
     */
    @Test
    public void convertFileToHtml_shouldConvertRtfFileCorrectly() throws Exception {
        Path rtfFile = Paths.get("resources/test/data/example.rtf");
        Assert.assertTrue(Files.isRegularFile(rtfFile));
        String html = StringTools.convertFileToHtml(rtfFile);
        Assert.assertNotNull(html);
        //                FileTools.getFileFromString(html, "resources/test/data/433 SR (rtf).htm", Helper.DEFAULT_ENCODING, false);
    }

}