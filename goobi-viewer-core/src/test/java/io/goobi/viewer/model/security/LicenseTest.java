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
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.security.License.AccessType;
import io.goobi.viewer.model.security.user.User;

class LicenseTest {

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
        lic.getLicensees().get(0).setType(AccessType.USER);
        lic.getLicensees().get(0).setUser(new User());
        lic.setLicenseType(new LicenseType());
        Assertions.assertNull(lic.getDisabledStatus());
    }
}
