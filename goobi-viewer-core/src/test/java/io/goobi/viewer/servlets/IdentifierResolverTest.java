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
package io.goobi.viewer.servlets;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.HttpInternalErrorException;
import com.meterware.httpunit.HttpNotFoundException;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.TestUtils;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.ConfigurationTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrConstants.DocType;

public class IdentifierResolverTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final String RESOLVER_NAME = "identifierResolver";

    private ServletRunner sr;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));

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
        WebRequest request = new GetMethodWebRequest(ConfigurationTest.APPLICATION_ROOT_URL + RESOLVER_NAME);
        WebResponse response = sc.getResponse(request);
    }

    /**
     * @see IdentifierResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 404 if record not found
     */
    @Test(expected = HttpNotFoundException.class)
    public void doGet_shouldReturn404IfRecordNotFound() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(ConfigurationTest.APPLICATION_ROOT_URL + RESOLVER_NAME);
        request.setParameter("urn", "NOTFOUND");
        WebResponse response = sc.getResponse(request);
    }

    /**
     * @see IdentifierResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 500 if record field name bad
     */
    @Test(expected = HttpInternalErrorException.class)
    public void doGet_shouldReturn500IfRecordFieldNameBad() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(ConfigurationTest.APPLICATION_ROOT_URL + RESOLVER_NAME);
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
        WebRequest request = new GetMethodWebRequest(ConfigurationTest.APPLICATION_ROOT_URL + RESOLVER_NAME);
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
        String pi = PI_KLEIUNIV;
        QueryResponse qr = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":" + pi, 0, 1, null, null, null);
        Assert.assertEquals(1, qr.getResults().size());
        Assert.assertEquals("/object/" + pi + "/1/LOG_0000/", IdentifierResolver.constructUrl(qr.getResults().get(0), false));
    }

    /**
     * @see IdentifierResolver#constructUrl(String,SolrDocument,String)
     * @verifies construct anchor url correctly
     */
    @Test
    public void constructUrl_shouldConstructAnchorUrlCorrectly() throws Exception {
        String pi = "306653648";
        QueryResponse qr = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":" + pi, 0, 1, null, null, null);
        Assert.assertEquals(1, qr.getResults().size());
        Assert.assertEquals("/toc/" + pi + "/1/LOG_0000/", IdentifierResolver.constructUrl(qr.getResults().get(0), false));
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
        Assert.assertEquals("/toc/" + pi + "/1/-/", IdentifierResolver.constructUrl(doc, false));
    }

    /**
     * @see IdentifierResolver#constructUrl(String,SolrDocument)
     * @verifies construct page url correctly
     */
    @Test
    public void constructUrl_shouldConstructPageUrlCorrectly() throws Exception {
        String urn = "urn\\:nbn\\:at\\:at-akw\\:g-86493";
        String pi = "AC11442160";
        QueryResponse qr = DataManager.getInstance().getSearchIndex().search(SolrConstants.IMAGEURN + ":" + urn, 0, 1, null, null, null);
        Assert.assertEquals(1, qr.getResults().size());
        Assert.assertEquals("/object/" + pi + "/2/-/", IdentifierResolver.constructUrl(qr.getResults().get(0), true));
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
        Assert.assertEquals("/toc/" + pi + "/1/-/", IdentifierResolver.constructUrl(doc, false));
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
        Assert.assertEquals("/metadata/" + pi + "/1/-/", IdentifierResolver.constructUrl(doc, false));
    }

    /**
     * @see IdentifierResolver#parseFieldValueParameters(HttpServletRequest,Map,Map)
     * @verifies parse fields and values correctly
     */
    @Test
    public void parseFieldValueParameters_shouldParseFieldsAndValuesCorrectly() throws Exception {
        Map<Integer, String> moreFields = new HashMap<>();
        Map<Integer, String> moreValues = new HashMap<>();
        Map<String, String[]> parameterMap = new HashMap<>(6);
        parameterMap.put("field2", new String[] { "FIELD2" });
        parameterMap.put("field3", new String[] { "FIELD3" });
        parameterMap.put("value2", new String[] { "val2" });
        parameterMap.put("value3", new String[] { "val3" });
        IdentifierResolver.parseFieldValueParameters(parameterMap, moreFields, moreValues);
        Assert.assertEquals(2, moreFields.size());
        Assert.assertEquals("FIELD2", moreFields.get(2));
        Assert.assertEquals("FIELD3", moreFields.get(3));
        Assert.assertEquals(2, moreValues.size());
        Assert.assertEquals("val2", moreValues.get(2));
        Assert.assertEquals("val3", moreValues.get(3));
    }
}