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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.security.user.User;

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
        Date prevDate = null;
        for (Comment comment : bean.getLazyModelComments().getPaginatorList()) {
            if (prevDate != null) {
                Assert.assertTrue(prevDate.getTime() >= comment.getDateUpdated().getTime());
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
     * @verifies delete all user comments correctly
     */
    @Test
    public void deleteUserAction_shouldDeleteAllUserCommentsCorrectly() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        AdminBean bean = new AdminBean();
        bean.setEmailConfirmation(user.getEmail());

        bean.deleteUserAction(user, true);
        Assert.assertNull(DataManager.getInstance().getDao().getComment(1));
        Assert.assertNull(DataManager.getInstance().getDao().getComment(3));
        Assert.assertNull(DataManager.getInstance().getDao().getComment(4));
    }
}