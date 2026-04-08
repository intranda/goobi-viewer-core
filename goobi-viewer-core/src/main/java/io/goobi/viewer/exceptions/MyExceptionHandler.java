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
package io.goobi.viewer.exceptions;

import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import jakarta.faces.FacesException;
import jakarta.faces.application.NavigationHandler;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import jakarta.faces.event.PhaseId;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.PrettyException;

import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;

/*
 * taken from here:
 * http://www.facebook.com/note.php?note_id=125229397708&comments&ref=mf
 */
/**
 * MyExceptionHandler class.
 */
public class MyExceptionHandler extends ExceptionHandlerWrapper {

    private static final Logger logger = LogManager.getLogger(MyExceptionHandler.class);

    /**
     * Creates a new MyExceptionHandler instance.
     *
     * @param wrapped a {@link jakarta.faces.context.ExceptionHandler} object.
     */
    public MyExceptionHandler(ExceptionHandler wrapped) {
        super(wrapped);
    }

    /** {@inheritDoc} */
    @Override
    public void handle() throws FacesException {
        for (Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator(); i.hasNext();) {
            ExceptionQueuedEvent event = i.next();
            ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();
            Throwable t = context.getException();
            Throwable cause = getCause(t);
            // Handle ViewExpiredExceptions here ... or even others :)
            if (t.getClass().equals(ViewExpiredException.class) || t.getClass().equals(PrettyException.class)) {
                logger.trace(t.getClass().getSimpleName());
            } else if (cause instanceof IllegalStateException && cause.getMessage() != null
                    && cause.getMessage().contains("Session already invalidated")) {
                logger.warn("CLASS: {} (session invalidated)", t.getClass().getName());
            } else {
                logger.error("CLASS: {}", t.getClass().getName());
            }
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc == null) {
                return;
            }

            try {
                if (cause instanceof AjaxResponseException) {
                    FacesContext facesContext = FacesContext.getCurrentInstance();
                    ExternalContext externalContext = facesContext.getExternalContext();
                    externalContext.setResponseStatus(500);
                    externalContext.setResponseContentType(StringConstants.MIMETYPE_TEXT_PLAIN);
                    externalContext.setResponseCharacterEncoding(StandardCharsets.UTF_8.name());
                    try {
                        externalContext.getResponseOutputWriter().write(cause.getMessage());
                    } catch (IOException e) {
                        logger.error("Error writing response ", e);
                    } finally {
                        facesContext.responseComplete();
                    }
                } else if (t instanceof ViewExpiredException) {
                    // handleError(getSessionDetails(fc), "viewExpired");
                    // Messages.error(ViewerResourceBundle.getTranslation("sessionExpired", null));
                    // TODO visualize expiration error
                    redirect(StringConstants.PREFIX_PRETTY + PrettyContext.getCurrentInstance().getCurrentMapping().getId());
                } else if (t instanceof RecordNotFoundException || isCausedByExceptionType(t, RecordNotFoundException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(RecordNotFoundException.class.getSimpleName()))) {
                    String pi = "";
                    int index = t.getMessage().indexOf("RecordNotFoundException: ");
                    if (index >= 0) {
                        pi = t.getMessage()
                                .substring(t.getMessage().indexOf("RecordNotFoundException: "))
                                .replace("RecordNotFoundException: ", "");
                    }
                    String msg = ViewerResourceBundle.getTranslation("errRecordNotFoundMsg", null).replace("{0}", pi);
                    handleError(msg, "recordNotFound");

                } else if (t instanceof RecordDeletedException || isCausedByExceptionType(t, RecordDeletedException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(RecordDeletedException.class.getSimpleName()))) {
                    String pi = t.getMessage().substring(t.getMessage().indexOf("RecordDeletedException: ")).replace("RecordDeletedException: ", "");
                    String msg = ViewerResourceBundle.getTranslation("errRecordDeletedMsg", null).replace("{0}", pi);
                    handleError(msg, "recordDeleted");
                } else if (t instanceof RecordLimitExceededException || isCausedByExceptionType(t, RecordLimitExceededException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(RecordLimitExceededException.class.getSimpleName()))) {
                    String msg = createRecodLimitExceededMessage(t);
                    handleError(msg, "errRecordLimitExceeded");
                } else if (t instanceof IndexUnreachableException || isCausedByExceptionType(t, IndexUnreachableException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(IndexUnreachableException.class.getSimpleName()))) {
                    logger.trace("Caused by IndexUnreachableException");
                    logger.error(t.getMessage());
                    handleError(null, "indexUnreachable");
                } else if (t instanceof DAOException || isCausedByExceptionType(t, DAOException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(DAOException.class.getSimpleName()))) {
                    logger.trace("Caused by DAOException");
                    handleError(null, "dao");
                } else if (t instanceof ViewerConfigurationException || isCausedByExceptionType(t, ViewerConfigurationException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(ViewerConfigurationException.class.getSimpleName()))) {
                    logger.trace("Caused by ViewerConfigurationException");
                    String msg = getRootCause(t).getMessage();
                    logger.error(getRootCause(t).getMessage());
                    handleError(msg, "configuration");
                } else if (t instanceof SocketException || isCausedByExceptionType(t, SocketException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(SocketException.class.getSimpleName()))) {
                    //do nothing
                    logger.trace("Caused by SocketException");
                } else if (t instanceof DownloadException || isCausedByExceptionType(t, DownloadException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(DownloadException.class.getSimpleName()))) {
                    logger.error(getRootCause(t).getMessage());
                    String rawMsg = getRootCause(t).getMessage();
                    String msg;
                    // Build a translated message when the exception carries a "file not found" payload
                    if (rawMsg != null && rawMsg.contains("Download file not found: ")) {
                        String filename = rawMsg.substring(rawMsg.indexOf("Download file not found: ") + "Download file not found: ".length()).trim();
                        msg = ViewerResourceBundle.getTranslation("errDownloadFileNotFoundMsg", null).replace("{0}", filename);
                    } else if (rawMsg != null && rawMsg.contains(DownloadException.class.getSimpleName() + ":")) {
                        msg = rawMsg.substring(rawMsg.lastIndexOf(":") + 1).trim();
                    } else {
                        msg = rawMsg;
                    }
                    handleError(msg, "download");
                } else if (t instanceof IllegalUrlParameterException || isCausedByExceptionType(t, IllegalUrlParameterException.class.getName())) {
                    // Use getCause() (walks the full cause chain) instead of getRootCause() (JSF spec,
                    // only unwraps FacesException/ELException — leaves PrettyException as-is)
                    String msg = getCause(t).getMessage();
                    logger.warn(msg);
                    handleError(msg, "general_no_url");
                } else if (cause instanceof IllegalStateException && cause.getMessage() != null
                        && cause.getMessage().contains("Session already invalidated")) {
                    // Session was invalidated (e.g. timeout) while the request was still rendering — expected, not an error
                    logger.warn("Session invalidated during request rendering: {}", cause.getMessage());
                } else if (t instanceof PrettyException
                        && isCausedByExceptionType(t, StringIndexOutOfBoundsException.class.getName())) {
                    // A crafted URL parameter caused a StringIndexOutOfBoundsException during EL
                    // expression evaluation (e.g. a malformed facet value in a CMS page URL).
                    // Downgrade to WARN and treat as an invalid URL rather than an application error.
                    logger.warn("Invalid URL parameter caused StringIndexOutOfBoundsException: {}", t.getMessage());
                    handleError(null, "general_no_url");
                } else if (t instanceof PrettyException
                        && isCausedByExceptionType(t, "jakarta.faces.convert.ConverterException")) {
                    // PrettyFaces URL parameter type conversion failed (e.g. a non-numeric value such as
                    // "+(foo)" in a URL segment that maps to an Integer bean property). This is a
                    // malformed or crafted URL — downgrade to WARN and treat as an invalid URL rather
                    // than an application error.
                    String msg = getCause(t).getMessage();
                    logger.warn("Invalid URL parameter in PrettyFaces mapping: {}", t.getMessage());
                    handleError(msg, "general_no_url");
                } else {
                    // All other exceptions — show root cause class and message for better diagnostics
                    logger.error(t.getMessage(), t);
                    Throwable rootCause = getCause(t);
                    String msg = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
                    handleError(msg, "general");
                }
            } finally {
                i.remove();
            }

        }

        // At this point, the queue will not contain any ViewExpiredEvents.
        // Therefore, let the parent handle them.
        getWrapped().handle();

    }

    /**
     * @param errorDetails human-readable error message stored in request/session
     * @param errorType logical error category used for navigation to the error page
     */
    private void handleError(String errorDetails, String errorType) {
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, Object> requestMap = fc.getExternalContext().getRequestMap();

        boolean responseCommitted = fc.getExternalContext().isResponseCommitted();

        requestMap.put("errMsg", errorDetails);
        requestMap.put("errorType", errorType);

        if (responseCommitted) {
            // Cannot redirect when response is already committed.
            fc.responseComplete();
        } else {
            HttpSession session = (HttpSession) fc.getExternalContext().getSession(true);
            session.setAttribute("ErrorPhase", fc.getCurrentPhaseId().toString());
            session.setAttribute("errorDetails", errorDetails);
            // Use a human-readable format without the ISO 'T' separator and without sub-second precision
            session.setAttribute("errorTime", LocalDateTime.now().format(DateTools.FORMATTERISO8601DATETIMEMS));
            session.setAttribute("errorType", errorType);
            putNavigationState(requestMap, session);
            redirect("pretty:error");
        }
    }

    /**
     *
     * @param target PrettyFaces navigation outcome or view ID to redirect to
     */
    private static void redirect(String target) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc.getExternalContext().isResponseCommitted()) {
            logger.warn("Response already committed, cannot redirect to: {}", target);
            return;
        }
        NavigationHandler nav = fc.getApplication().getNavigationHandler();
        nav.handleNavigation(fc, null, target);
        fc.renderResponse();
    }

    /**
     * @param t throwable carrying the RecordLimitExceededException message
     * @return String
     */
    public String createRecodLimitExceededMessage(Throwable t) {
        String data = t.getMessage()
                .substring(t.getMessage().indexOf("RecordLimitExceededException: "))
                .replace("RecordLimitExceededException: ", "");
        String pi;
        String limit;
        String[] dataSplit = data.split(":");
        if (dataSplit.length == 2) {
            pi = dataSplit[0];
            limit = dataSplit[1];
        } else {
            pi = data;
            limit = "???";
        }

        return ViewerResourceBundle.getTranslation("errRecordLimitExceededMsg", null)
                .replace("{0}", pi)
                .replace("{1}", limit);
    }

    /**
     * @param requestMap current JSF request attribute map
     * @param session current HTTP session for storing navigation state
     */
    public void putNavigationState(Map<String, Object> requestMap, HttpSession session) {
        NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
        if (navigationHelper != null) {
            try {
                // The Weld CDI proxy for NavigationHelper is non-null even when the underlying
                // session-scoped bean's session has been invalidated. Calling getCurrentUrl()
                // on the proxy triggers an internal getAttribute() on the invalidated session,
                // which throws IllegalStateException — guard against this here.
                String currentUrl = navigationHelper.getCurrentUrl();
                requestMap.put("sourceUrl", currentUrl);
                session.setAttribute("sourceUrl", currentUrl);
            } catch (IllegalStateException e) {
                logger.warn("Could not store navigation state: session invalidated during error handling");
            }
        }
    }

    /**
     * @param fc current JSF faces context for accessing the session
     * @return String
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String getSessionDetails(FacesContext fc) {
        HttpSession session = (HttpSession) fc.getExternalContext().getSession(false);
        if (session == null) {
            return "No session details available";
        }

        StringBuilder details = new StringBuilder()
                .append("Session ID: ")
                .append(session.getId())
                .append(StringConstants.HTML_BR)
                .append("Session created: ")
                .append(DateTools.getLocalDateTimeFromMillis(session.getCreationTime(), false))
                .append(StringConstants.HTML_BR)
                .append("Session last accessed: ")
                .append(DateTools.getLocalDateTimeFromMillis(session.getLastAccessedTime(), false));

        Optional<Map<Object, Map>> logicalViews =
                Optional.ofNullable((Map) session.getAttribute("com.sun.faces.renderkit.ServerSideStateHelper.LogicalViewMap"));
        Integer numberOfLogicalViews = logicalViews.map(map -> map.keySet().size()).orElse(0);
        Integer numberOfTotalViews =
                logicalViews.map(map -> map.values().stream().mapToInt(value -> value.keySet().size()).sum()).orElse(0);
        details.append(StringConstants.HTML_BR);
        details.append("Logical Views stored in session: ").append(numberOfLogicalViews.toString());
        details.append(StringConstants.HTML_BR);
        details.append("Total views stored in session: ").append(numberOfTotalViews.toString());

        return details.toString();
    }

    /**
     * Checks whether the given Throwable was at some point caused by an IndexUnreachableException.
     *
     * @param t the throwable to inspect
     * @param className fully qualified class name of the exception type to search for
     * @return true if the root cause of the exception is className
     */
    @SuppressWarnings("rawtypes")
    private static boolean isCausedByExceptionType(Throwable t, String className) {
        Throwable cause = t;
        Class clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        while (cause != null) {
            if (cause.getClass().equals(clazz)) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

    private static Throwable getCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

}
