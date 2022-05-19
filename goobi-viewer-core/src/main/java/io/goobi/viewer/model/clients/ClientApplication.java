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
package io.goobi.viewer.model.clients;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.model.security.License;

/**
 * @author florian
 *
 * This class represents clients accessing the viewer not through web-browsers but
 * using dedicated client-applications which must register with the server to view any data
 * but which may also enjoy unique viewing rights via dedicated {@link License Licenses}
 *
 */
@Entity
@Table(name = "client_applications")
public class ClientApplication {


    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_application_id")
    private Long id;
    
    @Column(name = "client_identifier")
    private String clientIdentifier = "";
    
    
    @Column(name = "client_ip")
    private String clientIp = "";
    
    @Column(name = "name")
    private String name = "";
    
    @Column(name = "description")
    private String description = "";
    
    @Column(name = "date_registered")
    private LocalDateTime dateRegistered = LocalDateTime.now();
    
    @Column(name = "date_last_access")
    private LocalDateTime dateLastAccess = LocalDateTime.now();

    @Column(name = "access_status")
    @Enumerated(EnumType.STRING)
    private AccessStatus accessStatus;
    
    public static enum AccessStatus {
        REQUESTED,
        GRANTED,
        DENIED,
    }
    
    /**
     * internal constructor for deserializing from database
     */
    public ClientApplication() {
        
    }
    
    /**
     * constructor to create a new ClientApplication from a client request
     * @param identifier the client identifier
     */
    public ClientApplication(String identifier) {
        this.clientIdentifier = identifier;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * @return the accessStatus
     */
    public AccessStatus getAccessStatus() {
        return accessStatus;
    }
    
    /**
     * @param accessStatus the accessStatus to set
     */
    public void setAccessStatus(AccessStatus accessStatus) {
        this.accessStatus = accessStatus;
    }

    /**
     * @return the clientIdentifier
     */
    public String getClientIdentifier() {
        return clientIdentifier;
    }
    
    /**
     * @param clientIdentifier the clientIdentifier to set
     */
    public void setClientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }

    /**
     * @return the clientIp
     */
    public String getClientIp() {
        return clientIp;
    }

    /**
     * @param clientIp the clientIp to set
     */
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    /**
     * @return the dateRegistered
     */
    public LocalDateTime getDateRegistered() {
        return dateRegistered;
    }

    /**
     * @param dateRegistered the dateRegistered to set
     */
    public void setDateRegistered(LocalDateTime dateRegistered) {
        this.dateRegistered = dateRegistered;
    }

    /**
     * @return the dateLastAccess
     */
    public LocalDateTime getDateLastAccess() {
        return dateLastAccess;
    }

    /**
     * @param dateLastAccess the dateLastAccess to set
     */
    public void setDateLastAccess(LocalDateTime dateLastAccess) {
        this.dateLastAccess = dateLastAccess;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean matchesClientIdentifier(String identifier) {
        return StringUtils.isNotBlank(identifier) && identifier.equals(this.clientIdentifier);
    }
    
    public boolean isRegistrationPending() {
        return AccessStatus.REQUESTED.equals(this.getAccessStatus());
    }
    
    @Override
    public int hashCode() {
        if(this.clientIdentifier != null) {
            return this.clientIdentifier.hashCode();
        } else {
            return 0;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj.getClass().equals(this.getClass())) {
            ClientApplication other = (ClientApplication)obj;
            return Objects.equals(other.clientIdentifier, this.clientIdentifier);
        } else {
            return false;
        }
    }
}
