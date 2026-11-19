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
package io.goobi.viewer.api.rest.v2.records.media;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.RequiredPrivileges;
import io.goobi.viewer.solr.SolrConstants.DocType;

class RecordsFilesImageResourceTest {

    /**
     * @verifies reject illegal chars
     * @see RecordsFilesImageResource#requireValidPi
     */
    @Test
    void requireValidPi_shouldRejectIllegalChars() {
        // Characters that cause ImageResource to throw ContentLibException (HTTP 500)
        // when building file:// URIs from the PI as folder name
        assertThrows(BadRequestException.class, () -> RecordsFilesImageResource.requireValidPi(" "));
        assertThrows(BadRequestException.class, () -> RecordsFilesImageResource.requireValidPi("|"));
        assertThrows(BadRequestException.class, () -> RecordsFilesImageResource.requireValidPi("<xml>"));
        assertThrows(BadRequestException.class, () -> RecordsFilesImageResource.requireValidPi("%00"));
    }

    /**
     * @verifies require born digital privilege for download resources
     * @see RecordsFilesImageResource#getRequiredPrivileges
     */
    @Test
    void getRequiredPrivileges_shouldRequireBornDigitalPrivilegeForDownloadResources() {
        RequiredPrivileges required = RecordsFilesImageResource.getRequiredPrivileges(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        assertArrayEquals(new String[] { IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES }, required.getPrivileges(DocType.DOWNLOAD_RESOURCE));
    }

    /**
     * @verifies require given privileges for pages
     * @see RecordsFilesImageResource#getRequiredPrivileges
     */
    @Test
    void getRequiredPrivileges_shouldRequireGivenPrivilegesForPages() {
        String[] viewAndZoom = { IPrivilegeHolder.PRIV_VIEW_IMAGES, IPrivilegeHolder.PRIV_ZOOM_IMAGES };
        assertArrayEquals(viewAndZoom, RecordsFilesImageResource.getRequiredPrivileges(viewAndZoom).getPrivileges(DocType.PAGE));
    }

    /**
     * @verifies reject path separators and dot segments
     * @see RecordsFilesImageResource#requireValidFilename
     */
    @Test
    void requireValidFilename_shouldRejectPathSeparatorsAndDotSegments() {
        for (String filename : new String[] { "..U002FPPN_CLOSEDU002F00000001.tif", "..U002F..U002FetcU002Fx.jpg", "U002FetcU002Fx.jpg",
                "..U005Cx.jpg", "../x.jpg", "a/b.jpg", "a\\b.jpg", "..", ".", "http:U002FU002F127.0.0.1U002Fx.jpg",
                "U0025U0032eU0025U0032eU002Fx.jpg", "%2e%2e%2fx.jpg", "", " ", null }) {
            assertThrows(BadRequestException.class, () -> RecordsFilesImageResource.requireValidFilename(filename), String.valueOf(filename));
        }
    }

    /**
     * @verifies accept regular filenames
     * @see RecordsFilesImageResource#requireValidFilename
     */
    @Test
    void requireValidFilename_shouldAcceptRegularFilenames() {
        for (String filename : new String[] { "00000001.tif", "IMG+20200322+144253.jpg", "file name with spaces.jpg", "a..b.jpg",
                "00000001" }) {
            assertEquals(filename, RecordsFilesImageResource.requireValidFilename(filename));
        }
    }
}
