package de.intranda.digiverso.presentation.controller;

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
}