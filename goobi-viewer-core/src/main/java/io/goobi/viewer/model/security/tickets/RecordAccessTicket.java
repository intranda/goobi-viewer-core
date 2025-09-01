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
package io.goobi.viewer.model.security.tickets;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

/**
 * This class describes license types for record access conditions and also system user roles (not to be confused with the class Role, however), also
 * known as core license types.
 */
@Entity
@DiscriminatorValue("record_access")
public class RecordAccessTicket extends AbstractTicket implements Serializable {

    private static final long serialVersionUID = -7722500146728015725L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(RecordAccessTicket.class);

    /** Default validity for a ticket in days. */
    public static final int VALIDITY_DAYS = 30;

    @Transient
    private transient String password;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "title")
    private String title;

    @Column(name = "request_message", columnDefinition = "MEDIUMTEXT")
    private String requestMessage;

    /**
     * Zero argument constructor.
     */
    public RecordAccessTicket() {
        super();
    }

    /**
     * Returns the title of the associated record, with a fallback to PI.
     * 
     * @return title if present; otherwise pi
     */
    public String getLabel() {
        return StringUtils.isNotEmpty(title) ? title : getPi();
    }
}
