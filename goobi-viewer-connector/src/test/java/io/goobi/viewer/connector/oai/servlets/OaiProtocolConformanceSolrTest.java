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

import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.oai.OaiServletInvoker;
import io.goobi.viewer.exceptions.DAOException;

/**
 * Checks the protocol rules that require an index behind the interface.
 *
 * <p>The assertions deliberately do not depend on individual records: they either describe a structure the
 * specification prescribes, or they use arguments whose result is certain regardless of what the index contains.
 * That keeps the tests stable when the test index changes.
 */
class OaiProtocolConformanceSolrTest extends AbstractSolrEnabledTest {

    private static final Namespace OAI_NS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/");

    @BeforeAll
    static void injectEmptyDao() throws DAOException {
        OaiServletInvoker.injectEmptyDao();
    }

    private static void assertError(Document response, String expectedCode) {
        Element error = response.getRootElement().getChild("error", OAI_NS);
        Assertions.assertNotNull(error, "Response carries no error element");
        Assertions.assertEquals(expectedCode, error.getAttributeValue("code"));
    }

    /**
     * @verifies describe the repository with every element the specification requires
     */
    @Test
    void doGet_shouldDescribeTheRepositoryWithEveryElementTheSpecificationRequires() throws Exception {
        Element identify = OaiServletInvoker.call(OaiServletInvoker.params("verb", "Identify"))
                .getRootElement()
                .getChild("Identify", OAI_NS);
        Assertions.assertNotNull(identify);
        for (String required : List.of("repositoryName", "baseURL", "protocolVersion", "earliestDatestamp", "deletedRecord",
                "granularity", "adminEmail")) {
            Assertions.assertNotNull(identify.getChildText(required, OAI_NS), "Identify is missing " + required);
        }
        Assertions.assertEquals("2.0", identify.getChildText("protocolVersion", OAI_NS));
        Assertions.assertTrue(List.of("no", "persistent", "transient").contains(identify.getChildText("deletedRecord", OAI_NS)),
                "deletedRecord must be one of no, persistent or transient");
        Assertions.assertTrue(List.of("YYYY-MM-DD", "YYYY-MM-DDThh:mm:ssZ").contains(identify.getChildText("granularity", OAI_NS)),
                "granularity must be one of the two values defined by the specification");
    }

    /**
     * @verifies answer idDoesNotExist for an unknown identifier
     */
    @Test
    void doGet_shouldAnswerIdDoesNotExistForAnUnknownIdentifier() throws Exception {
        assertError(OaiServletInvoker.call(OaiServletInvoker.params("verb", "GetRecord", "metadataPrefix", "oai_dc",
                "identifier", "no_such_record_in_any_index")), "idDoesNotExist");
    }

    /**
     * @verifies answer noRecordsMatch if the datestamp range cannot contain records
     */
    @Test
    void doGet_shouldAnswerNoRecordsMatchIfTheDatestampRangeCannotContainRecords() throws Exception {
        assertError(OaiServletInvoker.call(OaiServletInvoker.params("verb", "ListRecords", "metadataPrefix", "oai_dc",
                "from", "1000-01-01", "until", "1000-01-02")), "noRecordsMatch");
    }

    /**
     * @verifies give every listed identifier a datestamp
     */
    @Test
    void doGet_shouldGiveEveryListedIdentifierADatestamp() throws Exception {
        Element listIdentifiers = OaiServletInvoker.call(OaiServletInvoker.params("verb", "ListIdentifiers", "metadataPrefix", "oai_dc"))
                .getRootElement()
                .getChild("ListIdentifiers", OAI_NS);
        Assertions.assertNotNull(listIdentifiers);
        List<Element> headers = listIdentifiers.getChildren("header", OAI_NS);
        Assertions.assertFalse(headers.isEmpty(), "The test index is expected to hold records");
        for (Element header : headers) {
            Assertions.assertNotNull(header.getChildText("identifier", OAI_NS));
            Assertions.assertNotNull(header.getChildText("datestamp", OAI_NS));
        }
    }

    /**
     * The specification allows a resumption token to omit both attributes, but if they are given the first page must
     * start at cursor zero and announce the size of the complete list.
     *
     * @verifies start the first resumption token at cursor zero
     */
    @Test
    void doGet_shouldStartTheFirstResumptionTokenAtCursorZero() throws Exception {
        Element listIdentifiers = OaiServletInvoker.call(OaiServletInvoker.params("verb", "ListIdentifiers", "metadataPrefix", "oai_dc"))
                .getRootElement()
                .getChild("ListIdentifiers", OAI_NS);
        Element resumptionToken = listIdentifiers.getChild("resumptionToken", OAI_NS);
        if (resumptionToken == null) {
            return;
        }
        String cursor = resumptionToken.getAttributeValue("cursor");
        if (cursor != null) {
            Assertions.assertEquals("0", cursor, "The first page must start at cursor zero");
        }
        String completeListSize = resumptionToken.getAttributeValue("completeListSize");
        if (completeListSize != null) {
            Assertions.assertTrue(Integer.parseInt(completeListSize) > 0, "completeListSize must count the whole list");
        }
    }
}
