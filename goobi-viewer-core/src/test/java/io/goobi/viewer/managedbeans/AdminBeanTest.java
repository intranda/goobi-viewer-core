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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.security.user.UserRole;

class AdminBeanTest extends AbstractDatabaseEnabledTest {

    @AfterEach
    void resetTranslationLock() {
        AdminBean.setTranslationGroupsEditorSession(null);
    }

    /**
     * @see AdminBean#getConfiguredTranslationGroups()
     * @verifies not overwrite existing translation lock
     */
    @Test
    void getConfiguredTranslationGroups_shouldNotOverwriteExistingTranslationLock() {
        AdminBean.setTranslationGroupsEditorSession("session-A");

        new AdminBean().getConfiguredTranslationGroups();

        assertEquals("session-A", AdminBean.getTranslationGroupsEditorSession());
    }

    /**
     * @see AdminBean#getConfiguredTranslationGroups()
     * @verifies not set translation lock when none exists
     */
    @Test
    void getConfiguredTranslationGroups_shouldNotSetTranslationLockWhenNoneExists() {
        AdminBean.setTranslationGroupsEditorSession(null);

        new AdminBean().getConfiguredTranslationGroups();

        assertNull(AdminBean.getTranslationGroupsEditorSession());
    }

    /**
     * @verifies return all users except given
     */
    @Test
    void getAllUsersExcept_shouldReturnAllUsersExceptGiven() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assertions.assertNotNull(user);

        AdminBean bean = new AdminBean();
        bean.init();

        Assertions.assertEquals(3, bean.getAllUsers().size());
        List<User> result = bean.getAllUsersExcept(Collections.singleton(user));
        Assertions.assertEquals(2, result.size());
    }

    /**
     * @verifies remove user comments and campaign statistic entries when deleteContent flag is true
     */
    @Test
    void deleteUserAction_shouldRemoveUserCommentsAndCampaignStatisticEntriesWhenDeleteContentFlagIsTrue() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assertions.assertNotNull(user);
        AdminBean bean = new AdminBean();
        bean.setEmailConfirmation(user.getEmail());

        bean.deleteUserAction(user, true);

        // Comments
        Assertions.assertNull(DataManager.getInstance().getDao().getComment(2));

        // Campaign statistics
        List<CampaignRecordStatistic> statistics = DataManager.getInstance().getDao().getCampaignStatisticsForRecord("PI_1", null);
        Assertions.assertEquals(1, statistics.size());
        Assertions.assertTrue(statistics.get(0).getReviewers().isEmpty());
        Assertions.assertFalse(statistics.get(0).getReviewers().contains(user));
    }

    /**
     * @verifies reassign comments and statistics to different user when deleteContent flag is false
     */
    @Test
    void deleteUserAction_shouldReassignCommentsAndStatisticsToDifferentUserWhenDeleteContentFlagIsFalse() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assertions.assertNotNull(user);
        AdminBean bean = new AdminBean();
        bean.setEmailConfirmation(user.getEmail());

        bean.deleteUserAction(user, false);

        // Comments
        Comment comment = DataManager.getInstance().getDao().getComment(2);
        Assertions.assertNotNull(comment);
        Assertions.assertNotEquals(user, comment.getCreator());

        // Campaign statistics
        List<CampaignRecordStatistic> statistics = DataManager.getInstance().getDao().getCampaignStatisticsForRecord("PI_1", null);
        Assertions.assertEquals(1, statistics.size());
        Assertions.assertEquals(1, statistics.get(0).getReviewers().size());
        Assertions.assertFalse(statistics.get(0).getReviewers().contains(user));
    }

    /**
     * @verifies add user if not yet in group
     */
    @Test
    void addUserRoleAction_shouldAddUserIfNotYetInGroup() throws Exception {
        AdminBean bean = new AdminBean();
        bean.init();

        UserGroup group = new UserGroup();
        group.setName("group");

        User user = new User();
        Role role = DataManager.getInstance().getDao().getRole("member");
        UserRole userRole = new UserRole(group, user, role);

        bean.setCurrentUserGroup(group);
        bean.setCurrentUserRole(userRole);
        Assertions.assertFalse(group.getMembers().contains(user));

        bean.addUserRoleAction();
        Assertions.assertTrue(group.getMembers().contains(user));
    }

    /**
     * @verifies save dirty user roles to database so they become retrievable via DAO
     */
    @Test
    void updateUserRoles_shouldSaveDirtyUserRolesToDatabaseSoTheyBecomeRetrievableViaDAO() throws Exception {

        UserGroup group = DataManager.getInstance().getDao().getUserGroup(1);
        Assertions.assertNotNull(group);

        User user = DataManager.getInstance().getDao().getUser(3);
        Assertions.assertNotNull(user);

        Assertions.assertFalse(group.getMembers().contains(user));

        Role role = DataManager.getInstance().getDao().getRole("member");
        UserRole userRole = new UserRole(group, user, role);

        AdminBean bean = new AdminBean();
        bean.init();
        bean.setCurrentUserGroup(group);
        bean.setCurrentUserRole(userRole);

        Assertions.assertTrue(DataManager.getInstance().getDao().getUserRoles(group, user, role).isEmpty());

        bean.addUserRoleAction();
        Assertions.assertTrue(group.getMembers().contains(user));

        Assertions.assertTrue(bean.getUserRolesToSave().contains(userRole));
        bean.updateUserRoles();
        Assertions.assertFalse(DataManager.getInstance().getDao().getUserRoles(group, user, role).isEmpty());
    }

    /**
     * @verifies multiple roles added on new group
     */
    @Test
    void updateUserRoles_shouldMultipleRolesAddedOnNewGroup() throws Exception {
        AdminBean bean = new AdminBean();
        bean.init();

        User u1 = DataManager.getInstance().getDao().getUser(1l);
        User u2 = DataManager.getInstance().getDao().getUser(2l);

        UserGroup group = new UserGroup();
        group.setName("test");
        group.setOwner(u1);
        bean.setCurrentUserGroup(group);

        Role r1 = DataManager.getInstance().getDao().getRole("member");
        UserRole ur1 = new UserRole(group, u1, r1);
        bean.setCurrentUserRole(ur1);
        bean.addUserRoleAction();

        Role r2 = DataManager.getInstance().getDao().getRole("member");
        UserRole ur2 = new UserRole(group, u2, r2);
        bean.setCurrentUserRole(ur2);
        bean.addUserRoleAction();

        assertEquals(2, bean.getUserRolesToSave().size());
        assertTrue(bean.getUserRolesToSave().contains(ur1));
        assertTrue(bean.getUserRolesToSave().contains(ur2));

        bean.saveUserGroupAction();

        UserGroup loadedGroup = DataManager.getInstance().getDao().getUserGroup("test");
        assertEquals(group, loadedGroup);
        assertEquals(2, loadedGroup.getMemberships().size());
        assertTrue(loadedGroup.getMemberships().contains(ur1));
        assertTrue(loadedGroup.getMemberships().contains(ur2));
    }

    /**
     * @see AdminBean#deleteUserRoleAction(UserRole)
     * @verifies keep a pending added member when removing another member
     */
    @Test
    void deleteUserRoleAction_shouldKeepAddedMemberWhenRemovingAnother() throws Exception {
        UserGroup group = DataManager.getInstance().getDao().getUserGroup(1);
        Assertions.assertNotNull(group);

        User userA = DataManager.getInstance().getDao().getUser(3);
        User userB = DataManager.getInstance().getDao().getUser(2);
        Role role = DataManager.getInstance().getDao().getRole("member");

        AdminBean bean = new AdminBean();
        bean.init();
        bean.setCurrentUserGroup(group);

        // Group 1 already contains userB as a persisted member
        assertTrue(group.getMembers().contains(userB));

        // Add userA to the temporary list
        UserRole membershipA = new UserRole(group, userA, role);
        bean.setCurrentUserRole(membershipA);
        bean.addUserRoleAction();
        assertTrue(group.getMembers().contains(userA));

        // Remove userB from the temporary list
        UserRole membershipB = group.getMemberships().stream().filter(r -> userB.equals(r.getUser())).findFirst().orElse(null);
        Assertions.assertNotNull(membershipB);
        bean.deleteUserRoleAction(membershipB);

        // userA must still be present after removing userB
        assertTrue(group.getMembers().contains(userA));
        Assertions.assertFalse(group.getMembers().contains(userB));
        assertTrue(bean.getUserRolesToSave().contains(membershipA));
        assertTrue(bean.getUserRolesToDelete().contains(membershipB));

        assertDoesNotThrow(() -> bean.updateUserRoles());

        UserGroup reloaded = DataManager.getInstance().getDao().getUserGroup(1);
        assertTrue(reloaded.getMembers().contains(userA));
        Assertions.assertFalse(reloaded.getMembers().contains(userB));
    }

    /**
     * @see AdminBean#deleteUserRoleAction(UserRole)
     * @verifies cancel out adding and then removing the same unpersisted member
     */
    @Test
    void deleteUserRoleAction_shouldCancelOutAddThenRemoveOfSameMember() throws Exception {
        User owner = DataManager.getInstance().getDao().getUser(1);
        User userA = DataManager.getInstance().getDao().getUser(2);
        Role role = DataManager.getInstance().getDao().getRole("member");

        UserGroup group = new UserGroup();
        group.setName("addRemoveNoop");
        group.setOwner(owner);

        AdminBean bean = new AdminBean();
        bean.init();
        bean.setCurrentUserGroup(group);

        UserRole membershipA = new UserRole(group, userA, role);
        bean.setCurrentUserRole(membershipA);
        bean.addUserRoleAction();
        assertTrue(group.getMembers().contains(userA));
        assertEquals(1, bean.getUserRolesToSave().size());

        // Remove the just-added, unpersisted member again
        bean.deleteUserRoleAction(membershipA);
        assertTrue(bean.getUserRolesToSave().isEmpty());
        assertTrue(bean.getUserRolesToDelete().isEmpty());
        Assertions.assertFalse(group.getMembers().contains(userA));

        assertDoesNotThrow(() -> bean.updateUserRoles());
    }

    /**
     * @see AdminBean#unlockTranslation(String)
     * @verifies only unlock if sessionId matches current editor session
     */
    @Test
    void unlockTranslation_shouldOnlyUnlockForMatchingSession() {
        AdminBean.setTranslationGroupsEditorSession("session-owner");

        // Wrong session – should do nothing
        AdminBean.unlockTranslation("session-other");
        assertEquals("session-owner", AdminBean.getTranslationGroupsEditorSession());

        // Correct session – should release
        AdminBean.unlockTranslation("session-owner");
        assertNull(AdminBean.getTranslationGroupsEditorSession());
    }

    /**
     * @see AdminBean#unlockTranslation(String)
     * @verifies handle null session gracefully
     */
    @Test
    void unlockTranslation_shouldHandleNullSessionGracefully() {
        AdminBean.setTranslationGroupsEditorSession("session-owner");
        assertDoesNotThrow(() -> AdminBean.unlockTranslation(null));
        // Lock should remain (null doesn't match)
        assertEquals("session-owner", AdminBean.getTranslationGroupsEditorSession());
        // Cleanup
        AdminBean.setTranslationGroupsEditorSession(null);
    }
}
