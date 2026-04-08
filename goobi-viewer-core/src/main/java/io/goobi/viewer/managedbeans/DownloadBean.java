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
import java.util.UUID;

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
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.download.EpubDownloadJob;
import io.goobi.viewer.model.job.download.PdfDownloadJob;
import io.goobi.viewer.model.statistics.usage.RequestType;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;

/**
 * JSF backing bean for managing file download requests from the viewer, tracking download status and links.
 */
@Named
@ViewScoped
public class DownloadBean implements Serializable {

    private static final long serialVersionUID = 1418828357626472799L;

    private static final Logger logger = LogManager.getLogger(DownloadBean.class);

    @Inject
    protected HttpServletRequest request;

    @Inject
    private transient MessageQueueManager messageBroker;

    private String downloadIdentifier;
    private ViewerMessage message;

    private String email = BeanUtils.getUserBean().getEmail();

    /**
     * reset.
     */
    public void reset() {
        synchronized (this) {
            logger.debug("reset (thread {})", Thread.currentThread().threadId());
            downloadIdentifier = null;
        }
    }

    /**
     * openDownloadAction.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws PresentationException
     */
    public String openDownloadAction() throws DAOException, PresentationException, RecordNotFoundException {

        if (StringUtils.isNotBlank(downloadIdentifier)) {
            //get message after creating file in message queue
            message = DataManager.getInstance().getDao().getViewerMessageByMessageID(downloadIdentifier);
            if (message == null && downloadIdentifier.matches("\\d+")) {
                //get message directly added to database because file already exists
                message = DataManager.getInstance().getDao().getViewerMessage(Long.parseLong(downloadIdentifier));
            }
            if (message == null) {
                message = messageBroker.getMessageById(downloadIdentifier).orElse(null);
            }
        }

        if (message != null) {
            return message.getMessageId();
        } else {
            // No download job found for this identifier — show the record-not-found error page
            // instead of a toast, as this is an invalid/unknown URL.
            throw new RecordNotFoundException(downloadIdentifier);
        }
    }

    /**
     * getQueuePosition.
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
     * Getter for the field <code>message</code>.
     *
     * @return a {@link io.goobi.viewer.controller.mq.ViewerMessage} object
     */
    public ViewerMessage getMessage() {
        return message;
    }

    /**
     * downloadFileAction.
     *
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.DownloadException if any.
     */
    public void downloadFileAction() throws IOException, DownloadException {
        DownloadJob job;
        try {
            job = DownloadJob.from(message);
        } catch (IllegalArgumentException e) {
            // Unknown task type (neither DOWNLOAD_PDF nor DOWNLOAD_EPUB) — surface as download error
            throw new DownloadException(e.getMessage(), e);
        }
        Path file = job.getPath();
        if (!Files.exists(file)) {
            // Job exists in the database but the file is gone (e.g. deleted externally or not yet created)
            throw new DownloadException("Download file not found: " + file.getFileName());
        }
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        // Some JSF component library or some Filter might have set some headers in the buffer beforehand.
        // We want to get rid of them, else it may collide.
        ec.responseReset();
        ec.setResponseContentType(job.getMimeType());
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
                        .recordRequest(RequestType.FILE_DOWNLOAD, job.getPi(), (HttpServletRequest) ec.getRequest());
            } catch (DAOException e) {
                logger.error("Cannot count usage statistics for file download of {}. Error connecting to database: {}",
                        job.getPi(), e.toString());
            }
        } else {
            logger.warn("Cannot count usage statistics for file download of {}. Request object is not of expected type {}",
                    job.getPi(), HttpServletRequest.class);
        }
        // os.flush();
        // Important! Otherwise JSF will attempt to render the response which obviously
        // will fail since it's already written with a file and closed.
        fc.responseComplete();
    }

    /**
     * Getter for the field <code>downloadIdentifier</code>.
     *
     * @return the identifier used to locate and serve the requested download resource
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
     * Setter for the field <code>downloadIdentifier</code>.
     *
     * @param downloadIdentifier the persistent identifier of the record whose download job is being managed
     */
    public void setDownloadIdentifier(String downloadIdentifier) {
        this.downloadIdentifier = downloadIdentifier;
    }

    public void createDownloadJob(String type, String pi, String logId, String usePdfSource, String configVariant)
            throws JsonProcessingException, DAOException, URISyntaxException {
        switch (type) {
            case "epub":
                createEpubDownloadJob(pi);
                break;
            case "pdf":
            default:
                createPDFDownloadJob(pi, logId, usePdfSource, configVariant);
        }
    }

    public void createEpubDownloadJob(String pi)
            throws DAOException, URISyntaxException, JsonProcessingException {

        if (this.messageBroker == null) {
            logger.error("No message broker avaible. Aborting task");
            return;
        }

        ViewerMessage message = new ViewerMessage(TaskType.DOWNLOAD_EPUB.name());

        if (StringUtils.isNotBlank(email)) {
            message.getProperties().put("email", email.toLowerCase());
        }
        message.getProperties().put("pi", pi);

        message.getProperties().put("viewerUrl", BeanUtils.getServletPathWithHostAsUrlFromJsfContext());

        EpubDownloadJob job = new EpubDownloadJob(message);
        try {
            if (Files.exists(job.getPath()) && !job.isLocked()) {
                message.setMessageStatus(MessageStatus.FINISH);
                //String fileId = URLEncoder.encode(FilenameUtils.getBaseName(job.getFilename()), Charset.defaultCharset());
                message.setMessageId(UUID.randomUUID().toString());
                DataManager.getInstance().getDao().addViewerMessage(message);
                if (message.getId() != null) {
                    URI downloadPageUrl = getDownloadPageUrl(message.getMessageId());
                    PrettyUrlTools.redirectToUrl(downloadPageUrl.toString());
                    return;
                } else {
                    logger.error("Unable to add message {} to database", message);
                }
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

    public void createPDFDownloadJob(String pi, String logId, String usePdfSource, String configVariant)
            throws DAOException, URISyntaxException, JsonProcessingException {

        if (this.messageBroker == null) {
            logger.error("No message broker avaible. Aborting task");
            return;
        }
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

        message.getProperties().put("viewerUrl", BeanUtils.getServletPathWithHostAsUrlFromJsfContext());

        PdfDownloadJob job = new PdfDownloadJob(message);
        try {
            if (Files.exists(job.getPath()) && !job.isLocked()) {
                //pdf already created
                message.setMessageStatus(MessageStatus.FINISH);
                //String fileId = URLEncoder.encode(FilenameUtils.getBaseName(job.getFilename()), Charset.defaultCharset());
                message.setMessageId(UUID.randomUUID().toString());
                DataManager.getInstance().getDao().addViewerMessage(message);
                if (message.getId() != null) {
                    URI downloadPageUrl = getDownloadPageUrl(message.getMessageId());
                    PrettyUrlTools.redirectToUrl(downloadPageUrl.toString());
                    return;
                } else {
                    logger.error("Unable to add message {} to database", message);
                }
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

    private URI getDownloadPageUrl(String id) throws URISyntaxException {
        String uri = PrettyUrlTools.getAbsolutePageUrl("download1", id);
        return URI.create(uri);
    }

}
