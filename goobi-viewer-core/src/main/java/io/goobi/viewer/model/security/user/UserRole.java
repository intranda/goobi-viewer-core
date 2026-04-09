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
package io.goobi.viewer.model.security.user;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.goobi.viewer.model.security.Role;

/**
 * Objects that map roles to users within a user group.
 */
@Entity
@Table(name = "user_role")
public class UserRole implements Serializable {

    private static final long serialVersionUID = -4122020685959116944L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_role_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_group_id", nullable = false)
    private UserGroup userGroup;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    /**
     * Empty constructor.
     */
    public UserRole() {
        // the emptiness inside
    }

    /**
     * Creates a new UserRole instance.
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     */
    public UserRole(UserGroup userGroup, User user, Role role) {
        this.userGroup = userGroup;
        this.user = user;
        this.role = role;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (user == null ? 0 : user.hashCode());
        result = prime * result + (userGroup == null ? 0 : userGroup.hashCode());
        return result;
    }

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
        UserRole other = (UserRole) obj;
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        } else if (!user.equals(other.user)) {
            return false;
        }
        if (userGroup == null) {
            if (other.userGroup != null) {
                return false;
            }
        } else if (!userGroup.equals(other.userGroup)) {
            return false;
        }
        return true;
    }

    /**
     * Getter for the field <code>id</code>.
     *
     * @return the database identifier of this user role assignment
     */
    public Long getId() {
        return id;
    }

    /**
     * Setter for the field <code>id</code>.
     *
     * @param id the database identifier to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Getter for the field <code>userGroup</code>.
     *
     * @return the user group this role assignment belongs to
     */
    public UserGroup getUserGroup() {
        return userGroup;
    }

    /**
     * Setter for the field <code>userGroup</code>.
     *
     * @param userGroup the user group this role assignment belongs to
     */
    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }

    /**
     * Getter for the field <code>user</code>.
     *
     * @return the user this role assignment belongs to
     */
    public User getUser() {
        return user;
    }

    /**
     * Setter for the field <code>user</code>.
     *
     * @param user the user this role assignment belongs to
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Getter for the field <code>role</code>.
     *
     * @return the role assigned to the user within the group
     */
    public Role getRole() {
        return role;
    }

    /**
     * Setter for the field <code>role</code>.
     *
     * @param role the role assigned to the user within the group
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "User '" + user + "' with the role '" + role + "' in group '" + userGroup + "'";
    }
}
