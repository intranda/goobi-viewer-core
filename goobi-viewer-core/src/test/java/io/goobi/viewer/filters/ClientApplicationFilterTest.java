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
package io.goobi.viewer.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.clients.ClientApplication.AccessStatus;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpSession;

class ClientApplicationFilterTest {

    private static final String CLIENT_ID = "test-client";

    private IDAO dao;
    private ClientApplication client;
    private ClientApplicationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        client = new ClientApplication(CLIENT_ID);
        client.setAccessStatus(AccessStatus.GRANTED);
        dao = mock(IDAO.class);
        when(dao.getAllClientApplications()).thenReturn(List.of(client));
        filter = new ClientApplicationFilter(new ClientApplicationManager(dao));
    }

    private static MockHttpServletRequest clientRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ClientApplicationManager.CLIENT_IDENTIFIER_HEADER, CLIENT_ID);
        return request;
    }

    /**
     * @see ClientApplicationFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, FilterChain)
     * @verifies register known client in newly created session
     */
    @Test
    void doFilter_shouldRegisterKnownClientInNewlyCreatedSession() throws Exception {
        MockHttpServletRequest request = clientRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        HttpSession session = request.getSession(false);
        assertNotNull(session, "a session must be created to keep the client registration");
        assertSame(client, session.getAttribute(ClientApplicationManager.CLIENT_SESSION_ATTRIBUTE));
        assertEquals(AccessStatus.GRANTED.toString(), response.getHeader(ClientApplicationManager.CLIENT_RESPONSE_HEADER));
        verify(chain).doFilter(request, response);
    }

    /**
     * @see ClientApplicationFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, FilterChain)
     * @verifies save client to database only once per session
     */
    @Test
    void doFilter_shouldSaveClientToDatabaseOnlyOncePerSession() throws Exception {
        MockHttpServletRequest first = clientRequest();
        filter.doFilter(first, new MockHttpServletResponse(), mock(FilterChain.class));
        MockHttpSession session = (MockHttpSession) first.getSession(false);

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest next = clientRequest();
            next.setSession(session);
            filter.doFilter(next, new MockHttpServletResponse(), mock(FilterChain.class));
        }

        verify(dao, times(1)).saveClientApplication(any());
    }

    /**
     * @see ClientApplicationFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, FilterChain)
     * @verifies not create session for requests without client identifier
     */
    @Test
    void doFilter_shouldNotCreateSessionForRequestsWithoutClientIdentifier() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertNull(request.getSession(false));
        verify(dao, never()).saveClientApplication(any());
    }
}
