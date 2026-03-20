package io.goobi.viewer.model.log;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class LogFileTest {

    @Test
    void fromNameKnown() {
        assertEquals(LogFile.VIEWER, LogFile.fromName("viewer").orElseThrow());
        assertEquals(LogFile.OAI, LogFile.fromName("OAI").orElseThrow()); // case-insensitive
    }

    @Test
    void fromNameUnknown() {
        assertTrue(LogFile.fromName("../../etc/passwd").isEmpty());
        assertTrue(LogFile.fromName(null).isEmpty());
        assertTrue(LogFile.fromName("").isEmpty());
        assertTrue(LogFile.fromName("unknown").isEmpty());
    }
}
