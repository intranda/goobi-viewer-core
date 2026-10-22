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
package io.goobi.viewer.api.rest.filters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;

class FilterToolsTest {

    private static final String[] VIEW_AND_ZOOM = { IPrivilegeHolder.PRIV_VIEW_IMAGES, IPrivilegeHolder.PRIV_ZOOM_IMAGES };

    /**
     * @see FilterTools#isGrantedForAllPrivileges(String[],FilterTools.PrivilegeCheck)
     * @verifies deny if only last privilege is granted
     */
    @Test
    void isGrantedForAllPrivileges_shouldDenyIfOnlyLastPrivilegeIsGranted() throws Exception {
        assertFalse(FilterTools.isGrantedForAllPrivileges(VIEW_AND_ZOOM,
                p -> IPrivilegeHolder.PRIV_ZOOM_IMAGES.equals(p) ? AccessPermission.granted() : AccessPermission.denied()));
    }

    /**
     * @see FilterTools#isGrantedForAllPrivileges(String[],FilterTools.PrivilegeCheck)
     * @verifies deny if only first privilege is granted
     */
    @Test
    void isGrantedForAllPrivileges_shouldDenyIfOnlyFirstPrivilegeIsGranted() throws Exception {
        assertFalse(FilterTools.isGrantedForAllPrivileges(VIEW_AND_ZOOM,
                p -> IPrivilegeHolder.PRIV_VIEW_IMAGES.equals(p) ? AccessPermission.granted() : AccessPermission.denied()));
    }

    /**
     * @see FilterTools#isGrantedForAllPrivileges(String[],FilterTools.PrivilegeCheck)
     * @verifies grant if all privileges are granted
     */
    @Test
    void isGrantedForAllPrivileges_shouldGrantIfAllPrivilegesAreGranted() throws Exception {
        assertTrue(FilterTools.isGrantedForAllPrivileges(VIEW_AND_ZOOM, p -> AccessPermission.granted()));
    }

    /**
     * @see FilterTools#isGrantedForAllPrivileges(String[],FilterTools.PrivilegeCheck)
     * @verifies deny if no privileges given
     */
    @Test
    void isGrantedForAllPrivileges_shouldDenyIfNoPrivilegesGiven() throws Exception {
        assertFalse(FilterTools.isGrantedForAllPrivileges(new String[0], p -> AccessPermission.granted()));
    }
}
