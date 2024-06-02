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

import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.exceptions.DAOException;


/**
 * @author florian
 *
 */
class CampaignEndpointTest extends AbstractDatabaseEnabledTest{

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

     /**
      * <p>setUp.</p>
      *
      * @throws java.lang.Exception if any.
      */
     @BeforeEach
     public void setUp() throws Exception {
         super.setUp();
         Mockito.when(httpSession1.getId()).thenReturn("http1");
         Mockito.when(httpSession2.getId()).thenReturn("http2");
         Mockito.when(httpSession3.getId()).thenReturn("http3");
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

    @Test
    void test() throws IOException, DAOException {
        endpoint1.onMessage(createMessage(1l, "PPN1234", 0));
        endpoint2.onMessage(createMessage(1l, "PPN1234", 2));

        Mockito.verify(remote1).sendText("{\"2\":\"LOCKED\"}");
        Mockito.verify(remote2).sendText("{\"0\":\"LOCKED\"}");

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
