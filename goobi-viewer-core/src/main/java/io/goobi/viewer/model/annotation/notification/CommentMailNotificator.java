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
import java.util.List;
import java.util.Locale;

import javax.mail.MessagingException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.User;

/**
 * @author florian
 *
 */
public class CommentMailNotificator implements ChangeNotificator {

    private static final Logger logger = LoggerFactory.getLogger(CommentMailNotificator.class);
    
    private final List<String> addresses;

    public CommentMailNotificator(List<String> addresses) {
        this.addresses = addresses;
    }
    
    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.notification.ChangeNotificator#notifyCreation(io.goobi.viewer.model.annotation.PersistentAnnotation)
     */
    @Override
    public void notifyCreation(CrowdsourcingAnnotation annotation, Locale locale) {

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
    public void notifyEdit(CrowdsourcingAnnotation oldAnnotation, CrowdsourcingAnnotation newAnnotation, Locale locale) {


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
    public void notifyDeletion(CrowdsourcingAnnotation annotation, Locale locale) {

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
        if (addresses == null || addresses.isEmpty()) {
            return false;
        }

        try {
            NetTools.postMail(addresses, subject, body);
            return true;
        } catch (UnsupportedEncodingException | MessagingException e) {
            logger.error(e.getMessage(), e);
        }

        return false;
    }
    
    private String getCreator(CrowdsourcingAnnotation annotation) {
        try {
            User user = annotation.getCreator();
            if(user != null) {                
                return annotation.getCreator().getDisplayName();
            } else {
                return "unknown";
            }
        } catch (DAOException e) {
            return "unknown";
        }
    }

}
