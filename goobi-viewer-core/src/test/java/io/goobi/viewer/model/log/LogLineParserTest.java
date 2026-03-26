package io.goobi.viewer.model.log;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LogLineParserTest {

    @Test
    void parseSingleLine() {
        String raw = "ERROR 2026-03-19 14:23:02.119 [main] io.goobi.viewer.solr.SolrSearchIndex:314\n"
                   + "        Connection refused";
        List<LogLine> lines = LogLineParser.parse(raw);
        assertEquals(1, lines.size());
        assertEquals("ERROR", lines.get(0).level());
        assertEquals("main", lines.get(0).thread());
        assertTrue(lines.get(0).message().contains("Connection refused"));
    }

    @Test
    void parseStacktraceContinuation() {
        String raw = "ERROR 2026-03-19 14:23:02.119 [main] io.goobi.viewer.Foo:10\n"
                   + "        Something failed\n"
                   + "  at io.goobi.viewer.Foo.bar(Foo.java:10)\n"
                   + "INFO  2026-03-19 14:23:03.000 [http-nio-8080-exec-1] io.goobi.viewer.Bar:5\n"
                   + "        Next line";
        List<LogLine> lines = LogLineParser.parse(raw);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).message().contains("at io.goobi.viewer.Foo.bar"));
        assertEquals("main", lines.get(0).thread());
        assertEquals("INFO", lines.get(1).level());
        assertEquals("http-nio-8080-exec-1", lines.get(1).thread());
    }

    @Test
    void parseEmptyInput() {
        assertEquals(0, LogLineParser.parse("").size());
        assertEquals(0, LogLineParser.parse(null).size());
    }

    @Test
    void parseSingleLineNewFormat() {
        String raw = "ERROR 2026-03-26 11:05:08.562 [Thread-13] io.goobi.viewer.solr.SolrSearchIndex.search(SolrSearchIndex.java:340) - IOException occurred";
        List<LogLine> lines = LogLineParser.parse(raw);
        assertEquals(1, lines.size());
        assertEquals("ERROR", lines.get(0).level());
        assertEquals("2026-03-26 11:05:08.562", lines.get(0).timestamp());
        assertEquals("Thread-13", lines.get(0).thread());
        assertEquals("io.goobi.viewer.solr.SolrSearchIndex.search(SolrSearchIndex.java:340)", lines.get(0).location());
        assertEquals("IOException occurred", lines.get(0).message());
    }

    @Test
    void parseSingleLineNewFormatWithStacktrace() {
        String raw = "ERROR 2026-03-26 11:05:08.562 [Thread-13] io.goobi.viewer.Foo.bar(Foo.java:10) - Something failed\n"
                   + "java.lang.NullPointerException: null\n"
                   + "    at io.goobi.viewer.Foo.bar(Foo.java:10)\n"
                   + "    at io.goobi.viewer.Baz.qux(Baz.java:20)\n"
                   + "Caused by: java.io.IOException: file not found\n"
                   + "    at java.base/java.io.FileInputStream.open(FileInputStream.java:195)\n"
                   + "INFO  2026-03-26 11:05:09.000 [main] io.goobi.viewer.Bar.init(Bar.java:5) - Starting up";
        List<LogLine> lines = LogLineParser.parse(raw);
        assertEquals(2, lines.size());
        assertEquals("ERROR", lines.get(0).level());
        assertTrue(lines.get(0).message().startsWith("Something failed"));
        assertTrue(lines.get(0).message().contains("NullPointerException"));
        assertTrue(lines.get(0).message().contains("Caused by:"));
        assertEquals("INFO", lines.get(1).level());
        assertEquals("Starting up", lines.get(1).message());
    }

    @Test
    void parseMixedOldAndNewFormat() {
        String raw = "ERROR 2026-03-26 11:05:08.562 [main] io.goobi.viewer.Foo:10\n"
                   + "        Old format message\n"
                   + "INFO  2026-03-26 11:05:09.000 [main] io.goobi.viewer.Bar.init(Bar.java:5) - New format message";
        List<LogLine> lines = LogLineParser.parse(raw);
        assertEquals(2, lines.size());
        assertEquals("Old format message", lines.get(0).message());
        assertEquals("New format message", lines.get(1).message());
    }

    @Test
    void parseNewFormatEmptyMessage() {
        String raw = "DEBUG 2026-03-26 11:05:08.562 [main] io.goobi.viewer.Foo.bar(Foo.java:10) - ";
        List<LogLine> lines = LogLineParser.parse(raw);
        assertEquals(1, lines.size());
        assertEquals("DEBUG", lines.get(0).level());
        assertEquals("", lines.get(0).message());
    }

    @Test
    void parseTraceLevel() {
        String raw = "TRACE 2026-03-26 10:00:00.001 [main] io.goobi.viewer.Foo.bar(Foo.java:5) - entering method";
        List<LogLine> lines = LogLineParser.parse(raw);
        assertEquals(1, lines.size());
        assertEquals("TRACE", lines.get(0).level());
        assertEquals("entering method", lines.get(0).message());
    }

    @Test
    void parseWarnLevel() {
        String raw = "WARN  2026-03-26 10:00:00.001 [main] io.goobi.viewer.Foo:10\n        something suspicious";
        List<LogLine> lines = LogLineParser.parse(raw);
        assertEquals(1, lines.size());
        assertEquals("WARN", lines.get(0).level());
    }

    @Test
    void parseLinesBeforeAnyHeader_areIgnored() {
        // Continuation lines that appear before the first header should be silently dropped
        String raw = "    orphan continuation line\n"
                   + "ERROR 2026-03-26 10:00:01.000 [main] io.goobi.viewer.Foo.bar(Foo.java:1) - actual message";
        List<LogLine> lines = LogLineParser.parse(raw);
        assertEquals(1, lines.size());
        assertEquals("ERROR", lines.get(0).level());
        assertEquals("actual message", lines.get(0).message());
    }

    @Test
    void parseWindowsLineEndings() {
        // \r\n line endings must be handled the same as \n
        String raw = "INFO  2026-03-26 10:00:01.000 [main] io.goobi.viewer.Foo.bar(Foo.java:1) - msg\r\n"
                   + "DEBUG 2026-03-26 10:00:02.000 [main] io.goobi.viewer.Bar.baz(Bar.java:2) - next";
        List<LogLine> lines = LogLineParser.parse(raw);
        assertEquals(2, lines.size());
        assertEquals("INFO", lines.get(0).level());
        assertEquals("msg", lines.get(0).message());
        assertEquals("DEBUG", lines.get(1).level());
        assertEquals("next", lines.get(1).message());
    }

    @Test
    void isHeaderLine_matchesNewFormat() {
        String header = "ERROR 2026-03-26 11:05:08.562 [Thread-13] io.goobi.viewer.Foo.bar(Foo.java:340) - message";
        assertTrue(LogLineParser.isHeaderLine(header));
    }

    @Test
    void isHeaderLine_matchesOldFormat() {
        String header = "INFO  2026-03-26 11:05:08.562 [main] io.goobi.viewer.Foo:10";
        assertTrue(LogLineParser.isHeaderLine(header));
    }

    @Test
    void isHeaderLine_doesNotMatchContinuationLine() {
        assertFalse(LogLineParser.isHeaderLine("    at io.goobi.viewer.Foo.bar(Foo.java:10)"));
        assertFalse(LogLineParser.isHeaderLine("Caused by: java.io.IOException"));
        assertFalse(LogLineParser.isHeaderLine("        continuation message text"));
    }

    @Test
    void isHeaderLine_handlesNull() {
        assertFalse(LogLineParser.isHeaderLine(null));
    }

    @Test
    void isHeaderLine_allLevels() {
        assertTrue(LogLineParser.isHeaderLine("ERROR 2026-03-26 10:00:00.000 [main] Foo:1"));
        assertTrue(LogLineParser.isHeaderLine("WARN  2026-03-26 10:00:00.000 [main] Foo:1"));
        assertTrue(LogLineParser.isHeaderLine("INFO  2026-03-26 10:00:00.000 [main] Foo:1"));
        assertTrue(LogLineParser.isHeaderLine("DEBUG 2026-03-26 10:00:00.000 [main] Foo:1"));
        assertTrue(LogLineParser.isHeaderLine("TRACE 2026-03-26 10:00:00.000 [main] Foo:1"));
    }

    @Test
    void parseWhitespaceOnlyInput() {
        assertEquals(0, LogLineParser.parse("   ").size());
        assertEquals(0, LogLineParser.parse("\n\n\n").size());
    }
}
