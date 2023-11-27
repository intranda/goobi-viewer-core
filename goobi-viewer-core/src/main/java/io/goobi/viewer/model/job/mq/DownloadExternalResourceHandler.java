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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.dao.IDAO;
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

    private static final int DAYS_BEFORE_DELETION = 1;

    private static final long MILLISPERDAY = 1000*60*60*24l;

    private static final Logger logger = LogManager.getLogger(DownloadExternalResourceHandler.class);

    @Inject
    PersistentStorageBean storageBean;
    
    @Override
    public MessageStatus call(ViewerMessage message, MessageQueueManager queueManager) {

        String pi = message.getProperties().get("pi");

        String url = message.getProperties().get("url");

        Path extractedFolder = Paths.get("");
        
        try {
            Path targetFolder = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getOrigContentFolder());
            if (!Files.isDirectory(targetFolder) && targetFolder.toFile().mkdir()) {
                logger.error("Error downloading resouce: Cannot create folder {}", targetFolder);
                return MessageStatus.ERROR;
            }

            String cleanedPi = StringTools.cleanUserGeneratedData(pi);
    
            URI uri = new URI(url);
            
            extractedFolder = downloadAndExtractFiles(uri, targetFolder.resolve(cleanedPi));
            
            storeProgress(new Progress(1,1), url, extractedFolder);
            
            triggerDeletion(queueManager, extractedFolder, MILLISPERDAY*DAYS_BEFORE_DELETION);
            
        } catch (PresentationException | IndexUnreachableException | IOException | URISyntaxException  e) {
            logger.error("Error downloading external resource: {}", e.toString());
            return MessageStatus.ERROR;
        } catch (MessageQueueException e) {
            //error in #triggerDeletion
            logger.error("Error sending message to trigger deletion of {}. Files will remain in the file system. Reason: {}", extractedFolder, e.toString());
        }

        return MessageStatus.FINISH;
    }

    private void triggerDeletion(MessageQueueManager queueManager, Path extractedFolder, long delay) throws MessageQueueException {
        ViewerMessage message = new ViewerMessage(TaskType.DELETE_RESOURCE.name());
        message.setDelay(delay);
        message.getProperties().put(DeleteResourceHandler.PARAMETER_RESOURCE_PATH, extractedFolder.toAbsolutePath().toString());
        queueManager.addToQueue(message);
    }

    private Path downloadAndExtractFiles(URI url, Path targetFolder) throws IOException {
        ExternalFilesDownloader downloader = new ExternalFilesDownloader(targetFolder, 
                p -> storeProgress(p, url.toString(), Paths.get("")));
        return downloader.downloadExternalFiles(url);
    }
    
    private void storeProgress(Progress progress, String identifier, Path path) {
        ExternalFilesDownloadJob job = new ExternalFilesDownloadJob(progress, identifier, path);
        storageBean.put(identifier, job);
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.DOWNLOAD_EXTERNAL_RESOURCE.name();
    }

}
