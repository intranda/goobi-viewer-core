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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PagePermissionsTest {

    // --- EMPTY sentinel ---

    @Test
    void empty_sentinel_isEmpty() {
        assertTrue(PagePermissions.EMPTY.isEmpty());
    }

    @Test
    void empty_deniesImageForAnyOrder() {
        assertFalse(PagePermissions.EMPTY.isImageGranted(1));
        assertFalse(PagePermissions.EMPTY.isImageGranted(9999));
    }

    @Test
    void empty_deniesFulltextForAnyOrder() {
        assertFalse(PagePermissions.EMPTY.isFulltextGranted(1));
    }

    @Test
    void empty_deniesPdfForAnyOrder() {
        assertFalse(PagePermissions.EMPTY.isPdfGranted(1));
    }

    // --- non-empty instance ---

    @Test
    void nonEmpty_isNotEmpty() {
        PagePermissions pp = new PagePermissions(
                Map.of(1, AccessPermission.granted()),
                Map.of(1, AccessPermission.granted()),
                Map.of(1, AccessPermission.granted()));
        assertFalse(pp.isEmpty());
    }

    @Test
    void isImageGranted_returnsTrueForGrantedOrder() {
        PagePermissions pp = new PagePermissions(
                Map.of(5, AccessPermission.granted()),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertTrue(pp.isImageGranted(5));
    }

    @Test
    void isImageGranted_returnsFalseForDeniedOrder() {
        PagePermissions pp = new PagePermissions(
                Map.of(5, AccessPermission.denied()),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertFalse(pp.isImageGranted(5));
    }

    @Test
    void isImageGranted_returnsFalseForUnknownOrder() {
        // Unknown orders must default to denied (fail-safe)
        PagePermissions pp = new PagePermissions(
                Map.of(5, AccessPermission.granted()),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertFalse(pp.isImageGranted(99));
    }

    @Test
    void isFulltextGranted_returnsTrueForGrantedOrder() {
        PagePermissions pp = new PagePermissions(
                Collections.emptyMap(),
                Map.of(3, AccessPermission.granted()),
                Collections.emptyMap());
        assertTrue(pp.isFulltextGranted(3));
    }

    @Test
    void isPdfGranted_returnsFalseForDeniedOrder() {
        PagePermissions pp = new PagePermissions(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of(7, AccessPermission.denied()));
        assertFalse(pp.isPdfGranted(7));
    }

    @Test
    void isPdfGranted_returnsFalseForUnknownOrder() {
        PagePermissions pp = new PagePermissions(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of(7, AccessPermission.granted()));
        assertFalse(pp.isPdfGranted(99));
    }
}
