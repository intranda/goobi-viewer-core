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
     * @see License#getAvailablePrivileges(Set)
     * @verifies return cms privileges if licenseType cms type
     */
    @Test
    void getAvailablePrivileges_shouldReturnCmsPrivilegesIfLicenseTypeCmsType() {
        // CMS license type returns CMS-specific privileges
        License lic = new License();
        LicenseType cmsType = new LicenseType(LicenseType.LICENSE_TYPE_CMS);
        lic.setLicenseType(cmsType);
        List<String> result = lic.getAvailablePrivileges(Collections.emptySet());
        // Result should contain CMS privileges (PRIV_CMS_PAGES, PRIV_CMS_MENU, etc.)
        Assertions.assertTrue(result.contains(IPrivilegeHolder.PRIV_CMS_PAGES));
        Assertions.assertTrue(result.contains(IPrivilegeHolder.PRIV_CMS_MENU));
        // Result should NOT contain record-only privileges
        Assertions.assertFalse(result.contains(IPrivilegeHolder.PRIV_VIEW_IMAGES));
    }

    /**
     * @see License#getAvailablePrivileges(Set)
     * @verifies return record privileges if licenseType regular
     */
    @Test
    void getAvailablePrivileges_shouldReturnRecordPrivilegesIfLicenseTypeRegular() {
        // Regular (non-CMS, non-UGC) license type returns record privileges
        License lic = new License();
        LicenseType regularType = new LicenseType("some_regular_type");
        lic.setLicenseType(regularType);
        List<String> result = lic.getAvailablePrivileges(Collections.emptySet());
        // Result should contain record privileges like LIST, VIEW_IMAGES, etc.
        Assertions.assertTrue(result.contains(IPrivilegeHolder.PRIV_LIST));
        Assertions.assertTrue(result.contains(IPrivilegeHolder.PRIV_VIEW_IMAGES));
        // Result should NOT contain CMS-only privileges
        Assertions.assertFalse(result.contains(IPrivilegeHolder.PRIV_CMS_MENU));
    }

    /**
     * @see License#getDisabledStatus()
     * @verifies return null if all relevant fields filled
     */
    @Test
    void getDisabledStatus_shouldReturnNullIfAllRelevantFieldsFilled() {
        // When licenseType is set and all licensees are valid, getDisabledStatus returns null
        License lic = new License();
        lic.getLicensees().get(0).setType(AccessType.USER);
        lic.getLicensees().get(0).setUser(new User());
        lic.setLicenseType(new LicenseType());
        Assertions.assertNull(lic.getDisabledStatus());
    }

    /**
     * @verifies only return null if all relevant fields filled
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
