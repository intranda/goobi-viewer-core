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
package io.goobi.viewer.model.security.authentication.model;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.model.security.authentication.model.VuAuthenticationResponse.Request;

/**
 * Authentication response object for X-Service.
 */
public class XServiceAuthenticationResponse {

    private static final Logger logger = LogManager.getLogger(XServiceAuthenticationResponse.class);

    private String id;
    private String errorMsg;
    private boolean expired = true;
    private Request request;

    /**
     * Parses and evaluates the given XML response from X-Service.
     *
     * @param xml a {@link java.lang.String} object.
     * @param encoding a {@link java.lang.String} object.
     * @throws org.jdom2.JDOMException if any.
     * @throws java.io.IOException if any.
     */
    public XServiceAuthenticationResponse(String xml, String encoding) throws JDOMException, IOException {
        if (xml == null) {
            throw new IllegalArgumentException("xml may not be null");
        }

        Document doc = XmlTools.getDocumentFromString(xml, encoding);
        if (doc == null || doc.getRootElement() == null || doc.getRootElement().getChild("z305") == null) {
            throw new IllegalArgumentException("xml invalid or incomplete");
        }

        Element eleZ305 = doc.getRootElement().getChild("z305");

        errorMsg = doc.getRootElement().getChildText("error");
        if (StringUtils.isNotEmpty(errorMsg)) {
            return;
        }

        id = eleZ305.getChildText("z305-id");
        logger.trace("id: {}", id);

        String expiryDateString = eleZ305.getChildText("z305-expiry-date");
        if (StringUtils.isNotEmpty(expiryDateString)) {
            LocalDateTime expiryDate = LocalDate.parse(expiryDateString, DateTools.FORMATTERISO8601DATEREVERSE).atStartOfDay();
            expired = expiryDate.isBefore(LocalDateTime.now());
            logger.trace("expired: {}", expired);
        }
    }

    /**
     * <p>
     * isValid.
     * </p>
     *
     * @return true if all relevant data indicates the account is valid; false otherwise
     */
    public boolean isValid() {
        return StringUtils.isNotEmpty(id) && !expired;
    }

    /**
     * <p>
     * Getter for the field <code>errorMsg</code>.
     * </p>
     *
     * @return the errorMsg
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * <p>
     * Setter for the field <code>errorMsg</code>.
     * </p>
     *
     * @param errorMsg the errorMsg to set
     */
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * <p>
     * isExpired.
     * </p>
     *
     * @return the expired
     */
    public boolean isExpired() {
        return expired;
    }

    /**
     * <p>
     * Getter for the field <code>request</code>.
     * </p>
     *
     * @return the request
     */
    public Request getRequest() {
        return request;
    }

    /**
     * <p>
     * Setter for the field <code>request</code>.
     * </p>
     *
     * @param request the request to set
     */
    public void setRequest(Request request) {
        this.request = request;
    }
}
