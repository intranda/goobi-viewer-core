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
package io.goobi.viewer.model.email;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

public class EMailSender {

    private static final Logger logger = LogManager.getLogger(EMailSender.class);

    private final String smtpServer;
    private final String smtpUser;
    private final String smtpPassword;
    private final String smtpSenderAddress;
    private final String smtpSenderName;
    private final String smtpSecurity;
    private final Integer smtpPort;

    public EMailSender() {
        this(DataManager.getInstance().getConfiguration());
    }

    /**
     * 
     * @param config
     */
    public EMailSender(Configuration config) {
        this(config.getSmtpServer(), config.getSmtpUser(), config.getSmtpPassword(), config.getSmtpSenderAddress(), config.getSmtpSenderName(),
                config.getSmtpSecurity(), config.getSmtpPort());
    }

    /**
     * @param smtpServer
     * @param smtpUser
     * @param smtpPassword
     * @param smtpSenderAddress
     * @param smtpSenderName
     * @param smtpSecurity
     * @param smtpPort
     */
    public EMailSender(String smtpServer, String smtpUser, String smtpPassword, String smtpSenderAddress, String smtpSenderName, String smtpSecurity,
            Integer smtpPort) {
        if (smtpServer == null) {
            throw new IllegalArgumentException("smtpServer may not be null");
        }
        if (smtpSenderAddress == null) {
            throw new IllegalArgumentException("smtpSenderAddress may not be null");
        }
        if (smtpSenderName == null) {
            throw new IllegalArgumentException("smtpSenderName may not be null");
        }
        if (smtpSecurity == null) {
            throw new IllegalArgumentException("smtpSecurity may not be null");
        }
        this.smtpServer = smtpServer;
        this.smtpUser = smtpUser;
        this.smtpPassword = smtpPassword;
        this.smtpSenderAddress = smtpSenderAddress;
        this.smtpSenderName = smtpSenderName;
        this.smtpSecurity = smtpSecurity;
        this.smtpPort = smtpPort;
    }

    /**
     * Sends an email to with the given subject and body to the given recipient list using the given SMTP parameters.
     *
     * @param recipients
     * @param cc
     * @param bcc
     * @param subject
     * @param body
     * @return true if mail sent successfully; false otherwise
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public boolean postMail(List<String> recipients, List<String> cc, List<String> bcc, String subject, String body)
            throws MessagingException, UnsupportedEncodingException {
        if (recipients == null) {
            throw new IllegalArgumentException("recipients may not be null");
        }
        if (subject == null) {
            throw new IllegalArgumentException("subject may not be null");
        }
        if (body == null) {
            throw new IllegalArgumentException("body may not be null");
        }
        if (StringUtils.isNotEmpty(smtpPassword) && StringUtils.isEmpty(smtpUser)) {
            logger.warn("stmpPassword is configured but smtpUser is not, ignoring smtpPassword.");
        }

        boolean debug = false;
        boolean auth = StringUtils.isNotEmpty(smtpUser);

        logger.debug("Connecting to email server {} on port {} via SMTP security {}", smtpServer, String.valueOf(smtpPort),
                smtpSecurity.toUpperCase());

        Session session = createSession(debug, auth);
        Message msg = createMessage(recipients, cc, bcc, subject, body, session);
        Transport.send(msg);

        return true;
    }

    /**
     * 
     * @param recipients
     * @param cc
     * @param bcc
     * @param subject
     * @param body
     * @param session
     * @return Created {@link Message}
     * @throws UnsupportedEncodingException
     * @throws MessagingException
     */
    private Message createMessage(List<String> recipients, List<String> cc, List<String> bcc, String subject, String body, Session session)
            throws UnsupportedEncodingException, MessagingException {
        Message msg = new MimeMessage(session);
        InternetAddress addressFrom = new InternetAddress(smtpSenderAddress, smtpSenderName);
        msg.setFrom(addressFrom);

        // Recipients
        msg.setRecipients(Message.RecipientType.TO, prepareRecipients(recipients));

        // CC
        if (cc != null && !cc.isEmpty()) {
            msg.setRecipients(Message.RecipientType.CC, prepareRecipients(cc));
        }

        // BCC

        if (bcc != null && !bcc.isEmpty()) {
            msg.setRecipients(Message.RecipientType.BCC, prepareRecipients(bcc));
        }
        // Optional : You can also set your custom headers in the Email if you want
        // msg.addHeader("MyHeaderName", "myHeaderValue");
        msg.setSubject(subject);

        // Message body
        MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setText(body, "utf-8");
        messagePart.setHeader(NetTools.HTTP_HEADER_CONTENT_TYPE, "text/html; charset=\"utf-8\"");
        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(messagePart);
        msg.setContent(multipart);

        msg.setSentDate(new Date());
        return msg;
    }

    private Session createSession(boolean debug, boolean auth) {
        Properties props = createProperties(auth);
        Session session;
        if (auth) {
            // with authentication
            session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, smtpPassword);
                }
            });
        } else {
            // w/o authentication
            session = Session.getInstance(props, null);
        }
        session.setDebug(debug);
        return session;
    }

    /**
     * 
     * @param auth
     * @return {@link Properties}
     */
    private Properties createProperties(boolean auth) {
        Properties props = new Properties();
        switch (smtpSecurity.toUpperCase()) {
            case "STARTTLS":
                logger.debug("Using STARTTLS");
                props.setProperty("mail.transport.protocol", "smtp");
                props.setProperty("mail.smtp.port", String.valueOf(smtpPort == -1 ? 25 : smtpPort));
                props.setProperty("mail.smtp.host", smtpServer);
                //                props.setProperty("mail.smtp.ssl.trust", "*");
                //                props.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
                props.setProperty("mail.smtp.starttls.enable", "true");
                props.setProperty("mail.smtp.starttls.required", "true");
                break;
            case "SSL":
                logger.debug("Using SSL");
                props.setProperty("mail.transport.protocol", "smtp");
                props.setProperty("mail.smtp.host", smtpServer);
                props.setProperty("mail.smtp.port", String.valueOf(smtpPort == -1 ? 465 : smtpPort));
                props.setProperty("mail.smtp.ssl.enable", "true");
                props.setProperty("mail.smtp.ssl.trust", "*");
                break;
            default:
                logger.debug("Using no SMTP security");
                props.setProperty("mail.transport.protocol", "smtp");
                props.setProperty("mail.smtp.port", String.valueOf(smtpPort == -1 ? 25 : smtpPort));
                props.setProperty("mail.smtp.host", smtpServer);
        }
        props.setProperty("mail.smtp.connectiontimeout", "30000");
        props.setProperty("mail.smtp.timeout", "30000");
        props.setProperty("mail.smtp.auth", String.valueOf(auth));
        return props;
    }

    /**
     *
     * @param recipients
     * @return {@link InternetAddress}[]
     * @throws AddressException
     */
    static InternetAddress[] prepareRecipients(List<String> recipients) throws AddressException {
        if (recipients == null) {
            return new InternetAddress[0];
        }

        InternetAddress[] ret = new InternetAddress[recipients.size()];
        int i = 0;
        for (String recipient : recipients) {
            ret[i] = new InternetAddress(recipient);
            i++;
        }

        return ret;
    }

}
