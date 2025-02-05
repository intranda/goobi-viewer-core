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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.eclipse.persistence.annotations.PrivateOwned;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import de.intranda.api.iiif.presentation.v3.IPresentationModelElement3;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.ILicensee;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.solr.SolrConstants;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author florian
 *
 *         This class represents clients accessing the viewer not through web-browsers but using dedicated client-applications which must register
 *         with the server to view any data but which may also enjoy unique viewing rights via dedicated {@link License Licenses}
 *
 */
@Entity
@Table(name = "client_applications")
public class ClientApplication implements ILicensee, Serializable {

    private static final long serialVersionUID = -6806071337346935488L;

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_application_id")
    @Schema(description = "The internal database identifier of the client", example = "2", type = "long", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    /**
     * A secret a client needs to pass to the server to identify itself
     */
    @Schema(description = "The internal identifier/secret of the client", example = "0D219Z74-F764-4CAD-8361-D9964FD1B186",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "client_identifier")
    private String clientIdentifier;

    /**
     * The IP under which the client first requested registration
     */
    @Schema(description = "The IP under which the client first requested registration",
            example = "192.168.172.13", //NOSONAR, the ip address here is an example for the documentation
            accessMode = Schema.AccessMode.READ_WRITE)
    @Column(name = "client_ip")
    private String clientIp;

    /**
     * The name to be displayed for the client
     */
    @Schema(description = "The name to be displayed for the client", example = "Windows Desktop 1", accessMode = Schema.AccessMode.READ_WRITE)
    @Column(name = "name")
    private String name;

    /**
     * A description of the client
     */
    @Schema(description = "A description of the client", example = "second workplace, right aisle", accessMode = Schema.AccessMode.READ_WRITE)
    @Column(name = "description")
    private String description;

    /**
     * The time at which the client was granted or denied access, or if not yet happened, the time at which it first requested access
     */
    @Schema(description = "The time at which the client was granted or denied access, or if not yet happened, "
            + "the time at which it first requested access",
            example = "2022-05-19T11:55:16Z", type = "date", format = "ISO 8601", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "date_registered", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = IPresentationModelElement3.DATETIME_FORMAT)
    private LocalDateTime dateRegistered = LocalDateTime.now();

    /**
     * The last time the client sent a request to the server
     */
    @Schema(description = "The last time the client sent a request to the server", example = "2022-05-19T11:55:16Z", type = "date",
            format = "ISO 8601", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "date_last_access", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = IPresentationModelElement3.DATETIME_FORMAT)
    private LocalDateTime dateLastAccess = LocalDateTime.now();

    /**
     * An IP Subnet mask. If present, the client may only log in if its current IP matches the mask
     */
    @Schema(description = "An IP Subnet mask. If present, the client may only log in if its current IP matches the mask",
            example = "192.168.0.1/16", //NOSONAR, the ip address here is an example for the documentation
            accessMode = Schema.AccessMode.READ_WRITE)
    @Column(name = "subnet_mask")
    private String subnetMask;

    /**
     * The access status of the client. Only clients with access status 'GRANTED' benefit from client privileges
     */
    @Schema(description = "The access status of the client. Only clients with access status 'GRANTED' benefit from client privileges",
            example = "GRANTED", accessMode = Schema.AccessMode.READ_WRITE, allowableValues = { "GRANTED, DENIED" })
    @Column(name = "access_status")
    @Enumerated(EnumType.STRING)
    private AccessStatus accessStatus;

    /**
     * List of {@link License Licenses} this client is privileged to
     */
    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE })
    @PrivateOwned
    @JsonIgnore
    private List<License> licenses = new ArrayList<>();

    /**
     * Status describing if the client is eligible to receive viewing privileges
     * 
     * @author florian
     *
     */
    public enum AccessStatus {
        /**
         * only used for the "all clients" core ClientApplication instance
         */
        NON_APPLICABLE,
        /**
         * The client has requested access but has not been granted it
         */
        REQUESTED,
        /**
         * The client has been granted access to viewing privileges
         */
        GRANTED,
        /**
         * The client has explicitly been denied access to viewing privileges. Not used currently
         */
        DENIED,
    }

    /**
     * internal constructor for deserializing from database
     */
    public ClientApplication() {

    }

    /**
     * Cloning constructor
     * 
     * @param source
     */
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
     * 
     * @param identifier the client identifier
     */
    public ClientApplication(String identifier) {
        this.clientIdentifier = identifier;
    }

    /**
     * Get the {@link #id}
     * 
     * @return the {@link #id}
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the {@link #id}
     * 
     * @param id the {@link #id} to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get the current {@link #accessStatus}
     * 
     * @return the {@link #accessStatus}
     */
    public AccessStatus getAccessStatus() {
        return accessStatus;
    }

    /**
     * Set the {@link #accessStatus}
     * 
     * @param accessStatus the {@link #accessStatus} to set
     */
    public void setAccessStatus(AccessStatus accessStatus) {
        this.accessStatus = accessStatus;
    }

    /**
     * Get the {@link #clientIdentifier}
     * 
     * @return the {@link #clientIdentifier}
     */
    public String getClientIdentifier() {
        return clientIdentifier;
    }

    /**
     * Set the {@link #clientIdentifier}
     * 
     * @param clientIdentifier the {@link #clientIdentifier} to set
     */
    public void setClientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }

    /**
     * Get the {@link #clientIp}
     * 
     * @return the {@link #clientIp}
     */
    public String getClientIp() {
        return clientIp;
    }

    /**
     * Set the {@link #clientIp}
     * 
     * @param clientIp the {@link #clientIp} to set
     */
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    /**
     * Get the {@link #dateRegistered}
     * 
     * @return the {@link #dateRegistered}
     */
    public LocalDateTime getDateRegistered() {
        return dateRegistered;
    }

    /**
     * Get the {@link #dateRegistered}
     * 
     * @param dateRegistered the {@link #dateRegistered} to set
     */
    public void setDateRegistered(LocalDateTime dateRegistered) {
        this.dateRegistered = dateRegistered;
    }

    /**
     * Get the {@link #dateLastAccess}
     * 
     * @return the {@link #dateLastAccess}
     */
    public LocalDateTime getDateLastAccess() {
        return dateLastAccess;
    }

    /**
     * Set the {@link #dateLastAccess}
     * 
     * @param dateLastAccess the {@link #dateLastAccess} to set
     */
    public void setDateLastAccess(LocalDateTime dateLastAccess) {
        this.dateLastAccess = dateLastAccess;
    }

    /**
     * Get the {@link #name}
     * 
     * @return the {@link #name}
     */
    public String getName() {
        return name;
    }

    /**
     * Set the {@link #name}
     * 
     * @param name the {@link #name} to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the {@link #description}
     * 
     * @return the {@link #description}
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the {@link #description}
     * 
     * @param description the {@link #description} to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Check if the given identifier matches this instances {@link #clientIdentifier}
     * 
     * @param identifier
     * @return true if the given identifier is not null and equals this instances {@link #clientIdentifier}
     */
    public boolean matchesClientIdentifier(String identifier) {
        return StringUtils.isNotBlank(identifier) && identifier.equals(this.clientIdentifier);
    }

    /**
     * Check if this client requires approval of its registration
     * 
     * @return true if the {@link #accessStatus} is {@link AccessStatus#REQUESTED}
     */
    @JsonIgnore
    public boolean isRegistrationPending() {
        return AccessStatus.REQUESTED.equals(this.getAccessStatus());
    }

    /**
     * Check if this client requires approval of its registration or this approval has been denied
     * 
     * @return true if the {@link #accessStatus} is {@link AccessStatus#REQUESTED} or {@link AccessStatus#DENIED}
     */
    @JsonIgnore
    public boolean isRegistrationPendingOrDenied() {
        return AccessStatus.REQUESTED.equals(this.getAccessStatus()) || AccessStatus.DENIED.equals(this.getAccessStatus());
    }

    /**
     * Use hash code of the {@link #clientIdentifier}
     */
    @Override
    public int hashCode() {
        if (this.clientIdentifier != null) {
            return this.clientIdentifier.hashCode();
        }

        return 0;
    }

    /**
     * Two clients are equal if heir {@link #clientIdentifier}s are equals
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            ClientApplication other = (ClientApplication) obj;
            return Objects.equals(other.clientIdentifier, this.clientIdentifier);
        }

        return false;
    }

    /**
     * Get the {@link License}s this client is privileged to
     * 
     * @return the licenses
     */
    public List<License> getLicenses() {
        return Collections.unmodifiableList(this.licenses);
    }

    /**
     * Add a {@link License} to the {@link #licenses}
     * 
     * @param license
     * @return true if added successfully; false otherwise
     */
    public boolean addLicense(License license) {
        return this.licenses.add(license);
    }

    /**
     * Remove {@link License} from the {@link #licenses}
     */
    @Override
    public boolean removeLicense(License license) {
        return this.licenses.remove(license);
    }

    /**
     * Get the {@link #subnetMask}
     * 
     * @return the {@link #subnetMask}
     */
    public String getSubnetMask() {
        return subnetMask;
    }

    /**
     * Set the {@link #subnetMask}
     * 
     * @param subnetMask the {@link #subnetMask} to set
     */
    public void setSubnetMask(String subnetMask) {
        this.subnetMask = subnetMask;
    }

    /**
     * Check if this client has the privilege of the given privilegeName via its {@link #licenses}
     * 
     * @param requiredAccessConditions List of access condition names to satisfy
     * @param privilegeName The privilege to check for
     * @param pi PI of a record to check
     * @return true if the privilege should be granted to the client
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public AccessPermission canSatisfyAllAccessConditions(Set<String> requiredAccessConditions, String privilegeName, String pi)
            throws PresentationException, IndexUnreachableException, DAOException {
        // always allow access if the only condition is open access and there is no special license configured for it
        if (requiredAccessConditions.size() == 1 && requiredAccessConditions.contains(SolrConstants.OPEN_ACCESS_VALUE)
                && DataManager.getInstance().getDao().getLicenseType(SolrConstants.OPEN_ACCESS_VALUE) == null) {
            return AccessPermission.granted();
        }

        Map<String, AccessPermission> permissionMap = new HashMap<>(requiredAccessConditions.size());
        for (String accessCondition : requiredAccessConditions) {
            AccessPermission access = hasLicense(accessCondition, privilegeName, pi);
            if (access.isGranted()) {
                permissionMap.put(accessCondition, access);
            }
        }
        if (!permissionMap.isEmpty()) {
            // TODO Prefer license with ticket requirement?
            for (Entry<String, AccessPermission> entry : permissionMap.entrySet()) {
                if (entry.getValue().isTicketRequired()) {
                    return entry.getValue();
                }
            }
            return AccessPermission.granted();
        }

        return AccessPermission.denied();
    }

    /** {@inheritDoc} */
    @Override
    public AccessPermission hasLicense(String licenseName, String privilegeName, String pi) throws PresentationException, IndexUnreachableException {
        // logger.trace("hasLicense({},{},{})", licenseName, privilegeName, pi); //NOSONAR Debug
        if (StringUtils.isEmpty(privilegeName)) {
            return AccessPermission.granted();
        }
        for (License license : getLicenses()) {
            if (license.isValid() && license.getLicenseType().getName().equals(licenseName)) {
                // LicenseType grants privilege
                if (license.getLicenseType().getPrivileges().contains(privilegeName)) {
                    return AccessPermission.granted();
                }
                // License grants privilege
                if (license.getPrivileges().contains(privilegeName)) {
                    if (StringUtils.isEmpty(license.getConditions())) {
                        return AccessPermission.granted()
                                .setTicketRequired(license.isTicketRequired())
                                .setRedirect(license.getLicenseType().isRedirect())
                                .setRedirectUrl(license.getLicenseType().getRedirectUrl());
                    } else if (StringUtils.isNotEmpty(pi)) {
                        // If PI and Solr condition subquery are present, check via Solr
                        StringBuilder sbQuery = new StringBuilder();
                        sbQuery.append(SolrConstants.PI).append(':').append(pi).append(" AND (").append(license.getConditions()).append(')');
                        if (DataManager.getInstance()
                                .getSearchIndex()
                                .getFirstDoc(sbQuery.toString(), Collections.singletonList(SolrConstants.IDDOC)) != null) {
                            return AccessPermission.granted()
                                    .setTicketRequired(license.isTicketRequired())
                                    .setRedirect(license.getLicenseType().isRedirect())
                                    .setRedirectUrl(license.getLicenseType().getRedirectUrl());
                        }
                    }
                }
            }
        }

        return AccessPermission.denied();
    }

    /**
     * Check if the given IP address matches the {@link #subnetMask} of this client
     *
     * @param inIp a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean matchIp(String inIp) {

        // Without subnet mask, allow access from all IPs
        if (StringUtils.isBlank(subnetMask)) {
            return true;
        }

        String ip = inIp;
        if (ip.equals(NetTools.ADDRESS_LOCALHOST_IPV6)) {
            ip = NetTools.ADDRESS_LOCALHOST_IPV4;
        }

        // Workaround for single IP ranges (isInRange() doesn't seem to match these)
        if (subnetMask.endsWith("/32") && subnetMask.substring(0, subnetMask.length() - 3).equals(ip)) {
            // logger.trace("Exact IP match: {}", inIp); //NOSONAR Debug
            return true;
        }

        try {
            SubnetUtils subnetUtils = new SubnetUtils(subnetMask);
            subnetUtils.setInclusiveHostCount(true);
            return subnetUtils.getInfo().isInRange(ip);
        } catch (IllegalArgumentException e) {
            return false;
        }

    }

    /**
     * If no subnet mask has been set, use the clientIp if available with a '/32' mask
     */
    public void initializeSubnetMask() {
        if (subnetMask == null && StringUtils.isNotBlank(clientIp)) {
            subnetMask = clientIp + "/32";
        }
    }

    @JsonIgnore
    public boolean isAccessGranted() {
        return AccessStatus.GRANTED.equals(this.getAccessStatus());
    }

    /**
     * @param remoteAddress
     * @return true if remoteAddress may log in; false otherwise
     */
    public boolean mayLogIn(String remoteAddress) {
        return isAccessGranted() && matchIp(remoteAddress);
    }

    @JsonIgnore
    public boolean isAllClients() throws DAOException {
        return DataManager.getInstance().getClientManager().isAllClients(this);
    }
}
