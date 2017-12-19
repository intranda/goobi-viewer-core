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
import de.intranda.digiverso.presentation.managedbeans.CmsBean;

@Entity
@DiscriminatorValue(EPUBDownloadJob.TYPE)
public class EPUBDownloadJob extends DownloadJob {

    private static final long serialVersionUID = 5799943793394080870L;

    public static final String TYPE = "epub";

    private static final Logger logger = LoggerFactory.getLogger(EPUBDownloadJob.class);

    public EPUBDownloadJob() {
        type = TYPE;
    }

    public EPUBDownloadJob(String pi, String logid, Date lastRequested, long ttl) {
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
        return "application/epub+zip";
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.misc.DownloadJob#getFileExtension()
     */
    @Override
    public String getFileExtension() {
        return ".epub";
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.download.DownloadJob#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return "EPUB";
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

        return getEpubSizeFromTaskManager(identifier);
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
                return getEPUBJobsInQueue(identifier);
        }
    }

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
        } catch (Throwable e) {
            logger.error("Error getting response from TaskManager", e);
            return -1;
        }
    }

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
        } catch (Throwable e) {
            logger.error("Error getting response from TaskManager", e);
            return -1;
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.download.DownloadJob#triggerCreation(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    protected void triggerCreation() throws PresentationException, IndexUnreachableException {
        triggerCreation(pi, identifier, DataManager.getInstance().getConfiguration().getDownloadFolder(EPUBDownloadJob.TYPE));
    }

    /**
     *
     * @param pi
     * @param downloadIdentifier
     * @param targetFolderPath
     * @return null if successful; error msg otherwise
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static void triggerCreation(String pi, String downloadIdentifier, String targetFolderPath) throws PresentationException,
            IndexUnreachableException, DownloadException {
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        File mediaRepository = new File(DataManager.getInstance().getConfiguration().getViewerHome());
        if (StringUtils.isNotEmpty(dataRepository)) {
            mediaRepository = new File(DataManager.getInstance().getConfiguration().getDataRepositoriesHome(), dataRepository);
        }
        File targetFolder = new File(DataManager.getInstance().getConfiguration().getDownloadFolder(EPUBDownloadJob.TYPE));
        if (!targetFolder.isDirectory() && !targetFolder.mkdir()) {
            throw new DownloadException("Cannot create download folder: " + targetFolder);
        }
        String title = pi;

        int priority = 10;
        HttpClient client = HttpClients.createDefault();
        String taskManagerUrl = DataManager.getInstance().getConfiguration().getTaskManagerServiceUrl();
        File metsFile = new File(mediaRepository + "/indexed_mets", pi + ".xml");
        HttpPost post = OcrClient.createPost(taskManagerUrl, metsFile.getAbsolutePath(), targetFolder.getAbsolutePath(), CmsBean.getCurrentLocale()
                .getLanguage(), "", priority, "", title, mediaRepository.getAbsolutePath(), "VIEWEREPUB", downloadIdentifier,
                "noServerTypeInTaskClient", "", "", "", CmsBean.getCurrentLocale().getLanguage(), false);
        try {
            JSONObject response = OcrClient.getJsonResponse(client, post);
            logger.trace(response.toString());
            if (response.get("STATUS").equals("ERROR")) {
                if (response.get("ERRORMESSAGE").equals("Job already in DB, not adding it!")) {
                    logger.debug("Job is already being processed");
                } else {
                    throw new DownloadException("Failed to start pdf creation for PI=" + pi + ": TaskManager returned error " + response.get(
                            "ERRORMESSAGE"));
                    //                    logger.error("Failed to start pdf creation for PI={} and LOGID={}: TaskManager returned error", pi, logId);
                    //                    return false;
                }
            }
        } catch (Exception e) {
            // Had to catch generic exception here because a ParseException triggered by Tomcat error HTML getting parsed as JSON cannot be caught
            throw new DownloadException("Failed to start pdf creation for PI=" + pi + ": " + e.getMessage());
            //            logger.error("Failed to start epub creation for PI={}: {}", pi, e.getMessage());
            //            logger.error(e.getMessage(), e);
            //            return false;
        }
    }
}
