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

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.managedbeans.AdminBean;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Endpoint that maps HTTP session IDs to connected web sockets.
 */
@ServerEndpoint(value = "/session.socket", configurator = GetHttpSessionConfigurator.class)
public class UserEndpoint extends Endpoint {

    private static final Logger logger = LogManager.getLogger(UserEndpoint.class);

    private static Map<String, Timer> sessionClearTimers = new ConcurrentHashMap<>();

    private HttpSession httpSession;

    @OnOpen
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        // logger.trace("onOpen: {}", session.getId()); //NOSONAR Debug
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        if (httpSession != null) {
            // logger.trace("HTTP session ID: {}", httpSession.getId()); //NOSONAR Debug
            cancelClearTimer(httpSession.getId());
        }
    }

    @OnMessage
    public void onMessage(String message) {
        // logger.trace("onMessage from {}: {}", session.getId(), message); //NOSONAR Debug
        if (httpSession != null) {
            cancelClearTimer(httpSession.getId());
        }
    }

    @OnClose
    public void onClose(Session session) {
        // logger.trace("onClose {}", session.getId()); //NOSONAR Debug
        if (httpSession != null) {
            delayedRemoveLocksForSessionId(httpSession.getId(), 30000L);
        }
    }

    @Override
    @OnError
    public void onError(Session session, Throwable t) {
        logger.warn(t.getMessage());
    }

    /**
     *
     * @param sessionId
     */
    private static void cancelClearTimer(String sessionId) {
        if (sessionId == null) {
            return;
        }
        if (sessionClearTimers.get(sessionId) == null) {
            return;
        }

        sessionClearTimers.get(sessionId).cancel();
        sessionClearTimers.remove(sessionId);
        logger.trace("Release timer cancelled for session {}", sessionId);
    }

    /**
     * Timed grace period before removing any locks for the given session ID.
     *
     * @param sessionId
     * @param delay
     */
    private static void delayedRemoveLocksForSessionId(String sessionId, long delay) {
        if (sessionId == null) {
            return;
        }

        // logger.trace("Starting timer for {}", sessionId); //NOSONAR Debug
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                if (sessionClearTimers.containsKey(sessionId)) {
                    // Remove record locks
                    int count = DataManager.getInstance().getRecordLockManager().removeLocksForSessionId(sessionId, null);
                    // logger.trace("Removed {} record locks for session '{}'.", count, sessionId); //NOSONAR Debug
                    sessionClearTimers.remove(sessionId);

                    // Remove translation editing lock
                    if (sessionId.equals(AdminBean.getTranslationGroupsEditorSession())) {
                        AdminBean.setTranslationGroupsEditorSession(null);
                        logger.trace("Removed translation editing lock for session '{}'.", sessionId);
                    }
                }
            }
        };
        Timer timer = new Timer("timer_" + sessionId);
        sessionClearTimers.put(sessionId, timer);
        timer.schedule(task, delay);
    }
}
