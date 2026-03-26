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
}
