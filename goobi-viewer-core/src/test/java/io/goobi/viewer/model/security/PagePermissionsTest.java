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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PagePermissionsTest {

    // --- EMPTY sentinel ---

    /**
     * @see PagePermissions#isEmpty()
     * @verifies sentinel is empty
     */
    @Test
    void isEmpty_shouldSentinelIsEmpty() {
        // The EMPTY sentinel must report itself as empty
        assertTrue(PagePermissions.EMPTY.isEmpty());
    }

    /**
     * @verifies deny image for any order when empty
     * @see PagePermissions#isImageGranted(int)
     */
    @Test
    void isImageGranted_shouldDenyImageForAnyOrderWhenEmpty() {
        assertFalse(PagePermissions.EMPTY.isImageGranted(1));
        assertFalse(PagePermissions.EMPTY.isImageGranted(9999));
    }

    /**
     * @verifies deny fulltext for any order when empty
     * @see PagePermissions#isFulltextGranted(int)
     */
    @Test
    void isFulltextGranted_shouldDenyFulltextForAnyOrderWhenEmpty() {
        assertFalse(PagePermissions.EMPTY.isFulltextGranted(1));
    }

    /**
     * @verifies deny pdf for any order when empty
     * @see PagePermissions#isPdfGranted(int)
     */
    @Test
    void isPdfGranted_shouldDenyPdfForAnyOrderWhenEmpty() {
        assertFalse(PagePermissions.EMPTY.isPdfGranted(1));
    }

    // --- non-empty instance ---

    /**
     * @verifies return false when permissions are present
     */
    @Test
    void isEmpty_shouldReturnFalseWhenPermissionsArePresent() {
        PagePermissions pp = new PagePermissions(
                Map.of(1, AccessPermission.granted()),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of(1, AccessPermission.granted()),
                Map.of(1, AccessPermission.granted()));
        assertFalse(pp.isEmpty());
    }

    /**
     * @verifies return true for granted order
     */
    @Test
    void isImageGranted_shouldReturnTrueForGrantedOrder() {
        PagePermissions pp = new PagePermissions(
                Map.of(5, AccessPermission.granted()),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertTrue(pp.isImageGranted(5));
    }

    /**
     * @verifies return false for denied order
     */
    @Test
    void isImageGranted_shouldReturnFalseForDeniedOrder() {
        PagePermissions pp = new PagePermissions(
                Map.of(5, AccessPermission.denied()),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertFalse(pp.isImageGranted(5));
    }

    /**
     * @verifies return false for unknown order
     */
    @Test
    void isImageGranted_shouldReturnFalseForUnknownOrder() {
        // Unknown orders must default to denied (fail-safe)
        PagePermissions pp = new PagePermissions(
                Map.of(5, AccessPermission.granted()),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertFalse(pp.isImageGranted(99));
    }

    /**
     * @verifies return true for granted order
     */
    @Test
    void isFulltextGranted_shouldReturnTrueForGrantedOrder() {
        PagePermissions pp = new PagePermissions(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of(3, AccessPermission.granted()),
                Collections.emptyMap());
        assertTrue(pp.isFulltextGranted(3));
    }

    /**
     * @verifies return false for denied order
     */
    @Test
    void isPdfGranted_shouldReturnFalseForDeniedOrder() {
        PagePermissions pp = new PagePermissions(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of(7, AccessPermission.denied()));
        assertFalse(pp.isPdfGranted(7));
    }

    /**
     * @verifies return false for unknown order
     */
    @Test
    void isPdfGranted_shouldReturnFalseForUnknownOrder() {
        PagePermissions pp = new PagePermissions(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of(7, AccessPermission.granted()));
        assertFalse(pp.isPdfGranted(99));
    }

    // --- new privilege maps: thumbnail / zoom / download ---

    /**
     * @see PagePermissions#isThumbnailGranted(int)
     * @verifies return true for granted order
     */
    @Test
    void isThumbnailGranted_shouldReturnTrueForGrantedOrder() {
        PagePermissions perms = new PagePermissions(
                Collections.emptyMap(),
                Map.of(1, AccessPermission.granted()),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertTrue(perms.isThumbnailGranted(1));
    }

    /**
     * @see PagePermissions#isThumbnailGranted(int)
     * @verifies return false for unknown order
     */
    @Test
    void isThumbnailGranted_shouldReturnFalseForUnknownOrder() {
        assertFalse(emptyPerms().isThumbnailGranted(99));
    }

    /**
     * @see PagePermissions#isZoomGranted(int)
     * @verifies return true for granted order
     */
    @Test
    void isZoomGranted_shouldReturnTrueForGrantedOrder() {
        PagePermissions perms = new PagePermissions(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of(1, AccessPermission.granted()),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertTrue(perms.isZoomGranted(1));
    }

    /**
     * @see PagePermissions#isZoomGranted(int)
     * @verifies return false for unknown order
     */
    @Test
    void isZoomGranted_shouldReturnFalseForUnknownOrder() {
        assertFalse(emptyPerms().isZoomGranted(99));
    }

    /**
     * @see PagePermissions#isDownloadGranted(int)
     * @verifies return true for granted order
     */
    @Test
    void isDownloadGranted_shouldReturnTrueForGrantedOrder() {
        PagePermissions perms = new PagePermissions(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of(1, AccessPermission.granted()),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertTrue(perms.isDownloadGranted(1));
    }

    /**
     * @see PagePermissions#isDownloadGranted(int)
     * @verifies return false for unknown order
     */
    @Test
    void isDownloadGranted_shouldReturnFalseForUnknownOrder() {
        assertFalse(emptyPerms().isDownloadGranted(99));
    }

    // --- raw permission accessors used by PhysicalElement seeding ---

    /**
     * @see PagePermissions#getImagePermission(int)
     * @verifies return permission for known order
     */
    @Test
    void getImagePermission_shouldReturnPermissionForKnownOrder() {
        AccessPermission granted = AccessPermission.granted();
        PagePermissions perms = new PagePermissions(
                Map.of(1, granted),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertEquals(granted, perms.getImagePermission(1));
    }

    /**
     * @see PagePermissions#getImagePermission(int)
     * @verifies return null for unknown order
     */
    @Test
    void getImagePermission_shouldReturnNullForUnknownOrder() {
        assertNull(emptyPerms().getImagePermission(99));
    }

    /**
     * @see PagePermissions#getThumbnailPermission(int)
     * @verifies return null for unknown order
     */
    @Test
    void getThumbnailPermission_shouldReturnNullForUnknownOrder() {
        assertNull(emptyPerms().getThumbnailPermission(99));
    }

    /**
     * @see PagePermissions#getZoomPermission(int)
     * @verifies return null for unknown order
     */
    @Test
    void getZoomPermission_shouldReturnNullForUnknownOrder() {
        assertNull(emptyPerms().getZoomPermission(99));
    }

    /**
     * @see PagePermissions#getDownloadPermission(int)
     * @verifies return null for unknown order
     */
    @Test
    void getDownloadPermission_shouldReturnNullForUnknownOrder() {
        assertNull(emptyPerms().getDownloadPermission(99));
    }

    /**
     * @see PagePermissions#getPdfPermission(int)
     * @verifies return null for unknown order
     */
    @Test
    void getPdfPermission_shouldReturnNullForUnknownOrder() {
        assertNull(emptyPerms().getPdfPermission(99));
    }

    private static PagePermissions emptyPerms() {
        return new PagePermissions(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }
}
