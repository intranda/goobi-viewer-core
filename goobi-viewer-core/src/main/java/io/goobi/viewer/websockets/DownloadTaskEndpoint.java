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
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Endpoint that maps HTTP session IDs to connected web sockets.
 */
@ServerEndpoint(value = "/tasks/download/monitor.socket", configurator = GetHttpSessionConfigurator.class)
public class DownloadTaskEndpoint extends Endpoint {

    private static final Logger logger = LogManager.getLogger(DownloadTaskEndpoint.class);

    private MessageQueueManager queueManager;
    private PersistentStorageBean storageBean;

    private HttpSession httpSession;
    private Session session;

    @OnOpen
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        this.session = session;
        this.storageBean = BeanUtils.getPersistentStorageBean();
        this.queueManager = this.storageBean.getMessageBroker();
    }

    @OnMessage
    public synchronized void onMessage(String messageString) {
        try {
            SocketMessage message = JsonTools.getAsObject(messageString, SocketMessage.class);
            switch (message.action) {
                case STARTDOWNLOAD:
                    handleDownloadRequest(message);
                    break;
                case CANCELDOWNLOAD:
                    cancelDownload(message);
                    break;
                case STATUS:
                    sendUpdate(message, false);
                    break;
                case UPDATE:
                    sendUpdate(message, true);
                    break;
                case LISTFILES:
                default:
                    try { //NOSONAR
                        listDownloadedFiles(message, getDownloadedFiles(message.pi, message.url));
                    } catch (PresentationException | IndexUnreachableException e) {
                        String errorMessage = "Error listing files for download url " + message.url + ": " + e.toString();
                        logger.error(errorMessage);
                        sendError(message, errorMessage);
                    }
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
                    .toList();
        } else {
            answer.files = Collections.emptyList();
        }
        sendMessage(answer);
    }

    private void sendUpdate(SocketMessage message, boolean startDownloadIfDormant) throws JsonProcessingException {
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
                if (storageBean != null) {
                    storageBean.remove(message.url); //reset the status so a new download attempt is possible
                }
            } else if (job.getProgress().complete() && !isFilesExist(message.pi, message.url)) {
                //download task has completed but files are no longer available. remove job from storage bean and return waiting status
                if (storageBean != null) {
                    storageBean.remove(message.url);
                }
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
        } else if (startDownloadIfDormant && StringUtils.isNotBlank(message.messageQueueId)) {
            //no job, but also no files. Either job finished without generating files or the files have been deleted after the job finished
            //try downloading again
            startDownload(message);
            return; // don't send the original messagehere. #startDownload(message) takes care of that
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
            if (storageBean != null) {
                storageBean.remove(message.url);
            }
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
            return new ArrayList<>(FileUtils.listFiles(resourceFolder.toFile(), DownloadExternalResourceHandler.ALLOWED_FILE_EXTENSIONS, true)
                    .stream()
                    .map(File::toPath)
                    .map(resourceFolder::relativize)
                    .toList());
        }
        return Collections.emptyList();
    }

    public Path getDownloadFolder(String pi, String downloadUrl) throws PresentationException, IndexUnreachableException {
        Path downloadFolder = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getDownloadFolder("resource"));
        return downloadFolder.resolve(getDownloadId(pi, downloadUrl));
    }

    private static URI getDownloadUrl(String pi, String taskId, Path p) {
        URI uri = toUri(p);
        return DataManager.getInstance()
                .getRestApiManager()
                .getDataApiManager()
                .map(urls -> urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_EXTERNAL_RESOURCE_DOWNLOAD_PATH)
                        .params(pi, taskId, uri)
                        .buildURI())
                .orElse(null);
    }

    private static URI toUri(Path p) {
        UriBuilder builder = UriBuilder.fromPath("");
        for (int i = 0; i < p.getNameCount(); i++) {
            builder.path(p.getName(i).toString());
        }
        return builder.build();
    }

    private static String getDownloadId(String pi, String downloadUrl) {
        return DownloadJob.generateDownloadJobId(TaskType.DOWNLOAD_EXTERNAL_RESOURCE.name(), pi,
                downloadUrl);
    }

    private synchronized void sendMessage(SocketMessage message) throws JsonProcessingException {
        try {
            session.getBasicRemote().sendText(JsonTools.getAsJson(message));
        } catch (IOException e) {
            logger.error("Experienced Exception while sending text {}", message, e);
        }
    }

    public String getMimetype(String path) {
        try {
            return FileTools.getMimeTypeFromFile(Path.of(path));
        } catch (IOException e) {
            logger.error("Error probing mimetype of path {}", path, e);
            return "?";
        }
    }

    private static String calculateSize(Path path) {
        try {
            return FileSizeCalculator.formatSize(FileSizeCalculator.getFileSize(path));
        } catch (IOException e) {
            logger.error("Error calculating file size of {}", path, e);
            return "?";
        }
    }

    @OnClose
    public void onClose(Session session) {
        logger.debug("Closing socket for session {}", this.httpSession);
    }

    @Override
    @OnError
    public void onError(Session session, Throwable t) {
        if (!(t instanceof EOFException)) {
            logger.warn(t.getMessage());
        }
    }

    public enum Action {
        STARTDOWNLOAD,
        CANCELDOWNLOAD,
        UPDATE,
        STATUS,
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

        private String url;
        private String path;
        private String description;
        private String size;
        private String mimeType;

        public Map<String, String> getJsonSignature() {
            return JsonObjectSignatureBuilder.listProperties(getClass());
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
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

        private Action action;
        private Status status;
        private String pi;
        private String url;
        private long progress;
        private long resourceSize;
        private String messageQueueId;
        private String errorMessage;
        private List<ResourceFile> files;

        public Map<String, String> getJsonSignature() {
            return JsonObjectSignatureBuilder.listProperties(getClass());
        }

        public Action getAction() {
            return action;
        }

        public void setAction(Action action) {
            this.action = action;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public String getPi() {
            return pi;
        }

        public void setPi(String pi) {
            this.pi = pi;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public long getProgress() {
            return progress;
        }

        public void setProgress(long progress) {
            this.progress = progress;
        }

        public long getResourceSize() {
            return resourceSize;
        }

        public void setResourceSize(long resourceSize) {
            this.resourceSize = resourceSize;
        }

        public String getMessageQueueId() {
            return messageQueueId;
        }

        public void setMessageQueueId(String messageQueueId) {
            this.messageQueueId = messageQueueId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public List<ResourceFile> getFiles() {
            return files;
        }

        public void setFiles(List<ResourceFile> files) {
            this.files = files;
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
