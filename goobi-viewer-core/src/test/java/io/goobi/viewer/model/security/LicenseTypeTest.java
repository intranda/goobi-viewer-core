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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

/**
 * @author Florian Alpers
 *
 */
class LicenseTypeTest extends AbstractTest {
    
    /**
     * @see LicenseType#setAccessTicketRequired(boolean)
     * @verifies add or remove list priviege
     */
    @Test
    void setAccessTicketRequired_shouldAddOrRemoveListPriviege() {
        LicenseType type = new LicenseType();
        Assertions.assertFalse(type.hasPrivilegeCopy(IPrivilegeHolder.PRIV_LIST));
        type.setAccessTicketRequired(true);
        Assertions.assertTrue(type.hasPrivilegeCopy(IPrivilegeHolder.PRIV_LIST));
        type.setAccessTicketRequired(false);
        Assertions.assertFalse(type.hasPrivilegeCopy(IPrivilegeHolder.PRIV_LIST));
    }

    /**
     * @verifies only return priv view ugc if ugc type
     */
    @Test
    void getAvailablePrivileges_shouldOnlyReturnPrivViewUgcIfUgcType() {
        LicenseType type = new LicenseType();
        type.setUgcType(true);
        List<String> result = type.getAvailablePrivileges(Collections.emptySet());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(IPrivilegeHolder.PRIV_VIEW_UGC, result.get(0));
    }

    /**
     * @verifies exclude already granted privileges
     */
    @Test
    void getAvailablePrivileges_shouldExcludeAlreadyGrantedPrivileges() {
        LicenseType type = new LicenseType();
        type.setUgcType(true);
        Set<String> privileges = new HashSet<>(Arrays.asList(IPrivilegeHolder.PRIV_VIEW_UGC));
        List<String> result = type.getAvailablePrivileges(privileges);
        Assertions.assertEquals(0, result.size());
    }

    /**
     * @see LicenseType#setAccessTicketRequired(boolean)
     * @verifies add or remove list privilege
     */
    @Test
    void setAccessTicketRequired_shouldAddOrRemoveListPrivilege() {
        // Setting accessTicketRequired to true adds LIST privilege, false removes it
        LicenseType type = new LicenseType();
        Assertions.assertFalse(type.hasPrivilegeCopy(IPrivilegeHolder.PRIV_LIST));
        type.setAccessTicketRequired(true);
        Assertions.assertTrue(type.hasPrivilegeCopy(IPrivilegeHolder.PRIV_LIST));
        type.setAccessTicketRequired(false);
        Assertions.assertFalse(type.hasPrivilegeCopy(IPrivilegeHolder.PRIV_LIST));
    }

    /**
     * @see LicenseType#getAvailablePrivileges(Set)
     * @verifies return record privileges if licenseType regular
     */
    @Test
    void getAvailablePrivileges_shouldReturnRecordPrivilegesIfLicenseTypeRegular() {
        // Regular (non-UGC) license type returns all record privileges
        LicenseType type = new LicenseType("regular_type");
        type.setUgcType(false);
        List<String> result = type.getAvailablePrivileges(Collections.emptySet());
        // Should contain record privileges
        Assertions.assertTrue(result.contains(IPrivilegeHolder.PRIV_LIST));
        Assertions.assertTrue(result.contains(IPrivilegeHolder.PRIV_VIEW_IMAGES));
        Assertions.assertTrue(result.contains(IPrivilegeHolder.PRIV_DOWNLOAD_PDF));
        // Should NOT contain only the single UGC privilege
        Assertions.assertTrue(result.size() > 1);
    }
}
