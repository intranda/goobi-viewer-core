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
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.container.ContainerRequestContext;

class PdfRequestFilterTest extends AbstractTest {

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

    @Test
    void test_shouldGrantAccess() throws IOException, PresentationException, IndexUnreachableException {
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

    @Test
    void test_shouldRefuseAccess() throws IOException, PresentationException, IndexUnreachableException {
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
}
