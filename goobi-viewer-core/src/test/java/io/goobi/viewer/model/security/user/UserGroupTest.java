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

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;

public class UserGroupTest extends AbstractDatabaseEnabledTest {

    /**
     * @see UserGroup#getMemberCount()
     * @verifies count correctly
     */
    @Test
    void getMemberCount_shouldCountCorrectly() throws Exception {
        {   //owner + user
            UserGroup ug = DataManager.getInstance().getDao().getUserGroup(1);
            Assertions.assertNotNull(ug);
            Assertions.assertEquals(2, ug.getMemberCount());
        }
        {   //only owner
            UserGroup ug = DataManager.getInstance().getDao().getUserGroup(2);
            Assertions.assertNotNull(ug);
            Assertions.assertEquals(1, ug.getMemberCount());
        }
        {   //owner + two users including owner
            UserGroup ug = DataManager.getInstance().getDao().getUserGroup(3);
            Assertions.assertNotNull(ug);
            Assertions.assertEquals(2, ug.getMemberCount());
        }
    }

    /**
     * @see UserGroup#getMembers()
     * @verifies return all members
     */
    @Test
    void getMembers_shouldReturnAllMembers() throws Exception {
        UserGroup ug = DataManager.getInstance().getDao().getUserGroup(1);
        Assertions.assertNotNull(ug);
        Set<User> members = ug.getMembers();
        Assertions.assertEquals(1, members.size());
        Assertions.assertEquals(Long.valueOf(2), members.iterator().next().getId());
    }

    /**
     * @see UserGroup#getMembersAndOwner()
     * @verifies return all members and owner
     */
    @Test
    void getMembersAndOwner_shouldReturnAllMembersAndOwner() throws Exception {
        {
        UserGroup ug = DataManager.getInstance().getDao().getUserGroup(1);
        Assertions.assertNotNull(ug);
        Assertions.assertEquals(2, ug.getMembersAndOwner().size());
        }
        {
            UserGroup ug = DataManager.getInstance().getDao().getUserGroup(2);
            Assertions.assertNotNull(ug);
            Assertions.assertEquals(1, ug.getMembersAndOwner().size());
        }
        {
            UserGroup ug = DataManager.getInstance().getDao().getUserGroup(3);
            Assertions.assertNotNull(ug);
            Assertions.assertEquals(2, ug.getMembersAndOwner().size());
        }
    }
}
