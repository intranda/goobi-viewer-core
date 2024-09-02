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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.goobi.presentation.contentServlet.controller.GetMetsPdfAction;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.contentlib.servlet.model.MetsPdfRequest;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.download.DownloadJobTools;
import io.goobi.viewer.model.job.download.PDFDownloadJob;
import io.goobi.viewer.model.viewer.Dataset;
import jakarta.mail.MessagingException;

public class PdfMessageHandler implements MessageHandler<MessageStatus> {

    private static final int DELAY_IF_PDF_IS_BEING_CREATED_MILLIS = 300_000;
    private static final int MAX_RETRIES = 2;
    private static final Logger logger = LogManager.getLogger(PdfMessageHandler.class);

    @Override
    public MessageStatus call(ViewerMessage message, MessageQueueManager queueManager) {

        String pi = message.getProperties().get("pi");

        String logId = message.getProperties().get("logId");

        boolean usPdfSource = Boolean.parseBoolean(message.getProperties().getOrDefault("usePdfSource", "false"));
        DownloadJob downloadJob = null;
        try {
            File targetFolder = new File(DataManager.getInstance().getConfiguration().getDownloadFolder(PDFDownloadJob.LOCAL_TYPE));
            if (!targetFolder.isDirectory() && !targetFolder.mkdir()) {
                throw new IOException("Download folder " + targetFolder + " not found");
            }

            String cleanedPi = StringTools.cleanUserGeneratedData(pi);
            String id = DownloadJob.generateDownloadJobId("pdf", pi, logId);
            downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(id);
            // save pdf file
            Dataset work = DataFileTools.getDataset(cleanedPi);

            Path pdfFile = DownloadJobTools.getDownloadFileStatic(downloadJob.getIdentifier(), downloadJob.getType(), downloadJob.getFileExtension())
                    .toPath();

            //if file is currently being created, wait 5 min and try again
            if (isLocked(pdfFile)) {
                message.setDelay(DELAY_IF_PDF_IS_BEING_CREATED_MILLIS);
                message.setRetryCount(message.getRetryCount() - 1);
                return MessageStatus.ERROR;
            }

            //if job is in error state of file doesn't exist, update job status
            if (JobStatus.ERROR == downloadJob.getStatus() || (JobStatus.READY == downloadJob.getStatus() && !Files.exists(pdfFile))) {
                downloadJob.setStatus(JobStatus.WAITING);
                DataManager.getInstance().getDao().updateDownloadJob(downloadJob);
            }

            //if the file does not exist, create it
            if (!Files.exists(pdfFile)) {
                createPdf(work, Optional.ofNullable(logId).filter(StringUtils::isNotBlank).filter(div -> !"-".equals(div)), pdfFile,
                        message.getMessageId(), usPdfSource);
            }
            downloadJob.setStatus(JobStatus.READY);
            try {
                downloadJob.notifyObservers(JobStatus.READY, message.getMessageId(), "");
            } catch (MessagingException e) {
                logger.error("Error notifying observers: {}", e.toString());
            }
            DataManager.getInstance().getDao().updateDownloadJob(downloadJob);
        } catch (PresentationException | IndexUnreachableException | RecordNotFoundException | IOException | ContentLibException | DAOException
                | URISyntaxException e) {
            if (downloadJob != null && message.getRetryCount() > MAX_RETRIES) {
                downloadJob.setStatus(JobStatus.ERROR);
                downloadJob.setMessage("Error creating PDF. Please contact support if the problem persists");
                message.getProperties().put("message", "Error creating PDF: " + e.toString());
                try {
                    DataManager.getInstance().getDao().updateDownloadJob(downloadJob);
                } catch (DAOException e1) {
                    logger.error("Error updating pdf download job in database after it reached an error status");
                }
            }
            return MessageStatus.ERROR;
        }

        return MessageStatus.FINISH;
    }

    private static void createPdf(Dataset work, Optional<String> divId, Path pdfFile, String taskId, boolean usePdfSource)
            throws IOException, ContentLibException, URISyntaxException {
        createLock(pdfFile);
        try (FileOutputStream fos = new FileOutputStream(pdfFile.toFile())) {
            MetsPdfRequest request = createPdfRequest(work, divId, usePdfSource);
            GetMetsPdfAction action = new GetMetsPdfAction();
            action.writePdf(request, ContentServerConfiguration.getInstance(), fos, p -> {
            });
        } catch (IOException | ContentLibException | URISyntaxException e) {
            Files.deleteIfExists(pdfFile);
            throw e;
        } finally {
            releaseLock(pdfFile);
        }
    }

    public static boolean createLock(Path pdfFile) throws IOException {
        try {
            Path lockFile = pdfFile.getParent().resolve(FilenameUtils.getBaseName(pdfFile.getFileName().toString()) + ".creating.lock");
            Files.createFile(lockFile);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        }
    }

    public static boolean releaseLock(Path pdfFile) throws IOException {
        Path lockFile = pdfFile.getParent().resolve(FilenameUtils.getBaseName(pdfFile.getFileName().toString()) + ".creating.lock");
        return Files.deleteIfExists(lockFile);
    }

    public static boolean isLocked(Path pdfFile) throws IOException {
        Path lockFile = pdfFile.getParent().resolve(FilenameUtils.getBaseName(pdfFile.getFileName().toString()) + ".creating.lock");
        return Files.exists(lockFile);
    }

    public static MetsPdfRequest createPdfRequest(Dataset work, Optional<String> divId, boolean usePdfSource) throws URISyntaxException {
        Map<String, String> params = new HashMap<>();
        params.put("metsFile", work.getMetadataFilePath().toString());
        params.put("imageSource", work.getMediaFolderPath().getParent().toUri().toString());
        divId.ifPresent(id -> params.put("divID", id));

        if (usePdfSource && work.getPdfFolderPath() != null) {
            params.put("pdfSource", work.getPdfFolderPath().getParent().toUri().toString());
        }
        if (work.getAltoFolderPath() != null) {
            params.put("altoSource", work.getAltoFolderPath().getParent().toUri().toString());
        }
        params.put("metsFileGroup", "PRESENTATION");
        params.put("goobiMetsFile", "false");
        MetsPdfRequest request = new MetsPdfRequest(params);
        return request;
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.DOWNLOAD_PDF.name();
    }

}
