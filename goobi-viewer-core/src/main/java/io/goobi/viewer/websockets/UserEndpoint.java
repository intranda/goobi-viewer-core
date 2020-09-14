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

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;

@ServerEndpoint(value = "/sessionsocket")
public class UserEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(UserEndpoint.class);

    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        logger.trace("onOpen: {}", session.getId());
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message) {
        logger.trace("onMessage: {}", session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        logger.trace("onClose {}", session.getId());
        int count = DataManager.getInstance().removeSessionIdFromLocks(session.getId());
        logger.trace("Removed {} record locks for session '{}'.", count, session.getId());
    }

    @OnError
    public void onError(Session session, Throwable t) {
        logger.error(t.getMessage());
    }
}
