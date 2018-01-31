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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.DownloadException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.download.DownloadJob;
import de.intranda.digiverso.presentation.model.download.EPUBDownloadJob;
import de.intranda.digiverso.presentation.model.download.PDFDownloadJob;

@Named
@SessionScoped
public class DownloadBean implements Serializable {

    private static final long serialVersionUID = 1418828357626472799L;

    private static final Logger logger = LoggerFactory.getLogger(DownloadBean.class);

    private static long ttl = 1209600000;

    private String downloadIdentifier;
    private DownloadJob downloadJob;

    public void reset() {
        synchronized (this) {
            logger.debug("reset (thread {})", Thread.currentThread().getId());
            downloadIdentifier = null;
            downloadJob = null;
        }
    }

    public static long getTimeToLive() {
        return ttl;
    }

    /**
     *
     * @param type
     * @param email
     * @param pi
     * @param logId
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @Deprecated
    public String checkDownloadAction(String type, String email, String pi, String logId) throws DAOException, PresentationException,
            IndexUnreachableException {
        if (DownloadJob.checkDownload(type, email, pi, logId, DownloadJob.generateDownloadJobId(type, pi, logId), ttl)) {
            return "pretty:download1";
        }

        return "pretty:error";
    }

    /**
     * 
     * @return
     * @throws DAOException
     * @throws DownloadException if download job not found
     */
    public String openDownloadAction() throws DAOException, DownloadException {
        downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(downloadIdentifier);
        //        downloadJob.updateStatus();
        if (downloadJob == null) {
            logger.error("Download job with the ID {} not found.", downloadIdentifier);
            throw new DownloadException("downloadErrorNotFound");
        }
        return "";
    }

    public void downloadFileAction() throws IOException, DownloadException {
        if (downloadJob != null) {
            Path file = downloadJob.getFile();
            if (file == null) {
                logger.error("File not found for job ID '{}'.", downloadJob.getIdentifier());
                throw new DownloadException("downloadErrorNotFound");
            }
            String fileName;
            switch (downloadJob.getType()) {
                case PDFDownloadJob.TYPE:
                    fileName = downloadJob.getPi() + (StringUtils.isNotEmpty(downloadJob.getLogId()) ? ("_" + downloadJob.getLogId()) : "") + ".pdf";
                    break;
                case EPUBDownloadJob.TYPE:
                    fileName = downloadJob.getPi() + (StringUtils.isNotEmpty(downloadJob.getLogId()) ? ("_" + downloadJob.getLogId()) : "") + ".epub";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported job type: " + downloadJob.getType());
            }

            FacesContext fc = FacesContext.getCurrentInstance();
            ExternalContext ec = fc.getExternalContext();
            ec.responseReset(); // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
            ec.setResponseContentType(downloadJob.getMimeType());
            ec.setResponseHeader("Content-Length", String.valueOf(Files.size(file)));
            ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            OutputStream os = ec.getResponseOutputStream();
            try (FileInputStream fis = new FileInputStream(file.toFile())) {
                byte[] buffer = new byte[1024];
                int bytesRead = 0;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (ClientAbortException e) {
                logger.warn("Download of '{}' aborted: {}", fileName, e.getMessage());
                Messages.error("downloadError");
                return;
            }
            // os.flush();
            fc.responseComplete(); // Important! Otherwise JSF will attempt to render the response which obviously will fail since it's already written with a file and closed.
        }
    }

    /**
     *
     * @param criteria
     * @return
     */
    public String getDownloadIdentifier(String... criteria) {
        return DownloadJob.generateDownloadJobId(criteria);
    }

    /**
     * @return the downloadIdentifier
     */
    public String getDownloadIdentifier() {
        return downloadIdentifier;
    }

    /**
     * @param downloadIdentifier the downloadIdentifier to set
     */
    public void setDownloadIdentifier(String downloadIdentifier) {
        this.downloadIdentifier = downloadIdentifier;
    }

    /**
     * @return the downloadJob
     */
    public DownloadJob getDownloadJob() {
        return downloadJob;
    }

    /**
     * @param downloadJob the downloadJob to set
     */
    public void setDownloadJob(DownloadJob downloadJob) {
        this.downloadJob = downloadJob;
    }

    public long getDownloadSize() {
        return downloadJob.getSize();
    }
}
