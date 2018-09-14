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
package de.intranda.digiverso.presentation.model.security.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.security.ILicensee;
import de.intranda.digiverso.presentation.model.security.License;

@Entity
@Table(name = "ip_ranges")
// @DiscriminatorValue("IpRange")
@XStreamAlias("ipRange")
public class IpRange implements ILicensee {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(IpRange.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ip_range_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "subnet_mask", nullable = false)
    private String subnetMask;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "ipRange", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE })
    @PrivateOwned
    private List<License> licenses = new ArrayList<>();

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        IpRange other = (IpRange) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public boolean matchIp(String inIp) {
        //        if (inIp.equals(Helper.ADDRESS_LOCALHOST_IPV6) || inIp.equals(Helper.ADDRESS_LOCALHOST_IPV4)) {
        //            return true;
        //        }

        // Workaround for single IP ranges (isInRange() doesn't seem to match these)
        if (subnetMask.endsWith("/32") && subnetMask.substring(0, subnetMask.length() - 3).equals(inIp)) {
            logger.debug("Exact IP match: {}", inIp);
            return true;
        }

        try {
            SubnetInfo subnet = new SubnetUtils(subnetMask).getInfo();
            if (subnet.isInRange(inIp)) {
                logger.debug("IP matches: {}", inIp);
            }
            return subnet.isInRange(inIp);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public boolean hasLicense(String licenseName, String privilegeName, String pi) throws PresentationException, IndexUnreachableException {
        for (License license : getLicenses()) {
            // logger.trace("License: {}", license.getId());
            if (license.isValid() && license.getLicenseType().getName().equals(licenseName)) {
                if (StringUtils.isEmpty(privilegeName)) {
                    return true;
                }
                if (license.getPrivileges().contains(privilegeName) || license.getLicenseType().getPrivileges().contains(privilegeName)) {
                    if (StringUtils.isEmpty(license.getConditions())) {
                        return true;
                    } else if (StringUtils.isNotEmpty(pi)) {
                        // If PI and Solr condition subquery are present, check via Solr
                        String query = SolrConstants.PI + ":" + pi + " AND (" + license.getConditions() + ")";
                        if (DataManager.getInstance().getSearchIndex().getFirstDoc(query, Collections.singletonList(SolrConstants.IDDOC)) != null) {
                            logger.debug("Permission found for IP range: {} (query: {})", name, query);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 
     * @param conditionList
     * @param privilegeName
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return true if condition is open access
     * @should return true if ip range has license
     * @should return false if ip range has no license
     * @should return true if condition list empty
     */
    public boolean canSatisfyAllAccessConditions(Set<String> conditionList, String privilegeName, String pi) throws PresentationException,
            IndexUnreachableException {
        // logger.trace("canSatisfyAllAccessConditions({},{},{})", conditionList, privilegeName, pi);

        if (conditionList.size() == 1 && conditionList.contains(SolrConstants.OPEN_ACCESS_VALUE)) {
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

        return permissionMap.isEmpty() || permissionMap.containsValue(true);
//        return !permissionMap.containsValue(false);
    }

    @Override
    public boolean addLicense(License license) {
        if (licenses == null) {
            licenses = new ArrayList<>();
        }
        if (!licenses.contains(license)) {
            licenses.add(license);
            license.setIpRange(this);
            return true;
        }

        return false;
    }

    @Override
    public boolean removeLicense(License license) {
        if (license != null && licenses != null) {
            // license.setIpRange(null);
            return licenses.remove(license);
        }

        return false;
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
     * @return the name
     */
    @Override
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

    /**
     * @return the licenses
     */
    @Override
    public List<License> getLicenses() {
        return licenses;
    }

    /**
     * @param licenses the licenses to set
     */
    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    public static void main(String[] args) {
        try {
            SubnetInfo subnet = new SubnetUtils("255.255.255.0").getInfo();
            logger.debug(subnet.getNetmask());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid subnet mask", e);
        }
    }
}
