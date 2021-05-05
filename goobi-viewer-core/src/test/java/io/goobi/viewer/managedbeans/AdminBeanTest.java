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
package io.goobi.viewer.managedbeans;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.security.user.UserRole;

public class AdminBeanTest extends AbstractDatabaseEnabledTest {

    /**
     * @see AdminBean#init()
     * @verifies sort lazyModelComments by dateUpdated desc by default
     */
    @Test
    public void init_shouldSortLazyModelCommentsByDateUpdatedDescByDefault() throws Exception {
        AdminBean bean = new AdminBean();
        bean.init();
        Assert.assertNotNull(bean.getLazyModelComments());
        Assert.assertEquals(4, bean.getLazyModelComments().getSizeOfDataList());
        LocalDateTime prevDate = null;
        for (Comment comment : bean.getLazyModelComments().getPaginatorList()) {
            if (prevDate != null) {
                Assert.assertTrue(DateTools.getMillisFromLocalDateTime(prevDate, false) >= DateTools
                        .getMillisFromLocalDateTime(comment.getDateUpdated(), false));
            }
            prevDate = comment.getDateUpdated();
        }
    }

    /**
     * @see AdminBean#getAllUsersExcept(List)
     * @verifies return all users except given
     */
    @Test
    public void getAllUsersExcept_shouldReturnAllUsersExceptGiven() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);

        AdminBean bean = new AdminBean();
        bean.init();

        Assert.assertEquals(3, bean.getAllUsers().size());
        List<User> result = bean.getAllUsersExcept(Collections.singleton(user));
        Assert.assertEquals(2, result.size());
    }

    /**
     * @see AdminBean#deleteUserAction(User,boolean)
     * @verifies delete all user public content correctly
     */
    @Test
    public void deleteUserAction_shouldDeleteAllUserPublicContentCorrectly() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assert.assertNotNull(user);
        AdminBean bean = new AdminBean();
        bean.setEmailConfirmation(user.getEmail());

        bean.deleteUserAction(user, true);

        // Comments
        Assert.assertNull(DataManager.getInstance().getDao().getComment(2));

        // Campaign statistics
        List<CampaignRecordStatistic> statistics = DataManager.getInstance().getDao().getCampaignStatisticsForRecord("PI_1", null);
        Assert.assertEquals(1, statistics.size());
        Assert.assertTrue(statistics.get(0).getReviewers().isEmpty());
        Assert.assertFalse(statistics.get(0).getReviewers().contains(user));
    }

    /**
     * @see AdminBean#deleteUserAction(User,boolean)
     * @verifies anonymize all user public content correctly
     */
    @Test
    public void deleteUserAction_shouldAnonymizeAllUserPublicContentCorrectly() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assert.assertNotNull(user);
        AdminBean bean = new AdminBean();
        bean.setEmailConfirmation(user.getEmail());

        bean.deleteUserAction(user, false);

        // Comments
        Comment comment = DataManager.getInstance().getDao().getComment(2);
        Assert.assertNotNull(comment);
        Assert.assertNotEquals(user, comment.getOwner());

        // Campaign statistics
        List<CampaignRecordStatistic> statistics = DataManager.getInstance().getDao().getCampaignStatisticsForRecord("PI_1", null);
        Assert.assertEquals(1, statistics.size());
        Assert.assertEquals(1, statistics.get(0).getReviewers().size());
        Assert.assertFalse(statistics.get(0).getReviewers().contains(user));
    }

    /**
     * @see AdminBean#getGroupedLicenseTypeSelectItems()
     * @verifies group license types in select item groups correctly
     */
    @Test
    public void getGroupedLicenseTypeSelectItems_shouldGroupLicenseTypesInSelectItemGroupsCorrectly() throws Exception {
        AdminBean bean = new AdminBean();
        bean.init();

        List<SelectItem> items = bean.getGroupedLicenseTypeSelectItems();
        Assert.assertEquals(2, items.size());
        Assert.assertEquals(1, ((SelectItemGroup) items.get(0)).getSelectItems().length);
        Assert.assertEquals(5, ((SelectItemGroup) items.get(1)).getSelectItems().length);
    }

	/**
	 * @see AdminBean#addUserRoleAction()
	 * @verifies add user if not yet in group
	 */
	@Test
	public void addUserRoleAction_shouldAddUserIfNotYetInGroup() throws Exception {
	    AdminBean bean = new AdminBean();
        bean.init();
        
        UserGroup group = new UserGroup();
        group.setName("group");
        
        User user = new User();
        Role role = DataManager.getInstance().getDao().getRole("member");
        UserRole userRole = new UserRole(group, user, role);
        
        bean.setCurrentUserGroup(group);
        bean.setCurrentUserRole(userRole);
        Assert.assertFalse(group.getMembers().contains(user));
        
        bean.addUserRoleAction();
        Assert.assertTrue(group.getMembers().contains(user));
	}
}