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

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.ReturnValue;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.model.job.download.PDFDownloadJob;

public class PdfMessageHandler implements MessageHandler<ReturnValue> {

    @Override
    public ReturnValue call(ViewerMessage message) {

        System.out.println("handle pdf download");

        String pi = message.getPi();

        String logId = message.getProperties().get("logId");
        String email = message.getProperties().get("email");

        File targetFolder = new File(DataManager.getInstance().getConfiguration().getDownloadFolder(PDFDownloadJob.LOCAL_TYPE));
        if (!targetFolder.isDirectory() && !targetFolder.mkdir()) {
            return ReturnValue.ERROR;
        }

        String cleanedPi = StringTools.cleanUserGeneratedData(pi);
        String cleanedLogId = StringTools.cleanUserGeneratedData(logId);

        String title = cleanedPi + "_" + cleanedLogId;

        return ReturnValue.FINISH;
    }

    private File createPdf(File metsFile, File imageFolder, File inputPdfFolder, File altoFolder, File exportFile) throws IOException,
            ContentLibException {
        //        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
        //            Map<String, String> params = new HashMap<String, String>();
        //            params.put("metsFile", metsFile.getAbsolutePath());
        //            if (imageFolder != null) {
        //                params.put("imageSource", imageFolder.toURI().toString());
        //            }
        //            if (inputPdfFolder != null && PdfCreationConfiguration.getInstance().isUsePdfDirectory()) {
        //                params.put("pdfSource", inputPdfFolder.toURI().toString());
        //            }
        //            if (altoFolder != null && PdfCreationConfiguration.getInstance().isUseAltoDirectory()) {
        //                params.put("altoSource", altoFolder.toURI().toString());
        //            }
        //            params.put("metsFileGroup", "LOCAL");
        //            params.put("goobiMetsFile", "true");
        //            GetMetsPdfAction action = new GetMetsPdfAction();
        //            logger.debug("Calling GetMetsPdfAction with parameters: " + params);
        //            action.writePdf(params, fos);
        //        }
        return exportFile;
    }

    @Override
    public String getMessageHandlerName() {
        return "pdfDownload";
    }

}
