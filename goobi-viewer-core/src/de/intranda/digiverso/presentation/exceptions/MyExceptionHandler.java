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
package de.intranda.digiverso.presentation.exceptions;

import java.net.SocketException;
import java.util.Iterator;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyException;

import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

/*
 * taken from here:
 * http://www.facebook.com/note.php?note_id=125229397708&comments&ref=mf
 */
public class MyExceptionHandler extends ExceptionHandlerWrapper {

    private static final Logger logger = LoggerFactory.getLogger(MyExceptionHandler.class);

    private ExceptionHandler wrapped;

    public MyExceptionHandler(ExceptionHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return this.wrapped;
    }

    @Override
    public void handle() throws FacesException {
        for (Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator(); i.hasNext();) {
            ExceptionQueuedEvent event = i.next();
            ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();
            Throwable t = context.getException();
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
            Map<String, Object> requestMap = fc.getExternalContext().getRequestMap();
            NavigationHandler nav = fc.getApplication().getNavigationHandler();
            Flash flash = fc.getExternalContext().getFlash();
            NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
            flash.setKeepMessages(true);
            if (navigationHelper != null) {
                requestMap.put("sourceUrl", navigationHelper.getCurrentUrl());
                // Flash data can only be read once
                flash.put("sourceUrl", navigationHelper.getCurrentUrl());
                flash.put("sourceUrl2", navigationHelper.getCurrentUrl());
            }
            if (t instanceof ViewExpiredException) {
                ViewExpiredException vee = (ViewExpiredException) t;
                try {
                    // Push some useful stuff to the request scope for use in the page
                    requestMap.put("currentViewId", vee.getViewId());
                    requestMap.put("errorType", "viewExpired");
                    flash.put("errorType", "viewExpired");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else if (t instanceof RecordNotFoundException || isCausedByExceptionType(t, RecordNotFoundException.class.getName())
                    || (t instanceof PrettyException && t.getMessage().contains(RecordNotFoundException.class.getSimpleName()))) {
                try {
                    String pi = t.getMessage().substring(t.getMessage().indexOf("RecordNotFoundException: ")).replace("RecordNotFoundException: ",
                            "");
                    String msg = Helper.getTranslation("errRecordNotFoundMsg", null).replace("{0}", pi);
                    flash.put("errorDetails", msg);
                    requestMap.put("errMsg", msg);
                    requestMap.put("errorType", "recordNotFound");
                    flash.put("errorType", "recordNotFound");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else if (t instanceof RecordDeletedException || isCausedByExceptionType(t, RecordDeletedException.class.getName())
                    || (t instanceof PrettyException && t.getMessage().contains(RecordDeletedException.class.getSimpleName()))) {
                try {
                    String pi = t.getMessage().substring(t.getMessage().indexOf("RecordDeletedException: ")).replace("RecordDeletedException: ", "");
                    String msg = Helper.getTranslation("errRecordDeletedMsg", null).replace("{0}", pi);
                    flash.put("errorDetails", msg);
                    requestMap.put("errMsg", msg);
                    requestMap.put("errorType", "recordDeleted");
                    flash.put("errorType", "recordDeleted");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else if (t instanceof IndexUnreachableException || isCausedByExceptionType(t, IndexUnreachableException.class.getName())
                    || (t instanceof PrettyException && t.getMessage().contains(IndexUnreachableException.class.getSimpleName()))) {
                logger.trace("Caused by IndexUnreachableException");
                try {
                    requestMap.put("errorType", "indexUnreachable");
                    flash.put("errorType", "indexUnreachable");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else if (t instanceof DAOException || isCausedByExceptionType(t, DAOException.class.getName()) || (t instanceof PrettyException && t
                    .getMessage().contains(IndexUnreachableException.class.getSimpleName()))) {
                logger.trace("Caused by DAOException");
                try {
                    requestMap.put("errorType", "dao");
                    flash.put("errorType", "dao");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else if (t instanceof SocketException || isCausedByExceptionType(t, SocketException.class.getName()) || (t instanceof PrettyException
                    && t.getMessage().contains(SocketException.class.getSimpleName()))) {
                logger.error(t.getMessage());
                try {
                } finally {
                    i.remove();
                }
            } else if (t instanceof DownloadException || isCausedByExceptionType(t, DownloadException.class.getName())
                    || (t instanceof PrettyException && t.getMessage().contains(DownloadException.class.getSimpleName()))) {
                logger.error(t.getMessage());
                String msg = t.getMessage();
                if (msg.contains(DownloadException.class.getSimpleName() + ":")) {
                    msg = msg.substring(StringUtils.lastIndexOf(msg, ":") + 1).trim();
                }
                try {
                    flash.put("errorDetails", msg);
                    requestMap.put("errMsg", msg);
                    requestMap.put("errorType", "download");
                    flash.put("errorType", "download");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else {
                // All other exceptions
                logger.error(t.getMessage(), t);
                try {
                    // Put the exception in the flash scope to be displayed in the error page if necessary ...
                    String msg = new StringBuilder(DateTools.formatterISO8601DateTime.print(System.currentTimeMillis())).append(": ").append(t
                            .getMessage()).toString();
                    flash.put("errorDetails", msg);
                    requestMap.put("errMsg", msg);
                    requestMap.put("errorType", "general");
                    flash.put("errorType", "general");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            }

        }

        // At this point, the queue will not contain any ViewExpiredEvents.
        // Therefore, let the parent handle them.
        getWrapped().handle();

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

}