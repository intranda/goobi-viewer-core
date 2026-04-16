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

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

class IdentifierResolverTest extends AbstractDatabaseAndSolrEnabledTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @verifies return 400 if record identifier missing
     */
    @Test
    void doGet_shouldReturn400IfRecordIdentifierMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        IdentifierResolver resolver = new IdentifierResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    }

    /**
     * @verifies return 404 if record not found
     */
    @Test
    void doGet_shouldReturn404IfRecordNotFound() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("urn", "NOTFOUND");
        MockHttpServletResponse response = new MockHttpServletResponse();

        IdentifierResolver resolver = new IdentifierResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
    }

    /**
     * @verifies return 400 if record field name bad
     */
    @Test
    void doGet_shouldReturn400IfRecordFieldNameBad() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("field", "NOSUCHFIELD");
        request.setParameter("identifier", "PPN123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        IdentifierResolver resolver = new IdentifierResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        Assertions.assertEquals("Undefined field name: NOSUCHFIELD", response.getErrorMessage());
    }

    /**
     * @verifies return 400 if record field value bad
     */
    @Test
    void doGet_shouldReturn400IfRecordFieldValueBad() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("field", SolrConstants.PI);
        request.setParameter("identifier", "a:b");
        MockHttpServletResponse response = new MockHttpServletResponse();

        IdentifierResolver resolver = new IdentifierResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        Assertions.assertEquals("Illegal identifier: a:b", response.getErrorMessage());
    }

    /**
     * @verifies return object page URL with PI for a regular work document
     */
    @Test
    void constructUrl_shouldReturnObjectPageURLWithPIForARegularWorkDocument() throws Exception {
        String pi = PI_KLEIUNIV;
        QueryResponse qr = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":" + pi, 0, 1, null, null, null);
        Assertions.assertEquals(1, qr.getResults().size());
        Assertions.assertEquals("/object/" + pi + "/", IdentifierResolver.constructUrl(qr.getResults().get(0), false));
    }

    /**
     * @verifies return toc page URL for an anchor document
     */
    @Test
    void constructUrl_shouldReturnTocPageURLForAnAnchorDocument() throws Exception {
        String pi = "306653648";
        QueryResponse qr = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":" + pi, 0, 1, null, null, null);
        Assertions.assertEquals(1, qr.getResults().size());
        Assertions.assertEquals("/toc/" + pi + "/", IdentifierResolver.constructUrl(qr.getResults().get(0), false));
    }

    /**
     * @verifies return toc page URL for a GROUP doctype document
     */
    @Test
    void constructUrl_shouldReturnTocPageURLForAGROUPDoctypeDocument() throws Exception {
        String pi = "PPN_GROUP";
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.DOCTYPE, DocType.GROUP.toString());
        doc.setField(SolrConstants.PI_TOPSTRUCT, pi);
        Assertions.assertEquals("/toc/" + pi + "/", IdentifierResolver.constructUrl(doc, false));
    }

    /**
     * @verifies return object URL with page number and logId when resolving a page document by URN
     */
    @Test
    void constructUrl_shouldReturnObjectURLWithPageNumberAndLogIdWhenResolvingAPageDocumentByURN() throws Exception {
        String urn = "urn\\:nbn\\:at\\:at-akw\\:g-86493";
        String pi = "AC11442160";
        QueryResponse qr = DataManager.getInstance().getSearchIndex().search(SolrConstants.IMAGEURN + ":" + urn, 0, 1, null, null, null);
        Assertions.assertEquals(1, qr.getResults().size());
        Assertions.assertEquals("/object/" + pi + "/2/LOG_0002/", IdentifierResolver.constructUrl(qr.getResults().get(0), true));
    }

    /**
     * @verifies return toc page URL when docstruct type has a preferred view configured
     */
    @Test
    void constructUrl_shouldReturnTocPageURLWhenDocstructTypeHasAPreferredViewConfigured() throws Exception {
        String pi = "123";
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.DOCSTRCT, "Catalogue");
        doc.setField(SolrConstants.PI_TOPSTRUCT, "123");
        doc.setField(SolrConstants.ISWORK, true);
        Assertions.assertEquals("/toc/" + pi + "/", IdentifierResolver.constructUrl(doc, false));
    }

    /**
     * @verifies return metadata page URL when document has application mime type
     */
    @Test
    void constructUrl_shouldReturnMetadataPageURLWhenDocumentHasApplicationMimeType() throws Exception {
        String pi = "123";
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.DOCSTRCT, "Monograph");
        doc.setField(SolrConstants.PI_TOPSTRUCT, "123");
        doc.setField(SolrConstants.ISWORK, true);
        doc.setField(SolrConstants.MIMETYPE, "application");
        Assertions.assertEquals("/metadata/" + pi + "/", IdentifierResolver.constructUrl(doc, false));
    }

    /**
     * @see IdentifierResolver#parseFieldValueParameters(Map,Map,Map)
     * @verifies parse fields and values correctly
     */
    @Test
    void parseFieldValueParameters_shouldParseFieldsAndValuesCorrectly() throws Exception {
        Map<Integer, String> moreFields = new HashMap<>();
        Map<Integer, String> moreValues = new HashMap<>();
        Map<String, String[]> parameterMap = new HashMap<>(6);
        parameterMap.put("field2", new String[] { "FIELD2" });
        parameterMap.put("field3", new String[] { "FIELD3" });
        parameterMap.put("value2", new String[] { "val2" });
        parameterMap.put("value3", new String[] { "val3" });
        IdentifierResolver.parseFieldValueParameters(parameterMap, moreFields, moreValues);
        Assertions.assertEquals(2, moreFields.size());
        Assertions.assertEquals("FIELD2", moreFields.get(2));
        Assertions.assertEquals("FIELD3", moreFields.get(3));
        Assertions.assertEquals(2, moreValues.size());
        Assertions.assertEquals("val2", moreValues.get(2));
        Assertions.assertEquals("val3", moreValues.get(3));
    }
}
