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
package de.intranda.digiverso.presentation.model.user;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseEnabledTest;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.security.user.User;

public class UserTest extends AbstractDatabaseEnabledTest {

    /**
     * @see User#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return true if condition is open access
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfConditionIsOpenAccess() throws Exception {
        User user = new User();
        user.setSuperuser(false);
        Assert.assertTrue(user.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList(SolrConstants.OPEN_ACCESS_VALUE)),
                IPrivilegeHolder.PRIV_LIST, "PPN123"));
    }

    /**
     * @see User#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return true if user is superuser
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfUserIsSuperuser() throws Exception {
        User user = new User();        
        user.setSuperuser(true);
        Assert.assertTrue(user.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList("restricted")), IPrivilegeHolder.PRIV_LIST,
                "PPN123"));
    }

    /**
     * @see User#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return true if user has license
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfUserHasLicense() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assert.assertNotNull(user);
        Assert.assertTrue(user.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList("license type 1 name")),
                IPrivilegeHolder.PRIV_LIST, "PPN123"));
    }

    /**
     * @see User#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return false if user has no license
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnFalseIfUserHasNoLicense() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assert.assertNotNull(user);
        Assert.assertFalse(user.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList("license type 1 name")),
                IPrivilegeHolder.PRIV_VIEW_IMAGES, "PPN123"));
    }

    /**
     * @see User#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return true if condition list empty
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfConditionListEmpty() throws Exception {
        User user = new User();
        user.setSuperuser(false);
        Assert.assertTrue(user.canSatisfyAllAccessConditions(new HashSet<String>(0), IPrivilegeHolder.PRIV_LIST, "PPN123"));
    }
}