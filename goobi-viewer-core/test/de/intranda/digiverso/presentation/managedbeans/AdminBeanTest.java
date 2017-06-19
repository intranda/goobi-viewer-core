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

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseEnabledTest;
import de.intranda.digiverso.presentation.model.annotation.Comment;

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
}