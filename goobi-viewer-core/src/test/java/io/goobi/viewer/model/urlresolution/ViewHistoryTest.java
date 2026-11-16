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
package io.goobi.viewer.model.urlresolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

class ViewHistoryTest {

    private static ViewerPath path(String pagePath) {
        return new ViewerPath("http://localhost:8080", "viewer", URI.create(pagePath), URI.create(""));
    }

    /**
     * @see ViewHistory#setCurrentView(ViewerPath, jakarta.servlet.http.HttpServletRequest)
     * @verifies make current view available in same request without creating a session
     */
    @Test
    void setCurrentView_shouldMakeCurrentViewAvailableInSameRequestWithoutCreatingASession() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ViewHistory.setCurrentView(path("search"), request);

        assertNull(request.getSession(false), "no http session must be created");
        Optional<ViewerPath> current = ViewHistory.getCurrentView(request);
        assertTrue(current.isPresent());
        assertEquals(URI.create("search"), current.get().getPagePath());
    }

    /**
     * @see ViewHistory#setCurrentView(ViewerPath, jakarta.servlet.http.HttpServletRequest)
     * @verifies store current and previous view in existing session
     */
    @Test
    void setCurrentView_shouldStoreCurrentAndPreviousViewInExistingSession() {
        MockHttpSession session = new MockHttpSession();

        MockHttpServletRequest first = new MockHttpServletRequest();
        first.setSession(session);
        ViewHistory.setCurrentView(path("search"), first);

        MockHttpServletRequest second = new MockHttpServletRequest();
        second.setSession(session);
        ViewHistory.setCurrentView(path("browse"), second);

        // A later request without its own request attribute falls back to the session
        MockHttpServletRequest third = new MockHttpServletRequest();
        third.setSession(session);
        assertEquals(URI.create("browse"), ViewHistory.getCurrentView(third).map(ViewerPath::getPagePath).orElse(null));
        assertEquals(URI.create("search"), ViewHistory.getPreviousView(third).map(ViewerPath::getPagePath).orElse(null));
    }

    /**
     * @see ViewHistory#getCurrentView(jakarta.servlet.ServletRequest)
     * @verifies prefer path stored in request over path stored in session
     */
    @Test
    void getCurrentView_shouldPreferPathStoredInRequestOverPathStoredInSession() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest older = new MockHttpServletRequest();
        older.setSession(session);
        ViewHistory.setCurrentView(path("search"), older);

        // Current request without session: e.g. the session cookie was not sent along
        MockHttpServletRequest request = new MockHttpServletRequest();
        ViewHistory.setCurrentView(path("browse"), request);
        // Session gets attached afterwards (as JSF does while rendering)
        request.setSession(session);

        assertEquals(URI.create("browse"), ViewHistory.getCurrentView(request).map(ViewerPath::getPagePath).orElse(null));
    }

    /**
     * @see ViewHistory#getCurrentView(jakarta.servlet.ServletRequest)
     * @verifies return empty optional if neither request nor session contain a path
     */
    @Test
    void getCurrentView_shouldReturnEmptyOptionalIfNeitherRequestNorSessionContainAPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertTrue(ViewHistory.getCurrentView(request).isEmpty());
        assertNull(request.getSession(false));
    }

    /**
     * @see ViewHistory#getCurrentView(jakarta.servlet.ServletRequest)
     * @verifies return a copy of the stored path
     */
    @Test
    void getCurrentView_shouldReturnACopyOfTheStoredPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ViewerPath stored = path("search");
        ViewHistory.setCurrentView(stored, request);

        ViewerPath returned = ViewHistory.getCurrentView(request).orElse(null);
        assertNotNull(returned);
        assertTrue(returned != stored);
    }
}
