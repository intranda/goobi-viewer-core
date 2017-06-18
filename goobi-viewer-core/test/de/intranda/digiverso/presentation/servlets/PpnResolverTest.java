/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.HttpNotFoundException;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

import de.intranda.digiverso.presentation.AbstractSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;

public class PpnResolverTest extends AbstractSolrEnabledTest {

    private static final String RESOLVER_NAME = "ppnResolver";

    private ServletRunner sr;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));

        sr = new ServletRunner();
        sr.registerServlet(RESOLVER_NAME, PpnResolver.class.getName());
    }

    /**
     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 400 if record identifier missing
     */
    @Test(expected = HttpException.class)
    public void doGet_shouldReturn400IfRecordIdentifierMissing() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest("http://test.intranda.com/" + RESOLVER_NAME);
        WebResponse response = sc.getResponse(request);
    }

    /**
     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 404 if record not found
     */
    @Test(expected = HttpNotFoundException.class)
    public void doGet_shouldReturn404IfRecordNotFound() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest("http://test.intranda.com/" + RESOLVER_NAME);
        request.setParameter("id", "NOTFOUND");
        WebResponse response = sc.getResponse(request);
    }

    /**
     * @see PpnResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 500 if record identifier bad
     */
    @Test(expected = HttpException.class)
    public void doGet_shouldReturn500IfRecordIdentifierBad() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest("http://test.intranda.com/" + RESOLVER_NAME);
        request.setParameter("id", "a:b");
        WebResponse response = sc.getResponse(request);
    }
}