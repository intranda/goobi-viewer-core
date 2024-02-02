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
package io.goobi.viewer.model.annotation.notification;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.viewer.PageType;
import jakarta.mail.MessagingException;

/**
 * @author florian
 *
 */
public class CommentMailNotificator implements ChangeNotificator {

    private static final Logger logger = LogManager.getLogger(CommentMailNotificator.class);

    private List<String> recipients;
    private List<String> bcc;

    /**
     *
     * @param recipients
     */
    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    /**
     * @return the bcc
     */
    public List<String> getBcc() {
        return bcc;
    }

    /**
     * @param bcc the bcc to set
     */
    public void setBcc(List<String> bcc) {
        this.bcc = bcc;
    }

    /**
     * 
     * @param viewerRootUrl
     * @param annotation
     * @return Hyperlink element containing the record and page URL
     * @should build element correctly
     */
    static String buildRecordUrlElement(String viewerRootUrl, PersistentAnnotation annotation) {
        if (viewerRootUrl == null) {
            return "";
        }
        if (annotation == null) {
            return "";
        }

        String url = viewerRootUrl.trim() + (viewerRootUrl.trim().endsWith("/") ? "" : "/") + PageType.viewObject.getName() + '/'
                + annotation.getTargetPI()
                + '/' + annotation.getTargetPageOrder() + '/';

        return "<a href=\"" + url + "\">" + url + "</a><br/><br/>";
    }

    /** {@inheritDoc} */
    @Override
    public void notifyCreation(PersistentAnnotation annotation, Locale locale, String viewerRootUrl) {

        String subject = ViewerResourceBundle.getTranslation("commentNewNotificationEmailSubject", locale);
        subject = subject.replace("{0}", getCreator(annotation))
                .replace("{1}", annotation.getTargetPI())
                .replace("{2}", String.valueOf(annotation.getTargetPageOrder()));
        String url = buildRecordUrlElement(viewerRootUrl, annotation);
        String body = url + ViewerResourceBundle.getTranslation("commentNewNotificationEmailBody", locale);
        body = body.replace("{0}", annotation.getContentString());
        sendEmailNotifications(subject, body);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyEdit(PersistentAnnotation oldAnnotation, PersistentAnnotation newAnnotation, Locale locale, String viewerRootUrl) {

        String subject = ViewerResourceBundle.getTranslation("commentChangedNotificationEmailSubject", locale);
        subject = subject.replace("{0}", getCreator(newAnnotation))
                .replace("{1}", newAnnotation.getTargetPI())
                .replace("{2}", String.valueOf(newAnnotation.getTargetPageOrder()));
        String url = buildRecordUrlElement(viewerRootUrl, newAnnotation);
        String body = url + ViewerResourceBundle.getTranslation("commentChangedNotificationEmailBody", locale);
        body = body.replace("{0}", oldAnnotation.getContentString()).replace("{1}", newAnnotation.getContentString());
        sendEmailNotifications(subject, body);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyDeletion(PersistentAnnotation annotation, Locale locale) {

        //no notification
    }

    /** {@inheritDoc} */
    @Override
    public void notifyError(Exception exception, Locale locale) {

        //no notification
    }

    /**
     * Sends an email notification about a new or altered comment to the configured recipient addresses.
     *
     * @param subject E-mail subject
     * @param body E-mail body
     * @return true if mail sent successfully; false otherwise
     */
    private boolean sendEmailNotifications(String subject, String body) {
        if (recipients == null || recipients.isEmpty()) {
            return false;
        }

        try {
            NetTools.postMail(recipients, null, bcc, subject, body);
            return true;
        } catch (UnsupportedEncodingException | MessagingException e) {
            logger.error(e.getMessage(), e);
        }

        return false;
    }

    /**
     * 
     * @param annotation
     * @return Creator display name; 'unknown' if none foudn
     */
    private static String getCreator(PersistentAnnotation annotation) {
        try {
            User user = annotation.getCreator();
            if (user != null) {
                return annotation.getCreator().getDisplayName();
            }
            return "unknown";
        } catch (DAOException e) {
            return "unknown";
        }
    }

}
