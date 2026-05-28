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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.filters.CSRFRequestFilter;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCode;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

/**
 * Shared guards for {@link jakarta.websocket.server.ServerEndpoint} {@code @OnOpen} handlers.
 *
 * <p>Provides three building blocks every endpoint that should restrict its caller needs:
 * <ul>
 *   <li>{@link #requireUser(HttpSession, Session)} - enforce that the captured HTTP session
 *       contains a logged-in {@link User}, close the WebSocket otherwise.</li>
 *   <li>{@link #requireAllowedOrigin(EndpointConfig, Session)} - validate the {@code Origin}
 *       header captured by {@link GetHttpSessionConfigurator} against the same allowlist
 *       used by {@link CSRFRequestFilter}; only enforced when
 *       {@link Configuration#isWebSocketOriginValidationEnabled()} is on. Decoupled from the
 *       REST CSRF switch so the two transports can be hardened independently.</li>
 *   <li>{@link #closeSession(Session, CloseCode, String)} - best-effort polite close
 *       used by the guards above and by callers that detect a fatal handshake-time
 *       condition (unknown resource etc.).</li>
 * </ul>
 *
 * <p>Origin validation is gated by {@code webapi.websocket.originValidation[@enabled]},
 * separate from the REST {@code webapi.csrf[@enabled]} so operators can harden each
 * transport independently. The user check is always enforced when the endpoint calls it.
 */
public final class WebSocketTools {

    private static final Logger logger = LogManager.getLogger(WebSocketTools.class);

    /** Key under which {@link GetHttpSessionConfigurator} stashes the {@code Origin} header. */
    public static final String ORIGIN_PROPERTY = "origin";

    private WebSocketTools() {
        // utility
    }

    /**
     * Best-effort polite close. Swallows {@link IOException} - the peer may already be gone,
     * and there is nothing useful the caller can do at that point.
     *
     * @param session WebSocket session to close
     * @param code close code to send
     * @param reason short human-readable reason (will be truncated by the container if >123 bytes)
     */
    public static void closeSession(Session session, CloseCode code, String reason) {
        try {
            session.close(new CloseReason(code, reason));
        } catch (IOException e) {
            // Stack trace intentionally omitted - best-effort close, session may already be gone
            logger.debug("Could not close WebSocket session: {}", e.getMessage());
        }
    }

    /**
     * Returns the {@link User} bound to {@code httpSession}, or closes {@code ws} with
     * {@link CloseCodes#VIOLATED_POLICY} and returns {@code null} if no user is present.
     * Callers should {@code return} from {@code @OnOpen} immediately when the result is
     * {@code null}.
     *
     * @param httpSession HTTP session captured by {@link GetHttpSessionConfigurator}
     * @param ws WebSocket session to close on failure
     * @return the authenticated {@link User} or {@code null} if the socket was closed
     */
    public static User requireUser(HttpSession httpSession, Session ws) {
        User user = BeanUtils.getUserFromSession(httpSession);
        if (user == null) {
            logger.debug("WebSocket {} rejected: no authenticated user in HTTP session", ws.getId());
            closeSession(ws, CloseCodes.VIOLATED_POLICY, "authentication required");
            return null;
        }
        return user;
    }

    /**
     * Validates the {@code Origin} header captured at handshake time against
     * {@link Configuration#getViewerBaseUrl()} and
     * {@link Configuration#getCsrfAdditionalAllowedOrigins()}. No-op (returns {@code true})
     * when {@link Configuration#isWebSocketOriginValidationEnabled()} is {@code false}.
     *
     * @param config endpoint config, expected to carry the {@link #ORIGIN_PROPERTY} value
     * @param ws WebSocket session to close on failure
     * @return {@code true} if the origin is allowed (or validation is disabled);
     *         {@code false} if the socket was closed
     */
    public static boolean requireAllowedOrigin(EndpointConfig config, Session ws) {
        Configuration configuration = DataManager.getInstance().getConfiguration();
        if (!configuration.isWebSocketOriginValidationEnabled()) {
            return true;
        }
        String origin = CSRFRequestFilter.normalizeOrigin((String) config.getUserProperties().get(ORIGIN_PROPERTY));
        if (origin == null) {
            logger.warn("WebSocket {} rejected: missing or unparseable Origin header", ws.getId());
            closeSession(ws, CloseCodes.VIOLATED_POLICY, "origin not allowed");
            return false;
        }
        if (!CSRFRequestFilter.isAllowedOrigin(origin, configuration)) {
            logger.warn("WebSocket {} rejected: Origin {} not allowed", ws.getId(), origin);
            closeSession(ws, CloseCodes.VIOLATED_POLICY, "origin not allowed");
            return false;
        }
        return true;
    }
}
