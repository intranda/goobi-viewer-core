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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

class UrlRedirectFilterTest {

    private static HttpServletRequest request(String method, String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn("/viewer");
        return request;
    }

    /**
     * @see UrlRedirectFilter#isIndexFileRequest(HttpServletRequest)
     */
    @Test
    void isIndexFileRequest_shouldMatchIndexFilesBelowContextPath() {
        assertTrue(UrlRedirectFilter.isIndexFileRequest(request("GET", "/viewer/index.xhtml")));
        assertTrue(UrlRedirectFilter.isIndexFileRequest(request("GET", "/viewer/index.html")));
        assertTrue(UrlRedirectFilter.isIndexFileRequest(request("HEAD", "/viewer/index.html")));
    }

    /**
     * @see UrlRedirectFilter#isIndexFileRequest(HttpServletRequest)
     */
    @Test
    void isIndexFileRequest_shouldNotMatchOtherRequests() {
        // welcome-file resolution keeps the original request uri
        assertFalse(UrlRedirectFilter.isIndexFileRequest(request("GET", "/viewer/")));
        assertFalse(UrlRedirectFilter.isIndexFileRequest(request("GET", "/viewer/index/")));
        assertFalse(UrlRedirectFilter.isIndexFileRequest(request("GET", "/viewer/sub/index.xhtml")));
        assertFalse(UrlRedirectFilter.isIndexFileRequest(request("POST", "/viewer/index.xhtml")));
    }

    /**
     * @see UrlRedirectFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, FilterChain)
     */
    @Test
    void doFilter_shouldRedirectIndexFileToRootPermanently() throws Exception {
        HttpServletRequest request = request("GET", "/viewer/index.xhtml");
        when(request.getQueryString()).thenReturn("locale=de");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        HttpSession session = Mockito.mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        new UrlRedirectFilter().doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        verify(response).setHeader("Location", "/viewer/?locale=de");
        verify(chain, never()).doFilter(request, response);
        verify(request, never()).getRequestDispatcher(anyString());
    }
}
