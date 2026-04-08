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
package io.goobi.viewer.model.viewer;

import java.io.Serializable;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Convenience class for user feedback submission.
 */
public class Feedback implements Serializable {

    private static final long serialVersionUID = 1774579852945909396L;

    private static final Logger logger = LogManager.getLogger(Feedback.class);

    private String name;
    private String senderAddress;
    private String url;
    private String type;
    private String message;
    private String recipientAddress;

    /**
     * getEmailSubject.
     *
     * @param key message key to resolve as email subject
     * @return the translated email subject text for the given message key
     */
    public String getEmailSubject(String key) {
        return ViewerResourceBundle.getTranslation(key, null);
    }

    /**
     * getEmailBody.
     *
     * @param key message key to resolve as email body template
     * @return the translated and placeholder-substituted email body text for the given message key
     */
    public String getEmailBody(String key) {
        String body = ViewerResourceBundle.getTranslation(key, null);
        if (body != null) {
            body = body
                    .replace("{0}", name)
                    .replace("{1}", senderAddress)
                    .replace("{2}", url == null ? "" : url)
                    .replace("{3}", message);
            // Feedback type only exists for crowdsourcing feedback
            if (type != null) {
                body = body.replace("{4}", type);
            }
        }

        return body;
    }

    /**
     * Getter for the field <code>name</code>.
     *
     * @return the sender's name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for the field <code>name</code>.
     *
     * @param name the sender's name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for the field <code>senderAddress</code>.
     *
     * @return the sender's email address
     */
    public String getSenderAddress() {
        return senderAddress;
    }

    /**
     * Setter for the field <code>senderAddress</code>.
     *
     * @param senderAddress the sender's email address to set
     */
    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    /**
     * Getter for the field <code>url</code>.
     *
     * @return the page URL from which the feedback was submitted
     */
    public String getUrl() {
        return url;
    }

    /**
     * Setter for the field <code>url</code>.
     *
     * @param url the page URL from which the feedback was submitted
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Getter for the field <code>type</code>.
     *
     * @return the feedback type or category
     */
    public String getType() {
        return type;
    }

    /**
     * Setter for the field <code>type</code>.
     *
     * @param type the feedback type or category to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Getter for the field <code>message</code>.
     *
     * @return the feedback message text
     */
    public String getMessage() {
        return message;
    }

    /**
     * Setter for the field <code>message</code>.
     *
     * @param message the feedback message text to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    
    public String getRecipientAddress() {
        return recipientAddress;
    }

    
    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }
}
