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
package io.goobi.viewer.model.download;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.ws.rs.core.Response;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DownloadException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

/**
 * <p>
 * PDFDownloadJob class.
 * </p>
 */
@Entity
@DiscriminatorValue(PDFDownloadJob.TYPE)
public class PDFDownloadJob extends DownloadJob {

    private static final long serialVersionUID = 250689453571003230L;

    /** Constant <code>TYPE="pdf"</code> */
    public static final String TYPE = "pdf";

    private static final Logger logger = LoggerFactory.getLogger(PDFDownloadJob.class);

    /**
     * <p>
     * Constructor for PDFDownloadJob.
     * </p>
     */
    public PDFDownloadJob() {
        type = TYPE;
    }

    /**
     * <p>
     * Constructor for PDFDownloadJob.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param logid a {@link java.lang.String} object.
     * @param lastRequested a {@link java.time.LocalDateTime} object.
     * @param ttl a long.
     */
    public PDFDownloadJob(String pi, String logid, LocalDateTime lastRequested, long ttl) {
        type = TYPE;
        this.pi = pi;
        this.logId = logid;
        this.lastRequested = lastRequested;
        this.ttl = ttl;
        this.setStatus(JobStatus.INITIALIZED);
        generateDownloadIdentifier();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.misc.DownloadJob#generateDownloadIdentifier()
     */
    /** {@inheritDoc} */
    @Override
    public final void generateDownloadIdentifier() {
        this.identifier = generateDownloadJobId(TYPE, pi, logId);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.misc.DownloadJob#getMimeType()
     */
    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return "application/pdf";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.misc.DownloadJob#getFileExtension()
     */
    /** {@inheritDoc} */
    @Override
    public String getFileExtension() {
        return ".pdf";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.download.DownloadJob#getDisplayName()
     */
    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return "PDF";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.download.DownloadJob#getSize()
     */
    /** {@inheritDoc} */
    @Override
    public long getSize() {
        File downloadFile = getDownloadFileStatic(identifier, type, getFileExtension());
        if (downloadFile.isFile()) {
            return downloadFile.length();
        }

        return getPdfSizeFromTaskManager(identifier);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.download.DownloadJob#triggerCreation(java.lang.String, java.lang.String, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    protected void triggerCreation() throws PresentationException, IndexUnreachableException {
        triggerCreation(pi, logId, identifier);
    }

    /**
     * <p>
     * triggerCreation.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param downloadIdentifier a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DownloadException if any.
     */
    public static void triggerCreation(String pi, String logId, String downloadIdentifier)
            throws PresentationException, IndexUnreachableException, DownloadException {
        File targetFolder = new File(DataManager.getInstance().getConfiguration().getDownloadFolder(PDFDownloadJob.TYPE));
        if (!targetFolder.isDirectory() && !targetFolder.mkdir()) {
            throw new DownloadException("Cannot create download folder: " + targetFolder);
        }
        String title = pi + "_" + logId;
        logger.debug("Trigger pdf generation for " + title);

        String taskManagerUrl = DataManager.getInstance().getConfiguration().getTaskManagerServiceUrl();
        String mediaRepository = DataFileTools.getDataRepositoryPathForRecord(pi);
        Path imageFolder = Paths.get(mediaRepository).resolve(DataManager.getInstance().getConfiguration().getMediaFolder()).resolve(pi);
        Path metsPath = Paths.get(mediaRepository).resolve(DataManager.getInstance().getConfiguration().getIndexedMetsFolder()).resolve(pi + ".xml");

        logger.debug("Calling taskManager at " + taskManagerUrl);

        TaskManagerPDFRequest requestObject = new TaskManagerPDFRequest();
        requestObject.pi = pi;
        requestObject.logId = logId;
        requestObject.goobiId = downloadIdentifier;
        requestObject.sourceDir = metsPath.toString();
        try {
            Response response = postJobRequest(taskManagerUrl, requestObject);
            String entity = response.readEntity(String.class);
            JSONObject entityJson = new JSONObject(entity);
            if (entityJson.has("STATUS") && entityJson.get("STATUS").equals("ERROR")) {
                if (entityJson.get("ERRORMESSAGE").equals("Job already in DB, not adding it!")) {
                    logger.debug("Job is already being processed");
                } else {
                    throw new DownloadException("Failed to start pdf creation for PI=" + pi + " and LOGID=" + logId + ": TaskManager returned error "
                            + entityJson.get("ERRORMESSAGE"));
                }
            }
        } catch (Exception e) {
            // Had to catch generic exception here because a ParseException triggered by Tomcat error HTML getting parsed as JSON cannot be caught
            throw new DownloadException("Failed to start pdf creation for PI=" + pi + " and LOGID=" + logId + ": " + e.getMessage());
        }
    }

    /**
     * <p>
     * getPDFJobsInQueue.
     * </p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @return a int.
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
            logger.error("Error getting response from TaskManager: " + e.toString());
            return -1;
        }
    }

    /**
     * <p>
     * getPdfSizeFromTaskManager.
     * </p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @return a long.
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
     * @see io.goobi.viewer.model.download.DownloadJob#getQueuePosition()
     */
    /** {@inheritDoc} */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.download.DownloadJob#getRestApiPath()
     */
    @Override
    protected String getRestApiPath() {
        return "/viewerpdf";
    }
}
