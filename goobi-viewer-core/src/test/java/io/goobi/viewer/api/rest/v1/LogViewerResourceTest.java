package io.goobi.viewer.api.rest.v1;

import io.goobi.viewer.model.log.LogLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LogViewerResourceTest {

    private static String logLine(int i) {
        return "ERROR 2026-03-19 14:23:0" + (i % 10) + ".000 [main] Foo:1\n        line " + i + "\n";
    }

    @Test
    void readLastNLines_returnsAtMostN(@TempDir Path tmp) throws Exception {
        Path log = tmp.resolve("test.log");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 600; i++) sb.append(logLine(i));
        Files.writeString(log, sb.toString());

        List<LogLine> lines = LogViewerResource.readLastNLines(log, 500);
        assertTrue(lines.size() <= 500);
        assertFalse(lines.isEmpty());
    }

    @Test
    void readFromOffset_returnsOnlyNewLines(@TempDir Path tmp) throws Exception {
        Path log = tmp.resolve("test.log");
        Files.writeString(log, "ERROR 2026-03-19 14:23:01.000 [main] Foo:1\n        first\n");
        long offset = Files.size(log);
        Files.writeString(log, "WARN  2026-03-19 14:23:02.000 [main] Bar:2\n        second\n",
            StandardOpenOption.APPEND);

        List<LogLine> lines = LogViewerResource.readFromOffset(log, offset);
        assertEquals(1, lines.size());
        assertEquals("WARN", lines.get(0).level());
    }

    @Test
    void readFromOffset_missingFile_returnsEmpty(@TempDir Path tmp) throws Exception {
        List<LogLine> lines = LogViewerResource.readFromOffset(tmp.resolve("missing.log"), 0);
        assertTrue(lines.isEmpty());
    }

    @Test
    void readFromOffset_offsetBeyondFileSize_returnsEmpty(@TempDir Path tmp) throws Exception {
        Path log = tmp.resolve("test.log");
        Files.writeString(log, "ERROR 2026-03-19 14:23:01.000 [main] Foo:1\n        first\n");
        long beyondEnd = Files.size(log) + 1000;

        List<LogLine> lines = LogViewerResource.readFromOffset(log, beyondEnd);
        assertTrue(lines.isEmpty());
    }

    @Test
    void readLastNLines_fewerEntriesThanRequested(@TempDir Path tmp) throws Exception {
        Path log = tmp.resolve("test.log");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(logLine(i));
        Files.writeString(log, sb.toString());

        List<LogLine> lines = LogViewerResource.readLastNLines(log, 500);
        assertEquals(10, lines.size());
    }

    @Test
    void readLastNLines_exactlyNEntries(@TempDir Path tmp) throws Exception {
        Path log = tmp.resolve("test.log");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) sb.append(logLine(i));
        Files.writeString(log, sb.toString());

        List<LogLine> lines = LogViewerResource.readLastNLines(log, 50);
        assertEquals(50, lines.size());
    }

    @Test
    void readLastNLines_newFormatEntries(@TempDir Path tmp) throws Exception {
        // Verify readLastNLines works with the new single-line log format
        Path log = tmp.resolve("test.log");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("INFO  2026-03-26 10:00:0").append(i % 10)
              .append(".000 [main] io.goobi.viewer.Foo.bar(Foo.java:").append(i)
              .append(") - message ").append(i).append("\n");
        }
        Files.writeString(log, sb.toString());

        List<LogLine> lines = LogViewerResource.readLastNLines(log, 10);
        assertEquals(10, lines.size());
        // Should return the last 10 entries
        assertEquals("message 19", lines.get(9).message());
    }

    @Test
    void readFromOffset_zeroOffset_returnsAllEntries(@TempDir Path tmp) throws Exception {
        Path log = tmp.resolve("test.log");
        Files.writeString(log,
            "ERROR 2026-03-19 14:23:01.000 [main] Foo:1\n        first\n"
            + "WARN  2026-03-19 14:23:02.000 [main] Bar:2\n        second\n"
            + "INFO  2026-03-19 14:23:03.000 [main] Baz:3\n        third\n");

        List<LogLine> lines = LogViewerResource.readFromOffset(log, 0L);
        assertEquals(3, lines.size());
        assertEquals("ERROR", lines.get(0).level());
        assertEquals("INFO", lines.get(2).level());
    }
}
