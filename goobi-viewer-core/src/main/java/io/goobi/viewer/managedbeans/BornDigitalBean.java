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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.BCrypt;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.faces.validators.EmailValidator;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.tickets.DownloadTicket;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.mail.MessagingException;

/**
 * Handles download ticket checks and requests for born-digital files.
 */
@Named
@SessionScoped
public class BornDigitalBean implements Serializable {

    private static final long serialVersionUID = -371794671604543166L;

    private static final Logger logger = LogManager.getLogger(BornDigitalBean.class);

    @Inject
    private ActiveDocumentBean activeDocumentBean;
    @Inject
    private CaptchaBean captchaBean;
    @Inject
    private UserBean userBean;

    private transient String downloadTicketPassword;
    private String downloadTicketEmail;
    private String downloadTicketRequestMessage;

    /**
     * 
     * @return true if session contains permission for current record, false otherwise;
     * @throws IndexUnreachableException
     */
    public boolean isHasDownloadTicket() throws IndexUnreachableException {
        return AccessConditionUtils.isHasDownloadTicket(activeDocumentBean.getPersistentIdentifier(), BeanUtils.getSession());
    }

    /**
     * Checks the given download ticket password for validity for the current record and persists valid permission in the agent session.
     * 
     * @return empty string
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public String checkDownloadTicketPasswordAction() throws DAOException, IndexUnreachableException {
        logger.trace("checkDownloadTicketPasswordAction");
        if (StringUtils.isEmpty(downloadTicketPassword)) {
            Messages.error(StringConstants.MSG_ERR_PASSWORD_INCORRECT);
            return "";
        }

        // brute force check
        String ipAddress = NetTools.getIpAddress(BeanUtils.getRequest());
        long delay = DataManager.getInstance().getSecurityManager().getDelayForIpAddress(ipAddress);
        if (delay > 0) {
            logger.trace("Password delay: {} ms", delay);
            String msg =
                    ViewerResourceBundle.getTranslation("errLoginDelay", BeanUtils.getLocale())
                            .replace("{0}", String.valueOf((int) Math.ceil(delay / 1000.0)));
            Messages.error(msg);
            return "";
        }

        try {
            String hash = BCrypt.hashpw(downloadTicketPassword, DownloadTicket.SALT);
            DownloadTicket ticket = DataManager.getInstance().getDao().getDownloadTicketByPasswordHash(hash);
            String pi = activeDocumentBean.getPersistentIdentifier();
            if ("-".equals(pi)) {
                Messages.error("errPassword");
                return "";
            }
            if (ticket != null && ticket.isActive() && ticket.getPi().equals(pi) && ticket.checkPassword(downloadTicketPassword)
                    && AccessConditionUtils.addDownloadTicketToSession(pi, BeanUtils.getSession())) {
                logger.trace("Born digital download permission for {} added to user session.", pi);
                DataManager.getInstance().getSecurityManager().resetFailedLoginAttemptForIpAddress(ipAddress);
                Messages.info("");
                return "";
            }

            logger.trace("password incorrect");
            DataManager.getInstance().getSecurityManager().addFailedLoginAttemptForIpAddress(ipAddress);
            Messages.error(StringConstants.MSG_ERR_PASSWORD_INCORRECT);
            return "";
        } finally {
            downloadTicketPassword = null;
        }
    }

    /**
     * 
     * @return empty string
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public String requestNewDownloadTicketAction() throws DAOException, IndexUnreachableException {
        if (StringUtils.isEmpty(downloadTicketEmail)) {
            Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
            return "";
        }

        if (captchaBean == null) {
            Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
            return "";
        }

        // Check whether the security question has been answered correct, if configured
        if (!captchaBean.checkAnswer()) {
            Messages.error("user__security_question_wrong");
            return "";
        }

        DownloadTicket ticket = new DownloadTicket();
        if (activeDocumentBean != null && activeDocumentBean.isRecordLoaded()) {
            ticket.setPi(activeDocumentBean.getPersistentIdentifier());
            ticket.setTitle(activeDocumentBean.getViewManager().getTopDocumentTitle());
        }
        ticket.setEmail(downloadTicketEmail);
        if (StringUtils.isNotEmpty(downloadTicketRequestMessage)) {
            ticket.setRequestMessage(downloadTicketRequestMessage);
        }

        if (DataManager.getInstance().getDao().addDownloadTicket(ticket)) {
            downloadTicketEmail = null;
            downloadTicketRequestMessage = null;

            // Notify the requesting party of a successful request via e-mail
            sendEmailNotification(Collections.singletonList(ticket.getEmail()),
                    ViewerResourceBundle.getTranslation(StringConstants.MSG_DOWNLOAD_TICKET_EMAIL_SUBJECT, BeanUtils.getLocale())
                            .replace("{0}", ticket.getPi()),
                    ViewerResourceBundle.getTranslation("download_ticket__email_body_request_sent", BeanUtils.getLocale())
                            .replace("{0}", ticket.getPi()));

            // Notify admin(s)
            if (EmailValidator.validateEmailAddress(DataManager.getInstance().getConfiguration().getDefaultFeedbackEmailAddress())) {
                sendEmailNotification(Collections.singletonList(DataManager.getInstance().getConfiguration().getDefaultFeedbackEmailAddress()),
                        ViewerResourceBundle.getTranslation("download_ticket__email_admin_subject", BeanUtils.getLocale())
                                .replace("{0}", ticket.getEmail())
                                .replace("{1}", ticket.getPi()),
                        ViewerResourceBundle.getTranslation("download_ticket__email_admin_body_request_sent", BeanUtils.getLocale())
                                .replace("{0}", ticket.getEmail())
                                .replace("{1}", ticket.getPi())
                                .replace("{2}", ticket.getRequestMessage()));
            }

            Messages.info("download_ticket__request_created");
        } else {
            Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
        }

        return "";
    }

    /**
     * 
     * @param recipients
     * @param subject
     * @param body
     */
    private static void sendEmailNotification(List<String> recipients, String subject, String body) {
        try {
            if (!NetTools.postMail(recipients, null, null, subject, body)) {
                Messages.error(StringConstants.MSG_ERR_SEND_EMAIL);
            }
        } catch (UnsupportedEncodingException | MessagingException e) {
            logger.error(e.getMessage());
            Messages.error(StringConstants.MSG_ERR_SEND_EMAIL);
        }
    }

    /**
     * @return the downloadTicketPassword
     */
    public String getDownloadTicketPassword() {
        return downloadTicketPassword;
    }

    /**
     * @param downloadTicketPassword the downloadTicketPassword to set
     */
    public void setDownloadTicketPassword(String downloadTicketPassword) {
        this.downloadTicketPassword = downloadTicketPassword;
    }

    /**
     * @return the downloadTicketEmail
     */
    public String getDownloadTicketEmail() {
        if (downloadTicketEmail == null && userBean != null && userBean.getUser() != null) {
            downloadTicketEmail = userBean.getUser().getEmail();
        }
        return downloadTicketEmail;
    }

    /**
     * @param downloadTicketEmail the downloadTicketEmail to set
     */
    public void setDownloadTicketEmail(String downloadTicketEmail) {
        this.downloadTicketEmail = downloadTicketEmail;
    }

    /**
     * @return the downloadTicketRequestMessage
     */
    public String getDownloadTicketRequestMessage() {
        return downloadTicketRequestMessage;
    }

    /**
     * @param downloadTicketRequestMessage the downloadTicketRequestMessage to set
     */
    public void setDownloadTicketRequestMessage(String downloadTicketRequestMessage) {
        this.downloadTicketRequestMessage = downloadTicketRequestMessage;
    }
}
