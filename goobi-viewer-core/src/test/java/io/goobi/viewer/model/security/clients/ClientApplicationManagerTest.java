/*
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.security.clients;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Unit tests for the static helpers on {@link ClientApplicationManager}. The instance-level
 * methods that touch the DAO are covered indirectly via {@code ClientApplicationsResourceTest};
 * the cases here pin down the contract of {@link ClientApplicationManager#getClientFromRequest}
 * after the switch from {@code request.getSession()} to {@code request.getSession(false)} that
 * prevents IllegalStateException when called during a committed JSF response.
 */
class ClientApplicationManagerTest {

    /**
     * @see ClientApplicationManager#getClientFromRequest(HttpServletRequest)
     * @verifies return empty optional when request is null
     */
    @Test
    void getClientFromRequest_shouldReturnEmptyOptionalWhenRequestIsNull() {
        // Null-guard contract: passing null returns empty without touching anything else
        Optional<ClientApplication> result = ClientApplicationManager.getClientFromRequest(null);
        assertTrue(result.isEmpty());
    }

    /**
     * @see ClientApplicationManager#getClientFromRequest(HttpServletRequest)
     * @verifies return empty optional when no session exists on the request
     */
    @Test
    void getClientFromRequest_shouldReturnEmptyOptionalWhenNoSessionExistsOnTheRequest() {
        // Request without a session must yield an empty Optional (no client can be stored)
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        Optional<ClientApplication> result = ClientApplicationManager.getClientFromRequest(request);

        assertTrue(result.isEmpty());
    }

    /**
     * @see ClientApplicationManager#getClientFromRequest(HttpServletRequest)
     * @verifies not create a new session when none exists
     */
    @Test
    void getClientFromRequest_shouldNotCreateANewSessionWhenNoneExists() {
        // The fix's central guarantee: getSession(false), never getSession() / getSession(true).
        // Creating a session after the response has been committed (JSF render path:
        // ActiveDocumentBean.getTitleBarLabel -> getToc -> TocMaker.buildToc ->
        // AccessConditionUtils -> ClientApplicationManager.getClientFromRequest) raises
        // IllegalStateException in Tomcat. Verify that no creating overload is ever called.
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        ClientApplicationManager.getClientFromRequest(request);

        verify(request).getSession(false);
        verify(request, never()).getSession();
        verify(request, never()).getSession(true);
    }

    /**
     * @see ClientApplicationManager#getClientFromRequest(HttpServletRequest)
     * @verifies return stored client when present in the session
     */
    @Test
    void getClientFromRequest_shouldReturnStoredClientWhenPresentInTheSession() {
        // Happy path: session exists and contains a registered client -> return it
        HttpSession session = Mockito.mock(HttpSession.class);
        ClientApplication stored = new ClientApplication("test-client-id");
        when(session.getAttribute(ClientApplicationManager.CLIENT_SESSION_ATTRIBUTE)).thenReturn(stored);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(session);

        Optional<ClientApplication> result = ClientApplicationManager.getClientFromRequest(request);

        assertTrue(result.isPresent());
        assertSame(stored, result.get());
    }
}
