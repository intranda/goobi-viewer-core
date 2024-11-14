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

package io.goobi.viewer.model.job.mq;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.PersistentStorageBean;
import io.goobi.viewer.model.files.external.ExternalFilesDownloader;
import io.goobi.viewer.model.files.external.Progress;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.download.ExternalFilesDownloadJob;

public class DownloadExternalResourceHandler implements MessageHandler<MessageStatus> {

    private static final String PARAMETER_PI = "pi";
    private static final String PARAMETER_URL = "url";

    public static final String[] ALLOWED_FILE_EXTENSIONS =
            new String[] { "xml", "html", "pdf", "epub", "jpg", "jpeg", "png", "mp3", "mp4", "zip", "xlsx", "doc", "docx", "gs" };

    private static final Logger logger = LogManager.getLogger(DownloadExternalResourceHandler.class);

    @Inject
    private PersistentStorageBean storageBean;

    @Override
    public MessageStatus call(ViewerMessage message, MessageQueueManager queueManager) {

        String pi = message.getProperties().get(PARAMETER_PI);

        String url = message.getProperties().get(PARAMETER_URL);

        String messageId = message.getMessageId();

        Path extractedFolder = Paths.get("");

        try {
            Path targetFolder = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getDownloadFolder("resource"));
            if (!Files.isDirectory(targetFolder) && !targetFolder.toFile().mkdirs()) {
                logger.error("Error downloading resouce: Cannot create folder {}", targetFolder);
                storeError("Error downloading resouce: Cannot create folder " + targetFolder, url, messageId);
                return MessageStatus.ERROR;
            }

            String downloadId = getDownloadId(pi, url);

            URI uri = new URI(url);
            if (!uri.isAbsolute()) {
                logger.error("Error downloading resouce: Cannot locate url {}", url);
                storeError("Error downloading resouce: Cannot locate url " + url, url, messageId);
                return MessageStatus.ERROR;
            }

            if (!isFilesExist(pi, url, downloadId)) {

                extractedFolder = downloadAndExtractFiles(uri, targetFolder.resolve(downloadId), messageId);

                removeProgress(url);

                Duration duration = DataManager.getInstance().getConfiguration().getExternalResourceTimeBeforeDeletion();

                triggerDeletion(queueManager, targetFolder.resolve(downloadId), duration.toMillis());

            }

        } catch (PresentationException | IndexUnreachableException | IOException | URISyntaxException e) {
            logger.error("Error downloading resouce from url {}: {}", url, e.toString());
            storeError("Error downloading resouce from url " + url + ": " + e.toString(), url, messageId);
            return MessageStatus.ERROR;
        } catch (MessageQueueException e) {
            //error in #triggerDeletion
            logger.error("Error sending message to trigger deletion of {}. Files will remain in the file system. Reason: {}", extractedFolder,
                    e.toString());
        }

        return MessageStatus.FINISH;
    }

    private void triggerDeletion(MessageQueueManager queueManager, Path extractedFolder, long delay) throws MessageQueueException {
        ViewerMessage message = new ViewerMessage(TaskType.DELETE_RESOURCE.name());
        message.setDelay(delay);
        message.getProperties().put(DeleteResourceHandler.PARAMETER_RESOURCE_PATH, extractedFolder.toAbsolutePath().toString());
        queueManager.addToQueue(message);
    }

    private Path downloadAndExtractFiles(URI url, Path targetFolder, String messageId) throws IOException {
        ExternalFilesDownloader downloader = new ExternalFilesDownloader(targetFolder,
                p -> storeProgress(p, url.toString(), Paths.get(""), messageId));
        return downloader.downloadExternalFiles(url);
    }

    private void storeProgress(Progress progress, String identifier, Path path, String messageId) {
        ExternalFilesDownloadJob job = new ExternalFilesDownloadJob(progress, identifier, path, messageId);
        storageBean.put(identifier, job);
    }

    private void storeError(String errorMessage, String identifier, String messageId) {
        ExternalFilesDownloadJob job = new ExternalFilesDownloadJob(identifier, messageId, errorMessage);
        storageBean.put(identifier, job);
    }

    private void removeProgress(String identifier) {
        storageBean.remove(identifier);
    }

    private boolean isFilesExist(String pi, String url, String downloadId) {

        try {
            List<Path> files = getDownloadedFiles(pi, url, downloadId);
            if (files.isEmpty()) {
                return false;
            }
        } catch (PresentationException | IndexUnreachableException e) {
            return false;
        }
        return true;
    }

    public List<Path> getDownloadedFiles(String pi, String downloadUrl, String downloadId) throws PresentationException, IndexUnreachableException {
        Path downloadFolder = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getDownloadFolder("resource"));
        Path resourceFolder = downloadFolder.resolve(downloadId);
        if (Files.exists(resourceFolder)) {
            return FileUtils.listFiles(resourceFolder.toFile(), ALLOWED_FILE_EXTENSIONS, true)
                    .stream()
                    .map(File::toPath)
                    .map(resourceFolder::relativize)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private String getDownloadId(String pi, String downloadUrl) {
        return DownloadJob.generateDownloadJobId(TaskType.DOWNLOAD_EXTERNAL_RESOURCE.name(), pi,
                downloadUrl);
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.DOWNLOAD_EXTERNAL_RESOURCE.name();
    }

    public static ViewerMessage createMessage(String pi, String url) {
        ViewerMessage message = new ViewerMessage(TaskType.DOWNLOAD_EXTERNAL_RESOURCE.name());
        message.setProperties(Map.of(PARAMETER_PI, pi, PARAMETER_URL, url));
        return message;
    }

    public PersistentStorageBean getStorageBean() {
        return storageBean;
    }

    public void setStorageBean(PersistentStorageBean storageBean) {
        this.storageBean = storageBean;
    }

}
