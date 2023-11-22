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

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import javax.ws.rs.core.Response;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DownloadException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.CmsBean;
import io.goobi.viewer.model.job.JobStatus;

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
        return "application/epub+zip";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.misc.DownloadJob#getFileExtension()
     */
    /** {@inheritDoc} */
    @Override
    public String getFileExtension() {
        return ".epub";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.download.DownloadJob#getDisplayName()
     */
    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return "EPUB";
    }


    /**
     * <p>
     * getEpubSizeFromTaskManager.
     * </p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @return a long.
     */
    protected static long getEpubSizeFromTaskManager(String identifier) {
        StringBuilder url = new StringBuilder();
        url.append(DataManager.getInstance().getConfiguration().getTaskManagerRestUrl());
        url.append("/viewerepub/epubSize/");
        url.append(identifier);
        ResponseHandler<String> handler = new BasicResponseHandler();
        HttpGet httpGet = new HttpGet(url.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            CloseableHttpResponse response = httpclient.execute(httpGet);
            String ret = handler.handleResponse(response);
            logger.trace("TaskManager response: {}", ret);
            return Long.parseLong(ret);
        } catch (Exception e) {
            logger.error("Error getting response from TaskManager", e);
            return -1;
        }
    }

    /**
     * <p>
     * getEPUBJobsInQueue.
     * </p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getEPUBJobsInQueue(String identifier) {
        StringBuilder url = new StringBuilder();
        url.append(DataManager.getInstance().getConfiguration().getTaskManagerRestUrl());
        url.append("/viewerepub/numJobsUntil/");
        url.append(identifier);
        ResponseHandler<String> handler = new BasicResponseHandler();
        HttpGet httpGet = new HttpGet(url.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            CloseableHttpResponse response = httpclient.execute(httpGet);
            String ret = handler.handleResponse(response);
            logger.trace("TaskManager response: {}", ret);
            return Integer.parseInt(ret);
        } catch (Exception e) {
            logger.error("Error getting response from TaskManager", e);
            return -1;
        }
    }

}
