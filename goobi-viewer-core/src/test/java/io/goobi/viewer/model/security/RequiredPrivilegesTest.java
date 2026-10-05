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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.solr.SolrConstants.DocType;

class RequiredPrivilegesTest {

    /**
     * @see RequiredPrivileges#getPrivileges(DocType)
     * @verifies return default privileges for unmapped doc type
     */
    @Test
    void getPrivileges_shouldReturnDefaultPrivilegesForUnmappedDocType() {
        RequiredPrivileges required = RequiredPrivileges.of(IPrivilegeHolder.PRIV_VIEW_IMAGES, IPrivilegeHolder.PRIV_ZOOM_IMAGES)
                .withDocType(DocType.DOWNLOAD_RESOURCE, IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES);
        assertArrayEquals(new String[] { IPrivilegeHolder.PRIV_VIEW_IMAGES, IPrivilegeHolder.PRIV_ZOOM_IMAGES },
                required.getPrivileges(DocType.PAGE));
        assertArrayEquals(new String[] { IPrivilegeHolder.PRIV_VIEW_IMAGES, IPrivilegeHolder.PRIV_ZOOM_IMAGES }, required.getPrivileges(null));
    }

    /**
     * @see RequiredPrivileges#getPrivileges(DocType)
     * @verifies return mapped privileges for mapped doc type
     */
    @Test
    void getPrivileges_shouldReturnMappedPrivilegesForMappedDocType() {
        RequiredPrivileges required = RequiredPrivileges.of(IPrivilegeHolder.PRIV_VIEW_IMAGES)
                .withDocType(DocType.DOWNLOAD_RESOURCE, IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES);
        assertArrayEquals(new String[] { IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES }, required.getPrivileges(DocType.DOWNLOAD_RESOURCE));
    }

    /**
     * @see RequiredPrivileges#withDocType(DocType,String[])
     * @verifies not modify original instance
     */
    @Test
    void withDocType_shouldNotModifyOriginalInstance() {
        RequiredPrivileges original = RequiredPrivileges.of(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        original.withDocType(DocType.DOWNLOAD_RESOURCE, IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES);
        assertArrayEquals(new String[] { IPrivilegeHolder.PRIV_VIEW_IMAGES }, original.getPrivileges(DocType.DOWNLOAD_RESOURCE));
    }

    /**
     * @see RequiredPrivileges#getCacheKey()
     * @verifies equal privilege name for single privilege without mapping
     */
    @Test
    void getCacheKey_shouldEqualPrivilegeNameForSinglePrivilegeWithoutMapping() {
        // Keeps session attribute names identical to the ones written by the single-privilege API
        assertEquals(IPrivilegeHolder.PRIV_VIEW_IMAGES, RequiredPrivileges.of(IPrivilegeHolder.PRIV_VIEW_IMAGES).getCacheKey());
    }

    /**
     * @see RequiredPrivileges#getCacheKey()
     * @verifies differ for different mappings
     */
    @Test
    void getCacheKey_shouldDifferForDifferentMappings() {
        RequiredPrivileges plain = RequiredPrivileges.of(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        RequiredPrivileges zoom = RequiredPrivileges.of(IPrivilegeHolder.PRIV_VIEW_IMAGES, IPrivilegeHolder.PRIV_ZOOM_IMAGES);
        RequiredPrivileges mapped =
                RequiredPrivileges.of(IPrivilegeHolder.PRIV_VIEW_IMAGES)
                        .withDocType(DocType.DOWNLOAD_RESOURCE, IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES);
        assertNotEquals(plain.getCacheKey(), zoom.getCacheKey());
        assertNotEquals(plain.getCacheKey(), mapped.getCacheKey());
        assertNotEquals(zoom.getCacheKey(), mapped.getCacheKey());
    }

    /**
     * @see RequiredPrivileges#contains(String)
     * @verifies find privilege in defaults and mappings
     */
    @Test
    void contains_shouldFindPrivilegeInDefaultsAndMappings() {
        RequiredPrivileges required = RequiredPrivileges.of(IPrivilegeHolder.PRIV_VIEW_IMAGES)
                .withDocType(DocType.DOWNLOAD_RESOURCE, IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES);
        assertTrue(required.contains(IPrivilegeHolder.PRIV_VIEW_IMAGES));
        assertTrue(required.contains(IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES));
        assertFalse(required.contains(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS));
    }
}
