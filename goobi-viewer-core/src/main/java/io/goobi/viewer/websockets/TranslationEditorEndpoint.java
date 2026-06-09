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

import java.io.EOFException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.managedbeans.AdminBean;
import io.goobi.viewer.model.security.user.User;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * WebSocket endpoint that releases the translation editor lock when the user navigates away from
 * the translations editor page. The connection is opened by the page and closed automatically
 * by the browser on navigation, triggering {@link #onClose}.
 *
 * @see AdminBean#unlockTranslation(String)
 */
@ServerEndpoint(value = "/admin/translations/edit.socket", configurator = GetHttpSessionConfigurator.class)
public class TranslationEditorEndpoint {

    private static final Logger logger = LogManager.getLogger(TranslationEditorEndpoint.class);

    private Optional<String> httpSessionId = Optional.empty();

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        User user = WebSocketTools.requireUser(httpSession, session);
        if (user == null) {
            return; // requireUser already closed the socket
        }
        if (!user.isSuperuser()) {
            WebSocketTools.closeSession(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Admin access required");
            return;
        }
        this.httpSessionId = Optional.of(httpSession.getId());
    }

    @OnClose
    public void onClose(Session session) {
        httpSessionId.ifPresent(sessionId -> {
            AdminBean.unlockTranslation(sessionId);
            logger.trace("TranslationEditorEndpoint.onClose: released lock for session '{}'", sessionId);
        });
    }

    @OnError
    public void onError(Session session, Throwable t) {
        if (!(t instanceof EOFException)) {
            logger.warn("TranslationEditorEndpoint error: {}", t.getMessage());
        }
    }

}
