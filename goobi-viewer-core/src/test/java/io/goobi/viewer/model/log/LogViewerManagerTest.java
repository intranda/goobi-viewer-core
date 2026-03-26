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
        Mockito.verify(openRemote, Mockito.atLeastOnce()).sendText(Mockito.anyString());
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
}
