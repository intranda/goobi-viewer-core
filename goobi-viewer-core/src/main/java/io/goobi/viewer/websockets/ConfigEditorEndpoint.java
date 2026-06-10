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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import io.goobi.viewer.managedbeans.AdminConfigEditorBean;
import io.goobi.viewer.model.security.user.User;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Endpoint for unlocking files opened in {@link AdminConfigEditorBean} when leaving a page.
 */
@ServerEndpoint(value = "/admin/config/edit.socket", configurator = GetHttpSessionConfigurator.class)
public class ConfigEditorEndpoint {

    private static final Logger logger = LogManager.getLogger(ConfigEditorEndpoint.class);

    private volatile Optional<Path> lockedFilePath = Optional.empty();
    private volatile Optional<String> httpSessionId = Optional.empty();

    /**
     * Store id of http session.
     * 
     * @param session WebSocket session being opened
     * @param config endpoint configuration providing HTTP session properties
     */
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

    /**
     * Handles three JSON message kinds:
     * <ul>
     *   <li>{@code {"fileToLock":"/path"}} — on (re)connect: remember the path and (re-)acquire the lock so a
     *       dropped-and-restored socket restores it.</li>
     *   <li>{@code {"heartbeat":true}} — renew the lease (no re-acquire); reports {@code lost} if the lease is gone.</li>
     *   <li>{@code {"release":true}} — on real page unload: release the lock immediately (owner-checked).</li>
     * </ul>
     * In both cases the client is told whether it still holds the lock via {@code {"lockStatus":"held"|"lost"}}.
     *
     * @param message a json object string
     * @param session the websocket session, used to reply
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JSONObject json = new JSONObject(message);
            String sessionId = httpSessionId.orElse(null);
            if (json.has("release")) {
                // Explicit release sent by the client on real page unload (navigation/close), NOT on idle/reconnect.
                // Frees the lock immediately instead of waiting for the TTL. Owner-checked, so it only releases
                // this session's own lock — other tabs/sessions are unaffected.
                Path path = lockedFilePath.orElse(null);
                if (path != null && sessionId != null) {
                    AdminConfigEditorBean.unlockFile(path, sessionId);
                }
                return;
            }
            if (json.has("heartbeat")) {
                // Heartbeat only RENEWS an existing own lease. It must not re-acquire (lockFile): otherwise a lock
                // that was intentionally released (navigation/close) would be silently re-taken by a still-open
                // socket. A lost lease is reported as "lost"; re-acquire happens only on (re)connect via fileToLock.
                Path path = lockedFilePath.orElse(null);
                if (path != null && sessionId != null) {
                    replyLockStatus(session, AdminConfigEditorBean.renewLock(path, sessionId));
                }
                return;
            }
            Path path = Paths.get(json.getString("fileToLock"));
            this.lockedFilePath = Optional.of(path);
            if (sessionId != null) {
                // (re-)acquire on (re)connect
                replyLockStatus(session, AdminConfigEditorBean.lockFile(path, sessionId));
            }
        } catch (JSONException | NullPointerException e) {
            logger.error("Error interpreting message {}", message);
        }
    }

    /** Sends the current lock ownership to the client. */
    private static void replyLockStatus(Session session, boolean held) {
        try {
            // getBasicRemote (synchronous) serialises sends per session and preserves order; getAsyncRemote would
            // throw IllegalStateException on overlapping writes and could deliver held/lost out of order.
            session.getBasicRemote().sendText("{\"lockStatus\":\"" + (held ? "held" : "lost") + "\"}");
        } catch (IOException e) {
            logger.trace("Could not send lock status to client: {}", e.getMessage());
        }
    }

    /**
     * Called when the websocket closes. Intentionally does NOT release the lock: an idle- or proxy-closed socket must
     * not be treated as the user leaving. The lease expires via TTL (reaped by
     * {@link io.goobi.viewer.model.administration.configeditor.FileLockReaper}) once heartbeats stop; an explicit
     * close releases it through {@code AdminConfigEditorBean.closeCurrentFileAction()}.
     *
     * @param session WebSocket session being closed
     */
    @OnClose
    public void onClose(Session session) {
        // No unlock here by design — see method Javadoc.
    }

    @OnError
    public void onError(Session session, Throwable t) {
        if (!(t instanceof EOFException)) {
            logger.warn("ConfigEditorEndpoint:" + t.getMessage());
        }
    }

}
