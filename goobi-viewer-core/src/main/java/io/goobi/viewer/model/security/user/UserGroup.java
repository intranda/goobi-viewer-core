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
package io.goobi.viewer.model.security.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.security.ILicensee;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.Role;

/**
 * <p>
 * UserGroup class.
 * </p>
 */
@Entity
@Table(name = "user_groups")
// @DiscriminatorValue("UserGroup")
public class UserGroup implements ILicensee, Serializable {

    private static final long serialVersionUID = -3038659744043035929L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(UserGroup.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_group_id")
    private Long id;

    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private boolean active = true;

    @OneToMany(mappedBy = "userGroup", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE })
    @PrivateOwned
    private List<License> licenses = new ArrayList<>();

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        if (id != null) {
            result = prime * result + (int) (id ^ id >>> 32);
        }
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
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
        UserGroup other = (UserGroup) obj;
        if (id != other.id) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * add User to group
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addMember(User user, Role role) throws PresentationException, DAOException {
        if (user != null && role != null) {
            UserRole membership = new UserRole(this, user, role);
            return DataManager.getInstance().getDao().addUserRole(membership);
        }

        return false;
    }

    /**
     * <p>
     * changeMemberRole.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return a boolean.
     */
    public boolean changeMemberRole(User user, Role role) {
        if (user != null && role != null) {
            UserRole membership = new UserRole(this, user, null);
            membership.setRole(role);
            return true;
        }

        return false;
    }

    /**
     * remove User from Group
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean removeMember(User user) throws DAOException {
        if (user != null) {
            UserRole membership = new UserRole(this, user, null);
            DataManager.getInstance().getDao().deleteUserRole(membership);
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasLicense(String licenseName, String privilegeName, String pi) throws PresentationException, IndexUnreachableException {
        // logger.trace("hasLicense({},{},{})", licenseName, privilegeName, pi);
        if (StringUtils.isEmpty(privilegeName)) {
            return true;
        }
        if (getLicenses() != null) {
            for (License license : getLicenses()) {
                if (license.isValid() && license.getLicenseType().getName().equals(licenseName)) {
                    if (license.getPrivileges().contains(privilegeName) || license.getLicenseType().getPrivileges().contains(privilegeName)) {
                        if (StringUtils.isEmpty(license.getConditions())) {
                            // logger.debug("Permission found for user group: {}", name);
                            return true;
                        } else if (StringUtils.isNotEmpty(pi)) {
                            // If PI and Solr condition subquery are present, check via Solr
                            String query = SolrConstants.PI + ":" + pi + " AND (" + license.getConditions() + ")";
                            if (DataManager.getInstance()
                                    .getSearchIndex()
                                    .getFirstDoc(query, Collections.singletonList(SolrConstants.IDDOC)) != null) {
                                logger.debug("Permission found for user group: {} (query: {})", name, query);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * <p>
     * hasUserPrivilege.
     * </p>
     *
     * @param privilegeName a {@link java.lang.String} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean hasUserPrivilege(String privilegeName) throws DAOException {
        for (UserRole role : DataManager.getInstance().getDao().getUserRoles(this, null, null)) {
            if (role.getRole().hasPrivilege(privilegeName)) {
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean addLicense(License license) {
        if (licenses == null) {
            licenses = new ArrayList<>();
        }
        if (!licenses.contains(license)) {
            licenses.add(license);
            license.setUserGroup(this);
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeLicense(License license) {
        if (license != null && licenses != null) {
            // license.setUserGroup(null);
            return licenses.remove(license);
        }

        return false;
    }

    /*********************************** Getter and Setter ***************************************/

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

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Setter for the field <code>name</code>.
     * </p>
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>
     * Getter for the field <code>description</code>.
     * </p>
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * <p>
     * Setter for the field <code>description</code>.
     * </p>
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * <p>
     * Getter for the field <code>owner</code>.
     * </p>
     *
     * @return the owner
     */
    public User getOwner() {
        return owner;
    }

    /**
     * <p>
     * Setter for the field <code>owner</code>.
     * </p>
     *
     * @param owner the owner to set
     */
    public void setOwner(User owner) {
        this.owner = owner;
    }

    /**
     * <p>
     * isActive.
     * </p>
     *
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * <p>
     * Setter for the field <code>active</code>.
     * </p>
     *
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** {@inheritDoc} */
    @Override
    public List<License> getLicenses() {
        return licenses;
    }

    /**
     * <p>
     * Setter for the field <code>licenses</code>.
     * </p>
     *
     * @param licenses the licenses to set
     */
    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    /**
     * <p>
     * Getter for the field <code>licenses</code>.
     * </p>
     *
     * @param core a boolean.
     * @return List of filtered licenses whose type's core attribute matches the given value
     */
    public List<License> getLicenses(boolean core) {
        if (licenses == null || licenses.isEmpty()) {
            return Collections.emptyList();
        }

        List<License> ret = new ArrayList<>(licenses.size());
        for (License license : licenses) {
            if (license.getLicenseType().isCore() == core) {
                ret.add(license);
            }
        }

        return ret;
    }
    
    /**
     * 
     * @return
     * @throws DAOException
     */
    public int getMemberCount() throws DAOException {
        return DataManager.getInstance().getDao().getUserRoles(this, null, null).size();
    }

    /**
     * <p>
     * getMemberships.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserRole> getMemberships() throws DAOException {
        return DataManager.getInstance().getDao().getUserRoles(this, null, null);
    }

    /**
     * <p>
     * getMembers.
     * </p>
     *
     * @return a {@link java.util.Set} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Set<User> getMembers() throws DAOException {
        Set<User> ret = new HashSet<>();

        for (UserRole membership : getMemberships()) {
            ret.add(membership.getUser());
        }

        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name;
    }
}
