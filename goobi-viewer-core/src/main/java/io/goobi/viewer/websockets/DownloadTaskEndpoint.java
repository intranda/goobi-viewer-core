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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_EXTERNAL_RESOURCE_DOWNLOAD;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.core.Context;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.exceptions.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.PersistentStorageBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.download.ExternalFilesDownloadJob;
import io.goobi.viewer.model.job.mq.DownloadExternalResourceHandler;


/**
 * Endpoint that maps HTTP session IDs to connected web sockets.
 */
@ServerEndpoint(value = "/tasks/download/monitor.socket", configurator = GetHttpSessionConfigurator.class)
public class DownloadTaskEndpoint {

    private static final String[] ALLOWED_FILE_EXTENSIONS = new String[] {"xml", "html", "pdf", "epub", "jpg", "jpeg", "png", "mp3", "mp4", "zip", "xlsx", "doc", "docx", "gs"};

    private static final String JSON_MESSAGE_ACTION = "action";

    private static final String JSON_MESSAGE_JOB_STATUS = "status";

    private static final String JSON_MESSAGE_DOWNLOAD_SIZE = "size";

    private static final String JSON_MESSAGE_PROGRESS = "progress";

    private static final String JSON_MESSAGE_URL = "url";

    private static final String JSON_MESSAGE_PI = "pi";
    
    private static final String JSON_MESSAGE_QUEUE_ID = "message-queue-id";
    
    
    private static final Logger logger = LogManager.getLogger(DownloadTaskEndpoint.class);

    MessageQueueManager queueManager;
    PersistentStorageBean storageBean;
    
    private HttpSession httpSession;
    private Session session;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        this.session = session;
        this.storageBean = BeanUtils.getPersistentStorageBean();
        this.queueManager = this.storageBean.getMessageBroker();
    }

    @OnMessage
    public void onMessage(String message) {
        try {            
            JSONObject json = new JSONObject(message);
            String action = json.getString(JSON_MESSAGE_ACTION);
            String pi = json.getString(JSON_MESSAGE_PI);
            String downloadUrl = json.getString(JSON_MESSAGE_URL);
            if(StringUtils.isNotBlank(action)) {
                switch(action) {
                    case "start-download":
                        startDownload(pi, downloadUrl);
                        break;
                    case "cancel-download":
                        String messageQueueId = json.getString(JSON_MESSAGE_QUEUE_ID);
                        cancelDownload(pi, downloadUrl, messageQueueId);
                        break;
                    case "update":
                        sendUpdate(pi, downloadUrl);
                        break;
                    case "list-files":
                        listDownloadedFiles(pi, downloadUrl);
                        break;
                }
            }
        } catch(JSONException e) {
            logger.error("Error interpreting download task message {}", message);
        }
    }

    private void listDownloadedFiles(String pi, String downloadUrl) {
        try {
            String taskId = getDownloadId(pi, downloadUrl);
            Path downloadFolder = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getDownloadFolder("resource"));
            Path resourceFolder = downloadFolder.resolve(taskId);
            List<Path> filePaths = FileUtils.listFiles(resourceFolder.toFile(), ALLOWED_FILE_EXTENSIONS, true).stream()
                    .map(File::toPath)
                    .map(p -> resourceFolder.relativize(p))
                    .collect(Collectors.toList());
            JSONObject object = new JSONObject();
            object.put(JSON_MESSAGE_PI, pi);
            object.put(JSON_MESSAGE_URL, downloadUrl);
            JSONArray filesJson = new JSONArray();
            filePaths.forEach(p -> {
                JSONObject fileJson = new JSONObject();
                fileJson.put("path", p);
                fileJson.put("url", getDownloadUrl(pi, taskId, p));
                filesJson.put(fileJson);
            });
            object.put("files", filesJson);
        } catch (PresentationException | IndexUnreachableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private URI getDownloadUrl(String pi, String taskId, Path p) {
        return DataManager.getInstance().getRestApiManager().getDataApiManager().map(urls -> urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_EXTERNAL_RESOURCE_DOWNLOAD).params(pi, taskId, p).buildURI()).orElse(null);
    }

    private void cancelDownload(String pi, String downloadUrl, String messageId) {
        boolean deleted = this.queueManager.deleteMessage(TaskType.DOWNLOAD_EXTERNAL_RESOURCE.name(), messageId);
        if(deleted) {
            sendMessage(pi, downloadUrl, 
                    JSON_MESSAGE_JOB_STATUS, "canceled");
        } else {
            sendMessage(pi, downloadUrl, 
                    JSON_MESSAGE_JOB_STATUS, "error");
        }
    }

    private void sendUpdate(String pi, String downloadUrl) {
        ExternalFilesDownloadJob job = Optional.ofNullable(storageBean)
                .map(bean -> bean.get(getDownloadId(pi, downloadUrl)))
                .map(ExternalFilesDownloadJob.class::cast).orElse(null);
        if(job != null) {
            sendMessage(pi, downloadUrl, 
                    JSON_MESSAGE_PROGRESS, Long.toString(job.getProgress().getProgressAbsolute()),
                    JSON_MESSAGE_DOWNLOAD_SIZE, Long.toString(job.getProgress().getTotalSize()),
                    JSON_MESSAGE_QUEUE_ID, job.getMessageId(),
                    JSON_MESSAGE_JOB_STATUS, job.getProgress().complete() ? "p" : "processing"
                    );
        } else {
            sendMessage(pi, downloadUrl,
                    JSON_MESSAGE_JOB_STATUS, "waiting");
        }
                
        
        
    }

    public void startDownload(String pi, String downloadUrl) {
        ViewerMessage mqMessage = DownloadExternalResourceHandler.createMessage(pi, downloadUrl);
        try {
            String messageId = queueManager.addToQueue(mqMessage);
            sendMessage(pi, downloadUrl, Map.of(
                    JSON_MESSAGE_PROGRESS, "0", 
                    JSON_MESSAGE_JOB_STATUS, "waiting", 
                    JSON_MESSAGE_QUEUE_ID, messageId));
        } catch (MessageQueueException e) {
            logger.error("Error adding message '{}' to queue: {}", mqMessage, e);
            sendMessage(pi, downloadUrl, JSON_MESSAGE_JOB_STATUS, "error", "message", "Failed to add message to queue: " + e.getMessage());
        }
    }
    
    private String getDownloadId(String pi, String downloadUrl) {
        return DownloadJob.generateDownloadJobId(TaskType.DOWNLOAD_EXTERNAL_RESOURCE.name(), StringTools.cleanUserGeneratedData(pi), StringTools.cleanUserGeneratedData(downloadUrl));
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
    

    @OnClose
    public void onClose(Session session) {
        logger.info("Closing socket for sessio {}", this.httpSession);
    }

    @OnError
    public void onError(Session session, Throwable t) {
        logger.warn(t.toString(), t);
    }

}
