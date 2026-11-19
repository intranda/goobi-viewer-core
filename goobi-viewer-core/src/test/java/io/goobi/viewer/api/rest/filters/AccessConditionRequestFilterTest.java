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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.RequiredPrivileges;
import io.goobi.viewer.solr.SolrConstants.DocType;
import jakarta.servlet.http.HttpServletRequest;

class AccessConditionRequestFilterTest extends AbstractTest {

    private static final String PI = "PPN123";

    private static HttpServletRequest requestWithPrivilege(Object privilege) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(AccessConditionRequestFilter.REQUIRED_PRIVILEGE)).thenReturn(privilege);
        return request;
    }

    /**
     * @see AccessConditionRequestFilter#getRequiredPrivilegeSet(HttpServletRequest)
     * @verifies return attribute if it is a required privileges instance
     */
    @Test
    void getRequiredPrivilegeSet_shouldReturnAttributeIfItIsARequiredPrivilegesInstance() {
        RequiredPrivileges required = RequiredPrivileges.of(IPrivilegeHolder.PRIV_VIEW_IMAGES)
                .withDocType(DocType.DOWNLOAD_RESOURCE, IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES);
        assertSame(required, AccessConditionRequestFilter.getRequiredPrivilegeSet(requestWithPrivilege(required)));
    }

    /**
     * @see AccessConditionRequestFilter#getRequiredPrivilegeSet(HttpServletRequest)
     * @verifies wrap plain privilege attributes
     */
    @Test
    void getRequiredPrivilegeSet_shouldWrapPlainPrivilegeAttributes() {
        assertArrayEquals(new String[] { IPrivilegeHolder.PRIV_VIEW_IMAGES },
                AccessConditionRequestFilter.getRequiredPrivilegeSet(requestWithPrivilege(IPrivilegeHolder.PRIV_VIEW_IMAGES))
                        .getPrivileges(DocType.PAGE));
        String[] viewAndZoom = { IPrivilegeHolder.PRIV_VIEW_IMAGES, IPrivilegeHolder.PRIV_ZOOM_IMAGES };
        assertArrayEquals(viewAndZoom,
                AccessConditionRequestFilter.getRequiredPrivilegeSet(requestWithPrivilege(viewAndZoom)).getPrivileges(DocType.PAGE));
    }

    /**
     * @see AccessConditionRequestFilter#getRequiredPrivilegeSet(HttpServletRequest)
     * @verifies fall back to list privilege if attribute missing
     */
    @Test
    void getRequiredPrivilegeSet_shouldFallBackToListPrivilegeIfAttributeMissing() {
        assertArrayEquals(new String[] { IPrivilegeHolder.PRIV_LIST },
                AccessConditionRequestFilter.getRequiredPrivilegeSet(requestWithPrivilege(null)).getPrivileges(DocType.PAGE));
    }

    /**
     * <<<<<<< Upstream, based on branch 'develop' of git@gitea.intranda.com:goobi-viewer/goobi-viewer-core.git
     * 
     * @see AccessConditionRequestFilter#filterForAccessConditions(HttpServletRequest,String,String,String)
     * @verifies grant access to page range if all files granted
     */
    @Test
    void filterForAccessConditions_shouldGrantAccessToPageRangeIfAllFilesGranted() {
        HttpServletRequest request = requestWithPrivilege(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        try (MockedStatic<AccessConditionUtils> utils = mockStatic(AccessConditionUtils.class)) {
            utils.when(() -> AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(any(), eq(PI), anyString(),
                    any(RequiredPrivileges.class), any(), any())).thenReturn(AccessPermission.granted());

            assertDoesNotThrow(() -> AccessConditionRequestFilter.filterForAccessConditions(request, PI, null, "00000001.tif$00000002.tif"));
            // Each file of the range is checked individually, not the entire file name list
            utils.verify(() -> AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(any(), eq(PI), eq("00000001.tif"),
                    any(RequiredPrivileges.class), any(), any()));
            utils.verify(() -> AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(any(), eq(PI), eq("00000002.tif"),
                    any(RequiredPrivileges.class), any(), any()));
        }
    }

    /**
     * @see AccessConditionRequestFilter#filterForAccessConditions(HttpServletRequest,String,String,String)
     * @verifies deny access to page range if any file denied
     */
    @Test
    void filterForAccessConditions_shouldDenyAccessToPageRangeIfAnyFileDenied() {
        HttpServletRequest request = requestWithPrivilege(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        try (MockedStatic<AccessConditionUtils> utils = mockStatic(AccessConditionUtils.class)) {
            utils.when(() -> AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(any(), eq(PI), anyString(),
                    any(RequiredPrivileges.class), any(), any())).thenReturn(AccessPermission.granted());
            utils.when(() -> AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(any(), eq(PI), eq("00000002.tif"),
                    any(RequiredPrivileges.class), any(), any())).thenReturn(AccessPermission.denied());

            assertThrows(ServiceNotAllowedException.class,
                    () -> AccessConditionRequestFilter.filterForAccessConditions(request, PI, null, "00000001.tif$00000002.tif$00000003.tif"));
            assertThrows(ServiceNotAllowedException.class,
                    () -> AccessConditionRequestFilter.filterForAccessConditions(request, PI, null, "00000002.tif"));
        }
    }

    /**
     * @see AccessConditionRequestFilter#filterForAccessConditions(HttpServletRequest, String, String, String)
     * @verifies deny thumbnail request without pi
     */
    @Test
    void filterForAccessConditions_shouldDenyThumbnailRequestWithoutPi() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);
        request.setAttribute("iiif-region", "full");
        request.setAttribute("iiif-size", "1,");
        assertTrue(FilterTools.isThumbnail(request));
        assertThrows(ServiceNotAllowedException.class,
                () -> AccessConditionRequestFilter.filterForAccessConditions(request, null, null, "file:///opt/digiverso/viewer/data/foo.tif"));
    }
}
