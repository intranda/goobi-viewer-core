package io.goobi.viewer.model.log;

import java.io.IOException;

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

    /**
     * @see LogViewerManager#broadcastParsed(LogFile, String)
     * @verifies send exactly one message per open session and skip closed sessions
     */
    @Test
    void broadcastParsed_shouldSendExactlyOneMessagePerOpenSessionAndSkipClosedSessions() throws Exception {
        Session open = Mockito.mock(Session.class);
        Session closed = Mockito.mock(Session.class);
        RemoteEndpoint.Basic openRemote = Mockito.mock(RemoteEndpoint.Basic.class);
        Mockito.when(open.isOpen()).thenReturn(true);
        Mockito.when(open.getBasicRemote()).thenReturn(openRemote);
        Mockito.when(closed.isOpen()).thenReturn(false);

        manager.registerSession(LogFile.VIEWER, open);
        manager.registerSession(LogFile.VIEWER, closed);

        manager.broadcastParsed(LogFile.VIEWER,
            "ERROR 2026-03-26 11:05:08.562 [main] io.goobi.viewer.Foo.bar(Foo.java:10) - test line");

        // Exactly one sendText call per open session (all entries bundled in one JSON array)
        Mockito.verify(openRemote, Mockito.times(1)).sendText(Mockito.anyString());
    }

    /**
     * @see LogViewerManager#broadcastParsed(LogFile, String)
     * @verifies send multiple entries of one flush as a single JSON array message
     */
    @Test
    void broadcastParsed_shouldSendMultipleEntriesOfOneFlushAsSingleJsonArrayMessage() throws Exception {
        // Regression test for IllegalStateException: TEXT_FULL_WRITING.
        // Multiple log entries in one flush must result in exactly ONE sendText() call per session.
        Session session = Mockito.mock(Session.class);
        RemoteEndpoint.Basic remote = Mockito.mock(RemoteEndpoint.Basic.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        Mockito.when(session.getBasicRemote()).thenReturn(remote);

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

    /**
     * @see LogViewerManager#broadcastParsed(LogFile, String)
     * @verifies not send and not throw when raw block is empty or null
     */
    @Test
    void broadcastParsed_shouldNotSendAndNotThrowWhenRawBlockIsEmptyOrNull() throws Exception {
        Session session = Mockito.mock(Session.class);
        RemoteEndpoint.Basic remote = Mockito.mock(RemoteEndpoint.Basic.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        Mockito.when(session.getBasicRemote()).thenReturn(remote);

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

    /**
     * @see LogViewerManager#broadcastParsed(LogFile, String)
     * @verifies use synchronous basic remote and never use async remote across consecutive broadcasts
     */
    @Test
    void broadcastParsed_shouldUseBasicRemoteAndNeverAsyncRemoteAcrossConsecutiveBroadcasts() throws Exception {
        // API guard: consecutive flush cycles must each send via the blocking basic remote and never
        // via getAsyncRemote(). The async remote returns before the write completes and applies no
        // back-pressure, which caused TEXT_FULL_WRITING when a second send started before the first
        // finished. (This cannot reproduce the exception with a mock; it guards the API choice.)
        Session session = Mockito.mock(Session.class);
        RemoteEndpoint.Basic remote = Mockito.mock(RemoteEndpoint.Basic.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        Mockito.when(session.getBasicRemote()).thenReturn(remote);

        manager.registerSession(LogFile.VIEWER, session);

        manager.broadcastParsed(LogFile.VIEWER,
            "ERROR 2026-03-26 11:05:08.562 [main] io.goobi.viewer.Foo.bar(Foo.java:1) - first");
        manager.broadcastParsed(LogFile.VIEWER,
            "WARN  2026-03-26 11:05:09.000 [main] io.goobi.viewer.Bar.baz(Bar.java:2) - second");

        Mockito.verify(remote, Mockito.times(2)).sendText(Mockito.anyString());
        Mockito.verify(session, Mockito.never()).getAsyncRemote();
    }

    /**
     * @see LogViewerManager#broadcastParsed(LogFile, String)
     * @verifies keep delivering to remaining sessions when one session send fails
     */
    @Test
    void broadcastParsed_shouldKeepDeliveringToRemainingSessionsWhenOneSessionSendFails() throws Exception {
        Session failing = Mockito.mock(Session.class);
        Session healthy = Mockito.mock(Session.class);
        RemoteEndpoint.Basic failingRemote = Mockito.mock(RemoteEndpoint.Basic.class);
        RemoteEndpoint.Basic healthyRemote = Mockito.mock(RemoteEndpoint.Basic.class);
        Mockito.when(failing.isOpen()).thenReturn(true);
        Mockito.when(healthy.isOpen()).thenReturn(true);
        Mockito.when(failing.getBasicRemote()).thenReturn(failingRemote);
        Mockito.when(healthy.getBasicRemote()).thenReturn(healthyRemote);
        // The failing session throws on send; delivery to the healthy session must still happen.
        Mockito.doThrow(new IOException("boom")).when(failingRemote).sendText(Mockito.anyString());

        manager.registerSession(LogFile.VIEWER, failing);
        manager.registerSession(LogFile.VIEWER, healthy);

        assertDoesNotThrow(() -> manager.broadcastParsed(LogFile.VIEWER,
            "ERROR 2026-03-26 11:05:08.562 [main] io.goobi.viewer.Foo.bar(Foo.java:1) - line"));

        // The healthy session received its message despite the other session failing.
        Mockito.verify(healthyRemote, Mockito.times(1)).sendText(Mockito.anyString());

        // The failing session was dropped: a second broadcast must not attempt to send to it again.
        manager.broadcastParsed(LogFile.VIEWER,
            "WARN  2026-03-26 11:05:09.000 [main] io.goobi.viewer.Bar.baz(Bar.java:2) - line2");
        Mockito.verify(failingRemote, Mockito.times(1)).sendText(Mockito.anyString());
        Mockito.verify(healthyRemote, Mockito.times(2)).sendText(Mockito.anyString());
    }
}
