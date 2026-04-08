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

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.License.AccessType;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Licensee wrapper.
 */
@Entity
@Table(name = "license_rights_holders")
public class LicenseRightsHolder {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(LicenseRightsHolder.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "license_rights_holder_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "license_id", nullable = false)
    private License owner;

    @Column(name = "licensee_type")
    @Enumerated(EnumType.STRING)
    private AccessType type;

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

    public LicenseRightsHolder() {

    }

    public LicenseRightsHolder(License owner) {
        this.owner = owner;
    }

    public boolean isDisabled() {
        return getType() == null || (getUser() == null && getUserGroup() == null && getIpRange() == null && getClient() == null);
    }

    public ILicensee getLicensee() {
        return switch (type) {
            case USER -> user;
            case USER_GROUP -> userGroup;
            case IP_RANGE -> ipRange;
            case CLIENT -> client;
            case null -> null;
        };
    }

    
    public Long getId() {
        return id;
    }

    
    public void setId(Long id) {
        this.id = id;
    }

    
    public AccessType getType() {
        return type;
    }

    
    public License getOwner() {
        return owner;
    }

    
    public void setOwner(License owner) {
        this.owner = owner;
    }

    
    public void setType(AccessType type) {
        logger.trace("setType: {}", type);
        this.type = type;
    }

    
    public User getUser() {
        return user;
    }

    /**
     * @param user the user this rights holder represents (clears userGroup, ipRange and client when non-null)
     * @should set userGroup and ipRange to null if user not null
     * @should not set userGroup and ipRange to null if user null
     */
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userGroup = null;
            this.ipRange = null;
            this.client = null;
        }
    }

    
    public UserGroup getUserGroup() {
        return userGroup;
    }

    /**
     * @param userGroup the user group this rights holder represents (clears user, ipRange and client when non-null)
     * @should set user and ipRange to null if userGroup not null
     * @should not set user and ipRange to null if userGroup null
     */
    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
        if (userGroup != null) {
            this.user = null;
            this.ipRange = null;
            this.client = null;
        }
    }

    
    public IpRange getIpRange() {
        return ipRange;
    }

    /**
     * @param ipRange the IP range this rights holder represents (clears user, userGroup and client when non-null)
     * @should set user and userGroup to null if ipRange not null
     * @should not set user and userGroup to null if ipRange null
     */
    public void setIpRange(IpRange ipRange) {
        this.ipRange = ipRange;
        if (ipRange != null) {
            this.user = null;
            this.userGroup = null;
            this.client = null;
        }
    }

    
    public ClientApplication getClient() {
        return client;
    }

    
    public void setClient(ClientApplication client) {
        this.client = client;
    }

    public Long getClientId() {
        return Optional.ofNullable(client).map(ClientApplication::getId).orElse(null);
    }

    /**
     * @param clientId the database ID of the client application to associate with this rights holder
     * @throws DAOException
     */
    public void setClientId(Long clientId) throws DAOException {
        if (clientId != null) {
            this.client = DataManager.getInstance().getDao().getClientApplication(clientId);
        }
    }
}
