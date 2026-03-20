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
        assertTrue(lines.get(0).message().contains("Connection refused"));
    }

    @Test
    void parseStacktraceContinuation() {
        String raw = "ERROR 2026-03-19 14:23:02.119 [main] io.goobi.viewer.Foo:10\n"
                   + "        Something failed\n"
                   + "  at io.goobi.viewer.Foo.bar(Foo.java:10)\n"
                   + "INFO  2026-03-19 14:23:03.000 [main] io.goobi.viewer.Bar:5\n"
                   + "        Next line";
        List<LogLine> lines = LogLineParser.parse(raw);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).message().contains("at io.goobi.viewer.Foo.bar"));
        assertEquals("INFO", lines.get(1).level());
    }

    @Test
    void parseEmptyInput() {
        assertEquals(0, LogLineParser.parse("").size());
        assertEquals(0, LogLineParser.parse(null).size());
    }
}
