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
     * Test that constructUrl produces a TOC URL for an anchor document (ISANCHOR = true).
     *
     * @see IdentifierResolver#constructUrl(SolrDocument, boolean, int)
     * @verifies construct anchor url correctly
     */
    @Test
    void constructUrl_shouldConstructAnchorUrlCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.PI_TOPSTRUCT, "PPN_ANCHOR");
        doc.setField(SolrConstants.ISANCHOR, true);
        doc.setField(SolrConstants.ISWORK, false);
        // Anchor documents should resolve to the TOC page
        String url = IdentifierResolver.constructUrl(doc, false, 1);
        Assertions.assertTrue(url.contains("/toc/"), "Anchor URL should contain /toc/");
        Assertions.assertTrue(url.contains("PPN_ANCHOR"), "Anchor URL should contain PI");
    }

    /**
     * Test that constructUrl produces a TOC URL for a GROUP doctype document.
     *
     * @see IdentifierResolver#constructUrl(SolrDocument, boolean, int)
     * @verifies construct group url correctly
     */
    @Test
    void constructUrl_shouldConstructGroupUrlCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.PI_TOPSTRUCT, "PPN_GROUP");
        doc.setField(SolrConstants.DOCTYPE, DocType.GROUP.toString());
        doc.setField(SolrConstants.ISWORK, false);
        // Group documents should resolve to the TOC page
        String url = IdentifierResolver.constructUrl(doc, false, 1);
        Assertions.assertTrue(url.contains("/toc/"), "Group URL should contain /toc/");
        Assertions.assertTrue(url.contains("PPN_GROUP"), "Group URL should contain PI");
    }

    /**
     * Test that constructUrl includes page number and logId when resolving a page-level URL.
     *
     * @see IdentifierResolver#constructUrl(SolrDocument, boolean, int)
     * @verifies construct page url correctly
     */
    @Test
    void constructUrl_shouldConstructPageUrlCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.PI_TOPSTRUCT, "AC11442160");
        doc.setField(SolrConstants.ISWORK, false);
        doc.setField(SolrConstants.LOGID, "LOG_0002");
        doc.setField(SolrConstants.ORDER, 2);
        doc.setField(SolrConstants.THUMBNAIL, "image.tif");
        // Page-level resolution with order = 2 should produce an object URL containing the page number and logId
        String url = IdentifierResolver.constructUrl(doc, true, 2);
        Assertions.assertTrue(url.contains("AC11442160"), "Page URL should contain PI");
        Assertions.assertTrue(url.contains("/2/"), "Page URL should contain the page order number");
        Assertions.assertTrue(url.contains("LOG_0002"), "Page URL should contain the logId");
    }

    /**
     * Test that constructUrl returns the preferred view URL when a docstruct type has a
     * preferred page type configured (e.g. Catalogue -> viewToc).
     *
     * @see IdentifierResolver#constructUrl(SolrDocument, boolean, int)
     * @verifies construct preferred view url correctly
     */
    @Test
    void constructUrl_shouldConstructPreferredViewUrlCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.DOCSTRCT, "Catalogue");
        doc.setField(SolrConstants.PI_TOPSTRUCT, "PPN_PREF");
        doc.setField(SolrConstants.ISWORK, true);
        // Catalogue has a preferred page type (viewToc) configured in the test config
        String url = IdentifierResolver.constructUrl(doc, false, 1);
        Assertions.assertTrue(url.contains("/toc/"), "Preferred-view URL should contain the configured page type");
        Assertions.assertTrue(url.contains("PPN_PREF"), "Preferred-view URL should contain PI");
    }

    /**
     * Test that constructUrl returns a metadata page URL when the document has an application
     * MIME type (no image view possible).
     *
     * @see IdentifierResolver#constructUrl(SolrDocument, boolean, int)
     * @verifies construct application mime type url correctly
     */
    @Test
    void constructUrl_shouldConstructApplicationMimeTypeUrlCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.DOCSTRCT, "Monograph");
        doc.setField(SolrConstants.PI_TOPSTRUCT, "PPN_APP");
        doc.setField(SolrConstants.ISWORK, true);
        doc.setField(SolrConstants.MIMETYPE, "application");
        // Application mime type means no image view — should fall back to metadata page
        String url = IdentifierResolver.constructUrl(doc, false, 1);
        Assertions.assertTrue(url.contains("/metadata/"), "Application-mime-type URL should contain /metadata/");
        Assertions.assertTrue(url.contains("PPN_APP"), "Application-mime-type URL should contain PI");
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
