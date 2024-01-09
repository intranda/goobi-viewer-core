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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

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

public class AdminBeanTest extends AbstractDatabaseEnabledTest {

    /**
     * @see AdminBean#getAllUsersExcept(List)
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
     * @see AdminBean#deleteUserAction(User,boolean)
     * @verifies delete all user public content correctly
     */
    @Test
    void deleteUserAction_shouldDeleteAllUserPublicContentCorrectly() throws Exception {
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
     * @see AdminBean#deleteUserAction(User,boolean)
     * @verifies anonymize all user public content correctly
     */
    @Test
    void deleteUserAction_shouldAnonymizeAllUserPublicContentCorrectly() throws Exception {
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
     * @see AdminBean#addUserRoleAction()
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
     * @see AdminBean#updateUserRoles()
     * @verifies persist UserRole correctly
     */
    @Test
    void updateUserRoles_shouldPersistUserRoleCorrectly() throws Exception {

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

        Assertions.assertTrue(bean.getDirtyUserRoles().containsKey(userRole));
        bean.updateUserRoles();
        Assertions.assertFalse(DataManager.getInstance().getDao().getUserRoles(group, user, role).isEmpty());
    }

    @Test
    void updateUserRoles_multipleRolesAddedOnNewGroup() throws Exception {
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

        assertEquals(2, bean.getDirtyUserRoles().size());
        assertEquals("save", bean.getDirtyUserRoles().get(ur1));
        assertEquals("save", bean.getDirtyUserRoles().get(ur2));

        bean.saveUserGroupAction();

        UserGroup loadedGroup = DataManager.getInstance().getDao().getUserGroup("test");
        assertEquals(group, loadedGroup);
        assertEquals(2, loadedGroup.getMemberships().size());
        assertTrue(loadedGroup.getMemberships().contains(ur1));
        assertTrue(loadedGroup.getMemberships().contains(ur2));
    }
}
