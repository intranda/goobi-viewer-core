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
package io.goobi.viewer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.controller.DataManager;

/**
 * Unit tests for {@link SessionListener}.
 */
class SessionListenerTest {

    private static final String TEST_SESSION_ID = "test-session-1";

    @AfterEach
    void tearDown() {
        // Guard against test failure leaving state in the DataManager singleton
        DataManager.getInstance().getSessionMap().remove(TEST_SESSION_ID);
    }

    /**
     * @see SessionListener#sessionDestroyed(HttpSessionEvent)
     * @verifies remove all PRIV_ prefixed attributes on destroy
     */
    @Test
    void sessionDestroyed_shouldRemoveAllPrivPrefixedAttributesOnDestroy() {
        // Register the session id in DataManager so the cleanup block in sessionDestroyed
        // is entered (the listener guards on sessionMap.remove(...) returning non-null). In
        // production this put is performed by SessionCounterFilter.doFilter on each request.
        DataManager.getInstance().getSessionMap().put(TEST_SESSION_ID, new HashMap<>());

        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn(TEST_SESSION_ID);
        Enumeration<String> attrNames = Collections.enumeration(java.util.List.of(
                "PRIV_VIEW_IMAGES_pi_abc_file1.png",
                "PRIV_DOWNLOAD_PDF_pi_abc_file1.pdf",
                "currentPi",
                "user"));
        when(session.getAttributeNames()).thenReturn(attrNames);

        HttpSessionEvent event = mock(HttpSessionEvent.class);
        when(event.getSession()).thenReturn(session);

        SessionListener listener = new SessionListener();
        listener.sessionDestroyed(event);

        verify(session, times(1)).removeAttribute(eq("PRIV_VIEW_IMAGES_pi_abc_file1.png"));
        verify(session, times(1)).removeAttribute(eq("PRIV_DOWNLOAD_PDF_pi_abc_file1.pdf"));
        // non-PRIV attributes must not be removed by this listener
        verify(session, times(0)).removeAttribute(eq("currentPi"));
        verify(session, times(0)).removeAttribute(eq("user"));
    }
}
