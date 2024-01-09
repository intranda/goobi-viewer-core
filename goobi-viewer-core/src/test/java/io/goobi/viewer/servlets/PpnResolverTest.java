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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.HttpNotFoundException;
import com.meterware.httpunit.WebRequest;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;

class PpnResolverTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final String RESOLVER_NAME = "ppnResolver";

    private ServletRunner sr;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        sr = new ServletRunner();
        sr.registerServlet(RESOLVER_NAME, PpnResolver.class.getName());
    }

    /**
     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 400 if record identifier missing
     */
    @Test
    void doGet_shouldReturn400IfRecordIdentifierMissing() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest("http://test.intranda.com/" + RESOLVER_NAME);
        Assertions.assertThrows(HttpException.class, () -> sc.getResponse(request));
    }

    /**
     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 404 if record not found
     */
    @Test
    void doGet_shouldReturn404IfRecordNotFound() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest("http://test.intranda.com/" + RESOLVER_NAME);
        request.setParameter("id", "NOTFOUND");
        Assertions.assertThrows(HttpNotFoundException.class, () -> sc.getResponse(request));
    }

    /**
     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 500 if record identifier bad
     */
    @Test
    void doGet_shouldReturn500IfRecordIdentifierBad() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest("http://test.intranda.com/" + RESOLVER_NAME);
        request.setParameter("id", "a:b");
        Assertions.assertThrows(HttpException.class, () -> sc.getResponse(request));
    }

    //    /**
    //     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
    //     * @verifies forward to relative url
    //     */
    //    @Test
    //    public void doGet_shouldForwardToRelativeUrl() throws Exception {
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
    //    public void doGet_shouldRedirectToFullUrl() throws Exception {
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
