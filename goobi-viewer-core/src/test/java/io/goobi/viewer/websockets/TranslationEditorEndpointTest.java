package io.goobi.viewer.websockets;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.managedbeans.AdminBean;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

class TranslationEditorEndpointTest extends AbstractTest {

    @BeforeEach
    @AfterEach
    void resetLock() {
        AdminBean.setTranslationGroupsEditorSession(null);
    }

    @Test
    void onClose_shouldCallUnlockTranslationForStoredSession() {
        AdminBean.setTranslationGroupsEditorSession("session-123");

        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(httpSession.getId()).thenReturn("session-123");

        EndpointConfig config = Mockito.mock(EndpointConfig.class);
        Mockito.when(config.getUserProperties())
               .thenReturn(Map.of(HttpSession.class.getName(), httpSession));

        Session wsSession = Mockito.mock(Session.class);

        TranslationEditorEndpoint endpoint = new TranslationEditorEndpoint();
        endpoint.onOpen(wsSession, config);
        endpoint.onClose(wsSession);

        assertNull(AdminBean.getTranslationGroupsEditorSession(),
                "onClose should release translation lock");
    }

    @Test
    void onClose_shouldNotFailWhenNoSessionPresent() {
        EndpointConfig config = Mockito.mock(EndpointConfig.class);
        Mockito.when(config.getUserProperties()).thenReturn(Map.of());

        Session wsSession = Mockito.mock(Session.class);

        TranslationEditorEndpoint endpoint = new TranslationEditorEndpoint();
        endpoint.onOpen(wsSession, config);
        assertDoesNotThrow(() -> endpoint.onClose(wsSession));
    }
}
