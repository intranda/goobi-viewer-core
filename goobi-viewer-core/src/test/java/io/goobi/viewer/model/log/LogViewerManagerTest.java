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
    void broadcastSkipsFailedSessions() throws Exception {
        Session good = Mockito.mock(Session.class);
        Session bad = Mockito.mock(Session.class);
        RemoteEndpoint.Basic goodRemote = Mockito.mock(RemoteEndpoint.Basic.class);
        RemoteEndpoint.Basic badRemote = Mockito.mock(RemoteEndpoint.Basic.class);
        Mockito.when(good.getBasicRemote()).thenReturn(goodRemote);
        Mockito.when(bad.getBasicRemote()).thenReturn(badRemote);
        Mockito.doThrow(new java.io.IOException("dead")).when(badRemote).sendText(Mockito.anyString());

        manager.registerSession(LogFile.VIEWER, good);
        manager.registerSession(LogFile.VIEWER, bad);

        assertDoesNotThrow(() -> manager.broadcast(LogFile.VIEWER, "test line"));
        Mockito.verify(goodRemote).sendText(Mockito.anyString());
    }
}
