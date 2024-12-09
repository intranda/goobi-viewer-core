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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

class LicenseTest {

    /**
     * @see License#setIpRange(IpRange)
     * @verifies set user and userGroup to null if ipRange not null
     */
    @Test
    void setIpRange_shouldSetUserAndUserGroupToNullIfIpRangeNotNull() {
        {
            License lic = new License();
            lic.setUser(new User());
            Assertions.assertNotNull(lic.getUser());
            lic.setIpRange(new IpRange());
            Assertions.assertNull(lic.getUser());
        }
        {
            License lic = new License();
            lic.setUserGroup(new UserGroup());
            Assertions.assertNotNull(lic.getUserGroup());
            lic.setIpRange(new IpRange());
            Assertions.assertNull(lic.getUserGroup());
        }
    }

    /**
     * @see License#setIpRange(IpRange)
     * @verifies not set user and userGroup to null if ipRange null
     */
    @Test
    void setIpRange_shouldNotSetUserAndUserGroupToNullIfIpRangeNull() {
        {
            License lic = new License();
            lic.setUser(new User());
            Assertions.assertNotNull(lic.getUser());
            lic.setIpRange(null);
            Assertions.assertNotNull(lic.getUser());
        }
        {
            License lic = new License();
            lic.setUserGroup(new UserGroup());
            Assertions.assertNotNull(lic.getUserGroup());
            lic.setIpRange(null);
            Assertions.assertNotNull(lic.getUserGroup());
        }
    }

    /**
     * @see License#setUser(User)
     * @verifies set userGroup and ipRange to null if user not null
     */
    @Test
    void setUser_shouldSetUserGroupAndIpRangeToNullIfUserNotNull() {
        {
            License lic = new License();
            lic.setUserGroup(new UserGroup());
            Assertions.assertNotNull(lic.getUserGroup());
            lic.setUser(new User());
            Assertions.assertNull(lic.getUserGroup());
        }
        {
            License lic = new License();
            lic.setIpRange(new IpRange());
            Assertions.assertNotNull(lic.getIpRange());
            lic.setUser(new User());
            Assertions.assertNull(lic.getIpRange());
        }
    }

    /**
     * @see License#setUser(User)
     * @verifies not set userGroup and ipRange to null if user null
     */
    @Test
    void setUser_shouldNotSetUserGroupAndIpRangeToNullIfUserNull() {
        {
            License lic = new License();
            lic.setUserGroup(new UserGroup());
            Assertions.assertNotNull(lic.getUserGroup());
            lic.setUser(null);
            Assertions.assertNotNull(lic.getUserGroup());
        }
        {
            License lic = new License();
            lic.setIpRange(new IpRange());
            Assertions.assertNotNull(lic.getIpRange());
            lic.setUser(null);
            Assertions.assertNotNull(lic.getIpRange());
        }
    }

    /**
     * @see License#setUserGroup(UserGroup)
     * @verifies set user and ipRange to null if userGroup not null
     */
    @Test
    void setUserGroup_shouldSetUserAndIpRangeToNullIfUserGroupNotNull() {
        {
            License lic = new License();
            lic.setUser(new User());
            Assertions.assertNotNull(lic.getUser());
            lic.setUserGroup(new UserGroup());
            Assertions.assertNull(lic.getUser());
        }
        {
            License lic = new License();
            lic.setIpRange(new IpRange());
            Assertions.assertNotNull(lic.getIpRange());
            lic.setUserGroup(new UserGroup());
            Assertions.assertNull(lic.getIpRange());
        }
    }

    /**
     * @see License#setUserGroup(UserGroup)
     * @verifies not set user and ipRange to null if userGroup null
     */
    @Test
    void setUserGroup_shouldNotSetUserAndIpRangeToNullIfUserGroupNull() {
        {
            License lic = new License();
            lic.setUser(new User());
            Assertions.assertNotNull(lic.getUser());
            lic.setUserGroup(null);
            Assertions.assertNotNull(lic.getUser());
        }
        {
            License lic = new License();
            lic.setIpRange(new IpRange());
            Assertions.assertNotNull(lic.getIpRange());
            lic.setUserGroup(null);
            Assertions.assertNotNull(lic.getIpRange());
        }
    }

    /**
     * @see License#getAvailablePrivileges(Set)
     * @verifies only return priv view ugc if licenseType ugc type
     */
    @Test
    void getAvailablePrivileges_shouldOnlyReturnPrivViewUgcIfLicenseTypeUgcType() {
        License lic = new License();
        lic.setLicenseType(new LicenseType());
        lic.getLicenseType().setUgcType(true);
        List<String> result = lic.getAvailablePrivileges(Collections.emptySet());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(IPrivilegeHolder.PRIV_VIEW_UGC, result.get(0));
    }

    /**
     * @see License#getDisabledStatus()
     * @verifies return null if all relevant fields filled
     */
    @Test
    void getDisabledStatus_shouldOnlyReturnNullIfAllRelevantFieldsFilled() {
        License lic = new License();
        lic.setType("user");
        lic.setUser(new User());
        lic.setLicenseType(new LicenseType());
        Assertions.assertNull(lic.getDisabledStatus());
    }
}
