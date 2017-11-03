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
package de.intranda.digiverso.presentation.model.security;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Represents a role a user can have (e.g. administrator, lecturer, student, etc.). Contains a set of privileges. A role can also inherit from other
 * roles. The full potential of this class is not in use at the moment.
 */
@Entity
@Table(name = "roles")
@XStreamAlias("role")
public class Role implements IPrivilegeHolder {

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

    /** Empty constructor for XStream. */
    public Role() {
        // the emptiness inside
    }

    public Role(String name) {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
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
    @Override
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

    @Override
    public boolean isPrivList() {
        return hasPrivilege(IPrivilegeHolder.PRIV_LIST);
    }

    @Override
    public void setPrivList(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_LIST);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_LIST);
        }
    }

    @Override
    public boolean isPrivViewImages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_IMAGES);
    }

    @Override
    public void setPrivViewImages(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        }
    }

    @Override
    public boolean isPrivViewThumbnails() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
    }

    @Override
    public void setPrivViewThumbnails(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
        }
    }

    @Override
    public boolean isPrivViewFulltext() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
    }

    @Override
    public void setPrivViewFulltext(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        }
    }

    @Override
    public boolean isPrivViewVideo() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_VIDEO);
    }

    @Override
    public void setPrivViewVideo(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_VIDEO);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_VIDEO);
        }
    }

    @Override
    public boolean isPrivViewAudio() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_AUDIO);
    }

    @Override
    public void setPrivViewAudio(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_AUDIO);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_AUDIO);
        }
    }

    @Override
    public boolean isPrivDownloadPdf() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
    }

    @Override
    public void setPrivDownloadPdf(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.user.IPrivilegeHolder#isPrivDownloadOriginalContent()
     */
    @Override
    public boolean isPrivDownloadOriginalContent() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.user.IPrivilegeHolder#setPrivDownloadOriginalContent(boolean)
     */
    @Override
    public void setPrivDownloadOriginalContent(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
        }

    }

    @Override
    public boolean isPrivSetRepresentativeImage() {
        return hasPrivilege(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
    }

    @Override
    public void setPrivSetRepresentativeImage(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        }
    }

    /*********************************** Getter and Setter ***************************************/

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

    /**
     * @return the privileges
     */
    public Set<String> getPrivileges() {
        return privileges;
    }

    /**
     * @param privileges the privileges to set
     */
    public void setPrivileges(Set<String> privileges) {
        this.privileges = privileges;
    }

    /**
     * @return the inheritedRoles
     */
    public Set<Role> getInheritedRoles() {
        return inheritedRoles;
    }

    /**
     * @param inheritedRoles the inheritedRoles to set
     */
    public void setInheritedRoles(Set<Role> inheritedRoles) {
        this.inheritedRoles = inheritedRoles;
    }
}
