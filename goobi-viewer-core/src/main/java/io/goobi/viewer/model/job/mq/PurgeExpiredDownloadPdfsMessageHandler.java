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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.PDFDownloadJob;
import io.goobi.viewer.model.job.download.PdfGenerator;

public class PurgeExpiredDownloadPdfsMessageHandler implements MessageHandler<MessageStatus> {

    private static final Logger logger = LogManager.getLogger(PurgeExpiredDownloadPdfsMessageHandler.class);

    @Override
    public MessageStatus call(ViewerMessage message, MessageQueueManager queueManager) {

        Path targetFolder = Path.of(DataManager.getInstance().getConfiguration().getDownloadFolder(PDFDownloadJob.LOCAL_TYPE));
        List<Path> pdfs = getDownloadPdfFiles(targetFolder);

        for (Path pdfPath : pdfs) {
            try {
                if (Files.exists(pdfPath)) {
                    PdfGenerator job = new PdfGenerator(pdfPath);
                    if (!job.isLocked() && job.isExpired()) {
                        logger.debug("Deleting expired pdf download file {}", pdfPath);
                        Files.delete(pdfPath);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to delete expired pdf download file {}", pdfPath);
            }
        }

        return MessageStatus.FINISH;
    }

    private List<Path> getDownloadPdfFiles(Path targetFolder) {
        try {
            if (Files.isDirectory(targetFolder)) {

                return Files.list(targetFolder)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().matches("(?i).*\\.pdf$"))
                        .toList();
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
