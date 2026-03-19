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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.managedbeans.AdminConfigEditorBean;
import io.goobi.viewer.model.administration.configeditor.FileLocks;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

class UserEndpointTest extends AbstractTest {

    private static final String SESSION_ID = "test-session-lock-release";
    private static final Path CONFIG_FILE = Paths.get("/tmp/viewer-test-config.xml");

    @AfterEach
    void cleanupFileLocks() throws Exception {
        FileLocks fileLocks = getFileLocks();
        fileLocks.unlockFile(CONFIG_FILE, SESSION_ID);
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

        new UserEndpoint().onOpen(ws, cfg);

        ArgumentCaptor<CloseReason> reason = ArgumentCaptor.forClass(CloseReason.class);
        Mockito.verify(ws).close(reason.capture());
        assertEquals(CloseReason.CloseCodes.VIOLATED_POLICY, reason.getValue().getCloseCode());
    }

    /**
     * @see UserEndpoint#delayedRemoveLocksForSessionId(String, long)
     * @verifies release config file editor locks after grace period
     */
    @Test
    void delayedRemoveLocksForSessionId_shouldReleaseConfigFileEditorLocks() throws Exception {
        FileLocks fileLocks = getFileLocks();
        fileLocks.lockFile(CONFIG_FILE, SESSION_ID);
        assertTrue(fileLocks.isFileLockedByOthers(CONFIG_FILE, "other-session"),
                "File should be locked before cleanup");

        invokeDelayedRemoveLocksForSessionId(SESSION_ID, 1L);
        Thread.sleep(100);

        assertFalse(fileLocks.isFileLockedByOthers(CONFIG_FILE, "other-session"),
                "Config file lock should be released after grace period");
    }

    private static FileLocks getFileLocks() throws Exception {
        Field field = AdminConfigEditorBean.class.getDeclaredField("fileLocks");
        field.setAccessible(true);
        return (FileLocks) field.get(null);
    }

    private static void invokeDelayedRemoveLocksForSessionId(String sessionId, long delayMs) throws Exception {
        Method method = UserEndpoint.class.getDeclaredMethod("delayedRemoveLocksForSessionId", String.class, long.class);
        method.setAccessible(true);
        method.invoke(null, sessionId, delayMs);
    }
}
