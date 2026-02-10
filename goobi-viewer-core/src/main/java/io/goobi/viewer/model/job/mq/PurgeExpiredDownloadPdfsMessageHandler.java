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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.PdfDownloadJob;

public class PurgeExpiredDownloadPdfsMessageHandler implements MessageHandler<MessageStatus> {

    private static final Logger logger = LogManager.getLogger(PurgeExpiredDownloadPdfsMessageHandler.class);

    @Override
    public MessageStatus call(ViewerMessage message, MessageQueueManager queueManager) {

        Path targetFolder = Path.of(DataManager.getInstance().getConfiguration().getDownloadFolder(PdfDownloadJob.TYPE));
        List<Path> pdfs = getDownloadPdfFiles(targetFolder);

        int filesDeleted = 0;
        for (Path pdfPath : pdfs) {
            try {
                if (Files.exists(pdfPath)) {
                    PdfDownloadJob job = new PdfDownloadJob(pdfPath);
                    if (!job.isLocked() && job.isExpired()) {
                        logger.debug("Deleting expired pdf download file {}", pdfPath);
                        Files.delete(pdfPath);
                        ++filesDeleted;
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to delete expired pdf download file {}", pdfPath);
            }
        }

        message.getProperties().put("result", "Deleted %s PDFs in %s".formatted(filesDeleted, targetFolder));

        return MessageStatus.FINISH;
    }

    private static List<Path> getDownloadPdfFiles(Path targetFolder) {
        try {
            if (Files.isDirectory(targetFolder)) {
                try (Stream<Path> paths = Files.list(targetFolder)) {
                    return paths
                            .filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                            .toList();
                }
            }
        } catch (IOException e) {
            //ignore. No pdfs to download
        }
        return Collections.emptyList();
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.PURGE_EXPIRED_DOWNLOAD_PDFS.name();
    }

}
