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
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.faces.FacesException;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.event.PhaseId;
import javax.servlet.http.HttpSession;

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
 * <p>
 * MyExceptionHandler class.
 * </p>
 */
public class MyExceptionHandler extends ExceptionHandlerWrapper {

    private static final Logger logger = LogManager.getLogger(MyExceptionHandler.class);

    /**
     * <p>
     * Constructor for MyExceptionHandler.
     * </p>
     *
     * @param wrapped a {@link javax.faces.context.ExceptionHandler} object.
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
            if (!t.getClass().equals(ViewExpiredException.class) && !t.getClass().equals(PrettyException.class)) {
                logger.error("CLASS: {}", t.getClass().getName());
            } else {
                logger.trace(t.getClass().getSimpleName());
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
                } else if (t instanceof BaseXException || isCausedByExceptionType(t, BaseXException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(BaseXException.class.getSimpleName()))) {
                    logger.trace("Caused by BaseXException");
                    handleError(null, "basex");
                } else if (t instanceof ViewerConfigurationException || isCausedByExceptionType(t, ViewerConfigurationException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(ViewerConfigurationException.class.getSimpleName()))) {
                    logger.trace("Caused by ViewerConfigurationException");
                    String msg = getRootCause(t).getMessage();
                    logger.error(getRootCause(t).getMessage());
                    handleError(msg, "configuration");
                } else if (t instanceof SocketException || isCausedByExceptionType(t, SocketException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(SocketException.class.getSimpleName()))) {
                    //do nothing
                } else if (t instanceof DownloadException || isCausedByExceptionType(t, DownloadException.class.getName())
                        || (t instanceof PrettyException && t.getMessage().contains(DownloadException.class.getSimpleName()))) {
                    logger.error(getRootCause(t).getMessage());
                    String msg = getRootCause(t).getMessage();
                    if (msg.contains(DownloadException.class.getSimpleName() + ":")) {
                        msg = msg.substring(StringUtils.lastIndexOf(msg, ":") + 1).trim();
                    }
                    handleError(msg, "download");
                } else {
                    // All other exceptions
                    logger.error(t.getMessage(), t);
                    // Put the exception in the flash scope to be displayed in the error page if necessary ...

                    String msg = LocalDateTime.now().format(DateTools.formatterISO8601DateTime) + ": " + t.getMessage();
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
     * @param errorDetails
     * @param errorType
     */
    private void handleError(String errorDetails, String errorType) {
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, Object> requestMap = fc.getExternalContext().getRequestMap();

        Flash flash = fc.getExternalContext().getFlash();
        flash.setKeepMessages(true);

        putNavigationState(requestMap, flash);
        PhaseId phase = fc.getCurrentPhaseId();
        if (PhaseId.RENDER_RESPONSE == phase) {
            flash.putNow("ErrorPhase", phase.toString());
            flash.putNow("errorDetails", errorDetails);
            flash.putNow("errorTime", LocalDateTime.now().format(DateTools.formatterISO8601Full));
            flash.putNow("errorType", errorType);
        } else {
            flash.put("ErrorPhase", phase.toString());
            flash.put("errorDetails", errorDetails);
            flash.put("errorTime", LocalDateTime.now().format(DateTools.formatterISO8601Full));
            flash.put("errorType", errorType);
        }

        requestMap.put("errMsg", errorDetails);
        requestMap.put("errorType", errorType);

        redirect("pretty:error");
    }

    /**
     *
     * @param target
     */
    private static void redirect(String target) {
        FacesContext fc = FacesContext.getCurrentInstance();
        NavigationHandler nav = fc.getApplication().getNavigationHandler();
        nav.handleNavigation(fc, null, target);
        fc.renderResponse();
    }

    /**
     * @param t
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
     * @param requestMap
     * @param flash
     */
    public void putNavigationState(Map<String, Object> requestMap, Flash flash) {
        NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
        if (navigationHelper != null) {
            requestMap.put("sourceUrl", navigationHelper.getCurrentUrl());
            flash.put("sourceUrl", navigationHelper.getCurrentUrl());
        }
    }

    /**
     * @param fc
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
                .append("</br>")
                .append("Session created: ")
                .append(DateTools.getLocalDateTimeFromMillis(session.getCreationTime(), false))
                .append("</br>")
                .append("Session last accessed: ")
                .append(DateTools.getLocalDateTimeFromMillis(session.getLastAccessedTime(), false));

        Optional<Map<Object, Map>> logicalViews =
                Optional.ofNullable((Map) session.getAttribute("com.sun.faces.renderkit.ServerSideStateHelper.LogicalViewMap"));
        Integer numberOfLogicalViews = logicalViews.map(map -> map.keySet().size()).orElse(0);
        Integer numberOfTotalViews =
                logicalViews.map(map -> map.values().stream().mapToInt(value -> value.keySet().size()).sum()).orElse(0);
        details.append("</br>");
        details.append("Logical Views stored in session: ").append(numberOfLogicalViews.toString());
        details.append("</br>");
        details.append("Total views stored in session: ").append(numberOfTotalViews.toString());

        return details.toString();
    }

    /**
     * Checks whether the given Throwable was at some point caused by an IndexUnreachableException.
     *
     * @param t
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
