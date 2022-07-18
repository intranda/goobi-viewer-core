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
package io.goobi.viewer.model.security;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

/**
 * This class describes license types for record access conditions and also system user roles (not to be confused with the class Role, however), also
 * known as core license types.
 */
@Entity
@Table(name = "download_tickets")
public class DownloadTicket implements Serializable {

    private static final long serialVersionUID = -4208299894404324724L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(DownloadTicket.class);

    public static final int VALIDITY_DAYS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "download_ticket_id")
    private Long id;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "license_name", nullable = false)
    private String licenseName;

    @Column(name = "pi", nullable = false)
    private String pi;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "title")
    private String title;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    @ManyToOne
    @JoinColumn(name = "ip_range_id")
    private IpRange ipRange;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientApplication client;

    @Column(name = "request_message")
    private String requestMessage;

    /**
     * Empty constructor.
     */
    public DownloadTicket() {
        //
    }

    /**
     * Sets the dates.
     */
    public void start() {
        dateCreated = LocalDateTime.now();
        expirationDate = dateCreated.plusDays(VALIDITY_DAYS);
    }

    /**
     * 
     * @return
     */
    public boolean isValid() {
        return expirationDate.isAfter(LocalDateTime.now());
    }

    /**
     * 
     * @param days
     */
    public void extend(long days) {
        expirationDate = LocalDateTime.now().plusDays(days);
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
     * @return the licenseName
     */
    public String getLicenseName() {
        return licenseName;
    }

    /**
     * @param licenseName the licenseName to set
     */
    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
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
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * @return the userGroup
     */
    public UserGroup getUserGroup() {
        return userGroup;
    }

    /**
     * @param userGroup the userGroup to set
     */
    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }

    /**
     * @return the ipRange
     */
    public IpRange getIpRange() {
        return ipRange;
    }

    /**
     * @param ipRange the ipRange to set
     */
    public void setIpRange(IpRange ipRange) {
        this.ipRange = ipRange;
    }

    /**
     * @return the client
     */
    public ClientApplication getClient() {
        return client;
    }

    /**
     * @param client the client to set
     */
    public void setClient(ClientApplication client) {
        this.client = client;
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
