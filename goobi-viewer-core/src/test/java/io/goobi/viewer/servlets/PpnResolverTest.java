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
package io.goobi.viewer.servlets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;

class PpnResolverTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 400 if record identifier missing
     */
    @Test
    void doGet_shouldReturn400IfRecordIdentifierMissing() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        PpnResolver resolver = new PpnResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        Assertions.assertEquals(PpnResolver.ERRTXT_NO_ARGUMENT + PpnResolver.REQUEST_PARAM_NAME, response.getErrorMessage());
    }

    /**
     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 404 if record not found
     */
    @Test
    void doGet_shouldReturn404IfRecordNotFound() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("id", "NOTFOUND");
        MockHttpServletResponse response = new MockHttpServletResponse();

        PpnResolver resolver = new PpnResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        Assertions.assertEquals(PpnResolver.ERRTXT_DOC_NOT_FOUND, response.getErrorMessage());
    }

    /**
     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 400 if record identifier bad
     */
    @Test
    void doGet_shouldReturn400IfRecordIdentifierBad() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("id", "a:b");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MetsResolver resolver = new MetsResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        Assertions.assertEquals(MetsResolver.ERRTXT_ILLEGAL_IDENTIFIER + ": a:b", response.getErrorMessage());

    }

    //    /**
    //     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
    //     * @verifies forward to relative url
    //     */
    //    @Test
    //    void doGet_shouldForwardToRelativeUrl() throws Exception {
    //        PpnResolver resolver = new PpnResolver();
    //        String page = "/object/PPN517154005/1/LOG_0000/";
    //        HttpServletRequest request = TestUtils.mockHttpRequest(page);
    //        HttpServletResponse response = TestUtils.mockHttpResponse();
    //        request.setAttribute("id", PI_KLEIUNIV);
    //        Assertions.assertEquals(PI_KLEIUNIV, request.getAttribute("id"));
    //        resolver.service(request, response);
    //        Assertions.assertNotNull(request.getRequestDispatcher(page));
    //        request.getRequestDispatcher(page).forward(request, response);
    //    }
    //
    //    /**
    //     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
    //     * @verifies redirect to full url
    //     */
    //    @Test
    //    void doGet_shouldRedirectToFullUrl() throws Exception {
    //        DataManager.getInstance().getConfiguration().overrideValue("collections.redirectToWork", true);
    //        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isAllowRedirectCollectionToWork());
    //
    //        ServletUnitClient sc = sr.newClient();
    //        WebRequest request = new GetMethodWebRequest("http://test.intranda.com/" + RESOLVER_NAME);
    //        request.setParameter("id", PI_KLEIUNIV);
    //        WebResponse response = sc.getResponse(request);
    //        Assertions.assertEquals(200, response.getResponseCode());
    //    }
}
