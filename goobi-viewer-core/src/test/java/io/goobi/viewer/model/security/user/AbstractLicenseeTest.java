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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.security.AccessDeniedInfoConfig;
import io.goobi.viewer.model.security.AccessPermission;

class AbstractLicenseeTest {

    /**
     * @verifies return denied if permissionMap empty
     */
    @Test
    void getAccessPermissionFromMap_shouldReturnDeniedIfPermissionMapEmpty() {
        Assertions.assertFalse(AbstractLicensee.getAccessPermissionFromMap(Collections.emptyMap()).isGranted());
    }

    /**
     * @verifies return denied if all permissions in map denied
     */
    @Test
    void getAccessPermissionFromMap_shouldReturnDeniedIfAllPermissionsInMapDenied() {
        Assertions.assertFalse(AbstractLicensee.getAccessPermissionFromMap(Collections.singletonMap("", AccessPermission.denied())).isGranted());
    }

    /**
     * @verifies preserve accessTicketRequired
     */
    @Test
    void getAccessPermissionFromMap_shouldPreserveAccessTicketRequired() {
        Assertions.assertTrue(
                AbstractLicensee.getAccessPermissionFromMap(Collections.singletonMap("", AccessPermission.granted().setAccessTicketRequired(true)))
                        .isAccessTicketRequired());
    }

    /**
     * @verifies preserve downloadTicketRequired
     */
    @Test
    void getAccessPermissionFromMap_shouldPreserveDownloadTicketRequired() {
        Assertions.assertTrue(
                AbstractLicensee.getAccessPermissionFromMap(Collections.singletonMap("", AccessPermission.granted().setDownloadTicketRequired(true)))
                        .isDownloadTicketRequired());
    }

    /**
     * @verifies preserve redirect metadata
     */
    @Test
    void getAccessPermissionFromMap_shouldPreserveRedirectMetadata() {
        AccessPermission access =
                AbstractLicensee
                        .getAccessPermissionFromMap(Collections.singletonMap("",
                                AccessPermission.granted().setRedirect(true).setRedirectUrl("https://example.com")));
        Assertions.assertTrue(access.isRedirect());
        Assertions.assertEquals("https://example.com", access.getRedirectUrl());
    }
    
    /**
     * @verifies preserve access denied placeholder info
     */
    @Test
    void getAccessPermissionFromMap_shouldPreserveAccessDeniedPlaceholderInfo() {
        Map<String, AccessDeniedInfoConfig> accessDeniedPlaceholderInfo = new HashMap<>();
        accessDeniedPlaceholderInfo.put("en", new AccessDeniedInfoConfig("en", "https://example.com/denied.png", "NOES"));
        AccessPermission access =
                AbstractLicensee
                        .getAccessPermissionFromMap(Collections.singletonMap("",
                                AccessPermission.granted().setAccessDeniedPlaceholderInfo(accessDeniedPlaceholderInfo)));
        Assertions.assertNotNull(access.getAccessDeniedPlaceholderInfo().get("en"));
    }

    /**
     * @verifies preserve additional licensee
     */
    @Test
    void getAccessPermissionFromMap_shouldPreserveAdditionalLicensee() {
        IpRange additional = new IpRange();
        additional.setName("test range");
        AccessPermission access =
                AbstractLicensee
                        .getAccessPermissionFromMap(Collections.singletonMap("",
                                AccessPermission.granted().setAddionalCheckRequired(additional)));
        Assertions.assertEquals(additional, access.getAddionalCheckRequired());
    }
}