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
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.TestUtils;

class MetsResolverTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final String RESOLVER_NAME = "mets";

    private ServletRunner sr;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        sr = new ServletRunner();
        sr.registerServlet(RESOLVER_NAME, MetsResolver.class.getName());
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return METS file correctly via pi
     */
    @Test
    void doGet_shouldReturnMETSFileCorrectlyViaPi() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(TestUtils.APPLICATION_ROOT_URL + RESOLVER_NAME);
        request.setParameter("id", PI_KLEIUNIV);
        WebResponse response = sc.getResponse(request);
        Assertions.assertNotNull(response);
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return METS file correctly via urn
     */
    @Test
    void doGet_shouldReturnMETSFileCorrectlyViaUrn() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(TestUtils.APPLICATION_ROOT_URL + RESOLVER_NAME);
        request.setParameter("urn", "urn:nbn:de:gbv:9-g-4882158");
        WebResponse response = sc.getResponse(request);
        Assertions.assertNotNull(response);
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return LIDO file correctly
     */
    @Test
    void doGet_shouldReturnLIDOFileCorrectly() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(TestUtils.APPLICATION_ROOT_URL + RESOLVER_NAME);
        request.setParameter("id", "455820");
        WebResponse response = sc.getResponse(request);
        Assertions.assertNotNull(response);
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 404 if file not found
     */
    @Test
    void doGet_shouldReturn404IfFileNotFound() throws Exception {
        ServletUnitClient sc = sr.newClient();
        {
            WebRequest request = new GetMethodWebRequest(TestUtils.APPLICATION_ROOT_URL + RESOLVER_NAME);
            request.setParameter("id", "NOTFOUND");
            Assertions.assertThrows(HttpNotFoundException.class, () -> sc.getResponse(request));
        }
        {
            WebRequest request = new GetMethodWebRequest(TestUtils.APPLICATION_ROOT_URL + RESOLVER_NAME);
            request.setParameter("urn", "NOTFOUND");
            Assertions.assertThrows(HttpNotFoundException.class, () -> sc.getResponse(request));
        }
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 409 if more than one record matched
     */
    @Test
    void doGet_shouldReturn409IfMoreThanOneRecordMatched() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(TestUtils.APPLICATION_ROOT_URL + RESOLVER_NAME);
        request.setParameter("urn", "test:1234:goobi:3431");
        Assertions.assertThrows(HttpException.class, () -> sc.getResponse(request));
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 500 if record identifier bad
     */
    @Test
    void doGet_shouldReturn500IfRecordIdentifierBad() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(TestUtils.APPLICATION_ROOT_URL + RESOLVER_NAME);
        request.setParameter("id", "a:b");
        Assertions.assertThrows(HttpException.class, () -> sc.getResponse(request));
    }
}
