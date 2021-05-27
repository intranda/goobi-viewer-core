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
package io.goobi.viewer.solr;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.viewer.StringPair;

public class SolrSearchIndexTest extends AbstractSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SolrSearchIndexTest.class);

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies return correct results
     */
    @Test
    public void search_shouldReturnCorrectResults() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":PPN517154005 " + SolrConstants.PI + ":34115495_1940", 0, Integer.MAX_VALUE, null, null, null);
        Assert.assertEquals(2, response.getResults().size());
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies return correct number of rows
     */
    @Test
    public void search_shouldReturnCorrectNumberOfRows() throws Exception {
        QueryResponse response = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":*", 100, 10, null, null, null);
        Assert.assertEquals(10, response.getResults().size());
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies sort results correctly
     */
    @Test
    public void search_shouldSortResultsCorrectly() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":*", 0, 10, Collections.singletonList(new StringPair(SolrConstants.DATECREATED, "desc")), null, null);
        Assert.assertEquals(10, response.getResults().size());
        long previous = -1;
        for (SolrDocument doc : response.getResults()) {
            Long datecreated = (Long) doc.getFieldValue(SolrConstants.DATECREATED);
            Assert.assertNotNull(datecreated);
            if (previous != -1) {
                Assert.assertTrue(previous >= datecreated);
            }
            previous = datecreated;
        }
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies facet results correctly
     */
    @Test
    public void search_shouldFacetResultsCorrectly() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":*", 0, 10, null, Collections.singletonList(SolrConstants.DC), null);
        Assert.assertEquals(10, response.getResults().size());
        Assert.assertNotNull(response.getFacetField(SolrConstants.DC));
        Assert.assertNotNull(response.getFacetField(SolrConstants.DC).getValues());
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies filter fields correctly
     */
    @Test
    public void search_shouldFilterFieldsCorrectly() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":*", 0, 10, null, null, Collections.singletonList(SolrConstants.PI));
        Assert.assertEquals(10, response.getResults().size());
        for (SolrDocument doc : response.getResults()) {
            Assert.assertEquals(1, doc.getFieldNames().size());
            Assert.assertTrue(doc.getFieldNames().contains(SolrConstants.PI));

        }
    }

    /**
     * @see SolrSearchIndex#searchFacetsAndStatistics(String,List,boolean)
     * @verifies generate facets correctly
     */
    @Test
    public void searchFacetsAndStatistics_shouldGenerateFacetsCorrectly() throws Exception {
        String[] facetFields = { SolrConstants._CALENDAR_YEAR, SolrConstants._CALENDAR_MONTH };
        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(SolrConstants._CALENDAR_YEAR + ":*", null, Arrays.asList(facetFields), 0, false);
        Assert.assertNotNull(resp.getFacetField(SolrConstants._CALENDAR_YEAR));
        Assert.assertNotNull(resp.getFacetField(SolrConstants._CALENDAR_MONTH));
    }

    /**
     * @see SolrSearchIndex#searchFacetsAndStatistics(String,List,boolean)
     * @verifies generate field statistics for every facet field if requested
     */
    @Test
    public void searchFacetsAndStatistics_shouldGenerateFieldStatisticsForEveryFacetFieldIfRequested() throws Exception {
        String[] facetFields = { SolrConstants._CALENDAR_YEAR };
        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(SolrConstants._CALENDAR_YEAR + ":*", null, Arrays.asList(facetFields), 0, true);
        Assert.assertNotNull(resp.getFieldStatsInfo());
        FieldStatsInfo info = resp.getFieldStatsInfo().get(SolrConstants._CALENDAR_YEAR);
        Assert.assertNotNull(info);
    }

    /**
     * @see SolrSearchIndex#searchFacetsAndStatistics(String,List,boolean)
     * @verifies not return any docs
     */
    @Test
    public void searchFacetsAndStatistics_shouldNotReturnAnyDocs() throws Exception {
        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(SolrConstants._CALENDAR_YEAR + ":*", null, Collections.singletonList(SolrConstants._CALENDAR_YEAR), 0,
                        false);
        Assert.assertTrue(resp.getResults().isEmpty());
    }

    /**
     * @see SolrSearchIndex#getImageOwnerIddoc(String,int)
     * @verifies retrieve correct IDDOC
     */
    @Test
    public void getImageOwnerIddoc_shouldRetrieveCorrectIDDOC() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getImageOwnerIddoc(PI_KLEIUNIV, 1);
        Assert.assertNotEquals(-1, iddoc);
    }

    /**
     * @see SolrSearchIndex#getFirstDoc(String,List)
     * @verifies return correct doc
     */
    @Test
    public void getFirstDoc_shouldReturnCorrectDoc() throws Exception {
        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc(new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(":")
                        .append(PI_KLEIUNIV)
                        .append(" AND ")
                        .append(SolrConstants.DOCTYPE)
                        .append(":PAGE")
                        .toString(), Collections.singletonList(SolrConstants.ORDER));
        Assert.assertNotNull(doc);
        Assert.assertEquals(1, doc.getFieldValue(SolrConstants.ORDER));
    }

    /**
     * @see SolrSearchIndex#getDocumentByIddoc(long)
     * @verifies return correct doc
     */
    @Test
    public void getDocumentByIddoc_shouldReturnCorrectDoc() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByIddoc(String.valueOf(iddocKleiuniv));
        Assert.assertNotNull(doc);
        Assert.assertEquals(String.valueOf(iddocKleiuniv), doc.getFieldValue(SolrConstants.IDDOC));
    }

    /**
     * @see SolrSearchIndex#getIddocFromIdentifier(String)
     * @verifies retrieve correct IDDOC
     */
    @Test
    public void getIddocFromIdentifier_shouldRetrieveCorrectIDDOC() throws Exception {
        Assert.assertEquals(iddocKleiuniv, DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(PI_KLEIUNIV));
    }

    /**
     * @see SolrSearchIndex#getIdentifierFromIddoc(long)
     * @verifies retrieve correct identifier
     */
    @Test
    public void getIdentifierFromIddoc_shouldRetrieveCorrectIdentifier() throws Exception {
        Assert.assertEquals(PI_KLEIUNIV, DataManager.getInstance().getSearchIndex().getIdentifierFromIddoc(iddocKleiuniv));
    }

    /**
     * @see SolrSearchIndex#getIddocByLogid(String,String)
     * @verifies retrieve correct IDDOC
     */
    @Test
    public void getIddocByLogid_shouldRetrieveCorrectIDDOC() throws Exception {
        Assert.assertNotEquals(-1, DataManager.getInstance().getSearchIndex().getIddocByLogid(PI_KLEIUNIV, "LOG_0001"));
    }

    /**
     * @see SolrSearchIndex#findDataRepositoryName(String)
     * @verifies return value from map if available
     */
    @Test
    public void findDataRepositoryName_shouldReturnValueFromMapIfAvailable() throws Exception {
        DataManager.getInstance().getSearchIndex().dataRepositoryNames.put("PPN123", "superrepo");
        Assert.assertEquals("superrepo", DataManager.getInstance().getSearchIndex().findDataRepositoryName("PPN123"));
    }

    /**
     * @see SolrSearchIndex#updateDataRepositoryNames(String,String)
     * @verifies update value correctly
     */
    @Test
    public void updateDataRepositoryNames_shouldUpdateValueCorrectly() throws Exception {
        Assert.assertNull(DataManager.getInstance().getSearchIndex().dataRepositoryNames.get("PPN123"));
        DataManager.getInstance().getSearchIndex().updateDataRepositoryNames("PPN123", "repo/a");
        Assert.assertEquals("repo/a", DataManager.getInstance().getSearchIndex().dataRepositoryNames.get("PPN123"));
    }

    /**
     * @see SolrSearchIndex#getLabelValuesForDrillDownField(String,String,Set)
     * @verifies return correct values
     */
    @Test
    public void getLabelValuesForDrillDownField_shouldReturnCorrectValues() throws Exception {
        String[] values = new String[] { "Groos, Karl", "Schubert, Otto", "Heinse, Gottlob Heinrich" };
        Map<String, String> result = DataManager.getInstance()
                .getSearchIndex()
                .getLabelValuesForDrillDownField("MD_AUTHOR", "MD_FIRSTNAME", new HashSet<>(Arrays.asList(values)));
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Karl", result.get("MD_AUTHOR:Groos, Karl"));
        Assert.assertEquals("Otto", result.get("MD_AUTHOR:Schubert, Otto"));
        Assert.assertEquals("Gottlob Heinrich", result.get("MD_AUTHOR:Heinse, Gottlob Heinrich"));
    }
}