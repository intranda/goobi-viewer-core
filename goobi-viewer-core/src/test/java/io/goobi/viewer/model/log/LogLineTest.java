package io.goobi.viewer.model.log;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogLineTest {

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
