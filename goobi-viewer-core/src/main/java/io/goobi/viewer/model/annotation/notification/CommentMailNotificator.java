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
package io.goobi.viewer.model.annotation.notification;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

/**
 * @author florian
 *
 */
public class CommentMailNotificator implements ChangeNotificator {

    private static final Logger logger = LoggerFactory.getLogger(CommentMailNotificator.class);

    private List<String> recipients;
    private List<String> bcc;

    public CommentMailNotificator() {
    }

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

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.notification.ChangeNotificator#notifyCreation(io.goobi.viewer.model.annotation.PersistentAnnotation)
     */
    @Override
    public void notifyCreation(PersistentAnnotation annotation, Locale locale) {

        String subject = ViewerResourceBundle.getTranslation("commentNewNotificationEmailSubject", locale);
        subject = subject.replace("{0}", getCreator(annotation))
                .replace("{1}", annotation.getTargetPI())
                .replace("{2}", String.valueOf(annotation.getTargetPageOrder()));
        String body = ViewerResourceBundle.getTranslation("commentNewNotificationEmailBody", locale);
        body = body.replace("{0}", annotation.getContentString());
        sendEmailNotifications(subject, body);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.notification.ChangeNotificator#notifyEdit(io.goobi.viewer.model.annotation.PersistentAnnotation, io.goobi.viewer.model.annotation.PersistentAnnotation)
     */
    @Override
    public void notifyEdit(PersistentAnnotation oldAnnotation, PersistentAnnotation newAnnotation, Locale locale) {

        String subject = ViewerResourceBundle.getTranslation("commentChangedNotificationEmailSubject", locale);
        subject = subject.replace("{0}", getCreator(newAnnotation))
                .replace("{1}", newAnnotation.getTargetPI())
                .replace("{2}", String.valueOf(newAnnotation.getTargetPageOrder()));
        String body = ViewerResourceBundle.getTranslation("commentChangedNotificationEmailBody", locale);
        body = body.replace("{0}", oldAnnotation.getContentString()).replace("{1}", newAnnotation.getContentString());
        sendEmailNotifications(subject, body);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.notification.ChangeNotificator#notifyDeletion(io.goobi.viewer.model.annotation.PersistentAnnotation)
     */
    @Override
    public void notifyDeletion(PersistentAnnotation annotation, Locale locale) {

        //no notification
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.notification.ChangeNotificator#notifyError(java.lang.Exception)
     */
    @Override
    public void notifyError(Exception exception, Locale locale) {

        //no notification
    }

    /**
     * Sends an email notification about a new or altered comment to the configured recipient addresses.
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @param oldText a {@link java.lang.String} object.
     * @param locale Language locale for the email text.
     * @return a boolean.
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
