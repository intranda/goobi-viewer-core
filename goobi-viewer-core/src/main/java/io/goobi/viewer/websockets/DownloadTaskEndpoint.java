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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
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
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileSizeCalculator;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.JsonObjectSignatureBuilder;
import io.goobi.viewer.controller.JsonTools;
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
    public void onMessage(String messageString) {
        try {
            SocketMessage message = JsonTools.getAsObject(messageString, SocketMessage.class);
            switch (message.action) {
                case STARTDOWNLOAD:
                    handleDownloadRequest(message);
                    break;
                case CANCELDOWNLOAD:
                    cancelDownload(message);
                    break;
                case UPDATE:
                    sendUpdate(message);
                    break;
                case LISTFILES:
                    try { //NOSONAR
                        listDownloadedFiles(message, getDownloadedFiles(message.pi, message.url));
                    } catch (PresentationException | IndexUnreachableException e) {
                        String errorMessage = "Error listing files for download url " + message.url + ": " + e.toString();
                        logger.error(errorMessage);
                        sendError(message, errorMessage);
                    }
                    break;
            }
        } catch (IOException e) {
            logger.error("Error interpreting download task message {}", messageString);
            try {
                sendError(new SocketMessage(Action.UPDATE, Status.ERROR, "", ""), "Error interpreting download task message " + messageString);
            } catch (JsonProcessingException e1) {
                logger.error("Error generating socket message message: {}", e1.toString());

            }
        }
    }

    public void handleDownloadRequest(SocketMessage message) throws JsonProcessingException {
        try {
            List<Path> filePaths = getDownloadedFiles(message.pi, message.url);
            if (!filePaths.isEmpty()) {
                listDownloadedFiles(message, filePaths);
            } else {
                startDownload(message);
            }
        } catch (PresentationException | IndexUnreachableException e) {
            sendError(message, "Error starting download task: " + e.toString());
        }
    }

    public void startDownload(SocketMessage message) throws JsonProcessingException {
        ViewerMessage mqMessage = DownloadExternalResourceHandler.createMessage(message.pi, message.url);
        try {
            String messageId = queueManager.addToQueue(mqMessage);
            SocketMessage answer = SocketMessage.buildAnswer(message, Status.WAITING);
            answer.progress = 0;
            answer.resourceSize = 1;
            answer.messageQueueId = messageId;
            sendMessage(answer);
        } catch (MessageQueueException e) {
            logger.error("Error adding message '{}' to queue: {}", mqMessage, e);
            sendError(message, "Failed to add message to queue: " + e.toString());
        }
    }

    private void listDownloadedFiles(SocketMessage message, List<Path> filePaths)
            throws JsonProcessingException, PresentationException, IndexUnreachableException {
        String taskId = getDownloadId(message.pi, message.url);
        SocketMessage answer = SocketMessage.buildAnswer(message, Status.COMPLETE);
        Path downloadFolder = getDownloadFolder(message.pi, message.url);
        if (Files.exists(downloadFolder)) {
            String description = "-";
            answer.files = filePaths.stream()
                    .map(p -> new ResourceFile(p.toString(), getDownloadUrl(message.pi, taskId, p).toString(), description, getMimetype(p.toString()),
                            calculateSize(downloadFolder.resolve(p))))
                    .collect(Collectors.toList());
        } else {
            answer.files = Collections.emptyList();
        }
        sendMessage(answer);
    }

    private void sendUpdate(SocketMessage message) throws JsonProcessingException {
        SocketMessage answer = SocketMessage.buildAnswer(message, Status.DORMANT);
        ExternalFilesDownloadJob job = Optional.ofNullable(storageBean)
                .flatMap(bean -> bean.getIfRecentOrRemove(message.url, 1, ChronoUnit.DAYS))
                .map(ExternalFilesDownloadJob.class::cast)
                .orElse(null);
        ViewerMessage queueMessage = queueManager.getMessageById(message.messageQueueId).orElse(null);
        if (queueMessage != null && job == null) {
            answer.status = Status.WAITING;
        } else if (job != null) {
            if (job.isError()) {
                answer.errorMessage = job.getErrorMessage();
                answer.status = Status.ERROR;
                storageBean.remove(message.url); //reset the status so a new download attempt is possible
            } else if (job.getProgress().complete() && !isFilesExist(message.pi, message.url)) {
                //download task has completed but files are no longer available. remove job from storage bean and return waiting status
                storageBean.remove(message.url);
                answer.status = Status.WAITING;
            } else {
                answer.messageQueueId = job.getMessageId();
                if (job.getProgress() != null) {
                    if (job.getProgress().getProgressRelative() == 1) {
                        answer.status = Status.COMPLETE;
                    } else if (job.getProgress().getProgressAbsolute() > 0) {
                        answer.status = Status.PROCESSING;
                    } else {
                        answer.status = Status.WAITING;
                    }
                    answer.progress = job.getProgress().getProgressAbsolute();
                    answer.resourceSize = job.getProgress().getTotalSize();
                }
            }
        } else if (isFilesExist(message.pi, message.url)) {
            answer.status = Status.COMPLETE;
        }
        sendMessage(answer);
    }

    private void cancelDownload(SocketMessage message) throws JsonProcessingException {
        try {
            boolean deleted = this.queueManager.deleteMessage(TaskType.DOWNLOAD_EXTERNAL_RESOURCE.name(), message.messageQueueId);
            Path resourceFolder = getDownloadFolder(message.pi, message.url);
            if (Files.exists(resourceFolder)) {
                FileUtils.deleteDirectory(resourceFolder.toFile());
            }
            storageBean.remove(message.url);
            if (deleted) {
                sendMessage(SocketMessage.buildAnswer(message, Status.CANCELED));
            } else {
                SocketMessage answer = SocketMessage.buildAnswer(message, Status.ERROR);
                answer.errorMessage = "Error canceling external resource download of url " + message.url;
                logger.error(answer.errorMessage);
                sendMessage(answer);
            }
        } catch (PresentationException | IOException | IndexUnreachableException e) {
            SocketMessage answer = SocketMessage.buildAnswer(message, Status.ERROR);
            answer.errorMessage = "Error canceling external resource download of url " + message.url;
            logger.error(answer.errorMessage);
            sendMessage(answer);
        }
    }

    private void sendError(SocketMessage message, String errorMessage) throws JsonProcessingException {
        SocketMessage answer = SocketMessage.buildAnswer(message, Status.ERROR);
        answer.errorMessage = errorMessage;
        sendMessage(answer);
    }

    private boolean isFilesExist(String pi, String url) {

        try {
            List<Path> files = getDownloadedFiles(pi, url);
            if (files.isEmpty()) {
                return false;
            }
        } catch (PresentationException | IndexUnreachableException e) {
            return false;
        }
        return true;
    }

    public List<Path> getDownloadedFiles(String pi, String downloadUrl) throws PresentationException, IndexUnreachableException {
        Path resourceFolder = getDownloadFolder(pi, downloadUrl);
        if (Files.exists(resourceFolder)) {
            return FileUtils.listFiles(resourceFolder.toFile(), DownloadExternalResourceHandler.ALLOWED_FILE_EXTENSIONS, true)
                    .stream()
                    .map(File::toPath)
                    .map(resourceFolder::relativize)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public Path getDownloadFolder(String pi, String downloadUrl) throws PresentationException, IndexUnreachableException {
        Path downloadFolder = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getDownloadFolder("resource"));
        Path resourceFolder = downloadFolder.resolve(getDownloadId(pi, downloadUrl));
        return resourceFolder;
    }

    private URI getDownloadUrl(String pi, String taskId, Path p) {
        URI uri = toUri(p);
        URI ret = DataManager.getInstance()
                .getRestApiManager()
                .getDataApiManager()
                .map(urls -> urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_EXTERNAL_RESOURCE_DOWNLOAD_PATH)
                        .params(pi, taskId, uri)
                        .buildURI())
                .orElse(null);
        return ret;
    }

    private URI toUri(Path p) {
        UriBuilder builder = UriBuilder.fromPath("");
        for (int i = 0; i < p.getNameCount(); i++) {
            builder.path(p.getName(i).toString());
        }
        return builder.build();
    }

    private String getDownloadId(String pi, String downloadUrl) {
        return DownloadJob.generateDownloadJobId(TaskType.DOWNLOAD_EXTERNAL_RESOURCE.name(), pi,
                downloadUrl);
    }

    private synchronized void sendMessage(SocketMessage message) throws JsonProcessingException {
        session.getAsyncRemote().sendText(JsonTools.getAsJson(message));
    }

    public String getMimetype(String path) {
        try {
            return FileTools.getMimeTypeFromFile(Path.of(path));
        } catch (IOException e) {
            logger.error("Error probing mimetype of path {}", path, e);
            return "?";
        }
    }

    private String calculateSize(Path path) {
        try {
            return FileSizeCalculator.formatSize(FileSizeCalculator.getFileSize(path));
        } catch (IOException e) {
            logger.error("Error calculating file size of {}", path, e);
            return "?";
        }
    }

    @OnClose
    public void onClose(Session session) {
        logger.info("Closing socket for sessio {}", this.httpSession);
    }

    @OnError
    public void onError(Session session, Throwable t) {
        logger.warn(t, t);
    }

    public enum Action {
        STARTDOWNLOAD,
        CANCELDOWNLOAD,
        UPDATE,
        LISTFILES;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum Status {
        DORMANT,
        WAITING,
        PROCESSING,
        COMPLETE,
        ERROR,
        CANCELED;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

    }

    public static class ResourceFile {

        public ResourceFile() {
        }

        public ResourceFile(String path, String url, String description, String mimeType, String size) {
            this.url = url;
            this.path = path;
            this.description = description;
            this.mimeType = mimeType;
            this.size = size;
        }

        public String url; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public String path; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public String description; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public String size; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public String mimeType; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters

        public Map<String, String> getJsonSignature() {
            return JsonObjectSignatureBuilder.listProperties(getClass());
        }
    }

    public static class SocketMessage {

        public SocketMessage() {
        }

        public SocketMessage(Action action, Status status, String pi, String url) {
            this.action = action;
            this.status = status;
            this.pi = pi;
            this.url = url;
        }

        public static SocketMessage buildAnswer(SocketMessage message, Status status) {
            SocketMessage answer = new SocketMessage(message.action, status, message.pi, message.url);
            answer.messageQueueId = message.messageQueueId;
            return answer;
        }

        public Action action; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public Status status; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public String pi; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public String url; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public long progress; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public long resourceSize; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public String messageQueueId; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public String errorMessage; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters
        public List<ResourceFile> files; //NOSONAR - this is a pure data exchange class and doesn't need getters and setters

        public Map<String, String> getJsonSignature() {
            return JsonObjectSignatureBuilder.listProperties(getClass());
        }

        @Override
        public String toString() {
            try {
                return JsonTools.getAsJson(this);
            } catch (JsonProcessingException e) {
                return "SocketMessage: action = " + this.action;
            }
        }
    }

}
