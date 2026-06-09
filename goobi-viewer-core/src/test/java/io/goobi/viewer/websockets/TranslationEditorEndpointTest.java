package io.goobi.viewer.websockets;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.managedbeans.AdminBean;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.model.security.user.User;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

class TranslationEditorEndpointTest extends AbstractTest {

    @BeforeEach
    @AfterEach
    void resetLock() {
        AdminBean.setTranslationGroupsEditorSession(null);
    }

    /**
     * Builds an EndpointConfig whose HTTP session carries a UserBean returning the given user.
     */
    private static EndpointConfig configForSession(HttpSession httpSession, User user) {
        UserBean userBean = Mockito.mock(UserBean.class);
        Mockito.when(userBean.getUser()).thenReturn(user);
        Mockito.when(httpSession.getAttribute("userBean")).thenReturn(userBean);

        EndpointConfig config = Mockito.mock(EndpointConfig.class);
        Mockito.when(config.getUserProperties()).thenReturn(Map.of(HttpSession.class.getName(), httpSession));
        return config;
    }

    @Test
    void onClose_shouldCallUnlockTranslationForStoredSession() {
        AdminBean.setTranslationGroupsEditorSession("session-123");

        User superuser = new User();
        superuser.setSuperuser(true);

        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(httpSession.getId()).thenReturn("session-123");
        EndpointConfig config = configForSession(httpSession, superuser);

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

    @Test
    void onOpen_noUser_socketClosedWithViolatedPolicy() throws Exception {
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(httpSession.getAttributeNames()).thenReturn(java.util.Collections.emptyEnumeration());
        EndpointConfig config = Mockito.mock(EndpointConfig.class);
        Mockito.when(config.getUserProperties()).thenReturn(Map.of(HttpSession.class.getName(), httpSession));

        Session wsSession = Mockito.mock(Session.class);

        new TranslationEditorEndpoint().onOpen(wsSession, config);

        ArgumentCaptor<CloseReason> reason = ArgumentCaptor.forClass(CloseReason.class);
        Mockito.verify(wsSession).close(reason.capture());
        assertEquals(CloseReason.CloseCodes.VIOLATED_POLICY, reason.getValue().getCloseCode());
    }

    @Test
    void onOpen_nonSuperuser_socketClosedWithViolatedPolicy() throws Exception {
        User user = new User();
        user.setSuperuser(false);

        HttpSession httpSession = Mockito.mock(HttpSession.class);
        EndpointConfig config = configForSession(httpSession, user);

        Session wsSession = Mockito.mock(Session.class);

        new TranslationEditorEndpoint().onOpen(wsSession, config);

        ArgumentCaptor<CloseReason> reason = ArgumentCaptor.forClass(CloseReason.class);
        Mockito.verify(wsSession).close(reason.capture());
        assertEquals(CloseReason.CloseCodes.VIOLATED_POLICY, reason.getValue().getCloseCode());
    }

    @Test
    void onOpen_nonSuperuser_doesNotReleaseLock() {
        AdminBean.setTranslationGroupsEditorSession("session-123");

        User user = new User();
        user.setSuperuser(false);

        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(httpSession.getId()).thenReturn("session-123");
        EndpointConfig config = configForSession(httpSession, user);

        Session wsSession = Mockito.mock(Session.class);

        TranslationEditorEndpoint endpoint = new TranslationEditorEndpoint();
        endpoint.onOpen(wsSession, config);
        endpoint.onClose(wsSession);

        assertEquals("session-123", AdminBean.getTranslationGroupsEditorSession(),
                "rejected connection must not release another session's lock");
    }
}
