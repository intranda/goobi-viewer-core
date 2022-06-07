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
package io.goobi.viewer.model.security.clients;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.eclipse.persistence.annotations.PrivateOwned;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import de.intranda.api.iiif.presentation.v3.IPresentationModelElement3;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.security.ILicensee;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.solr.SolrConstants;
import io.swagger.v3.oas.annotations.media.Schema;

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
public class ClientApplication  implements ILicensee {

    
    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_application_id")
    @Schema(description = "The internal database identifier of the client", example="2", type = "long", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;
    
    @Schema(description = "The internal identifier/secret of the client", example=" 0D219Z74-F764-4CAD-8361-D9964FD1B186", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "client_identifier")
    private String clientIdentifier;
    
    @Schema(description = "The IP under which the client first requested registration", example="192.168.172.13", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "client_ip")
    private String clientIp;
    
    @Schema(description = "The name to be displayed for the client", example="Windows Desktop 1", accessMode = Schema.AccessMode.READ_WRITE)
    @Column(name = "name")
    private String name;
    
    @Schema(description = "A description of the client", example="Die Treppe rauf, vorne links", accessMode = Schema.AccessMode.READ_WRITE)
    @Column(name = "description")
    private String description;
    
    @Schema(description = "The time at which the client was granted or denied access, or if not yet happened, the time at which it first requested access", example="2022-05-19T11:55:16Z", type="date", format="ISO 8601",  accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "date_registered")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = IPresentationModelElement3.DATETIME_FORMAT)
    private LocalDateTime dateRegistered = LocalDateTime.now();
    
    @Schema(description = "The last time the client sent a request to the server", example="2022-05-19T11:55:16Z", type="date", format="ISO 8601",  accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "date_last_access")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = IPresentationModelElement3.DATETIME_FORMAT)
    private LocalDateTime dateLastAccess = LocalDateTime.now();
    
    @Schema(description = "An IP Subnet mask. If present, the client may only log in if its current IP matches the mask", example="168.192.0.1/16", accessMode = Schema.AccessMode.READ_WRITE)
    @Column(name = "subnet_mask")
    private String subnetMask;

    @Schema(description = "The access status of the client. Only clients with access status 'GRANTED' benefit from client privileges", example="GRANTED", accessMode = Schema.AccessMode.READ_WRITE, allowableValues = {"GRANTED, DENIED"})
    @Column(name = "access_status")
    @Enumerated(EnumType.STRING)
    private AccessStatus accessStatus;
    
    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE })
    @PrivateOwned
    @JsonIgnore
    private List<License> licenses = new ArrayList<>();
    
    public static enum AccessStatus {
        NON_APPLICABLE, //only used for the "all clients" core ClientApplication instance
        REQUESTED,
        GRANTED,
        DENIED,
    }
    
    /**
     * internal constructor for deserializing from database
     */
    public ClientApplication() {
        
    }
    
    public ClientApplication(ClientApplication source) {
        this.id = source.getId();
        this.clientIdentifier = source.getClientIdentifier();
        this.accessStatus = source.getAccessStatus();
        this.clientIp = source.getClientIp();
        this.dateLastAccess = source.getDateLastAccess();
        this.dateRegistered = source.getDateRegistered();
        this.name = source.getName();
        this.description = source.getDescription();
        this.subnetMask = source.getSubnetMask();
        this.licenses = new ArrayList<>(source.getLicenses());
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
    
    @JsonIgnore
    public boolean isRegistrationPending() {
        return AccessStatus.REQUESTED.equals(this.getAccessStatus());
    }
    
    @JsonIgnore
    public boolean isRegistrationPendingOrDenied() {
        return AccessStatus.REQUESTED.equals(this.getAccessStatus()) || AccessStatus.DENIED.equals(this.getAccessStatus());
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

    
    /**
     * @return the licenses
     */
    public List<License> getLicenses() {
        return Collections.unmodifiableList(this.licenses);
    }
    
    public boolean addLicense(License license) {
        return this.licenses.add(license);
    }
    

    @Override
    public boolean removeLicense(License license) {
        return this.licenses.remove(license);
    }
    
    /**
     * @return the subnetMask
     */
    public String getSubnetMask() {
        return subnetMask;
    }
    
    /**
     * @param subnetMask the subnetMask to set
     */
    public void setSubnetMask(String subnetMask) {
        this.subnetMask = subnetMask;
    }

    
    public boolean canSatisfyAllAccessConditions(Set<String> conditionList, String privilegeName, String pi)
            throws PresentationException, IndexUnreachableException, DAOException {

        // always allow access if the only condition is open access and there is no special license configured for it
        if (conditionList.size() == 1 && conditionList.contains(SolrConstants.OPEN_ACCESS_VALUE)
                && DataManager.getInstance().getDao().getLicenseType(SolrConstants.OPEN_ACCESS_VALUE) == null) {
            return true;
        }

        Map<String, Boolean> permissionMap = new HashMap<>(conditionList.size());
        for (String accessCondition : conditionList) {
            permissionMap.put(accessCondition, false);
            // Check individual licenses
            if (hasLicense(accessCondition, privilegeName, pi)) {
                permissionMap.put(accessCondition, true);
                continue;
            }
        }
        // It should be sufficient if the user can satisfy one required licence
        return permissionMap.isEmpty() || permissionMap.containsValue(true);
        // return !permissionMap.containsValue(false);
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean hasLicense(String licenseName, String privilegeName, String pi) throws PresentationException, IndexUnreachableException {
        // logger.trace("hasLicense({},{},{})", licenseName, privilegeName, pi);
        if (StringUtils.isEmpty(privilegeName)) {
            return true;
        }
        for (License license : getLicenses()) {
            // logger.trace("license: {}, {}", license.getId(),
            // license.getPrivileges().toString());
            // logger.trace("license type: {}", license.getLicenseType().getName());
            if (license.isValid() && license.getLicenseType().getName().equals(licenseName)) {
                // TODO why not do this check right at the beginning?
                if (license.getPrivileges().contains(privilegeName) || license.getLicenseType().getPrivileges().contains(privilegeName)) {
                    if (StringUtils.isEmpty(license.getConditions())) {
                        // logger.trace("Permission found for user: {} ", id);
                        return true;
                    } else if (StringUtils.isNotEmpty(pi)) {
                        // If PI and Solr condition subquery are present, check via Solr
                        StringBuilder sbQuery = new StringBuilder();
                        sbQuery.append(SolrConstants.PI).append(':').append(pi).append(" AND (").append(license.getConditions()).append(')');
                        if (DataManager.getInstance()
                                .getSearchIndex()
                                .getFirstDoc(sbQuery.toString(), Collections.singletonList(SolrConstants.IDDOC)) != null) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
    
    /**
     * <p>
     * matchIp.
     * </p>
     *
     * @param inIp a {@link java.lang.String} object.
     * @return a boolean.
     * @should match IPv6 localhost to IPv4 mask
     * @should match edge addresses
     */
    public boolean matchIp(String inIp) {
        
        // Without subnet mask, allow access from all IPs
        if(StringUtils.isBlank(subnetMask)) {
            return true;
        }
        
        if (inIp.equals(NetTools.ADDRESS_LOCALHOST_IPV6)) {
            inIp = NetTools.ADDRESS_LOCALHOST_IPV4;
        }

        // Workaround for single IP ranges (isInRange() doesn't seem to match these)
        if (subnetMask.endsWith("/32") && subnetMask.substring(0, subnetMask.length() - 3).equals(inIp)) {
            // logger.trace("Exact IP match: {}", inIp);
            return true;
        }

        try {
            SubnetUtils subnetUtils = new SubnetUtils(subnetMask);
            subnetUtils.setInclusiveHostCount(true);
            return subnetUtils.getInfo().isInRange(inIp);
        } catch (IllegalArgumentException e) {
            return false;
        }

    }

    /**
     * If no subnet mask has been set, use the clientIp if available with a '/32' mask
     */
    public void initializeSubnetMask() {
        if(subnetMask == null && StringUtils.isNotBlank(clientIp)) {
            subnetMask = clientIp + "/32";
        }
    }
    
}
