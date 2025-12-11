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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.PDFDownloadJob;
import io.goobi.viewer.model.job.download.PdfGenerator;
import io.goobi.viewer.model.viewer.Dataset;
import jakarta.mail.MessagingException;

public class PdfMessageHandler implements MessageHandler<MessageStatus> {

    private static final int DELAY_IF_PDF_IS_BEING_CREATED_MILLIS = 300_000;
    private static final int MAX_RETRIES = 2;
    private static final Logger logger = LogManager.getLogger(PdfMessageHandler.class);

    @Override
    public MessageStatus call(ViewerMessage message, MessageQueueManager queueManager) {

        PdfGenerator job = new PdfGenerator(message);

        try {
            File targetFolder = new File(DataManager.getInstance().getConfiguration().getDownloadFolder(PDFDownloadJob.LOCAL_TYPE));
            if (!targetFolder.isDirectory() && !targetFolder.mkdir()) {
                throw new IOException("Download folder " + targetFolder + " not found");
            }

            String cleanedPi = StringTools.cleanUserGeneratedData(job.getPi());
            Dataset work = DataFileTools.getDataset(cleanedPi);

            Path pdfFile = job.getPath();

            //if file is currently being created, wait 5 min and try again
            if (job.isLocked()) {
                message.setDelay(DELAY_IF_PDF_IS_BEING_CREATED_MILLIS);
                message.setRetryCount(message.getRetryCount() - 1);
                return MessageStatus.WAIT;
            }

            //if the file does not exist, create it
            if (!Files.exists(pdfFile)) {
                job.createPdf(work);
            }
            try {
                job.notifyObserver(message.getProperties().get("email"), JobStatus.READY, message.getMessageId(), "");
            } catch (MessagingException e) {
                logger.error("Error notifying observers: {}", e.toString());
            }
        } catch (RecordNotFoundException | ContentLibException e) {
            message.getProperties().put("message", "Error creating PDF: " + e.getMessage());
            message.setDoNotRetry();
            return MessageStatus.ERROR;
        } catch (PresentationException | IndexUnreachableException | IOException
                | URISyntaxException e) {
            if (message.getRetryCount() > MAX_RETRIES) {
                message.getProperties().put("message", "Error creating PDF: " + e.toString());
            }
            return MessageStatus.ERROR;
        }

        return MessageStatus.FINISH;
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.DOWNLOAD_PDF.name();
    }

}
