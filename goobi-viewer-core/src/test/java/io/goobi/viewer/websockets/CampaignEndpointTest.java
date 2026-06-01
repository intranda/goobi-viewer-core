/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.websockets;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.model.security.user.User;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.RemoteEndpoint.Basic;
import jakarta.websocket.Session;

/**
 * @author florian
 *
 */
class CampaignEndpointTest extends AbstractDatabaseEnabledTest {

    private final Session session1 = Mockito.mock(Session.class);
    private final Session session2 = Mockito.mock(Session.class);
    private final Session session3 = Mockito.mock(Session.class);

    private final Basic remote1 = Mockito.spy(Basic.class);
    private final Basic remote2 = Mockito.spy(Basic.class);
    private final Basic remote3 = Mockito.spy(Basic.class);

    private final HttpSession httpSession1 = Mockito.mock(HttpSession.class);
    private final HttpSession httpSession2 = Mockito.mock(HttpSession.class);
    private final HttpSession httpSession3 = Mockito.mock(HttpSession.class);

    private final EndpointConfig config1 = Mockito.mock(EndpointConfig.class);
    private final EndpointConfig config2 = Mockito.mock(EndpointConfig.class);
    private final EndpointConfig config3 = Mockito.mock(EndpointConfig.class);

    private final CampaignEndpoint endpoint1 = new CampaignEndpoint();
    private final CampaignEndpoint endpoint2 = new CampaignEndpoint();
    private final CampaignEndpoint endpoint3 = new CampaignEndpoint();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Mockito.when(httpSession1.getId()).thenReturn("http1");
        Mockito.when(httpSession2.getId()).thenReturn("http2");
        Mockito.when(httpSession3.getId()).thenReturn("http3");
        // Bind a superuser to each session so WebSocketTools.requireUser succeeds and the
        // per-message campaign-authz check short-circuits. Tests that need a non-authorized
        // user should override the session attribute themselves.
        bindSuperuser(httpSession1);
        bindSuperuser(httpSession2);
        bindSuperuser(httpSession3);
        Mockito.when(config1.getUserProperties()).thenReturn(Collections.singletonMap(HttpSession.class.getName(), httpSession1));
        Mockito.when(config2.getUserProperties()).thenReturn(Collections.singletonMap(HttpSession.class.getName(), httpSession2));
        Mockito.when(config3.getUserProperties()).thenReturn(Collections.singletonMap(HttpSession.class.getName(), httpSession3));
        Mockito.when(session1.getBasicRemote()).thenReturn(remote1);
        Mockito.when(session2.getBasicRemote()).thenReturn(remote2);
        Mockito.when(session3.getBasicRemote()).thenReturn(remote3);
        endpoint1.onOpen(session1, config1);
        endpoint2.onOpen(session2, config2);
        endpoint3.onOpen(session3, config3);
    }

    private static void bindSuperuser(HttpSession httpSession) {
        User user = new User();
        user.setId(1L);
        user.setSuperuser(true);
        UserBean userBean = Mockito.mock(UserBean.class);
        Mockito.when(userBean.getUser()).thenReturn(user);
        Mockito.when(httpSession.getAttribute("userBean")).thenReturn(userBean);
        // BeanUtils.findInstanceInSessionAttributes iterates getAttributeNames() unconditionally.
        Enumeration<String> empty = Collections.emptyEnumeration();
        Mockito.when(httpSession.getAttributeNames()).thenReturn(empty);
    }

    /**
     * @verifies broadcast locked pages to other sessions
     */
    @Test
    void onMessage_shouldBroadcastLockedPagesToOtherSessions() throws IOException, DAOException {
        endpoint1.onMessage(createMessage(1l, "PPN1234", 0));
        endpoint2.onMessage(createMessage(1l, "PPN1234", 2));

        Mockito.verify(remote1).sendText("{\"2\":\"LOCKED\"}");
        Mockito.verify(remote2).sendText("{\"0\":\"LOCKED\"}");

    }

    /**
     * @verifies close socket when no authenticated user is in the HTTP session
     */
    @Test
    void onOpen_noUserInSession_socketClosed() throws IOException {
        Session ws = Mockito.mock(Session.class);
        HttpSession http = Mockito.mock(HttpSession.class);
        Mockito.when(http.getAttributeNames()).thenReturn(Collections.emptyEnumeration());
        EndpointConfig cfg = Mockito.mock(EndpointConfig.class);
        Mockito.when(cfg.getUserProperties()).thenReturn(Collections.singletonMap(HttpSession.class.getName(), http));
        // no userBean attribute -> BeanUtils.getUserFromSession returns null

        new CampaignEndpoint().onOpen(ws, cfg);

        ArgumentCaptor<CloseReason> reason = ArgumentCaptor.forClass(CloseReason.class);
        Mockito.verify(ws).close(reason.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                CloseReason.CloseCodes.VIOLATED_POLICY, reason.getValue().getCloseCode());
    }

    /**
     * @verifies reject setPageLock when the campaign cannot be loaded for the user
     */
    @Test
    void setPageLock_unknownCampaign_warningSent() throws IOException, DAOException {
        // A non-existent campaign id triggers the same authorization-denied path as a real
        // campaign on which the user has no role. We use an unknown id here because
        // Campaign.isUserAllowedAction(ANNOTATE) on a PRIVATE campaign without an active
        // group limit falls through to allow any logged-in user, which would defeat the test.
        Session ws = Mockito.mock(Session.class);
        Basic remote = Mockito.spy(Basic.class);
        Mockito.when(ws.getBasicRemote()).thenReturn(remote);

        HttpSession http = Mockito.mock(HttpSession.class);
        Mockito.when(http.getId()).thenReturn("http_unauth_" + System.nanoTime());
        Mockito.when(http.getAttributeNames()).thenReturn(Collections.emptyEnumeration());
        User nonMember = new User();
        nonMember.setId(99L);
        nonMember.setSuperuser(false);
        UserBean userBean = Mockito.mock(UserBean.class);
        Mockito.when(userBean.getUser()).thenReturn(nonMember);
        Mockito.when(http.getAttribute("userBean")).thenReturn(userBean);

        EndpointConfig cfg = Mockito.mock(EndpointConfig.class);
        Map<String, Object> props = new HashMap<>();
        props.put(HttpSession.class.getName(), http);
        Mockito.when(cfg.getUserProperties()).thenReturn(props);

        CampaignEndpoint endpoint = new CampaignEndpoint();
        endpoint.onOpen(ws, cfg);
        endpoint.onMessage(createMessage(99999L, "PPN_unauth", 0));

        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        Mockito.verify(remote).sendText(sent.capture());
        org.junit.jupiter.api.Assertions.assertTrue(
                sent.getValue().contains("\"status\":\"warning\""),
                () -> "expected warning, got: " + sent.getValue());
    }

    /**
     * @param l
     * @param string
     * @param i
     * @return
     */
    private String createMessage(long campaign, String record, int index) {
        JSONObject json = new JSONObject();
        json.put("campaign", campaign);
        json.put("record", record);
        json.put("page", index);
        return json.toString();
    }

}
