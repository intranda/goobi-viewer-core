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
package io.goobi.viewer.solr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.viewer.StringPair;

class SolrSearchIndexTest extends AbstractSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(SolrSearchIndexTest.class);

    /**
     * @see SolrSearchIndex#isSolrIndexOnline()
     * @verifies return true if solr online
     */
    @Test
    void isSolrIndexOnline_shouldReturnTrueIfSolrOnline() {
        assertTrue(DataManager.getInstance().getSearchIndex().isSolrIndexOnline());
    }

    /**
     * @see SolrSearchIndex#isSolrIndexOnline()
     * @verifies return false if solr offline
     */
    @Test
    void isSolrIndexOnline_shouldReturnFalseIfSolrOffline() {
        String solrUrl = DataManager.getInstance().getConfiguration().getSolrUrl();
        DataManager.getInstance().getConfiguration().overrideValue("urls.solr", "https://locahost:1234/solr");
        try {
            assertFalse(DataManager.getInstance().getSearchIndex().isSolrIndexOnline());
        } finally {
            DataManager.getInstance().getConfiguration().overrideValue("urls.solr", solrUrl);
        }
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies return correct results
     */
    @Test
    void search_shouldReturnCorrectResults() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":PPN517154005 " + SolrConstants.PI + ":34115495_1940", 0, Integer.MAX_VALUE, null, null, null);
        Assertions.assertEquals(2, response.getResults().size());
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies return correct number of rows
     */
    @Test
    void search_shouldReturnCorrectNumberOfRows() throws Exception {
        QueryResponse response = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":*", 100, 10, null, null, null);
        Assertions.assertEquals(10, response.getResults().size());
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies sort results correctly
     */
    @Test
    void search_shouldSortResultsCorrectly() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":*", 0, 10, Collections.singletonList(new StringPair(SolrConstants.DATECREATED, "desc")), null, null);
        Assertions.assertEquals(10, response.getResults().size());
        long previous = -1;
        for (SolrDocument doc : response.getResults()) {
            Long datecreated = (Long) doc.getFieldValue(SolrConstants.DATECREATED);
            Assertions.assertNotNull(datecreated);
            if (previous != -1) {
                Assertions.assertTrue(previous >= datecreated);
            }
            previous = datecreated;
        }
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies facet results correctly
     */
    @Test
    void search_shouldFacetResultsCorrectly() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":*", 0, 10, null, Collections.singletonList(SolrConstants.DC), null);
        Assertions.assertEquals(10, response.getResults().size());
        Assertions.assertNotNull(response.getFacetField(SolrConstants.DC));
        Assertions.assertNotNull(response.getFacetField(SolrConstants.DC).getValues());
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies filter fields correctly
     */
    @Test
    void search_shouldFilterFieldsCorrectly() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":*", 0, 10, null, null, Collections.singletonList(SolrConstants.PI));
        Assertions.assertEquals(10, response.getResults().size());
        for (SolrDocument doc : response.getResults()) {
            Assertions.assertEquals(1, doc.getFieldNames().size());
            Assertions.assertTrue(doc.getFieldNames().contains(SolrConstants.PI));

        }
    }

    /**
     * @see SolrSearchIndex#searchFacetsAndStatistics(String,List,boolean)
     * @verifies generate facets correctly
     */
    @Test
    void searchFacetsAndStatistics_shouldGenerateFacetsCorrectly() throws Exception {
        String[] facetFields = { SolrConstants.CALENDAR_YEAR, SolrConstants.CALENDAR_MONTH };
        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(SolrConstants.CALENDAR_YEAR + ":*", null, Arrays.asList(facetFields), 0, false);
        Assertions.assertNotNull(resp.getFacetField(SolrConstants.CALENDAR_YEAR));
        Assertions.assertNotNull(resp.getFacetField(SolrConstants.CALENDAR_MONTH));
    }

    /**
     * @see SolrSearchIndex#searchFacetsAndStatistics(String,List,boolean)
     * @verifies generate field statistics for every facet field if requested
     */
    @Test
    void searchFacetsAndStatistics_shouldGenerateFieldStatisticsForEveryFacetFieldIfRequested() throws Exception {
        String[] facetFields = { SolrConstants.CALENDAR_YEAR };
        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(SolrConstants.CALENDAR_YEAR + ":*", null, Arrays.asList(facetFields), 0, true);
        Assertions.assertNotNull(resp.getFieldStatsInfo());
        FieldStatsInfo info = resp.getFieldStatsInfo().get(SolrConstants.CALENDAR_YEAR);
        Assertions.assertNotNull(info);
    }

    /**
     * @see SolrSearchIndex#searchFacetsAndStatistics(String,List,boolean)
     * @verifies not return any docs
     */
    @Test
    void searchFacetsAndStatistics_shouldNotReturnAnyDocs() throws Exception {
        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(SolrConstants.CALENDAR_YEAR + ":*", null, Collections.singletonList(SolrConstants.CALENDAR_YEAR), 0,
                        false);
        Assertions.assertTrue(resp.getResults().isEmpty());
    }

    /**
     * @see SolrSearchIndex#getImageOwnerIddoc(String,int)
     * @verifies retrieve correct IDDOC
     */
    @Test
    void getImageOwnerIddoc_shouldRetrieveCorrectIDDOC() throws Exception {
        Assertions.assertNotNull(DataManager.getInstance().getSearchIndex().getImageOwnerIddoc(PI_KLEIUNIV, 1));
    }

    /**
     * @see SolrSearchIndex#getFirstDoc(String,List)
     * @verifies return correct doc
     */
    @Test
    void getFirstDoc_shouldReturnCorrectDoc() throws Exception {
        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc(new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(":")
                        .append(PI_KLEIUNIV)
                        .append(" AND ")
                        .append(SolrConstants.DOCTYPE)
                        .append(":PAGE")
                        .toString(), Collections.singletonList(SolrConstants.ORDER));
        Assertions.assertNotNull(doc);
        Assertions.assertEquals(1, doc.getFieldValue(SolrConstants.ORDER));
    }

    /**
     * @see SolrSearchIndex#getDocumentByIddoc(long)
     * @verifies return correct doc
     */
    @Test
    void getDocumentByIddoc_shouldReturnCorrectDoc() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByIddoc(String.valueOf(iddocKleiuniv));
        Assertions.assertNotNull(doc);
        Assertions.assertEquals(String.valueOf(iddocKleiuniv), doc.getFieldValue(SolrConstants.IDDOC));
    }

    /**
     * @see SolrSearchIndex#getIddocFromIdentifier(String)
     * @verifies retrieve correct IDDOC
     */
    @Test
    void getIddocFromIdentifier_shouldRetrieveCorrectIDDOC() throws Exception {
        Assertions.assertEquals(iddocKleiuniv, DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(PI_KLEIUNIV));
    }

    /**
     * @see SolrSearchIndex#getIdentifierFromIddoc(long)
     * @verifies retrieve correct identifier
     */
    @Test
    void getIdentifierFromIddoc_shouldRetrieveCorrectIdentifier() throws Exception {
        Assertions.assertEquals(PI_KLEIUNIV, DataManager.getInstance().getSearchIndex().getIdentifierFromIddoc(iddocKleiuniv));
    }

    /**
     * @see SolrSearchIndex#getIddocByLogid(String,String)
     * @verifies retrieve correct IDDOC
     */
    @Test
    void getIddocByLogid_shouldRetrieveCorrectIDDOC() throws Exception {
        Assertions.assertNotNull(DataManager.getInstance().getSearchIndex().getIddocByLogid(PI_KLEIUNIV, "LOG_0001"));
    }

    /**
     * @see SolrSearchIndex#findDataRepositoryName(String)
     * @verifies return value from map if available
     */
    @Test
    void findDataRepositoryName_shouldReturnValueFromMapIfAvailable() throws Exception {
        DataManager.getInstance().getSearchIndex().getDataRepositoryNames().put("PPN123", "superrepo");
        Assertions.assertEquals("superrepo", DataManager.getInstance().getSearchIndex().findDataRepositoryName("PPN123"));
    }

    /**
     * @see SolrSearchIndex#updateDataRepositoryNames(String,String)
     * @verifies update value correctly
     */
    @Test
    void updateDataRepositoryNames_shouldUpdateValueCorrectly() {
        Assertions.assertNull(DataManager.getInstance().getSearchIndex().getDataRepositoryNames().get("PPN123"));
        DataManager.getInstance().getSearchIndex().updateDataRepositoryNames("PPN123", "repo/a");
        Assertions.assertEquals("repo/a", DataManager.getInstance().getSearchIndex().getDataRepositoryNames().get("PPN123"));
    }

    /**
     * @see SolrSearchIndex#getLabelValuesForFacetField(String,String,Set)
     * @verifies return correct values
     */
    @Test
    void getLabelValuesForFacetField_shouldReturnCorrectValues() throws Exception {
        String[] values = new String[] { "Groos, Karl", "Schubert, Otto", "Heinse, Gottlob Heinrich" };
        Map<String, String> result = DataManager.getInstance()
                .getSearchIndex()
                .getLabelValuesForFacetField("MD_AUTHOR", "MD_FIRSTNAME", new HashSet<>(Arrays.asList(values)));
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("Karl", result.get("MD_AUTHOR:Groos, Karl"));
        Assertions.assertEquals("Otto", result.get("MD_AUTHOR:Schubert, Otto"));
        Assertions.assertEquals("Gottlob Heinrich", result.get("MD_AUTHOR:Heinse, Gottlob Heinrich"));
    }

    @Test
    void test_getHeatMap() throws IndexUnreachableException {

        String world = "[\"-180 -90\" TO \"180 90\"]";
        String query = "*:*";

        String string = DataManager.getInstance()
                .getSearchIndex()
                .getHeatMap("WKT_COORDS", world, query, "", 1);
        assertNotNull(string);
        JSONObject json = new JSONObject(string);

        assertEquals(1, json.get("gridLevel"));
        assertEquals(8, json.get("columns"));
        assertEquals(4, json.get("rows"));
        assertEquals(-180.0, json.getDouble("minX"), 0.0);
        assertEquals(180.0, json.getDouble("maxX"), 0.0);
        assertEquals(-90.0, json.getDouble("minY"), 0.0);
        assertEquals(90.0, json.getDouble("maxY"), 0.0);

        JSONArray rows = json.getJSONArray("counts_ints2D");
        assertEquals(4, rows.length());
        assertEquals(8, rows.getJSONArray(0).length());
        assertEquals(0, rows.getJSONArray(0).getInt(0));
        assertEquals(209, rows.getJSONArray(0).getInt(4));
        assertEquals(JSONObject.NULL, rows.get(2));
        assertEquals(JSONObject.NULL, rows.get(3));
    }
}
