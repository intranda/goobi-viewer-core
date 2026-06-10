package io.goobi.viewer.websockets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.model.security.user.User;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

class ConfigEditorEndpointTest extends AbstractTest {

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
    void onOpen_noUser_socketClosedWithViolatedPolicy() throws Exception {
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(httpSession.getAttributeNames()).thenReturn(Collections.emptyEnumeration());
        EndpointConfig config = Mockito.mock(EndpointConfig.class);
        Mockito.when(config.getUserProperties()).thenReturn(Map.of(HttpSession.class.getName(), httpSession));

        Session wsSession = Mockito.mock(Session.class);

        new ConfigEditorEndpoint().onOpen(wsSession, config);

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

        new ConfigEditorEndpoint().onOpen(wsSession, config);

        ArgumentCaptor<CloseReason> reason = ArgumentCaptor.forClass(CloseReason.class);
        Mockito.verify(wsSession).close(reason.capture());
        assertEquals(CloseReason.CloseCodes.VIOLATED_POLICY, reason.getValue().getCloseCode());
    }

    @Test
    void onOpen_superuser_socketNotClosed() throws Exception {
        User superuser = new User();
        superuser.setSuperuser(true);

        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(httpSession.getId()).thenReturn("admin-session");
        EndpointConfig config = configForSession(httpSession, superuser);

        Session wsSession = Mockito.mock(Session.class);

        ConfigEditorEndpoint endpoint = new ConfigEditorEndpoint();
        endpoint.onOpen(wsSession, config);

        Mockito.verify(wsSession, Mockito.never()).close(Mockito.any());
        // The accepted connection should process messages and close without error.
        assertDoesNotThrow(() -> {
            // onMessage now takes the Session to reply lock status over; pass the same mock used above
            endpoint.onMessage("{\"fileToLock\":\"/opt/digiverso/viewer/config/config_viewer.xml\"}", wsSession);
            endpoint.onClose(wsSession);
        });
    }
}
