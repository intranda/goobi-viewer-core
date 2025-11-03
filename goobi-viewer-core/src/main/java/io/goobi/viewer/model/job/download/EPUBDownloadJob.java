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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.job.JobStatus;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * <p>
 * EPUBDownloadJob class.
 * </p>
 */
@Entity
@DiscriminatorValue(EPUBDownloadJob.LOCAL_TYPE)
public class EPUBDownloadJob extends DownloadJob {

    private static final long serialVersionUID = 5799943793394080870L;

    /** Constant <code>TYPE="epub"</code> */
    public static final String LOCAL_TYPE = "epub";

    private static final Logger logger = LogManager.getLogger(EPUBDownloadJob.class);

    /**
     * <p>
     * Constructor for EPUBDownloadJob.
     * </p>
     */
    public EPUBDownloadJob() {
        type = LOCAL_TYPE;
    }

    /**
     * <p>
     * Constructor for EPUBDownloadJob.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param logid a {@link java.lang.String} object.
     * @param lastRequested a {@link java.time.LocalDateTime} object.
     * @param ttl a long.
     */
    public EPUBDownloadJob(String pi, String logid, LocalDateTime lastRequested, long ttl) {
        type = LOCAL_TYPE;
        this.pi = pi;
        this.logId = logid;
        this.lastRequested = lastRequested;
        this.ttl = ttl;
        this.setStatus(JobStatus.INITIALIZED);
        generateDownloadIdentifier();
    }

    /** {@inheritDoc} */
    @Override
    public final void generateDownloadIdentifier() {
        this.identifier = generateDownloadJobId(LOCAL_TYPE, pi, logId);
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return "application/epub+zip";
    }

    /** {@inheritDoc} */
    @Override
    public String getFileExtension() {
        return ".epub";
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return "EPUB";
    }

    /** {@inheritDoc} */
    @Override
    protected String getRestApiPath() {
        return "/viewerepub";
    }

    @Deprecated(since = "24.10")
    @Override
    protected void triggerCreation() throws PresentationException, IndexUnreachableException {
        // TODO Auto-generated method stub

    }
}
