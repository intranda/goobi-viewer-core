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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.event.ValueChangeEvent;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

/**
 * <p>
 * UserGroupBean class.
 * </p>
 */
@Named
@SessionScoped
public class UserGroupBean implements Serializable {

    private static final long serialVersionUID = -6982988135819597474L;

    private static final Logger logger = LogManager.getLogger(UserGroupBean.class);

    private int currentOwnUserGroupId;
    private UserGroup currentOwnUserGroup;
    private UserGroup currentOtherUserGroup;
    private User currentMember;
    private Role currentRole;

    /**
     * Empty constructor.
     */
    public UserGroupBean() {
        // the emptiness inside
    }

    /**
     * <p>
     * init.
     * </p>
     */
    @PostConstruct
    public void init() {
        if (currentOwnUserGroup == null) {
            resetCurrentUserGroupAction();
        }
    }

    /**
     * Creates or updates (if already exists) currentOwnUserGroup.
     *
     * @param actionEvent a {@link jakarta.faces.event.ActionEvent} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveCurrentOwnUserGroupAction(ActionEvent actionEvent) throws DAOException {
        UserBean ub = BeanUtils.getUserBean();
        if (ub != null && ub.getUser() != null && StringUtils.isNotEmpty(currentOwnUserGroup.getName())) {
            if (ub.getUser().getUserGroupOwnerships().contains(currentOwnUserGroup)) {
                DataManager.getInstance().getDao().updateUserGroup(currentOwnUserGroup);
                Messages.info("updatedSuccessfully");
                logger.debug("Bookshelf '{}' updated.", currentOwnUserGroup.getName());
                return;
            }
            currentOwnUserGroup.setOwner(ub.getUser());
            if (DataManager.getInstance().getDao().addUserGroup(currentOwnUserGroup)) {
                resetCurrentUserGroupAction();
                Messages.info("savedSuccessfully");
                logger.debug("Bookshelf '{}' added.", currentOwnUserGroup.getName());
                return;
            }
        }
        Messages.error("errSave");
        logger.error("error while saving user group");
    }

    /**
     * Deletes currentUserGroup. TODO Some sort of confirmation dialog
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteCurrentUserGroupAction() throws DAOException {
        UserBean ub = BeanUtils.getUserBean();
        if (ub != null && ub.getUser() != null) {
            logger.debug(currentOwnUserGroup.getName());
            if (DataManager.getInstance().getDao().deleteUserGroup(currentOwnUserGroup)) {
                Messages.info("deletedSuccessfully");
                logger.debug("UserGroup '{}' deleted.", currentOwnUserGroup.getName());
            }
        }
        resetCurrentUserGroupAction();
    }

    /**
     * Revokes the current user's membership in currentOtherUserGroup. TODO Some sort of confirmation dialog
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void leaveCurrentUserGroupAction() throws DAOException {
        UserBean ub = BeanUtils.getUserBean();
        if (ub != null && ub.getUser() != null && currentOtherUserGroup.removeMember(ub.getUser())) {
            Messages.info("userGroup_leftSuccessfully");
            logger.debug("User '{}' left user group '{}'.", ub.getUser().getEmail(), currentOtherUserGroup.getName());
        }
        resetCurrentUserGroupAction();
    }

    /**
     * Sets currentUserGroup to a new object.
     */
    public final void resetCurrentUserGroupAction() {
        currentOwnUserGroup = new UserGroup();
    }

    /**
     * Add currentMember to the member list of currentOwnUserGroup.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @Deprecated(since = "24.10")
    public void saveMembershipAction() throws DAOException {
        currentRole = new Role();
        UserBean ub = BeanUtils.getUserBean();
        if (ub != null && ub.getUser() != null && currentOwnUserGroup != null && currentMember != null) {
            try {
                if (currentOwnUserGroup.addMember(currentMember, currentRole)) {
                    Messages.info("userGroup_memberAddSuccess");
                    logger.debug("'{}' added to user group '{}'.", currentMember.getEmail(), currentOwnUserGroup.getName());
                } else {
                    Messages.error("userGroup_memberAddFailure");
                }
            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
                Messages.error("userGroup_memberAddFailure");
            }
        }
    }

    /**
     * Removes currentMember from the member list of currentUserGroup.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void removeCurrentMemberAction() throws DAOException {
        UserBean ub = BeanUtils.getUserBean();
        if (ub != null && ub.getUser() != null && currentMember != null) {
            if (currentOwnUserGroup.removeMember(currentMember)) {
                Messages.info("userGroup_memberRemoveSuccess");
                logger.debug("'{}' removed fromuser group '{}'.", currentMember.getEmail(), currentOwnUserGroup.getName());
            } else {
                Messages.error("userGroup_memberRemoveFailure");
            }
        }
    }

    /**
     * Returns the names all users that are not already members of the currently selected user group. TODO Filter some user groups, if required (e.g.
     * admins)
     *
     * @should return all non member names
     * @should not return active user name
     * @should not return group member names
     * @should not modify global user group list
     * @should return empty list if no remaining user group names
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<SelectItem> getRemainingUsers() throws DAOException {
        List<SelectItem> ret = new ArrayList<>();

        List<User> allUsers = new ArrayList<>();
        allUsers.addAll(DataManager.getInstance().getDao().getAllUsers(true));
        UserBean ub = BeanUtils.getUserBean();
        if (ub != null) {
            allUsers.remove(ub.getUser());
        }
        allUsers.removeAll(currentOwnUserGroup.getMembers());
        for (User u : allUsers) {
            ret.add(new SelectItem(u.getId(), u.getDisplayName()));
        }

        return ret;
    }

    /**
     * <p>
     * memberSelectedAction.
     * </p>
     *
     * @param event {@link jakarta.faces.event.ValueChangeEvent}
     */
    public void memberSelectedAction(ValueChangeEvent event) {
        // currentMember = DataManager.getInstance().getUserByName(String.valueOf(event.getNewValue()));
    }

    /**
     * Returns a list of all existing roles (minus superuser).
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Role> getAllRoles() throws DAOException {
        List<Role> ret = new ArrayList<>();

        for (Role role : DataManager.getInstance().getDao().getAllRoles()) {
            if (!role.getName().equals(Role.SUPERUSER_ROLE)) {
                ret.add(role);
            }
        }

        return ret;
    }

    /*********************************** Getter and Setter ***************************************/

    /**
     * <p>
     * Getter for the field <code>currentOtherUserGroup</code>.
     * </p>
     *
     * @return the currentOtherUserGroup
     */
    public UserGroup getCurrentOtherUserGroup() {
        return currentOtherUserGroup;
    }

    /**
     * <p>
     * Setter for the field <code>currentOtherUserGroup</code>.
     * </p>
     *
     * @param currentOtherUserGroup the currentOtherUserGroup to set
     */
    public void setCurrentOtherUserGroup(UserGroup currentOtherUserGroup) {
        this.currentOtherUserGroup = currentOtherUserGroup;
    }

    /**
     * <p>
     * Getter for the field <code>currentOwnUserGroupId</code>.
     * </p>
     *
     * @return the currentOwnUserGroupId
     */
    public int getCurrentOwnUserGroupId() {
        return currentOwnUserGroupId;
    }

    /**
     * <p>
     * Setter for the field <code>currentOwnUserGroupId</code>.
     * </p>
     *
     * @param currentOwnUserGroupId the currentOwnUserGroupId to set
     */
    public void setCurrentOwnUserGroupId(int currentOwnUserGroupId) {
        this.currentOwnUserGroupId = currentOwnUserGroupId;
    }

    /**
     * <p>
     * Getter for the field <code>currentOwnUserGroup</code>.
     * </p>
     *
     * @return the currentOwnUserGroup
     */
    public UserGroup getCurrentOwnUserGroup() {
        return currentOwnUserGroup;
    }

    /**
     * <p>
     * Setter for the field <code>currentOwnUserGroup</code>.
     * </p>
     *
     * @param currentOwnUserGroup the currentOwnUserGroup to set
     */
    public void setCurrentOwnUserGroup(UserGroup currentOwnUserGroup) {
        UserBean ub = BeanUtils.getUserBean();
        if (currentOwnUserGroup != null && ub != null && !currentOwnUserGroup.getOwner().equals(ub.getUser())) {
            logger.debug("not allowed");
            return;
        }
        this.currentOwnUserGroup = currentOwnUserGroup;
    }

    /**
     * <p>
     * Setter for the field <code>currentOwnUserGroup</code>.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void setCurrentOwnUserGroup() throws DAOException {
        setCurrentOwnUserGroup(DataManager.getInstance().getDao().getUserGroup(getCurrentOwnUserGroupId()));
    }

    /**
     * <p>
     * Getter for the field <code>currentMember</code>.
     * </p>
     *
     * @return the currentMember
     */
    public User getCurrentMember() {
        return currentMember;
    }

    /**
     * <p>
     * Setter for the field <code>currentMember</code>.
     * </p>
     *
     * @param currentMember the currentMember to set
     */
    public void setCurrentMember(User currentMember) {
        this.currentMember = currentMember;
    }

    /**
     * <p>
     * Getter for the field <code>currentRole</code>.
     * </p>
     *
     * @return the currentRole
     */
    public Role getCurrentRole() {
        return currentRole;
    }

    /**
     * <p>
     * Setter for the field <code>currentRole</code>.
     * </p>
     *
     * @param currentRole the currentRole to set
     */
    public void setCurrentRole(Role currentRole) {
        this.currentRole = currentRole;
    }

    /**
     * Used for the 'add member' selectbox.
     *
     * @return a {@link java.lang.Long} object.
     */
    public Long getCurrentId() {
        return -1L;
    }

    /**
     * Used for the 'add member' selectbox.
     *
     * @param id a {@link java.lang.Long} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void setCurrentId(Long id) throws DAOException {
        if (id != null) {
            currentMember = DataManager.getInstance().getDao().getUser(id);
        }
    }

    /**
     * <p>
     * isNewUserGroup.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isNewUserGroup() {
        return StringUtils.isEmpty(currentOwnUserGroup.getName());
    }
}
