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

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.HttpNotFoundException;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;

public class IdentifierResolverTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final String RESOLVER_NAME = "identifierResolver";

    private ServletRunner sr;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));

        sr = new ServletRunner();
        sr.registerServlet(RESOLVER_NAME, IdentifierResolver.class.getName());
    }

    /**
     * @see IdentifierResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 400 if record identifier missing
     */
    @Test(expected = HttpException.class)
    public void doGet_shouldReturn400IfRecordIdentifierMissing() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest("http://test.intranda.com/" + RESOLVER_NAME);
        WebResponse response = sc.getResponse(request);
    }

    /**
     * @see IdentifierResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 404 if record not found
     */
    @Test(expected = HttpNotFoundException.class)
    public void doGet_shouldReturn404IfRecordNotFound() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest("http://test.intranda.com/" + RESOLVER_NAME);
        request.setParameter("urn", "NOTFOUND");
        WebResponse response = sc.getResponse(request);
    }

    /**
     * @see IdentifierResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 500 if record field name bad
     */
    @Test(expected = SolrException.class)
    public void doGet_shouldReturn500IfRecordFieldNameBad() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest("http://test.intranda.com/" + RESOLVER_NAME);
        request.setParameter("field", "NOSUCHFIELD");
        request.setParameter("identifier", "PPN123");
        WebResponse response = sc.getResponse(request);
    }

    /**
     * @see IdentifierResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 500 if record field value bad
     */
    @Test(expected = HttpException.class)
    public void doGet_shouldReturn500IfRecordFieldValueBad() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest("http://test.intranda.com/" + RESOLVER_NAME);
        request.setParameter("field", SolrConstants.PI);
        request.setParameter("identifier", "a:b");
        WebResponse response = sc.getResponse(request);
    }

    /**
     * @see IdentifierResolver#constructUrl(String,SolrDocument,String)
     * @verifies construct url correctly
     */
    @Test
    public void constructUrl_shouldConstructUrlCorrectly() throws Exception {
        String pi = "PPN517154005";
        QueryResponse qr = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":" + pi, 0, 1, null, null, null);
        Assert.assertEquals(1, qr.getResults().size());
        Assert.assertEquals("/viewImage_value/" + pi + "/1/LOG_0000/", IdentifierResolver.constructUrl(qr.getResults().get(0), false, true));
    }

    /**
     * @see IdentifierResolver#constructUrl(String,SolrDocument,String)
     * @verifies construct anchor url correctly
     */
    @Test
    public void constructUrl_shouldConstructAnchorUrlCorrectly() throws Exception {
        String pi = "10089470";
        QueryResponse qr = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":" + pi, 0, 1, null, null, null);
        Assert.assertEquals(1, qr.getResults().size());
        Assert.assertEquals("/toc/" + pi + "/1/LOG_0000/", IdentifierResolver.constructUrl(qr.getResults().get(0), false, true));
    }

    /**
     * @see IdentifierResolver#constructUrl(SolrDocument,boolean,boolean)
     * @verifies construct group url correctly
     */
    @Test
    public void constructUrl_shouldConstructGroupUrlCorrectly() throws Exception {
        String pi = "PPN_GROUP";
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.DOCTYPE, DocType.GROUP.toString());
        doc.setField(SolrConstants.PI_TOPSTRUCT, pi);
        Assert.assertEquals("/toc/" + pi + "/1/", IdentifierResolver.constructUrl(doc, false, false));
    }

    /**
     * @see IdentifierResolver#constructUrl(String,SolrDocument)
     * @verifies construct page url correctly
     */
    @Test
    public void constructUrl_shouldConstructPageUrlCorrectly() throws Exception {
        String urn = "urn\\:nbn\\:de\\:0111-bbf-spo-14109476";
        String pi = "134997743"; // This record has an overview page that should not override the page URL
        QueryResponse qr = DataManager.getInstance().getSearchIndex().search(SolrConstants.IMAGEURN + ":" + urn, 0, 1, null, null, null);
        Assert.assertEquals(1, qr.getResults().size());
        Assert.assertEquals("/viewImage_value/" + pi + "/2/", IdentifierResolver.constructUrl(qr.getResults().get(0), true, true));
    }

    /**
     * @see IdentifierResolver#constructUrl(String,SolrDocument,boolean,boolean)
     * @verifies construct preferred view url correctly
     */
    @Test
    public void constructUrl_shouldConstructPreferredViewUrlCorrectly() throws Exception {
        String pi = "123";
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.DOCSTRCT, "Catalogue");
        doc.setField(SolrConstants.PI_TOPSTRUCT, "123");
        Assert.assertEquals("/toc/" + pi + "/1/", IdentifierResolver.constructUrl(doc, false, true));
    }

    /**
     * @see IdentifierResolver#constructUrl(String,SolrDocument,boolean,boolean)
     * @verifies construct application mime type url correctly
     */
    @Test
    public void constructUrl_shouldConstructApplicationMimeTypeUrlCorrectly() throws Exception {
        String pi = "123";
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.DOCSTRCT, "Monograph");
        doc.setField(SolrConstants.PI_TOPSTRUCT, "123");
        doc.setField(SolrConstants.MIMETYPE, "application");
        Assert.assertEquals("/metadata/" + pi + "/1/", IdentifierResolver.constructUrl(doc, false, true));
    }
}