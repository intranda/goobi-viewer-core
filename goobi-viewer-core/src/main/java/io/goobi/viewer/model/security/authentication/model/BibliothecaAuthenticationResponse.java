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
package io.goobi.viewer.model.security.authentication.model;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.XmlTools;

/**
 * Authentication response object for Bibliotheca.
 */
public class BibliothecaAuthenticationResponse {

    private static final Logger logger = LoggerFactory.getLogger(BibliothecaAuthenticationResponse.class);

    private String userid;
    private String fsk;
    private boolean expired = false;

    /**
     * Parses and evaluates the given XML response from X-Service.
     *
     * @param xml a {@link java.lang.String} object.
     * @param encoding a {@link java.lang.String} object.
     * @throws org.jdom2.JDOMException if any.
     * @throws java.io.IOException if any.
     */
    public BibliothecaAuthenticationResponse(String xml, String encoding) throws JDOMException, IOException {
        if (xml == null) {
            throw new IllegalArgumentException("XML response may not be null");
        }

        Document doc = XmlTools.getDocumentFromString(xml, encoding);
        if (doc == null || doc.getRootElement() == null || doc.getRootElement().getChild("status") == null) {
            throw new IllegalArgumentException("XML response invalid or incomplete");
        }

        String status = doc.getRootElement().getChildText("status");
        if ("-1".equals(status)) {
            return;
        }
        userid = doc.getRootElement().getChildText("userid");

        fsk = doc.getRootElement().getChildText("fsk");
    }

    /**
     * <p>
     * isValid.
     * </p>
     *
     * @return true if all relevant data indicates the account is valid; false otherwise
     */
    public boolean isValid() {
        return StringUtils.isNotEmpty(userid) && !expired;
    }

    /**
     * @return the userid
     */
    public String getUserid() {
        return userid;
    }

    /**
     * @param userid the userid to set
     */
    public void setUserid(String userid) {
        this.userid = userid;
    }

    /**
     * @return the fsk
     */
    public String getFsk() {
        return fsk;
    }

    /**
     * @param fsk the fsk to set
     */
    public void setFsk(String fsk) {
        this.fsk = fsk;
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
}
