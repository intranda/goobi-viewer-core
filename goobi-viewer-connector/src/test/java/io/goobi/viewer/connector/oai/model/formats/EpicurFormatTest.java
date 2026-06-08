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
package io.goobi.viewer.connector.oai.model.formats;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jdom2.Element;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.enums.Verb;
import io.goobi.viewer.connector.utils.Utils;

class EpicurFormatTest extends AbstractSolrEnabledTest {

    private static RequestHandler listRecordsHandler() {
        RequestHandler handler = new RequestHandler();
        handler.setMetadataPrefix(Metadata.EPICUR);
        handler.setVerb(Verb.LISTRECORDS);
        return handler;
    }

    /**
     * @see EpicurFormat#getCompleteListSize(java.util.Map,String)
     * @verifies equal the number of emitted records
     */
    @Test
    void getCompleteListSize_shouldEqualNumberOfEmittedRecords() throws Exception {
        RequestHandler handler = listRecordsHandler();
        EpicurFormat format = new EpicurFormat();

        // Harvest the whole result set in a single batch (no resumption token), then count the emitted <record> elements.
        Element listRecords = format.createListRecords(handler, 0, 0, Integer.MAX_VALUE, null, "");
        Assertions.assertEquals("ListRecords", listRecords.getName());
        List<Element> records = listRecords.getChildren("record", Format.OAI_NS);
        Assertions.assertFalse(records.isEmpty());

        long completeListSize = format.getCompleteListSize(Utils.filterDatestampFromRequest(handler), "");
        Assertions.assertEquals(records.size(), completeListSize);
    }

    /**
     * @see EpicurFormat#createListRecords(RequestHandler,int,int,int,String,String)
     * @verifies report virtual hits including page records in resumption token
     */
    @Test
    void createListRecords_shouldReportVirtualHitsIncludingPageRecordsInResumptionToken() throws Exception {
        Path tokenFolder = Paths.get(DataManager.getInstance().getConfiguration().getResumptionTokenFolder());
        Files.createDirectories(tokenFolder);

        RequestHandler handler = listRecordsHandler();
        EpicurFormat format = new EpicurFormat();

        // Small raw batch size so that a resumption token is created.
        Element listRecords = format.createListRecords(handler, 0, 0, 5, null, "");
        Element resumptionToken = listRecords.getChild("resumptionToken", Format.OAI_NS);
        Assertions.assertNotNull(resumptionToken, "Expected a resumption token for a partial batch");
        try {
            // completeListSize must equal the virtual total (document records + page records), not just the raw Solr document count.
            long completeListSize = Long.parseLong(resumptionToken.getAttributeValue("completeListSize"));
            Assertions.assertEquals(format.getCompleteListSize(Utils.filterDatestampFromRequest(handler), ""), completeListSize);

            // The first batch emits more <record> elements than the raw document batch size, because each record also yields page records.
            long emittedInFirstBatch = listRecords.getChildren("record", Format.OAI_NS).size();
            Assertions.assertTrue(emittedInFirstBatch > 5, "Expected page records to inflate the batch beyond the raw document count");
            Assertions.assertTrue(completeListSize >= emittedInFirstBatch);
            Assertions.assertEquals("0", resumptionToken.getAttributeValue("cursor"));
        } finally {
            Files.deleteIfExists(tokenFolder.resolve(resumptionToken.getText()));
        }
    }
}
