/**
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
package io.goobi.viewer.websockets;

import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;

@ServerEndpoint(value = "/session.socket", configurator = GetHttpSessionConfigurator.class)
public class UserEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(UserEndpoint.class);

    private Session session;
    private HttpSession httpSession;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        logger.trace("onOpen: {}", session.getId());
        this.session = session;
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        if (httpSession != null) {
            logger.trace("HTTP session ID: {}", httpSession.getId());
        }
    }

    @OnMessage
    public void onMessage(String message) {
        logger.trace("onMessage from {}: {}", session.getId(), message);
    }

    @OnClose
    public void onClose(Session session) {
        logger.trace("onClose {}", session.getId());
        if (httpSession != null) {
            // TODO grace period before removing locks
            int count = DataManager.getInstance().removeSessionIdFromLocks(httpSession.getId());
            logger.trace("Removed {} record locks for session '{}'.", count, httpSession.getId());
        }
    }

    @OnError
    public void onError(Session session, Throwable t) {
        logger.error(t.getMessage());
    }
}
