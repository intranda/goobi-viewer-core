package io.goobi.viewer.model.log;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogLineTest {

    @Test
    void toJson_allFieldsPresentWithCorrectKeys() {
        LogLine line = new LogLine("2026-03-26 12:00:00.000", "INFO", "main", "Foo:1", "hello");

        String json = line.toJson();

        assertTrue(json.startsWith("{") && json.endsWith("}"));
        assertTrue(json.contains("\"timestamp\":"));
        assertTrue(json.contains("\"level\":"));
        assertTrue(json.contains("\"thread\":"));
        assertTrue(json.contains("\"location\":"));
        assertTrue(json.contains("\"message\":"));
        assertTrue(json.contains("\"timestamp\":\"2026-03-26 12:00:00.000\""));
        assertTrue(json.contains("\"level\":\"INFO\""));
        assertTrue(json.contains("\"thread\":\"main\""));
        assertTrue(json.contains("\"location\":\"Foo:1\""));
        assertTrue(json.contains("\"message\":\"hello\""));
    }

    @Test
    void toJson_controlCharactersEscapedToUnicode() {
        // Characters below 0x20 that are not \n \r \t \b \f must become \\uNNNN unicode escapes
        LogLine line = new LogLine(null, null, null, null, "before\u0001after\u001Fend");

        String json = line.toJson();

        // Raw control chars must not appear in JSON output
        assertFalse(json.contains("\u0001"), "SOH must not appear unescaped");
        assertFalse(json.contains("\u001F"), "US must not appear unescaped");
        assertTrue(json.contains("\\u0001"));
        assertTrue(json.contains("\\u001f"));
        // Surrounding text must be preserved
        assertTrue(json.contains("before"));
        assertTrue(json.contains("after"));
        assertTrue(json.contains("end"));
    }

    @Test
    void toJson_specialCharacters() {
        LogLine line = new LogLine(
            "2026-03-26 12:00:00.000", "ERROR", "main",
            "Foo:1", "Message with \"quotes\" and \\backslash\nand newline and </script><img>");

        String json = line.toJson();

        assertTrue(json.contains("\\\"quotes\\\""));
        assertTrue(json.contains("\\\\backslash"));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("</script>"));
        assertFalse(json.contains("\n"));
        assertTrue(json.startsWith("{") && json.endsWith("}"));
    }

    @Test
    void toJson_nullFields() {
        LogLine line = new LogLine(null, null, null, null, null);

        String json = line.toJson();

        assertTrue(json.contains("\"timestamp\":\"\""));
        assertTrue(json.contains("\"level\":\"\""));
        assertTrue(json.contains("\"thread\":\"\""));
        assertTrue(json.contains("\"location\":\"\""));
        assertTrue(json.contains("\"message\":\"\""));
    }
}
