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
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Represents a role a user can have within a user group. Contains a set of privileges. A role can also inherit from other roles. The full potential
 * of this class is not in use at the moment.
 */
@Entity
@Table(name = "user_roles")
public class Role implements Serializable {

    private static final long serialVersionUID = -264290351046020590L;

    /** Constant <code>SUPERUSER_ROLE="admin"</code> */
    public static final String SUPERUSER_ROLE = "admin";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    // Field length had to be limited to 180 chars because InnoDB only supports 767 bytes per index,
    // and the unique index will require 255*n bytes (where n depends on the charset)
    @Column(name = "name", nullable = false, unique = true, columnDefinition = "VARCHAR(180)")
    private String name;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_privileges", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "privilege_name")
    private Set<String> privileges = new HashSet<>();

    // TODO implement role inheritance
    @Transient
    private Set<Role> inheritedRoles = new HashSet<>();

    /**
     * Empty constructor.
     */
    public Role() {
        // the emptiness inside
    }

    /**
     * <p>
     * Constructor for Role.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public Role(String name) {
        this.name = name;
    }

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
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Role other = (Role) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
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
     * Checks whether this role or any role from which this role inherits has the privilege with the given name.
     *
     * @param privilegeName The name of the priv to check for.
     * @return true if any of the roles has this privilege; false otherwise.
     */
    public boolean hasPrivilege(String privilegeName) {
        if (name.equals(SUPERUSER_ROLE)) {
            return true;
        }

        if (privileges.contains(privilegeName)) {
            return true;
        }

        for (Role role : inheritedRoles) {
            if (role.hasPrivilege(privilegeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * isPrivDeleteOcrPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPrivDeleteOcrPage() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
    }

    /**
     * <p>
     * setPrivDeleteOcrPage.
     * </p>
     *
     * @param priv a boolean.
     */
    public void setPrivDeleteOcrPage(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
        }
    }

    /**
     * <p>
     * isPrivSetRepresentativeImage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPrivSetRepresentativeImage() {
        return hasPrivilege(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
    }

    /**
     * <p>
     * setPrivSetRepresentativeImage.
     * </p>
     *
     * @param priv a boolean.
     */
    public void setPrivSetRepresentativeImage(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        }
    }

    /**
     * <p>
     * isPrivCmsPages.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsPages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_PAGES);
    }

    /**
     * <p>
     * setPrivCmsPages.
     * </p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsPages(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_PAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_PAGES);
        }
    }

    /**
     * <p>
     * isPrivCmsMenu.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsMenu() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_MENU);
    }

    /**
     * <p>
     * setPrivCmsMenu.
     * </p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsMenu(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_MENU);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_MENU);
        }
    }

    /**
     * <p>
     * isPrivCmsStaticPages.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsStaticPages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
    }

    /**
     * <p>
     * setPrivCmsStaticPages.
     * </p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsStaticPages(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        }
    }

    /**
     * <p>
     * isPrivCmsCollections.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsCollections() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
    }

    /**
     * <p>
     * setPrivCmsCollections.
     * </p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsCollections(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
        }
    }

    /**
     * <p>
     * isPrivCmsCategories.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsCategories() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
    }

    /**
     * <p>
     * setPrivCmsCategories.
     * </p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsCategories(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
        }
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

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return the name
     */
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
     * Getter for the field <code>privileges</code>.
     * </p>
     *
     * @return the privileges
     */
    public Set<String> getPrivileges() {
        return privileges;
    }

    /**
     * <p>
     * Setter for the field <code>privileges</code>.
     * </p>
     *
     * @param privileges the privileges to set
     */
    public void setPrivileges(Set<String> privileges) {
        this.privileges = privileges;
    }

    /**
     * <p>
     * Getter for the field <code>inheritedRoles</code>.
     * </p>
     *
     * @return the inheritedRoles
     */
    public Set<Role> getInheritedRoles() {
        return inheritedRoles;
    }

    /**
     * <p>
     * Setter for the field <code>inheritedRoles</code>.
     * </p>
     *
     * @param inheritedRoles the inheritedRoles to set
     */
    public void setInheritedRoles(Set<Role> inheritedRoles) {
        this.inheritedRoles = inheritedRoles;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name;
    }
}
