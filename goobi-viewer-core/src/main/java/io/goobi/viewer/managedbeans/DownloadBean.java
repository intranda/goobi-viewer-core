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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetAction;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.DownloadException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.PdfGenerator;
import io.goobi.viewer.model.statistics.usage.RequestType;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;

/**
 * <p>
 * DownloadBean class.
 * </p>
 */
@Named
@ViewScoped
public class DownloadBean implements Serializable {

    private static final long serialVersionUID = 1418828357626472799L;

    private static final Logger logger = LogManager.getLogger(DownloadBean.class);

    private static long ttl = 1209600000;

    @Inject
    private transient MessageQueueManager messageBroker;

    private String downloadIdentifier;
    //    private DownloadJob downloadJob;
    private ViewerMessage message;

    private String email = BeanUtils.getUserBean().getEmail();

    /**
     * <p>
     * reset.
     * </p>
     */
    public void reset() {
        synchronized (this) {
            logger.debug("reset (thread {})", Thread.currentThread().threadId());
            downloadIdentifier = null;
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
     * openDownloadAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.DownloadException if any.
     */
    public String openDownloadAction() throws DAOException, DownloadException {
        message = DataManager.getInstance().getDao().getViewerMessageByMessageID(downloadIdentifier);
        if (message == null) {
            String filename = downloadIdentifier + ".pdf";
            Path filePath = Path.of(DataManager.getInstance().getConfiguration().getDownloadFolder(PdfGenerator.TYPE)).resolve(filename);
            if (Files.exists(filePath)) {
                message = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
                message.setMessageStatus(MessageStatus.FINISH);
                message.getProperties().put("path", filePath.toString());
            }
        }
        if (message == null) {
            message = messageBroker.getMessageById(downloadIdentifier).orElse(null);
        }
        if (message != null) {
            return message.getMessageId();
        }
        logger.error("Download job with the ID {} not found.", downloadIdentifier);
        throw new DownloadException("downloadErrorNotFound");
    }

    /**
     * <p>
     * getQueuePosition.
     * </p>
     *
     * @return a int
     */
    public int getQueuePosition() {
        if (message != null) {
            return this.messageBroker.countMessagesBefore(MessageQueueManager.getQueueForMessageType(message.getTaskName()), message.getTaskName(),
                    message.getMessageId());
        }
        return 0;
    }

    /**
     * <p>
     * Getter for the field <code>message</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.controller.mq.ViewerMessage} object
     */
    public ViewerMessage getMessage() {
        return message;
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
        Path file = new PdfGenerator(message).getPath();
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        // Some JSF component library or some Filter might have set some headers in the buffer beforehand.
        // We want to get rid of them, else it may collide.
        ec.responseReset();
        ec.setResponseContentType("application/pdf");
        ec.setResponseHeader("Content-Length", String.valueOf(Files.size(file)));
        ec.setResponseHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION,
                NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + file.getFileName().toString() + "\"");
        OutputStream os = ec.getResponseOutputStream();
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            if (GetAction.isClientAbort(e)) {
                logger.trace("Download of '{}' aborted: {}", file.getFileName(), e.getMessage());
                return;
            }
            throw e;
        }
        if (ec.getRequest() instanceof HttpServletRequest) {
            try {
                DataManager.getInstance()
                        .getUsageStatisticsRecorder()
                        .recordRequest(RequestType.FILE_DOWNLOAD, this.message.getProperties().get("pi"), (HttpServletRequest) ec.getRequest());
            } catch (DAOException e) {
                logger.error("Cannot count usage statistics for file download of {}. Error connecting to database: {}",
                        this.message.getProperties().get("pi"), e.toString());
            }
        } else {
            logger.warn("Cannot count usage statistics for file download of {}. Request object is not of expected type {}",
                    this.message.getProperties().get("pi"), HttpServletRequest.class);
        }
        // os.flush();
        // Important! Otherwise JSF will attempt to render the response which obviously
        // will fail since it's already written with a file and closed.
        fc.responseComplete();
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public void createPDFDownloadJob(String pi, String logId, String usePdfSource, String configVariant)
            throws DAOException, URISyntaxException, JsonProcessingException {

        ViewerMessage message = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());

        if (StringUtils.isNotBlank(email)) {
            message.getProperties().put("email", email.toLowerCase());
        }
        message.getProperties().put("pi", pi);
        if (StringUtils.isNotBlank(logId)) {
            message.getProperties().put("logId", logId);
        }
        if (StringUtils.isNotBlank(usePdfSource)) {
            message.getProperties().put("usePdfSource", usePdfSource);
        }
        if (StringUtils.isNotBlank(configVariant)) {
            message.getProperties().put("configVariant", configVariant);
        }

        PdfGenerator job = new PdfGenerator(message);
        try {
            if (Files.exists(job.getPath()) && !job.isLocked()) {
                //pdf already created
                this.message = message;
                this.message.setMessageStatus(MessageStatus.FINISH);
                String fileId = URLEncoder.encode(FilenameUtils.getBaseName(job.getFilename()), Charset.defaultCharset());
                URI downloadPageUrl = getDownloadPageUrl(fileId);
                PrettyUrlTools.redirectToUrl(downloadPageUrl.toString());
                return;
            }
        } catch (IOException e) {
            logger.warn("Unable to check existence of pdf file {}. Generating pdf in message queue");
        }

        // create new activemq message
        String messageId = message.getMessageId();
        try {
            messageId = this.messageBroker.addToQueue(message);
            messageId = URLEncoder.encode(messageId, Charset.defaultCharset());
        } catch (MessageQueueException e) {
            throw new WebApplicationException(e);
        }

        // forward to download page
        URI downloadPageUrl = getDownloadPageUrl(messageId);
        PrettyUrlTools.redirectToUrl(downloadPageUrl.toString());
    }

    /**
     * 
     * @param id
     * @return {@link URI}
     * @throws URISyntaxException
     */
    private URI getDownloadPageUrl(String id) throws URISyntaxException {
        if (StringUtils.isNotBlank(id)) {
            return new URI(BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/download/" + id + "/");
        } else {
            return new URI(BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/download/");
        }

    }

}
