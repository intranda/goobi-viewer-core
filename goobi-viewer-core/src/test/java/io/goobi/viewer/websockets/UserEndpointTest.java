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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

class UserEndpointTest extends AbstractTest {

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

        new UserEndpoint().onOpen(ws, cfg);

        ArgumentCaptor<CloseReason> reason = ArgumentCaptor.forClass(CloseReason.class);
        Mockito.verify(ws).close(reason.capture());
        assertEquals(CloseReason.CloseCodes.VIOLATED_POLICY, reason.getValue().getCloseCode());
    }
}
