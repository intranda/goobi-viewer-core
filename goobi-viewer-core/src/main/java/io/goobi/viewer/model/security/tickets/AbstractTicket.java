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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.BCrypt;
import io.goobi.viewer.controller.StringTools;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * This class describes license types for record access conditions and also system user roles (not to be confused with the class Role, however), also
 * known as core license types.
 */
@Entity
@Table(name = "tickets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ticket_type")
public abstract class AbstractTicket {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(AbstractTicket.class);

    /** Default validity for a ticket in days. */
    public static final int VALIDITY_DAYS = 30;
    /** Static salt for password hashes. */
    public static final String SALT = "$2a$10$H580saN37o2P03A5myUCm.";
    /** Random object for password generation. */
    protected static final Random RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Transient
    private transient String password;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "title")
    private String title;

    @Column(name = "request_message", columnDefinition = "MEDIUMTEXT")
    private String requestMessage;

    @Transient
    protected transient BCrypt bcrypt = new BCrypt();

    public AbstractTicket() {
        dateCreated = LocalDateTime.now();
    }

    /**
     * 
     * @return true if ticket granted and not expired; false otherwise
     * @should return true if ticket active
     */
    public boolean isActive() {
        return !isRequest() && !isExpired();
    }

    /**
     * 
     * @return true if expiration date is in the past; false otherwise
     * @should return true if expiration date before now
     * @should return false if expiration date after now
     */
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDateTime.now());
    }

    /**
     * 
     * @return true if ticket is requested but not yet issued; false otherwise
     */
    public boolean isRequest() {
        return StringUtils.isEmpty(passwordHash);
    }

    /**
     * 
     * @param password Password to check
     * @return true if password correct; false otherwise
     * @should check password correctly
     */
    public boolean checkPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return false;
        }

        return BCrypt.checkPassword(password, passwordHash);
    }

    /**
     * Sets the dates.
     */
    public void activate() {
        if (passwordHash == null) {
            password = StringTools.generateHash("xxx" + RANDOM.nextInt()).substring(0, 12);
            passwordHash = BCrypt.hashpw(password, SALT);
        }
        expirationDate = LocalDateTime.now().plusDays(VALIDITY_DAYS);
    }

    /**
     * Extends the ticket by another <code>days</code> days.
     * 
     * @param days Number of days to extend
     */
    public void extend(long days) {
        if (days <= 0) {
            throw new IllegalArgumentException("days must be a number greater than 0");
        }

        if (expirationDate != null) {
            expirationDate = expirationDate.plusDays(days);
        } else {
            expirationDate = LocalDateTime.now().plusDays(days);
        }
    }

    /**
     * Resets the ticket's password and expiration date.
     */
    public void reset() {
        passwordHash = null;
        activate();
    }

    /**
     * 
     * @return <code>VALIDITY_DAYS</code>
     */
    public String getDefaultValidityAsString() {
        return String.valueOf(VALIDITY_DAYS);
    }

    /**
     * Returns the title of the associated record, with a fallback to PI.
     * 
     * @return title if present; otherwise pi
     */
    public String getLabel() {
        return title;
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the dateCreated
     */
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the expirationDate
     */
    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    /**
     * @param expirationDate the expirationDate to set
     */
    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the passwordHash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * @param passwordHash the passwordHash to set
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the requestMessage
     */
    public String getRequestMessage() {
        return requestMessage;
    }

    /**
     * @param requestMessage the requestMessage to set
     */
    public void setRequestMessage(String requestMessage) {
        this.requestMessage = requestMessage;
    }

}
