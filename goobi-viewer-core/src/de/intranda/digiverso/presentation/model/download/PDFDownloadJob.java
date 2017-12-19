/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.model.download;

import java.io.File;
import java.util.Date;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.ocr.OcrClient;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DownloadException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;

@Entity
@DiscriminatorValue(PDFDownloadJob.TYPE)
public class PDFDownloadJob extends DownloadJob {

    private static final long serialVersionUID = 250689453571003230L;

    public static final String TYPE = "pdf";

    private static final Logger logger = LoggerFactory.getLogger(PDFDownloadJob.class);

    public PDFDownloadJob() {
        type = TYPE;
    }

    public PDFDownloadJob(String pi, String logid, Date lastRequested, long ttl) {
        type = TYPE;
        this.pi = pi;
        this.logId = logid;
        this.lastRequested = lastRequested;
        this.ttl = ttl;
        this.setStatus(JobStatus.INITIALIZED);
        generateDownloadIdentifier();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.misc.DownloadJob#generateDownloadIdentifier()
     */
    @Override
    public final void generateDownloadIdentifier() {
        this.identifier = generateDownloadJobId(TYPE, pi, logId);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.misc.DownloadJob#getMimeType()
     */
    @Override
    public String getMimeType() {
        return "application/pdf";
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.misc.DownloadJob#getFileExtension()
     */
    @Override
    public String getFileExtension() {
        return ".pdf";
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.download.DownloadJob#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return "PDF";
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.download.DownloadJob#getSize()
     */
    @Override
    public long getSize() {
        File downloadFile = getDownloadFileStatic(identifier, type, getFileExtension());
        if (downloadFile.isFile()) {
            return downloadFile.length();
        }

        return getPdfSizeFromTaskManager(identifier);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.download.DownloadJob#triggerCreation(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    protected void triggerCreation() throws PresentationException, IndexUnreachableException {
        triggerCreation(pi, logId, identifier);
    }

    /**
     *
     * @param pi
     * @param logId
     * @param downloadIdentifier
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static void triggerCreation(String pi, String logId, String downloadIdentifier) throws PresentationException, IndexUnreachableException,
            DownloadException {
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        File mediaRepository = new File(DataManager.getInstance().getConfiguration().getViewerHome());
        if (StringUtils.isNotEmpty(dataRepository)) {
            mediaRepository = new File(DataManager.getInstance().getConfiguration().getDataRepositoriesHome(), dataRepository);
        }
        File targetFolder = new File(DataManager.getInstance().getConfiguration().getDownloadFolder(PDFDownloadJob.TYPE));
        if (!targetFolder.isDirectory() && !targetFolder.mkdir()) {
            //            logger.error("Cannot create download folder: {}", targetFolder);
            throw new DownloadException("Cannot create download folder: " + targetFolder);
        }
        String title = pi + "_" + logId;
        logger.debug("Trigger pdf generation for " + title);

        int priority = 10;
        HttpClient client = HttpClients.createDefault();
        String taskManagerUrl = DataManager.getInstance().getConfiguration().getTaskManagerServiceUrl();
        logger.debug("Calling taskManager at " + taskManagerUrl);
        File metsFile = new File(mediaRepository + "/indexed_mets", pi + ".xml");
        HttpPost post = OcrClient.createPost(taskManagerUrl, metsFile.getAbsolutePath(), targetFolder.getAbsolutePath(), "", "", priority, logId,
                title, mediaRepository.getAbsolutePath(), "VIEWERPDF", downloadIdentifier, "noServerTypeInTaskClient", "", "", "", "", false);
        try {
            JSONObject response = OcrClient.getJsonResponse(client, post);
            logger.trace(response.toString());
            if (response.get("STATUS").equals("ERROR")) {
                if (response.get("ERRORMESSAGE").equals("Job already in DB, not adding it!")) {
                    logger.debug("Job is already being processed");
                } else {
                    throw new DownloadException("Failed to start pdf creation for PI=" + pi + " and LOGID=" + logId + ": TaskManager returned error "
                            + response.get("ERRORMESSAGE"));
                    //                    logger.error("Failed to start pdf creation for PI={} and LOGID={}: TaskManager returned error", pi, logId);
                    //                    return false;
                }
            }
        } catch (Exception e) {
            // Had to catch generic exception here because a ParseException triggered by Tomcat error HTML getting parsed as JSON cannot be caught
            throw new DownloadException("Failed to start pdf creation for PI=" + pi + " and LOGID=" + logId + ": " + e.getMessage());
            //            logger.error("Failed to start pdf creation for PI={} and LOGID={}: {}", pi, logId, e.getMessage());
            //            logger.error(e.getMessage(), e);
            //            return false;
        }
    }

    /**
     *
     * @param identtifier The identifier/has of the last job to count
     * @return
     */
    public static int getPDFJobsInQueue(String identifier) {
        StringBuilder url = new StringBuilder();
        url.append(DataManager.getInstance().getConfiguration().getTaskManagerRestUrl());
        url.append("/viewerpdf/numJobsUntil/");
        url.append(identifier);
        ResponseHandler<String> handler = new BasicResponseHandler();
        HttpGet httpGet = new HttpGet(url.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            CloseableHttpResponse response = httpclient.execute(httpGet);
            String ret = handler.handleResponse(response);
            logger.trace("TaskManager response: {}", ret);
            return Integer.parseInt(ret);
        } catch (Throwable e) {
            logger.error("Error getting response from TaskManager", e);
            return -1;
        }
    }

    /**
     * @param identifier
     */
    protected static long getPdfSizeFromTaskManager(String identifier) {
        StringBuilder url = new StringBuilder();
        url.append(DataManager.getInstance().getConfiguration().getTaskManagerRestUrl());
        url.append("/viewerpdf/pdfSize/");
        url.append(identifier);
        ResponseHandler<String> handler = new BasicResponseHandler();
        HttpGet httpGet = new HttpGet(url.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            CloseableHttpResponse response = httpclient.execute(httpGet);
            String ret = handler.handleResponse(response);
            logger.trace("TaskManager response: {}", ret);
            return Long.parseLong(ret);
        } catch (Throwable e) {
            logger.error("Error getting response from TaskManager", e);
            return -1;
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.download.DownloadJob#getQueuePosition()
     */
    @Override
    public int getQueuePosition() {
        switch (status) {
            case ERROR:
                return -1;
            case READY:
                return 0;
            default:
                return getPDFJobsInQueue(identifier);
        }
    }
}
