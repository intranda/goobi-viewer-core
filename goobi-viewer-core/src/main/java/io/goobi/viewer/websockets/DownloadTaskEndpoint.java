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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.managedbeans.PersistentStorageBean;
import io.goobi.viewer.model.job.download.ExternalFilesDownloadJob;
import io.goobi.viewer.model.job.mq.DownloadExternalResourceHandler;


/**
 * Endpoint that maps HTTP session IDs to connected web sockets.
 */
@ServerEndpoint(value = "/tasks/download/monitor.socket", configurator = GetHttpSessionConfigurator.class)
public class DownloadTaskEndpoint {

    private static final String JSON_MESSAGE_URL = "url";

    private static final String JSON_MESSAGE_PI = "pi";

    private static final Logger logger = LogManager.getLogger(DownloadTaskEndpoint.class);

    @Inject
    MessageQueueManager queueManager;
    @Inject
    PersistentStorageBean storageBean;
    
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
        String action = json.getString("action");
        String pi = json.getString(JSON_MESSAGE_PI);
        String downloadUrl = json.getString(JSON_MESSAGE_URL);
        if(StringUtils.isNotBlank(action)) {
            switch(action) {
                case "start-download":
                    startDownload(pi, downloadUrl);
                    break;
                case "cancel-download":
                    cancelDownload(pi, downloadUrl);
                    break;
                case "update":
                    sendUpdate(pi, downloadUrl);
                    break;
                case "get-files":
                    listDownloadedFiles(pi, downloadUrl);
                    break;
            }
        }
        
        
        
    }

    private void cancelDownload(String pi, String downloadUrl) {
        this.queueManager.deleteMessage(null)
        
    }

    private void sendUpdate(String pi, String downloadUrl) {
        ExternalFilesDownloadJob job = Optional.ofNullable(storageBean)
                .map(bean -> bean.get(downloadUrl))
                .map(ExternalFilesDownloadJob.class::cast).orElse(null);
        if(job != null) {
            sendMessage(pi, downloadUrl, 
                    "progress", Long.toString(job.getProgress().getProgressAbsolute()),
                    "size", Long.toString(job.getProgress().getTotalSize()),
                    "complete", Boolean.toString(job.getProgress().complete()),
                    "path", job.getPath().toAbsolutePath().toString(),
                    "message-queue-id", job.getMessageId(),
                    "status", "processing"
                    );
        } else {
            sendMessage(pi, downloadUrl,
                    "status", "waiting",
                    "message", "No progress registered for download job of " + downloadUrl);
        }
                
        
        
    }

    public void startDownload(String pi, String downloadUrl) {
        ViewerMessage mqMessage = DownloadExternalResourceHandler.createMessage(pi, downloadUrl);
        try {
            String messageId = queueManager.addToQueue(mqMessage);
            sendMessage(pi, downloadUrl, Map.of("message", "Download started", "progress", "0", "status", "waiting", "message-queue-id", messageId));
        } catch (MessageQueueException e) {
            logger.error("Error adding message '{}' to queue: {}", mqMessage, e);
            sendMessage(pi, downloadUrl, "status", "error", "message", "Failed to add message to queue: " + e.getMessage());
        }
    }
    
    private void sendMessage(String pi, String url, String...properties) {
        Map<String, String> propertyMap = new HashMap<>();
        for (int i = 0; i < properties.length; i+=2) {
            if(properties.length > i+1) {
                propertyMap.put(properties[i], properties[i+1]);
            }
        }
        sendMessage(pi, url, propertyMap);
    }

    
    private void sendMessage(String pi, String url, Map<String, String> properties) {
        JSONObject object = new JSONObject();
        object.put(JSON_MESSAGE_PI, pi);
        object.put(JSON_MESSAGE_URL, url);
        properties.entrySet().forEach(entry -> object.put(entry.getKey(), entry.getValue()));
        session.getAsyncRemote().sendText(object.toString());
    }
    
    private void updateProgress(String pi, URI url) {
        
    }

    @OnClose
    public void onClose(Session session) {

    }

    @OnError
    public void onError(Session session, Throwable t) {
        logger.warn(t.getMessage());
    }

}
