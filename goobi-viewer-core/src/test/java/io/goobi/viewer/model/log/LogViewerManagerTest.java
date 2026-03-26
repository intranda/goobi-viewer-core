package io.goobi.viewer.model.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.websocket.Session;
import jakarta.websocket.RemoteEndpoint;
import static org.junit.jupiter.api.Assertions.*;

class LogViewerManagerTest {

    private LogViewerManager manager;

    @BeforeEach
    void setUp() {
        manager = new LogViewerManager();
    }

    @Test
    void registerAndUnregisterSession() {
        Session session = Mockito.mock(Session.class);
        manager.registerSession(LogFile.VIEWER, session);
        assertTrue(manager.hasActiveSessions(LogFile.VIEWER));

        manager.unregisterSession(LogFile.VIEWER, session);
        assertFalse(manager.hasActiveSessions(LogFile.VIEWER));
    }

    @Test
    void broadcastSkipsClosedSessions() throws Exception {
        Session open = Mockito.mock(Session.class);
        Session closed = Mockito.mock(Session.class);
        RemoteEndpoint.Async openRemote = Mockito.mock(RemoteEndpoint.Async.class);
        Mockito.when(open.isOpen()).thenReturn(true);
        Mockito.when(open.getAsyncRemote()).thenReturn(openRemote);
        Mockito.when(closed.isOpen()).thenReturn(false);

        manager.registerSession(LogFile.VIEWER, open);
        manager.registerSession(LogFile.VIEWER, closed);

        manager.broadcastParsed(LogFile.VIEWER,
            "ERROR 2026-03-26 11:05:08.562 [main] io.goobi.viewer.Foo.bar(Foo.java:10) - test line");

        // Exactly one sendText call per session (all entries bundled in one JSON array)
        Mockito.verify(openRemote, Mockito.times(1)).sendText(Mockito.anyString());
    }

    @Test
    void broadcastParsed_multipleEntries_sentAsSingleArrayMessage() throws Exception {
        // Regression test for IllegalStateException: TEXT_FULL_WRITING.
        // Multiple log entries in one flush must result in exactly ONE sendText() call per session,
        // not one call per entry — the WebSocket async endpoint rejects concurrent sends.
        Session session = Mockito.mock(Session.class);
        RemoteEndpoint.Async remote = Mockito.mock(RemoteEndpoint.Async.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        Mockito.when(session.getAsyncRemote()).thenReturn(remote);

        manager.registerSession(LogFile.VIEWER, session);

        String threeEntries =
            "ERROR 2026-03-26 11:05:08.562 [main] io.goobi.viewer.Foo.bar(Foo.java:1) - first\n"
            + "WARN  2026-03-26 11:05:09.000 [main] io.goobi.viewer.Bar.baz(Bar.java:2) - second\n"
            + "INFO  2026-03-26 11:05:10.000 [main] io.goobi.viewer.Baz.qux(Baz.java:3) - third";

        manager.broadcastParsed(LogFile.VIEWER, threeEntries);

        // Must be called exactly once — not three times
        Mockito.verify(remote, Mockito.times(1)).sendText(Mockito.anyString());

        // The payload must be a JSON array
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        Mockito.verify(remote).sendText(captor.capture());
        String payload = captor.getValue();
        assertTrue(payload.startsWith("["), "Payload must be a JSON array: " + payload);
        assertTrue(payload.endsWith("]"), "Payload must be a JSON array: " + payload);
        assertTrue(payload.contains("\"level\":\"ERROR\""));
        assertTrue(payload.contains("\"level\":\"WARN\""));
        assertTrue(payload.contains("\"level\":\"INFO\""));
    }

    @Test
    void unregisterLastSession_removesFromActiveSessions() {
        Session s1 = Mockito.mock(Session.class);
        Session s2 = Mockito.mock(Session.class);

        manager.registerSession(LogFile.VIEWER, s1);
        manager.registerSession(LogFile.VIEWER, s2);
        assertTrue(manager.hasActiveSessions(LogFile.VIEWER));

        manager.unregisterSession(LogFile.VIEWER, s1);
        assertTrue(manager.hasActiveSessions(LogFile.VIEWER));

        manager.unregisterSession(LogFile.VIEWER, s2);
        assertFalse(manager.hasActiveSessions(LogFile.VIEWER));
    }

    @Test
    void shutdown_clearsSessions() {
        Session session = Mockito.mock(Session.class);
        manager.registerSession(LogFile.VIEWER, session);
        assertTrue(manager.hasActiveSessions(LogFile.VIEWER));

        manager.shutdown();

        assertFalse(manager.hasActiveSessions(LogFile.VIEWER));
    }

    @Test
    void broadcastParsed_noSessionsRegistered_doesNotThrow() {
        // Must not throw even when no sessions exist for the log file
        assertDoesNotThrow(() ->
            manager.broadcastParsed(LogFile.OAI,
                "INFO  2026-03-26 10:00:00.000 [main] Foo.bar(Foo.java:1) - message"));
    }

    @Test
    void broadcastParsed_emptyRaw_doesNotThrow() {
        Session session = Mockito.mock(Session.class);
        RemoteEndpoint.Async remote = Mockito.mock(RemoteEndpoint.Async.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        Mockito.when(session.getAsyncRemote()).thenReturn(remote);

        manager.registerSession(LogFile.VIEWER, session);

        // Empty raw text must not throw and must not invoke sendText
        assertDoesNotThrow(() -> manager.broadcastParsed(LogFile.VIEWER, ""));
        assertDoesNotThrow(() -> manager.broadcastParsed(LogFile.VIEWER, null));
        Mockito.verify(remote, Mockito.never()).sendText(Mockito.anyString());
    }

    @Test
    void broadcastParsed_closedSessionIsRemovedFromSet() {
        Session closed = Mockito.mock(Session.class);
        Mockito.when(closed.isOpen()).thenReturn(false);

        manager.registerSession(LogFile.VIEWER, closed);
        assertTrue(manager.hasActiveSessions(LogFile.VIEWER));

        manager.broadcastParsed(LogFile.VIEWER,
            "WARN  2026-03-26 10:00:00.000 [main] Foo.bar(Foo.java:1) - cleanup check");

        // After broadcast the closed session should have been pruned
        assertFalse(manager.hasActiveSessions(LogFile.VIEWER));
    }

    @Test
    void differentLogFiles_haveIndependentSessions() {
        Session viewerSession = Mockito.mock(Session.class);
        Session oaiSession = Mockito.mock(Session.class);

        manager.registerSession(LogFile.VIEWER, viewerSession);
        manager.registerSession(LogFile.OAI, oaiSession);

        assertTrue(manager.hasActiveSessions(LogFile.VIEWER));
        assertTrue(manager.hasActiveSessions(LogFile.OAI));

        manager.unregisterSession(LogFile.VIEWER, viewerSession);

        assertFalse(manager.hasActiveSessions(LogFile.VIEWER));
        assertTrue(manager.hasActiveSessions(LogFile.OAI));
    }
}
