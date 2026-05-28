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

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Custom configurator that exposes the HTTP session and the {@code Origin} request header
 * to web socket endpoints. The captured values are stashed in
 * {@link ServerEndpointConfig#getUserProperties()} under
 * {@link HttpSession#getClass()} name and {@link WebSocketTools#ORIGIN_PROPERTY}
 * respectively, so endpoints can run the auth and origin guard from
 * {@link WebSocketTools} on every handshake.
 */
public class GetHttpSessionConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config,
            HandshakeRequest request,
            HandshakeResponse response) {
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        config.getUserProperties().put(HttpSession.class.getName(), httpSession);

        // JSR-356: HandshakeRequest.getHeaders() returns a case-insensitive header map.
        // Take only the first value - a well-formed Origin request carries exactly one.
        Map<String, List<String>> headers = request.getHeaders();
        if (headers != null) {
            List<String> originValues = headers.get("Origin");
            if (originValues != null && !originValues.isEmpty()) {
                config.getUserProperties().put(WebSocketTools.ORIGIN_PROPERTY, originValues.get(0));
            }
        }
    }
}
