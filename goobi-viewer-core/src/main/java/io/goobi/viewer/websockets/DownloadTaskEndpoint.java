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

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.mq.DownloadExternalResourceHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;


/**
 * Endpoint that maps HTTP session IDs to connected web sockets.
 */
@ServerEndpoint(value = "/tasks/download/monitor.socket", configurator = GetHttpSessionConfigurator.class)
public class DownloadTaskEndpoint {

    private static final Logger logger = LogManager.getLogger(DownloadTaskEndpoint.class);

    @Inject
    MessageQueueManager queueManager;
    
    private HttpSession httpSession;
    private Session session;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message) {
        JSONObject json = new JSONObject(message);
        String pi = json.getString("pi");
        int downloadUrl = json.getInt("url");
        
        ViewerMessage message = new ViewerMessage(TaskType.DOWNLOAD_EXTERNAL_RESOURCE.name());
        message.setProperties(Map.of(DownloadExternalResourceHandler.));
        
    }

    @OnClose
    public void onClose(Session session) {

    }

    @OnError
    public void onError(Session session, Throwable t) {
        logger.warn(t.getMessage());
    }

}
