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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.security.AccessPermission;

public class AbstractLicenseeTest {

    /**
     * @see AbstractLicensee#getAccessPermissionFromMap(Map)
     * @verifies return denied if permissionMap empty
     */
    @Test
    public void getAccessPermissionFromMap_shouldReturnDeniedIfPermissionMapEmpty() throws Exception {
        Assertions.assertFalse(AbstractLicensee.getAccessPermissionFromMap(Collections.emptyMap()).isGranted());
    }

    /**
     * @see AbstractLicensee#getAccessPermissionFromMap(Map)
     * @verifies return denied if all permissions in map denied
     */
    @Test
    public void getAccessPermissionFromMap_shouldReturnDeniedIfAllPermissionsInMapDenied() throws Exception {
        Assertions.assertFalse(AbstractLicensee.getAccessPermissionFromMap(Collections.singletonMap("", AccessPermission.denied())).isGranted());
    }

    /**
     * @see AbstractLicensee#getAccessPermissionFromMap(Map)
     * @verifies preserve ticketRequired
     */
    @Test
    public void getAccessPermissionFromMap_shouldPreserveTicketRequired() throws Exception {
        Assertions.assertTrue(
                AbstractLicensee.getAccessPermissionFromMap(Collections.singletonMap("", AccessPermission.granted().setTicketRequired(true)))
                        .isTicketRequired());
    }

    /**
     * @see AbstractLicensee#getAccessPermissionFromMap(Map)
     * @verifies preserve redirect metadata
     */
    @Test
    public void getAccessPermissionFromMap_shouldPreserveRedirectMetadata() throws Exception {
        AccessPermission access =
                AbstractLicensee
                        .getAccessPermissionFromMap(Collections.singletonMap("",
                                AccessPermission.granted().setRedirect(true).setRedirectUrl("https://example.com")));
        Assertions.assertTrue(access.isRedirect());
        Assertions.assertEquals("https://example.com", access.getRedirectUrl());
    }
}