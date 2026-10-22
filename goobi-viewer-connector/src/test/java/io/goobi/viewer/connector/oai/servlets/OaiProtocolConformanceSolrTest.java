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

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
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
     * Returns the identifiers listed in the given ListIdentifiers response.
     *
     * @param listIdentifiers the ListIdentifiers element
     * @return the identifiers, in document order
     */
    private static List<String> identifiersOf(Element listIdentifiers) {
        return listIdentifiers.getChildren("header", OAI_NS).stream().map(header -> header.getChildText("identifier", OAI_NS)).toList();
    }

    /**
     * An incomplete list must be continued with the token it carries, and the continuation must neither repeat nor
     * skip entries. The size of the complete list must not change between pages.
     *
     * @verifies continue an incomplete list without repeating entries
     */
    @Test
    void doGet_shouldContinueAnIncompleteListWithoutRepeatingEntries() throws Exception {
        Element firstPage = OaiServletInvoker.call(OaiServletInvoker.params("verb", "ListIdentifiers", "metadataPrefix", "oai_dc"))
                .getRootElement()
                .getChild("ListIdentifiers", OAI_NS);
        Element token = firstPage.getChild("resumptionToken", OAI_NS);
        Assumptions.assumeTrue(token != null && StringUtils.isNotBlank(token.getText()),
                "The test index holds too few records to page through");

        Element secondPage = OaiServletInvoker.call(OaiServletInvoker.params("verb", "ListIdentifiers", "resumptionToken", token.getText()))
                .getRootElement()
                .getChild("ListIdentifiers", OAI_NS);
        Assertions.assertNotNull(secondPage, "The resumption token was not accepted");

        List<String> first = identifiersOf(firstPage);
        List<String> second = identifiersOf(secondPage);
        Assertions.assertFalse(second.isEmpty(), "The continuation must not be empty");
        Assertions.assertTrue(Collections.disjoint(first, second), "The continuation must not repeat entries of the first page");

        Element secondToken = secondPage.getChild("resumptionToken", OAI_NS);
        if (secondToken != null && secondToken.getAttributeValue("cursor") != null && token.getAttributeValue("cursor") != null) {
            Assertions.assertTrue(Integer.parseInt(secondToken.getAttributeValue("cursor")) > Integer.parseInt(token.getAttributeValue("cursor")),
                    "The cursor must advance from page to page");
        }
        if (secondToken != null && secondToken.getAttributeValue("completeListSize") != null
                && token.getAttributeValue("completeListSize") != null) {
            Assertions.assertEquals(token.getAttributeValue("completeListSize"), secondToken.getAttributeValue("completeListSize"),
                    "The size of the complete list must not change between pages");
        }
    }

    /**
     * The bounds are inclusive: "from" means greater than or equal, "until" means less than or equal.
     *
     * @verifies treat the datestamp bounds as inclusive
     */
    @Test
    void doGet_shouldTreatTheDatestampBoundsAsInclusive() throws Exception {
        Element header = OaiServletInvoker.call(OaiServletInvoker.params("verb", "ListIdentifiers", "metadataPrefix", "oai_dc"))
                .getRootElement()
                .getChild("ListIdentifiers", OAI_NS)
                .getChildren("header", OAI_NS)
                .get(0);
        String identifier = header.getChildText("identifier", OAI_NS);
        String day = header.getChildText("datestamp", OAI_NS).substring(0, 10);

        Element listIdentifiers = OaiServletInvoker.call(OaiServletInvoker.params("verb", "ListIdentifiers", "metadataPrefix", "oai_dc",
                "from", day, "until", day)).getRootElement().getChild("ListIdentifiers", OAI_NS);
        Assertions.assertNotNull(listIdentifiers, "A range covering the record's own datestamp must return it");
        Assertions.assertTrue(identifiersOf(listIdentifiers).contains(identifier),
                "A range whose bounds equal the record's datestamp must include that record");
    }

    /**
     * @verifies state datestamps in the granularity it announces
     */
    @Test
    void doGet_shouldStateDatestampsInTheGranularityItAnnounces() throws Exception {
        String granularity = OaiServletInvoker.call(OaiServletInvoker.params("verb", "Identify"))
                .getRootElement()
                .getChild("Identify", OAI_NS)
                .getChildText("granularity", OAI_NS);
        String pattern = "YYYY-MM-DD".equals(granularity) ? "\\d{4}-\\d{2}-\\d{2}" : "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";

        Element listIdentifiers = OaiServletInvoker.call(OaiServletInvoker.params("verb", "ListIdentifiers", "metadataPrefix", "oai_dc"))
                .getRootElement()
                .getChild("ListIdentifiers", OAI_NS);
        for (Element header : listIdentifiers.getChildren("header", OAI_NS)) {
            String datestamp = header.getChildText("datestamp", OAI_NS);
            Assertions.assertTrue(datestamp.matches(pattern),
                    "Datestamp '" + datestamp + "' does not match the announced granularity " + granularity);
        }
    }

    /**
     * A repository either offers a set hierarchy, in which case every set needs a spec and a name, or it says that it
     * has none.
     *
     * @verifies either describe its sets or state that it has none
     */
    @Test
    void doGet_shouldEitherDescribeItsSetsOrStateThatItHasNone() throws Exception {
        Element root = OaiServletInvoker.call(OaiServletInvoker.params("verb", "ListSets")).getRootElement();
        Element listSets = root.getChild("ListSets", OAI_NS);
        if (listSets == null) {
            assertError(root.getDocument(), "noSetHierarchy");
            return;
        }
        Assertions.assertFalse(listSets.getChildren("set", OAI_NS).isEmpty(), "ListSets must describe at least one set");
        for (Element set : listSets.getChildren("set", OAI_NS)) {
            Assertions.assertNotNull(set.getChildText("setSpec", OAI_NS));
            Assertions.assertNotNull(set.getChildText("setName", OAI_NS));
        }
    }

    /**
     * @verifies return exactly the requested record
     */
    @Test
    void doGet_shouldReturnExactlyTheRequestedRecord() throws Exception {
        String identifier = OaiServletInvoker.call(OaiServletInvoker.params("verb", "ListIdentifiers", "metadataPrefix", "oai_dc"))
                .getRootElement()
                .getChild("ListIdentifiers", OAI_NS)
                .getChildren("header", OAI_NS)
                .get(0)
                .getChildText("identifier", OAI_NS);

        Element getRecord = OaiServletInvoker.call(OaiServletInvoker.params("verb", "GetRecord", "metadataPrefix", "oai_dc",
                "identifier", identifier)).getRootElement().getChild("GetRecord", OAI_NS);
        Assertions.assertNotNull(getRecord, "GetRecord must answer with a record for an identifier it just listed");
        List<Element> records = getRecord.getChildren("record", OAI_NS);
        Assertions.assertEquals(1, records.size(), "GetRecord must return exactly one record");
        Element record = records.get(0);
        Assertions.assertEquals(identifier, record.getChild("header", OAI_NS).getChildText("identifier", OAI_NS));
        Assertions.assertNotNull(record.getChild("metadata", OAI_NS), "A record that is not deleted must carry metadata");
    }

    /**
     * The specification requires repositories to support both GET and POST.
     *
     * @verifies answer a request submitted by POST
     */
    @Test
    void doGet_shouldAnswerARequestSubmittedByPost() throws Exception {
        Element identify = OaiServletInvoker.callPost(OaiServletInvoker.params("verb", "Identify"))
                .getRootElement()
                .getChild("Identify", OAI_NS);
        Assertions.assertNotNull(identify, "A request submitted by POST must be answered like one submitted by GET");
        Assertions.assertEquals("2.0", identify.getChildText("protocolVersion", OAI_NS));
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
