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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private String currentPi;

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
     * @return the message ID of the located download job
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws PresentationException
     * @should throw download exception when message status is error
     * @should throw download exception with fallback message when properties message is blank
     * @should return message id when message status is finish
     */
    public String openDownloadAction() throws DAOException, PresentationException, RecordNotFoundException, DownloadException {

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
            // Job failed during background generation: the worker (e.g. CreateDownloadPdfMessageHandler)
            // stored the cause in properties['message'] and set MessageStatus.ERROR. Route it through
            // MyExceptionHandler -> error.xhtml (errorType "download") instead of letting the JSF view
            // render its inline "Warteschleife" card, which would otherwise mislead the user into
            // believing the job is still queued.
            //
            // Caveat for future maintainers: see MyExceptionHandler.handle(), the DownloadException
            // branch — it inspects this exception's message text. If the string ever contains the
            // literal substring "DownloadException:" (the simple class name followed by a colon, as
            // inserted by Throwable.toString() when the underlying cause is itself a DownloadException),
            // the handler treats it as a wrapped form and keeps only the suffix after the LAST colon —
            // losing context. Today's worker stores either "Error creating PDF: " + e.getMessage() or
            // "...: " + e.toString() with non-Download exception types, so the literal substring is
            // absent and we fall through to the safe pass-through branch. Keep that invariant if you
            // change the worker's message format.
            if (MessageStatus.ERROR.equals(message.getMessageStatus())) {
                // Sanitise the worker-stored detail for the UI (strips absolute paths, rewrites
                // the most common contentlib failure into a friendly form). The un-sanitised
                // message remains intact in the worker log.
                String detail = sanitizeDownloadErrorMessage(
                        message.getProperties().get("message"),
                        message.getProperties().get("pi"));
                throw new DownloadException(StringUtils.isNotBlank(detail) ? detail : "PDF generation failed");
            }
            return message.getMessageId();
        } else {
            // No download job found for this identifier — show the record-not-found error page
            // instead of a toast, as this is an invalid/unknown URL.
            throw new RecordNotFoundException(downloadIdentifier);
        }
    }

    /** Matches a "file:///" URI ending in a filename — capture group 1 is the filename. */
    private static final Pattern FILE_URI_PATH_PATTERN = Pattern.compile("file:///\\S+/([^/\\s]+)");

    /**
     * Matches an absolute Unix filesystem path (at least two segments) ending in a filename — capture group 1 is the filename. The negative
     * lookbehind anchors the match to a path boundary (start of string or whitespace) so we don't accidentally consume parts of arbitrary identifiers
     * that happen to contain slashes (e.g. {@code I/O}).
     */
    // Avoid (?:...)*-style group repetition (Sonar S5998: stack overflow risk on long inputs, since Java's regex
    // engine recurses per group iteration). Greedy \S+ followed by /([^/\s]+) backtracks once to the last slash and
    // is matched iteratively, eliminating the per-segment recursion. Behaviour for path-shaped substrings is
    // unchanged for all realistic worker error messages.
    private static final Pattern ABSOLUTE_PATH_PATTERN =
            Pattern.compile("(?<!\\S)/\\S+/([^/\\s]+)");

    /**
     * Matches the contentlib's most common PDF generation failure, e.g.
     * <code>"Failed to write page 8 to pdf: neither X.tif nor Y.pdf could be resolved"</code>. Run after path stripping so the captured groups are
     * bare filenames rather than file:/// URIs.
     */
    private static final Pattern IMAGE_NOT_FOUND_PATTERN =
            Pattern.compile("Failed to write page \\d+ to pdf: neither (\\S+\\.tif) nor (\\S+\\.pdf) could be resolved",
                    Pattern.CASE_INSENSITIVE);

    /**
     * Sanitises a worker-stored PDF generation error message for display in the UI.
     *
     * <p>
     * Two-tier transform: (1) strips absolute filesystem paths and {@code "file:///"} URIs to filenames so the server-side directory layout is not
     * leaked to end users; (2) when the contentlib's most common "image not found" failure pattern matches, rewrites the text into a user-friendly
     * form including the record PI.
     * </p>
     *
     * <p>
     * The original (un-sanitised) message remains intact in the worker log — this helper only affects the user-facing copy passed to
     * {@link DownloadException}.
     * </p>
     *
     * @param raw worker-stored message text (may be null/blank)
     * @param pi record PI to include in the rewritten form (may be null/blank)
     * @return sanitised, user-safe text; null when raw is blank
     * @should strip file uri paths to filename only
     * @should strip absolute filesystem paths to filename only
     * @should rewrite contentlib image not found pattern into friendly form with pi
     * @should append pi when not already present and pattern does not match
     * @should return null when raw is blank
     * @should not overflow stack on very long paths
     */
    static String sanitizeDownloadErrorMessage(String raw, String pi) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String stripped = FILE_URI_PATH_PATTERN.matcher(raw).replaceAll("$1");
        stripped = ABSOLUTE_PATH_PATTERN.matcher(stripped).replaceAll("$1");

        if (StringUtils.isNotBlank(pi)) {
            Matcher m = IMAGE_NOT_FOUND_PATTERN.matcher(stripped);
            if (m.find()) {
                return "Failed to generate PDF: Unable to find image " + m.group(1)
                        + " or PDF " + m.group(2) + " for PI " + pi + " in the file system.";
            }
            // Fallback: append PI for context if the message doesn't already mention it
            if (!stripped.contains(pi)) {
                return stripped + " (PI: " + pi + ")";
            }
        }
        return stripped;
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
     * @return the ViewerMessage associated with the current download job
     */
    public ViewerMessage getMessage() {
        return message;
    }

    /**
     * downloadFileAction.
     *
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.DownloadException if any.
     * @should throw download exception when unknown task type
     * @should throw download exception when file not found
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
                NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + job.getDownloadFilename() + "\"");
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

    public String getCurrentPi() {
        return currentPi;
    }

    public void setCurrentPi(String currentPi) {
        this.currentPi = currentPi;
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
