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
    void fromNameAllValues() {
        // All four enum constants must be reachable by their symbolic name
        assertEquals(LogFile.VIEWER, LogFile.fromName("viewer").orElseThrow());
        assertEquals(LogFile.OAI, LogFile.fromName("oai").orElseThrow());
        assertEquals(LogFile.ICS, LogFile.fromName("ics").orElseThrow());
        assertEquals(LogFile.INDEXER, LogFile.fromName("indexer").orElseThrow());
    }

    @Test
    void fromNameCaseInsensitive() {
        assertEquals(LogFile.VIEWER, LogFile.fromName("VIEWER").orElseThrow());
        assertEquals(LogFile.ICS, LogFile.fromName("ICS").orElseThrow());
        assertEquals(LogFile.INDEXER, LogFile.fromName("INDEXER").orElseThrow());
    }

    @Test
    void fromNameUnknown() {
        assertTrue(LogFile.fromName("../../etc/passwd").isEmpty());
        assertTrue(LogFile.fromName(null).isEmpty());
        assertTrue(LogFile.fromName("").isEmpty());
        assertTrue(LogFile.fromName("unknown").isEmpty());
    }

    @Test
    void fromName_blankWhitespace_returnsEmpty() {
        assertTrue(LogFile.fromName("   ").isEmpty());
        assertTrue(LogFile.fromName("\t").isEmpty());
    }

    @Test
    void getName_returnsCorrectLowercaseName() {
        assertEquals("viewer", LogFile.VIEWER.getName());
        assertEquals("oai", LogFile.OAI.getName());
        assertEquals("ics", LogFile.ICS.getName());
        assertEquals("indexer", LogFile.INDEXER.getName());
    }
}
