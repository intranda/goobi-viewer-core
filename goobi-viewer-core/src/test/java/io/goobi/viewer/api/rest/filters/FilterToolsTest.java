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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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

    /**
     * @see FilterTools#splitFileNames(String)
     * @verifies return single file name
     */
    @Test
    void splitFileNames_shouldReturnSingleFileName() {
        assertEquals(List.of("00000001.tif"), FilterTools.splitFileNames("00000001.tif"));
    }

    /**
     * @see FilterTools#splitFileNames(String)
     * @verifies split multiple file names
     */
    @Test
    void splitFileNames_shouldSplitMultipleFileNames() {
        assertEquals(List.of("00000001.tif", "00000002.tif", "00000003.tif"), FilterTools.splitFileNames("00000001.tif$00000002.tif$00000003.tif"));
    }

    /**
     * @see FilterTools#splitFileNames(String)
     * @verifies skip blank file names
     */
    @Test
    void splitFileNames_shouldSkipBlankFileNames() {
        assertEquals(List.of("00000001.tif", "00000002.tif"), FilterTools.splitFileNames("$00000001.tif$$ $00000002.tif$"));
    }

    /**
     * @see FilterTools#splitFileNames(String)
     * @verifies return empty list if blank
     */
    @Test
    void splitFileNames_shouldReturnEmptyListIfBlank() {
        assertTrue(FilterTools.splitFileNames(null).isEmpty());
        assertTrue(FilterTools.splitFileNames(" ").isEmpty());
    }

    /**
     * @see FilterTools#isGrantedForAllFiles(List,FilterTools.FileCheck)
     * @verifies grant if all files are granted
     */
    @Test
    void isGrantedForAllFiles_shouldGrantIfAllFilesAreGranted() throws Exception {
        assertTrue(FilterTools.isGrantedForAllFiles(List.of("00000001.tif", "00000002.tif"), f -> AccessPermission.granted()));
    }

    /**
     * @see FilterTools#isGrantedForAllFiles(List,FilterTools.FileCheck)
     * @verifies deny if any file is denied
     */
    @Test
    void isGrantedForAllFiles_shouldDenyIfAnyFileIsDenied() throws Exception {
        List<String> fileNames = List.of("00000001.tif", "00000002.tif", "00000003.tif");
        assertFalse(FilterTools.isGrantedForAllFiles(fileNames,
                f -> "00000002.tif".equals(f) ? AccessPermission.denied() : AccessPermission.granted()));
        assertFalse(FilterTools.isGrantedForAllFiles(fileNames,
                f -> "00000003.tif".equals(f) ? AccessPermission.denied() : AccessPermission.granted()));
    }

    /**
     * @see FilterTools#isGrantedForAllFiles(List,FilterTools.FileCheck)
     * @verifies deny if no files given
     */
    @Test
    void isGrantedForAllFiles_shouldDenyIfNoFilesGiven() throws Exception {
        assertFalse(FilterTools.isGrantedForAllFiles(List.of(), f -> AccessPermission.granted()));
        assertFalse(FilterTools.isGrantedForAllFiles(null, f -> AccessPermission.granted()));
    }
}
