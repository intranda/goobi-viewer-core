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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.goobi.presentation.contentServlet.controller.GetMetsPdfAction;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.download.DownloadJobTools;
import io.goobi.viewer.model.job.download.PDFDownloadJob;
import io.goobi.viewer.model.viewer.Dataset;
import jakarta.mail.MessagingException;

public class PdfMessageHandler implements MessageHandler<MessageStatus> {

    public static final String NAME = "pdfDownload";
    
    @Override
    public MessageStatus call(ViewerMessage message) {

        String pi = message.getPi();

        String logId = message.getProperties().get("logId");

        File targetFolder = new File(DataManager.getInstance().getConfiguration().getDownloadFolder(PDFDownloadJob.LOCAL_TYPE));
        if (!targetFolder.isDirectory() && !targetFolder.mkdir()) {
            return MessageStatus.ERROR;
        }

        String cleanedPi = StringTools.cleanUserGeneratedData(pi);

        String id = DownloadJob.generateDownloadJobId("pdf", pi, logId);

        try {
            DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(id);
            // save pdf file
            Dataset work = DataFileTools.getDataset(cleanedPi);

            Path pdfFile = DownloadJobTools.getDownloadFileStatic(downloadJob.getIdentifier(), downloadJob.getType(), downloadJob.getFileExtension())
                    .toPath();
            createPdf(work, pdfFile);
            // inform user and update DownloadJob

            downloadJob.setStatus(JobStatus.READY);
            downloadJob.notifyObservers(JobStatus.READY, "");
            DataManager.getInstance().getDao().updateDownloadJob(downloadJob);

        } catch (PresentationException | IndexUnreachableException | RecordNotFoundException | IOException | ContentLibException | DAOException
                | MessagingException e) {

            return MessageStatus.ERROR;
        }

        return MessageStatus.FINISH;
    }

    private void createPdf(Dataset work, Path pdfFile) throws IOException, ContentLibException {
        try (FileOutputStream fos = new FileOutputStream(pdfFile.toFile())) {
            Map<String, String> params = new HashMap<>();
            params.put("metsFile", work.getMetadataFilePath().toString());
            params.put("imageSource", work.getMediaFolderPath().toUri().toString());

            if (work.getPdfFolderPath() != null) {
                params.put("pdfSource", work.getPdfFolderPath().toUri().toString());
            }
            if (work.getAltoFolderPath() != null) {
                params.put("altoSource", work.getAltoFolderPath().toUri().toString());
            }
            params.put("metsFileGroup", "LOCAL");
            params.put("goobiMetsFile", "true");
            GetMetsPdfAction action = new GetMetsPdfAction();
            action.writePdf(params, fos);
        }
    }

    @Override
    public String getMessageHandlerName() {
        return NAME;
    }

}
