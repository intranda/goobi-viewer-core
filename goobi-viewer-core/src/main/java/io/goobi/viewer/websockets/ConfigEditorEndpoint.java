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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import io.goobi.viewer.managedbeans.AdminConfigEditorBean;
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
 * Endpoint for unlocking files opened in {@link AdminConfigEditorBean} when leaving a page
 */
@ServerEndpoint(value = "/admin/config/edit.socket", configurator = GetHttpSessionConfigurator.class)
public class ConfigEditorEndpoint extends Endpoint {

    private static final Logger logger = LogManager.getLogger(ConfigEditorEndpoint.class);

    private Optional<Path> lockedFilePath = Optional.empty();
    private Optional<String> httpSessionId = Optional.empty();

    /**
     * Store id of http session
     * 
     * @param session
     * @param config
     */
    @OnOpen
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        this.httpSessionId = Optional.ofNullable(httpSession).map(HttpSession::getId);
    }

    /**
     * Accept messages containing a file path which is locked by the curren page and needs to be unlocked upon leaving the page
     * 
     * @param message a json object string in the form "{'fileToLock' : '/path/to/config/file'}"
     */
    @OnMessage
    public void onMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String pathString = json.getString("fileToLock");
            Path path = Paths.get(pathString);
            this.lockedFilePath = Optional.of(path);
        } catch (JSONException | NullPointerException e) {
            logger.error("Error interpreting message {}", message);
        }
    }

    /**
     * Called when leaving a adminConfigEditor page. Unlocks the file set by {@link #onMessage(String)} for the session set by
     * {@link #onOpen(Session, EndpointConfig)} using {@link AdminConfigEditorBean#unlockFile(Path, String)}
     * 
     * @param session
     */
    @OnClose
    public void onClose(Session session) {
        lockedFilePath.ifPresent(path -> httpSessionId.ifPresent(sessionId -> AdminConfigEditorBean.unlockFile(path, sessionId)));
    }

    @Override
    @OnError
    public void onError(Session session, Throwable t) {
        logger.warn(t.getMessage());
    }

}
