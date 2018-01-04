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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.security.Role;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.security.user.UserGroup;

@Named
@SessionScoped
public class UserGroupBean implements Serializable {

    private static final long serialVersionUID = -6982988135819597474L;

    private static final Logger logger = LoggerFactory.getLogger(UserGroupBean.class);

    private int currentOwnUserGroupId;
    private UserGroup currentOwnUserGroup;
    private UserGroup currentOtherUserGroup;
    private User currentMember;
    private Role currentRole;

    /** Empty constructor. */
    public UserGroupBean() {
        // the emptiness inside
    }

    @PostConstruct
    public void init() {
        if (currentOwnUserGroup == null) {
            resetCurrentUserGroupAction();
        }
    }

    /**
     * Creates or updates (if already exists) currentOwnUserGroup.
     *
     * @throws DAOException
     */
    public void saveCurrentOwnUserGroupAction(ActionEvent actionEvent) throws DAOException {
        UserBean ub = BeanUtils.getUserBean();
        if (ub != null && ub.getUser() != null && StringUtils.isNotEmpty(currentOwnUserGroup.getName())) {
            if (ub.getUser().getUserGroupOwnerships().contains(currentOwnUserGroup)) {
                DataManager.getInstance().getDao().updateUserGroup(currentOwnUserGroup);
                Messages.info("updatedSuccessfully");
                logger.debug("Bookshelf '" + currentOwnUserGroup.getName() + "' updated.");
                return;
            }
            currentOwnUserGroup.setOwner(ub.getUser());
            if (DataManager.getInstance().getDao().addUserGroup(currentOwnUserGroup)) {
                resetCurrentUserGroupAction();
                Messages.info("savedSuccessfully");
                logger.debug("Bookshelf '" + currentOwnUserGroup.getName() + "' added.");
                return;
            }
        }
        Messages.error("errSave");
        logger.error("error while saving user group");
    }

    /**
     * Deletes currentUserGroup. TODO Some sort of confirmation dialog
     *
     * @throws DAOException
     */
    public void deleteCurrentUserGroupAction() throws DAOException {
        UserBean ub = BeanUtils.getUserBean();
        if (ub != null && ub.getUser() != null) {
            logger.debug(currentOwnUserGroup.getName());
            if (DataManager.getInstance().getDao().deleteUserGroup(currentOwnUserGroup)) {
                Messages.info("deletedSuccessfully");
                logger.debug("UserGroup '" + currentOwnUserGroup.getName() + "' deleted.");
            }
        }
        resetCurrentUserGroupAction();
    }

    /**
     * Revokes the current user's membership in currentOtherUserGroup. TODO Some sort of confirmation dialog
     *
     * @throws DAOException
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
     * @throws DAOException
     */
    public void saveMembershipAction() throws DAOException {
        currentRole = new Role();
        UserBean ub = BeanUtils.getUserBean();
        if (ub != null && ub.getUser() != null && currentOwnUserGroup != null && currentMember != null && currentRole != null) {
            try {
                if (currentOwnUserGroup.addMember(currentMember, currentRole)) {
                    Messages.info("userGroup_memberAddSuccess");
                    logger.debug("'" + currentMember.getEmail() + "' added to user group '" + currentOwnUserGroup.getName() + "'.");
                } else {
                    Messages.error("userGroup_memberAddFailure");
                }
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
                Messages.error("userGroup_memberAddFailure");
            }
        }
    }

    /**
     * Removes currentMember from the member list of currentUserGroup.
     *
     * @throws DAOException
     */
    public void removeCurrentMemberAction() throws DAOException {
        UserBean ub = BeanUtils.getUserBean();
        if (ub != null && ub.getUser() != null && currentMember != null) {
            if (currentOwnUserGroup.removeMember(currentMember)) {
                Messages.info("userGroup_memberRemoveSuccess");
                logger.debug("'" + currentMember.getEmail() + "' removed fromuser group '" + currentOwnUserGroup.getName() + "'.");
            } else {
                Messages.error("userGroup_memberRemoveFailure");
            }
        }
    }

    /**
     * Returns the names all users that are not already members of the currently selected user group. TODO Filter some user groups, if required (e.g.
     * admins)
     *
     * @return
     * @throws DAOException
     * @should return all non member names
     * @should not return active user name
     * @should not return group member names
     * @should not modify global user group list
     * @should return empty list if no remaining user group names
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
     * @param event {@link ValueChangeEvent}
     */
    public void memberSelectedAction(ValueChangeEvent event) {
        // currentMember = DataManager.getInstance().getUserByName(String.valueOf(event.getNewValue()));
    }

    /**
     * Returns a list of all existing roles (minus superuser).
     *
     * @return
     * @throws DAOException
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
     * @return the currentOtherUserGroup
     */
    public UserGroup getCurrentOtherUserGroup() {
        return currentOtherUserGroup;
    }

    /**
     * @param currentOtherUserGroup the currentOtherUserGroup to set
     */
    public void setCurrentOtherUserGroup(UserGroup currentOtherUserGroup) {
        this.currentOtherUserGroup = currentOtherUserGroup;
    }

    /**
     * @return the currentOwnUserGroupId
     */
    public int getCurrentOwnUserGroupId() {
        return currentOwnUserGroupId;
    }

    /**
     * @param currentOwnUserGroupId the currentOwnUserGroupId to set
     */
    public void setCurrentOwnUserGroupId(int currentOwnUserGroupId) {
        this.currentOwnUserGroupId = currentOwnUserGroupId;
    }

    /**
     * @return the currentOwnUserGroup
     */
    public UserGroup getCurrentOwnUserGroup() {
        return currentOwnUserGroup;
    }

    /**
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
     * @throws DAOException
     *
     */
    public void setCurrentOwnUserGroup() throws DAOException {
        setCurrentOwnUserGroup(DataManager.getInstance().getDao().getUserGroup(getCurrentOwnUserGroupId()));
    }

    /**
     * @return the currentMember
     */
    public User getCurrentMember() {
        return currentMember;
    }

    /**
     * @param currentMember the currentMember to set
     */
    public void setCurrentMember(User currentMember) {
        this.currentMember = currentMember;
    }

    /**
     * @return the currentRole
     */
    public Role getCurrentRole() {
        return currentRole;
    }

    /**
     * @param currentRole the currentRole to set
     */
    public void setCurrentRole(Role currentRole) {
        this.currentRole = currentRole;
    }

    /**
     * Used for the 'add member' selectbox.
     *
     * @return
     */
    public Long getCurrentId() {
        return -1L;
    }

    /**
     * Used for the 'add member' selectbox.
     *
     * @throws DAOException
     */
    public void setCurrentId(Long id) throws DAOException {
        if (id != null) {
            currentMember = DataManager.getInstance().getDao().getUser(id);
        }
    }

    public boolean isNewUserGroup() {
        return StringUtils.isEmpty(currentOwnUserGroup.getName());
    }
}
