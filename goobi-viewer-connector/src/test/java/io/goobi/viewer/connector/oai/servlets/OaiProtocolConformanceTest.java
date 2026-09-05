/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package io.goobi.viewer.connector.oai.servlets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractTest;
import io.goobi.viewer.connector.oai.OaiResponseValidator;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Checks the OAI-PMH envelope against the protocol specification.
 *
 * <p>The rules verified here are taken from the OAI-PMH 2.0 specification and from OAI-PMH.xsd; they concern the
 * response envelope only. Metadata payloads are produced elsewhere and are treated as opaque.
 *
 * <p>Only request paths that answer without querying the index are covered, so the tests run without a Solr server.
 */
class OaiProtocolConformanceTest extends AbstractTest {

    private static final Namespace OAI_NS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/");

    /**
     * Puts a DAO without any license types in place. The servlet builds an access filter for every request; without a
     * DAO that would fail on the persistence unit, and the license types themselves are irrelevant for the envelope.
     */
    @BeforeAll
    static void injectEmptyDao() throws DAOException {
        IDAO dao = mock(IDAO.class);
        when(dao.getRecordLicenseTypes()).thenReturn(Collections.emptyList());
        when(dao.getAllLicenseTypes()).thenReturn(Collections.emptyList());
        io.goobi.viewer.controller.DataManager.getInstance().injectDao(dao);
    }

    /**
     * Runs the servlet with the given request parameters and returns the parsed response.
     *
     * @param parameters query parameters of the protocol request
     * @return the response document
     */
    private static Document callServlet(Map<String, String> parameters) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/viewer/oai"));
        when(request.getQueryString()).thenReturn(null);
        Map<String, String[]> parameterMap = new HashMap<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            parameterMap.put(entry.getKey(), new String[] { entry.getValue() });
            when(request.getParameter(entry.getKey())).thenReturn(entry.getValue());
            when(request.getParameterValues(entry.getKey())).thenReturn(new String[] { entry.getValue() });
        }
        when(request.getParameterMap()).thenReturn(parameterMap);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // no asynchronous writing in this test
            }
        });

        new OaiServlet().doGet(request, response);
        return OaiResponseValidator.assertValid(out.toString(StandardCharsets.UTF_8));
    }

    private static Map<String, String> params(String... keyValuePairs) {
        Map<String, String> ret = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            ret.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return ret;
    }

    /**
     * Asserts that the response carries exactly the given error code.
     *
     * @param response response document
     * @param expectedCode expected value of the error element's code attribute
     */
    private static void assertError(Document response, String expectedCode) {
        Element error = response.getRootElement().getChild("error", OAI_NS);
        Assertions.assertNotNull(error, "Response carries no error element");
        Assertions.assertEquals(expectedCode, error.getAttributeValue("code"));
    }

    /**
     * @verifies answer badVerb if the verb argument is missing
     */
    @Test
    void doGet_shouldAnswerBadVerbIfTheVerbArgumentIsMissing() throws Exception {
        assertError(callServlet(params()), "badVerb");
    }

    /**
     * @verifies answer badVerb if the verb argument is not a legal verb
     */
    @Test
    void doGet_shouldAnswerBadVerbIfTheVerbArgumentIsNotALegalVerb() throws Exception {
        assertError(callServlet(params("verb", "nastyVerb")), "badVerb");
    }

    /**
     * @verifies answer badArgument if from is later than until
     */
    @Test
    void doGet_shouldAnswerBadArgumentIfFromIsLaterThanUntil() throws Exception {
        assertError(callServlet(params("verb", "ListIdentifiers", "metadataPrefix", "oai_dc",
                "from", "2024-12-31", "until", "2024-01-01")), "badArgument");
    }

    /**
     * @verifies answer badArgument if from and until use different granularities
     */
    @Test
    void doGet_shouldAnswerBadArgumentIfFromAndUntilUseDifferentGranularities() throws Exception {
        assertError(callServlet(params("verb", "ListIdentifiers", "metadataPrefix", "oai_dc",
                "from", "2024-01-01", "until", "2024-12-31T23:59:59Z")), "badArgument");
    }

    /**
     * The specification requires the base URL only, without attributes, for badVerb and badArgument responses.
     *
     * @verifies not describe the request if the verb is illegal
     */
    @Test
    void doGet_shouldNotDescribeTheRequestIfTheVerbIsIllegal() throws Exception {
        Element requestElement = callServlet(params("verb", "nastyVerb")).getRootElement().getChild("request", OAI_NS);
        Assertions.assertNotNull(requestElement);
        Assertions.assertTrue(requestElement.getAttributes().isEmpty(),
                "badVerb responses must not describe the request, but found: " + requestElement.getAttributes());
    }

    /**
     * @verifies answer badArgument if ListIdentifiers is called without a metadata prefix
     */
    @Test
    void doGet_shouldAnswerBadArgumentIfListIdentifiersIsCalledWithoutAMetadataPrefix() throws Exception {
        assertError(callServlet(params("verb", "ListIdentifiers")), "badArgument");
    }

    /**
     * @verifies answer badArgument if ListRecords is called without a metadata prefix
     */
    @Test
    void doGet_shouldAnswerBadArgumentIfListRecordsIsCalledWithoutAMetadataPrefix() throws Exception {
        assertError(callServlet(params("verb", "ListRecords")), "badArgument");
    }

    /**
     * The specification reserves badArgument for missing or syntactically illegal arguments; a syntactically fine
     * prefix that the repository does not support is a cannotDisseminateFormat condition.
     *
     * @verifies answer cannotDisseminateFormat if the metadata prefix is unknown
     */
    @Test
    void doGet_shouldAnswerCannotDisseminateFormatIfTheMetadataPrefixIsUnknown() throws Exception {
        assertError(callServlet(params("verb", "ListRecords", "metadataPrefix", "no_such_format")), "cannotDisseminateFormat");
    }

    /**
     * @verifies describe every metadata format with prefix, schema and namespace
     */
    @Test
    void doGet_shouldDescribeEveryMetadataFormatWithPrefixSchemaAndNamespace() throws Exception {
        Element listMetadataFormats = callServlet(params("verb", "ListMetadataFormats"))
                .getRootElement()
                .getChild("ListMetadataFormats", OAI_NS);
        Assertions.assertNotNull(listMetadataFormats);
        Assertions.assertFalse(listMetadataFormats.getChildren("metadataFormat", OAI_NS).isEmpty());
        for (Element metadataFormat : listMetadataFormats.getChildren("metadataFormat", OAI_NS)) {
            Assertions.assertNotNull(metadataFormat.getChildText("metadataPrefix", OAI_NS));
            Assertions.assertNotNull(metadataFormat.getChildText("schema", OAI_NS));
            Assertions.assertNotNull(metadataFormat.getChildText("metadataNamespace", OAI_NS));
        }
    }

    /**
     * @verifies answer badArgument if GetRecord is called without an identifier
     */
    @Test
    void doGet_shouldAnswerBadArgumentIfGetRecordIsCalledWithoutAnIdentifier() throws Exception {
        assertError(callServlet(params("verb", "GetRecord", "metadataPrefix", "oai_dc")), "badArgument");
    }

    /**
     * @verifies answer badResumptionToken if the resumption token is malformed
     */
    @Test
    void doGet_shouldAnswerBadResumptionTokenIfTheResumptionTokenIsMalformed() throws Exception {
        assertError(callServlet(params("verb", "ListRecords", "resumptionToken", "not-a-token")), "badResumptionToken");
    }

    /**
     * The specification declares resumptionToken an exclusive argument, so it may not be combined with any other.
     *
     * @verifies answer badArgument if the resumption token is combined with another argument
     */
    @Test
    void doGet_shouldAnswerBadArgumentIfTheResumptionTokenIsCombinedWithAnotherArgument() throws Exception {
        assertError(callServlet(params("verb", "ListRecords", "resumptionToken", "oai_1634822246437",
                "metadataPrefix", "oai_dc")), "badArgument");
    }

    /**
     * @verifies state the response date in UTC
     */
    @Test
    void doGet_shouldStateTheResponseDateInUtc() throws Exception {
        String responseDate = callServlet(params()).getRootElement().getChildText("responseDate", OAI_NS);
        Assertions.assertNotNull(responseDate);
        Assertions.assertTrue(responseDate.endsWith("Z"), "responseDate must be expressed in UTC, but was: " + responseDate);
    }
}
