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
package io.goobi.viewer.model.security;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

public class LicenseTest {

    /**
     * @see License#setIpRange(IpRange)
     * @verifies set user and userGroup to null if ipRange not null
     */
    @Test
    public void setIpRange_shouldSetUserAndUserGroupToNullIfIpRangeNotNull() throws Exception {
        {
            License lic = new License();
            lic.setUser(new User());
            Assert.assertNotNull(lic.getUser());
            lic.setIpRange(new IpRange());
            Assert.assertNull(lic.getUser());
        }
        {
            License lic = new License();
            lic.setUserGroup(new UserGroup());
            Assert.assertNotNull(lic.getUserGroup());
            lic.setIpRange(new IpRange());
            Assert.assertNull(lic.getUserGroup());
        }
    }

    /**
     * @see License#setIpRange(IpRange)
     * @verifies not set user and userGroup to null if ipRange null
     */
    @Test
    public void setIpRange_shouldNotSetUserAndUserGroupToNullIfIpRangeNull() throws Exception {
        {
            License lic = new License();
            lic.setUser(new User());
            Assert.assertNotNull(lic.getUser());
            lic.setIpRange(null);
            Assert.assertNotNull(lic.getUser());
        }
        {
            License lic = new License();
            lic.setUserGroup(new UserGroup());
            Assert.assertNotNull(lic.getUserGroup());
            lic.setIpRange(null);
            Assert.assertNotNull(lic.getUserGroup());
        }
    }

    /**
     * @see License#setUser(User)
     * @verifies set userGroup and ipRange to null if user not null
     */
    @Test
    public void setUser_shouldSetUserGroupAndIpRangeToNullIfUserNotNull() throws Exception {
        {
            License lic = new License();
            lic.setUserGroup(new UserGroup());
            Assert.assertNotNull(lic.getUserGroup());
            lic.setUser(new User());
            Assert.assertNull(lic.getUserGroup());
        }
        {
            License lic = new License();
            lic.setIpRange(new IpRange());
            Assert.assertNotNull(lic.getIpRange());
            lic.setUser(new User());
            Assert.assertNull(lic.getIpRange());
        }
    }

    /**
     * @see License#setUser(User)
     * @verifies not set userGroup and ipRange to null if user null
     */
    @Test
    public void setUser_shouldNotSetUserGroupAndIpRangeToNullIfUserNull() throws Exception {
        {
            License lic = new License();
            lic.setUserGroup(new UserGroup());
            Assert.assertNotNull(lic.getUserGroup());
            lic.setUser(null);
            Assert.assertNotNull(lic.getUserGroup());
        }
        {
            License lic = new License();
            lic.setIpRange(new IpRange());
            Assert.assertNotNull(lic.getIpRange());
            lic.setUser(null);
            Assert.assertNotNull(lic.getIpRange());
        }
    }

    /**
     * @see License#setUserGroup(UserGroup)
     * @verifies set user and ipRange to null if userGroup not null
     */
    @Test
    public void setUserGroup_shouldSetUserAndIpRangeToNullIfUserGroupNotNull() throws Exception {
        {
            License lic = new License();
            lic.setUser(new User());
            Assert.assertNotNull(lic.getUser());
            lic.setUserGroup(new UserGroup());
            Assert.assertNull(lic.getUser());
        }
        {
            License lic = new License();
            lic.setIpRange(new IpRange());
            Assert.assertNotNull(lic.getIpRange());
            lic.setUserGroup(new UserGroup());
            Assert.assertNull(lic.getIpRange());
        }
    }

    /**
     * @see License#setUserGroup(UserGroup)
     * @verifies not set user and ipRange to null if userGroup null
     */
    @Test
    public void setUserGroup_shouldNotSetUserAndIpRangeToNullIfUserGroupNull() throws Exception {
        {
            License lic = new License();
            lic.setUser(new User());
            Assert.assertNotNull(lic.getUser());
            lic.setUserGroup(null);
            Assert.assertNotNull(lic.getUser());
        }
        {
            License lic = new License();
            lic.setIpRange(new IpRange());
            Assert.assertNotNull(lic.getIpRange());
            lic.setUserGroup(null);
            Assert.assertNotNull(lic.getIpRange());
        }
    }

    /**
     * @see License#getAvailablePrivileges(Set)
     * @verifies only return priv view ugc if licenseType ugc type
     */
    @Test
    public void getAvailablePrivileges_shouldOnlyReturnPrivViewUgcIfLicenseTypeUgcType() throws Exception {
        License lic = new License();
        lic.setLicenseType(new LicenseType());
        lic.getLicenseType().setUgcType(true);
        List<String> result = lic.getAvailablePrivileges(Collections.emptySet());
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(IPrivilegeHolder.PRIV_VIEW_UGC, result.get(0));
    }
}
