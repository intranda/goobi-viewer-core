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
package io.goobi.viewer.connector.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.solr.SolrConstants;

class SolrSearchIndexTest extends AbstractSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(SolrSearchIndexTest.class);

    /**
     * @see SolrSearchIndex#checkReloadNeeded()
     * @verifies create new client if solr url changed
     */
    @Test
    void checkReloadNeeded_shouldCreateNewClientIfSolrUrlChanged() {
        SolrSearchIndex index = DataManager.getInstance().getSearchIndex();
        index.setTestMode(false);
        SolrClient oldClient = index.getClient();
        Assertions.assertEquals(oldClient, index.getClient());
        DataManager.getInstance().getConfiguration().overrideValue("solr.solrUrl", "http://localhost:8080/solr");
        index.checkReloadNeeded();
        Assertions.assertNotEquals(oldClient, index.getClient());
    }

    /**
     * @see SolrSearchIndex#checkReloadNeeded()
     * @verifies ping server if last ping too old
     */
    @Test
    void checkReloadNeeded_shouldPingServerIfLastPingTooOld() {
        SolrSearchIndex index = DataManager.getInstance().getSearchIndex();
        index.setTestMode(false);
        Assertions.assertEquals(0, index.getLastPing());
        index.checkReloadNeeded();
        Assertions.assertNotEquals(0, index.getLastPing());
    }

    /**
     * @see SolrSearchIndex#getSets(String)
     * @verifies return all values
     */
    @Test
    void getSets_shouldReturnAllValues() throws Exception {
        // Update expected result if test index changes
        Assertions.assertEquals(54, DataManager.getInstance()
                .getSearchIndex()
                .getSets(SolrConstants.DC)
                .size());
    }

    /**
     * @see SolrSearchIndex#getFirstDoc(String,List)
     * @verifies return correct doc
     */
    @Test
    void getFirstDoc_shouldReturnCorrectDoc() throws Exception {
        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc(SolrConstants.PI + ":PPN517154005", Collections.singletonList(SolrConstants.PI));
        Assertions.assertNotNull(doc);
        Assertions.assertEquals("PPN517154005", doc.getFieldValue(SolrConstants.PI));
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,List,Map)
     * @verifies return correct number of rows
     */
    @Test
    void search_shouldReturnCorrectNumberOfRows() throws Exception {
        QueryResponse qr = DataManager.getInstance()
                .getSearchIndex()
                .search(null, null, null, Metadata.OAI_DC.getMetadataPrefix(), 0, 55, false, null, "", null, null);
        Assertions.assertEquals(55, qr.getResults()
                .size());
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,List,Map)
     * @verifies sort results correctly
     */
    @Test
    void search_shouldSortResultsCorrectly() throws Exception {
        QueryResponse qr = DataManager.getInstance()
                .getSearchIndex()
                .search(null, null, null, Metadata.OAI_DC.getMetadataPrefix(), 0, 10, false, null, "", null, null);
        Assertions.assertTrue(qr.getResults().size() > 1);
        long previous = 0;
        for (SolrDocument doc : qr.getResults()) {
            Long dateCreated = (long) doc.getFieldValue(SolrConstants.DATECREATED);
            Assertions.assertNotNull(dateCreated);
            Assertions.assertTrue(dateCreated >= previous);
            previous = dateCreated;
        }
    }

    /**
     * @see SolrSearchIndex#getFulltextFileNames(String)
     * @verifies return file names correctly
     */
    @Test
    void getFulltextFileNames_shouldReturnFileNamesCorrectly() throws Exception {
        Map<Integer, String> result = DataManager.getInstance()
                .getSearchIndex()
                .getFulltextFileNames("lit30844");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("alto/lit30844/p0085.xml", result.get(1));
    }
}