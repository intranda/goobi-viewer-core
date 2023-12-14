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
package io.goobi.viewer.managedbeans;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetAction;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.DownloadException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.download.EPUBDownloadJob;
import io.goobi.viewer.model.job.download.PDFDownloadJob;

/**
 * <p>
 * DownloadBean class.
 * </p>
 */
@Named
@SessionScoped
public class DownloadBean implements Serializable {

    private static final long serialVersionUID = 1418828357626472799L;

    private static final Logger logger = LogManager.getLogger(DownloadBean.class);

    private static long ttl = 1209600000;

    private String downloadIdentifier;
    private DownloadJob downloadJob;

    /**
     * <p>
     * reset.
     * </p>
     */
    public void reset() {
        synchronized (this) {
            logger.debug("reset (thread {})", Thread.currentThread().getId());
            downloadIdentifier = null;
            downloadJob = null;
        }
    }

    /**
     * <p>
     * getTimeToLive.
     * </p>
     *
     * @return a long.
     */
    public static long getTimeToLive() {
        return ttl;
    }

    /**
     * <p>
     * checkDownloadAction.
     * </p>
     *
     * @param type a {@link java.lang.String} object.
     * @param email a {@link java.lang.String} object.
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    @Deprecated
    public String checkDownloadAction(String type, String email, String pi, String logId)
            throws DAOException, PresentationException, IndexUnreachableException {
        if (DownloadJob.checkDownload(type, email, pi, logId, DownloadJob.generateDownloadJobId(type, pi, logId), ttl) != null) {
            return "pretty:download1";
        }

        return "pretty:error";
    }

    /**
     * <p>
     * openDownloadAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.DownloadException if any.
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

    /**
     * <p>
     * downloadFileAction.
     * </p>
     *
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.DownloadException if any.
     */
    public void downloadFileAction() throws IOException, DownloadException {
        if (downloadJob != null) {
            Path file = downloadJob.getFile();
            if (file == null) {
                logger.error("File not found for job ID '{}'.", downloadJob.getIdentifier());
                throw new DownloadException("downloadErrorNotFound");
            }
            String fileName;
            switch (downloadJob.getType()) {
                case PDFDownloadJob.LOCAL_TYPE:
                    fileName = downloadJob.getPi() + (StringUtils.isNotEmpty(downloadJob.getLogId()) ? ("_" + downloadJob.getLogId()) : "") + ".pdf";
                    break;
                case EPUBDownloadJob.LOCAL_TYPE:
                    fileName = downloadJob.getPi() + (StringUtils.isNotEmpty(downloadJob.getLogId()) ? ("_" + downloadJob.getLogId()) : "") + ".epub";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported job type: " + downloadJob.getType());
            }

            FacesContext fc = FacesContext.getCurrentInstance();
            ExternalContext ec = fc.getExternalContext();
            // Some JSF component library or some Filter might have set some headers in the buffer beforehand.
            // We want to get rid of them, else it may collide.
            ec.responseReset();
            ec.setResponseContentType(downloadJob.getMimeType());
            ec.setResponseHeader("Content-Length", String.valueOf(Files.size(file)));
            ec.setResponseHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + fileName + "\"");
            OutputStream os = ec.getResponseOutputStream();
            try (FileInputStream fis = new FileInputStream(file.toFile())) {
                byte[] buffer = new byte[1024];
                int bytesRead = 0;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                if (GetAction.isClientAbort(e)) {
                    logger.trace("Download of '{}' aborted: {}", fileName, e.getMessage());
                    return;
                }
                throw e;
            }
            // os.flush();
            // Important! Otherwise JSF will attempt to render the response which obviously
            // will fail since it's already written with a file and closed.
            fc.responseComplete();
        }
    }

    /**
     * <p>
     * Getter for the field <code>downloadIdentifier</code>.
     * </p>
     *
     * @param criteria a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getDownloadIdentifier(String... criteria) {
        return DownloadJob.generateDownloadJobId(criteria);
    }

    /**
     * <p>
     * Getter for the field <code>downloadIdentifier</code>.
     * </p>
     *
     * @return the downloadIdentifier
     */
    public String getDownloadIdentifier() {
        return downloadIdentifier;
    }

    /**
     * <p>
     * Setter for the field <code>downloadIdentifier</code>.
     * </p>
     *
     * @param downloadIdentifier the downloadIdentifier to set
     */
    public void setDownloadIdentifier(String downloadIdentifier) {
        this.downloadIdentifier = downloadIdentifier;
    }

    /**
     * <p>
     * Getter for the field <code>downloadJob</code>.
     * </p>
     *
     * @return the downloadJob
     */
    public DownloadJob getDownloadJob() {
        return downloadJob;
    }

    /**
     * <p>
     * Setter for the field <code>downloadJob</code>.
     * </p>
     *
     * @param downloadJob the downloadJob to set
     */
    public void setDownloadJob(DownloadJob downloadJob) {
        this.downloadJob = downloadJob;
    }

}
