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
package io.goobi.viewer.model.job.download;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DownloadException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.job.JobStatus;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * <p>
 * PDFDownloadJob class.
 * </p>
 */
@Entity
@DiscriminatorValue(PDFDownloadJob.LOCAL_TYPE)
public class PDFDownloadJob extends DownloadJob {

    private static final long serialVersionUID = 250689453571003230L;

    /** Constant <code>TYPE="pdf"</code> */
    public static final String LOCAL_TYPE = "pdf";

    private static final Logger logger = LogManager.getLogger(PDFDownloadJob.class);

    /**
     * <p>
     * Constructor for PDFDownloadJob.
     * </p>
     */
    public PDFDownloadJob() {
        type = LOCAL_TYPE;
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
        type = LOCAL_TYPE;
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
        this.identifier = generateDownloadJobId(LOCAL_TYPE, pi, logId);
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
     */
    public static void triggerCreation(String pi, String logId, String downloadIdentifier)
            throws PresentationException, IndexUnreachableException {

        File targetFolder = new File(DataManager.getInstance().getConfiguration().getDownloadFolder(PDFDownloadJob.LOCAL_TYPE));
        if (!targetFolder.isDirectory() && !targetFolder.mkdir()) {
            throw new DownloadException("Cannot create download folder: " + targetFolder);
        }

        String cleanedPi = StringTools.cleanUserGeneratedData(pi);
        String cleanedLogId = StringTools.cleanUserGeneratedData(logId);

        String title = cleanedPi + "_" + cleanedLogId;
        logger.debug("Trigger PDF generation for {}", title);

        String taskManagerUrl = DataManager.getInstance().getConfiguration().getTaskManagerServiceUrl();
        String mediaRepository = DataFileTools.getDataRepositoryPathForRecord(cleanedPi);
        // Path imageFolder = Paths.get(mediaRepository).resolve(DataManager.getInstance().getConfiguration().getMediaFolder()).resolve(pi);
        Path metsPath =
                Paths.get(mediaRepository).resolve(DataManager.getInstance().getConfiguration().getIndexedMetsFolder()).resolve(cleanedPi + ".xml");

        logger.debug("Calling taskManager at {}", taskManagerUrl);

        TaskManagerPDFRequest requestObject = new TaskManagerPDFRequest();
        requestObject.setPi(cleanedPi);
        requestObject.setLogId(cleanedLogId);
        requestObject.setGoobiId(downloadIdentifier);
        requestObject.setSourceDir(metsPath.toString());
        try {
            Response response = postJobRequest(taskManagerUrl, requestObject);
            String entity = response.readEntity(String.class);
            JSONObject entityJson = new JSONObject(entity);
            if (entityJson.has("STATUS") && "ERROR".equals(entityJson.get("STATUS"))) {
                if ("Job already in DB, not adding it!".equals(entityJson.get("ERRORMESSAGE"))) {
                    logger.debug("Job is already being processed");
                } else {
                    throw new DownloadException(
                            "Failed to start pdf creation for PI=" + cleanedPi + " and LOGID=" + cleanedLogId + ": TaskManager returned error "
                                    + entityJson.get("ERRORMESSAGE"));
                }
            }
        } catch (Exception e) {
            // Had to catch generic exception here because a ParseException triggered by Tomcat error HTML getting parsed as JSON cannot be caught
            throw new DownloadException("Failed to start pdf creation for PI=" + cleanedPi + " and LOGID=" + cleanedLogId + ": " + e.getMessage());
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
        // TODO replace it with message count
        return 1;
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
