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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.container.ContainerRequestContext;

class PdfRequestFilterTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see PdfRequestFilter#getNumAllowedPages(int,int)
     * @verifies return 0 if percentage 0
     */
    @Test
    void getNumAllowedPages_shouldReturn0IfPercentage0() throws Exception {
        Assertions.assertEquals(0, PdfRequestFilter.getNumAllowedPages(0, 10));
    }

    /**
     * @see PdfRequestFilter#getNumAllowedPages(int,int)
     * @verifies return 0 if number of pages 0
     */
    @Test
    void getNumAllowedPages_shouldReturn0IfNumberOfPages0() throws Exception {
        Assertions.assertEquals(0, PdfRequestFilter.getNumAllowedPages(50, 0));
    }

    /**
     * @see PdfRequestFilter#getNumAllowedPages(int,int)
     * @verifies return number of pages if percentage 100
     */
    @Test
    void getNumAllowedPages_shouldReturnNumberOfPagesIfPercentage100() throws Exception {
        Assertions.assertEquals(10, PdfRequestFilter.getNumAllowedPages(100, 10));
    }

    /**
     * @see PdfRequestFilter#getNumAllowedPages(int,int)
     * @verifies calculate number correctly
     */
    @Test
    void getNumAllowedPages_shouldCalculateNumberCorrectly() throws Exception {
        Assertions.assertEquals(35, PdfRequestFilter.getNumAllowedPages(35, 100));
        Assertions.assertEquals(3, PdfRequestFilter.getNumAllowedPages(35, 10));
        Assertions.assertEquals(1, PdfRequestFilter.getNumAllowedPages(19, 10));
        Assertions.assertEquals(0, PdfRequestFilter.getNumAllowedPages(9, 10));
    }

    /**
     * @verifies grant access
     */
    @Test
    void filter_shouldGrantAccess() throws IOException, PresentationException, IndexUnreachableException {
        String pi = "15929110";

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttributeNames()).thenReturn(Collections.enumeration(List.of("pi")));
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getSession()).thenReturn(session);
        Mockito.when(request.getAttribute("pi"))
                .thenReturn(pi);
        ContainerRequestContext context = Mockito.spy(ContainerRequestContext.class);

        PdfRequestFilter filter = new PdfRequestFilter(request);
        filter.filter(context);

        Mockito.verify(context, Mockito.never()).abortWith(Mockito.any());
    }

    /**
     * @verifies refuse access
     */
    @Test
    void filter_shouldRefuseAccess() throws IOException, PresentationException, IndexUnreachableException {
        String pi = "PPNsas1_2_194";

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttributeNames()).thenReturn(Collections.enumeration(List.of("pi")));
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getSession()).thenReturn(session);
        Mockito.when(request.getAttribute("pi"))
                .thenReturn(pi);
        ContainerRequestContext context = Mockito.spy(ContainerRequestContext.class);

        PdfRequestFilter filter = new PdfRequestFilter(request);
        filter.filter(context);

        Mockito.verify(context).abortWith(Mockito.any());
    }

    /**
     * @see PdfRequestFilter#checkPageAllowed(String, String, int, int, HttpServletRequest)
     * @verifies return false if session unavailable
     */
    @Test
    void checkPageAllowed_shouldReturnFalseIfSessionUnavailable() throws Exception {
        // Request with null session should return false
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getSession()).thenReturn(null);

        Assertions.assertFalse(PdfRequestFilter.checkPageAllowed("PPN123", "00000001.tif", 50, 10, request));

        // Null request should also return false
        Assertions.assertFalse(PdfRequestFilter.checkPageAllowed("PPN123", "00000001.tif", 50, 10, null));
    }

    /**
     * @see PdfRequestFilter#checkPageAllowed(String, String, int, int, HttpServletRequest)
     * @verifies return false if no session attribute exists yet
     */
    @Test
    void checkPageAllowed_shouldReturnFalseIfNoSessionAttributeExistsYet() throws Exception {
        // Session exists but has no pdf_quota attribute yet; the method initializes it and then
        // checks whether the quota allows the requested page. With a 0% quota (0 pages allowed),
        // even the first page request must be rejected.
        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttribute("pdf_quota")).thenReturn(null);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getSession()).thenReturn(session);

        // 0% quota means 0 allowed pages, so even with empty quota map the page is rejected
        Assertions.assertFalse(PdfRequestFilter.checkPageAllowed("PPN123", "00000001.tif", 0, 10, request));
    }

    /**
     * @see PdfRequestFilter#checkPageAllowed(String, String, int, int, HttpServletRequest)
     * @verifies return true if page already part of quota
     */
    @Test
    void checkPageAllowed_shouldReturnTrueIfPageAlreadyPartOfQuota() throws Exception {
        // Pre-populate the quota map with the requested page so the method recognizes it
        Map<String, Set<String>> quotaMap = new HashMap<>();
        Set<String> pages = new HashSet<>();
        pages.add("00000001.tif");
        quotaMap.put("PPN123", pages);

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttribute("pdf_quota")).thenReturn(quotaMap);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getSession()).thenReturn(session);

        // Page is already in the quota set, so it should be allowed regardless of percentage
        Assertions.assertTrue(PdfRequestFilter.checkPageAllowed("PPN123", "00000001.tif", 10, 10, request));
    }

    /**
     * @see PdfRequestFilter#checkPageAllowed(String, String, int, int, HttpServletRequest)
     * @verifies return false if quota already filled
     */
    @Test
    void checkPageAllowed_shouldReturnFalseIfQuotaAlreadyFilled() throws Exception {
        // Pre-populate the quota with the maximum allowed pages (1 page at 10% of 10 total)
        Map<String, Set<String>> quotaMap = new HashMap<>();
        Set<String> pages = new HashSet<>();
        pages.add("00000001.tif");
        quotaMap.put("PPN123", pages);

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttribute("pdf_quota")).thenReturn(quotaMap);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getSession()).thenReturn(session);

        // 10% of 10 = 1 allowed page, and 1 page is already in the set, so a new page must be rejected
        Assertions.assertFalse(PdfRequestFilter.checkPageAllowed("PPN123", "00000002.tif", 10, 10, request));
    }

    /**
     * @see PdfRequestFilter#checkPageAllowed(String, String, int, int, HttpServletRequest)
     * @verifies return true and add page to map if quota not yet filled
     */
    @Test
    void checkPageAllowed_shouldReturnTrueAndAddPageToMapIfQuotaNotYetFilled() throws Exception {
        // Start with an empty quota map so the method can add the first page
        Map<String, Set<String>> quotaMap = new HashMap<>();

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttribute("pdf_quota")).thenReturn(quotaMap);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getSession()).thenReturn(session);

        // 50% of 10 = 5 allowed pages; quota is empty, so the page should be accepted and added
        Assertions.assertTrue(PdfRequestFilter.checkPageAllowed("PPN123", "00000001.tif", 50, 10, request));
        // Verify the page was actually added to the quota map
        Assertions.assertNotNull(quotaMap.get("PPN123"));
        Assertions.assertTrue(quotaMap.get("PPN123").contains("00000001.tif"));
    }
}
