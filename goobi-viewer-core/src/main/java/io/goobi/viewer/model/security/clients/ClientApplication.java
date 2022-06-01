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
import java.util.Optional;
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
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.eclipse.persistence.annotations.PrivateOwned;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.security.ILicensee;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.clients.ClientApplication.AccessStatus;
import io.goobi.viewer.solr.SolrConstants;

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
    
    @Column(name = "subnet_mask")
    private String subnetMask;

    @Column(name = "access_status")
    @Enumerated(EnumType.STRING)
    private AccessStatus accessStatus;
    
    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE })
    @PrivateOwned
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
    
}
