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

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.TaskType;
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
     * @see io.goobi.viewer.model.download.DownloadJob#getRestApiPath()
     */
    @Override
    protected String getRestApiPath() {
        return "/viewerpdf";
    }

    @Override
    protected void triggerCreation() throws PresentationException, IndexUnreachableException {
        ViewerMessage message = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        message.getProperties().put("pi", this.pi);
        if (StringUtils.isNotBlank(this.logId)) {
            message.getProperties().put("logId", this.logId);
        }
        try {
            BeanUtils.getPersistentStorageBean().getMessageBroker().addToQueue(message);
        } catch (MessageQueueException e) {
            throw new PresentationException("Error adding pdf creation message to message queue", e);
        }

    }

    public static String generateJobId(String pi, String logId) {
        return DownloadJob.generateDownloadJobId(LOCAL_TYPE, pi, logId);
    }

}
