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

import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.comments.CommentGroup;
import io.goobi.viewer.model.security.user.User;

public class AdminCommentBeanTest extends AbstractDatabaseEnabledTest {

    /**
     * @see AdminCommentBean#init()
     * @verifies sort lazyModelComments by dateCreated desc by default
     */
    @Test
    public void init_shouldSortLazyModelCommentsByDateCreatedDescByDefault() throws Exception {
        AdminCommentBean bean = new AdminCommentBean();
        bean.setUserBean(new UserBean());
        User admin = new User();
        admin.setSuperuser(true);
        bean.getUserBean().setUser(admin);
        bean.setCurrentCommentGroup(CommentGroup.createCommentGroupAll());

        bean.init();
        Assert.assertNotNull(bean.getLazyModelComments());
        Assert.assertEquals(4, bean.getLazyModelComments().getSizeOfDataList());
        LocalDateTime prevDate = null;
        for (Comment comment : bean.getLazyModelComments().getPaginatorList()) {
            if (prevDate != null && comment.getDateCreated() != null) {
                Assert.assertTrue(DateTools.getMillisFromLocalDateTime(prevDate, false) >= DateTools
                        .getMillisFromLocalDateTime(comment.getDateCreated(), false));
            }
            prevDate = comment.getDateCreated();
        }
    }
}
