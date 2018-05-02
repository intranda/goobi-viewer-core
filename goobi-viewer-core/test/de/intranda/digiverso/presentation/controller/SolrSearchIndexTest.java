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
package de.intranda.digiverso.presentation.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.AbstractSolrEnabledTest;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.StringPair;

public class SolrSearchIndexTest extends AbstractSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SolrSearchIndexTest.class);

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies return correct results
     */
    @Test
    public void search_shouldReturnCorrectResults() throws Exception {
        QueryResponse response = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":*", 0, Integer.MAX_VALUE, null, null, null);
        Assert.assertEquals(346, response.getResults().size());
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
        QueryResponse response = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":*", 0, 10, Collections.singletonList(
                new StringPair(SolrConstants.DATECREATED, "desc")), null, null);
        Assert.assertEquals(10, response.getResults().size());
        long previous = -1;
        for (SolrDocument doc : response.getResults()) {
            Long datecreated = (Long) doc.getFieldValue(SolrConstants.DATECREATED);
            Assert.assertNotNull(datecreated);
            if (previous != -1) {
                Assert.assertTrue(previous > datecreated);
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
        QueryResponse response = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":*", 0, 10, null, Collections.singletonList(
                SolrConstants.DC), null);
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
        QueryResponse response = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":*", 0, 10, null, null, Collections
                .singletonList(SolrConstants.PI));
        Assert.assertEquals(10, response.getResults().size());
        for (SolrDocument doc : response.getResults()) {
            Assert.assertEquals(1, doc.getFieldNames().size());
            Assert.assertTrue(doc.getFieldNames().contains(SolrConstants.PI));

        }
    }

    /**
     * @see SolrSearchIndex#getMetadataValues(SolrDocument,String)
     * @verifies return all values for the given field
     */
    @Test
    public void getMetadataValues_shouldReturnAllValuesForTheGivenField() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":PPN517154005", null);
        Assert.assertNotNull(doc);
        List<String> values = SolrSearchIndex.getMetadataValues(doc, SolrConstants.IMAGEURN_OAI);
        Assert.assertEquals(16, values.size());
    }

    /**
     * @see SolrSearchIndex#getFieldValueMap(SolrDocument)
     * @verifies return all fields in the given doc except page urns
     */
    @Test
    public void getFieldValueMap_shouldReturnAllFieldsInTheGivenDocExceptPageUrns() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":PPN517154005", null);
        Assert.assertNotNull(doc);
        Map<String, List<String>> fieldValueMap = SolrSearchIndex.getFieldValueMap(doc);
        Assert.assertEquals(34, fieldValueMap.keySet().size());
    }

    /**
     * @see SolrSearchIndex#searchFacetsAndStatistics(String,List,boolean)
     * @verifies generate facets correctly
     */
    @Test
    public void searchFacetsAndStatistics_shouldGenerateFacetsCorrectly() throws Exception {
        String[] facetFields = { SolrConstants._CALENDAR_YEAR, SolrConstants._CALENDAR_MONTH };
        QueryResponse resp = DataManager.getInstance().getSearchIndex().searchFacetsAndStatistics(SolrConstants._CALENDAR_YEAR + ":*", Arrays.asList(
                facetFields), 0, false);
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
        QueryResponse resp = DataManager.getInstance().getSearchIndex().searchFacetsAndStatistics(SolrConstants._CALENDAR_YEAR + ":*", Arrays.asList(
                facetFields), 0, true);
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
        QueryResponse resp = DataManager.getInstance().getSearchIndex().searchFacetsAndStatistics(SolrConstants._CALENDAR_YEAR + ":*", Collections
                .singletonList(SolrConstants._CALENDAR_YEAR), 0, false);
        Assert.assertTrue(resp.getResults().isEmpty());
    }

    /**
     * @see SolrSearchIndex#getImageOwnerIddoc(String,int)
     * @verifies retrieve correct IDDOC
     */
    @Test
    public void getImageOwnerIddoc_shouldRetrieveCorrectIDDOC() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getImageOwnerIddoc("PPN517154005", 0);
        Assert.assertEquals(1387459019067L, iddoc);
    }

    /**
     * @see SolrSearchIndex#getSolrSortFieldsAsList(String,String)
     * @verifies split fields correctly
     */
    @Test
    public void getSolrSortFieldsAsList_shouldSplitFieldsCorrectly() throws Exception {
        List<StringPair> result = SolrSearchIndex.getSolrSortFieldsAsList("SORT_A; SORT_B, desc;SORT_C,asc", ";", ",");
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("SORT_A", result.get(0).getOne());
        Assert.assertEquals("asc", result.get(0).getTwo());
        Assert.assertEquals("SORT_B", result.get(1).getOne());
        Assert.assertEquals("desc", result.get(1).getTwo());
        Assert.assertEquals("SORT_C", result.get(2).getOne());
        Assert.assertEquals("asc", result.get(2).getTwo());
    }

    /**
     * @see SolrSearchIndex#getSolrSortFieldsAsList(String,String)
     * @verifies split single field correctly
     */
    @Test
    public void getSolrSortFieldsAsList_shouldSplitSingleFieldCorrectly() throws Exception {
        List<StringPair> result = SolrSearchIndex.getSolrSortFieldsAsList("SORT_A , desc ", ";", ",");
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("SORT_A", result.get(0).getOne());
        Assert.assertEquals("desc", result.get(0).getTwo());
    }

    /**
     * @see SolrSearchIndex#getSolrSortFieldsAsList(String,String)
     * @verifies throw IllegalArgumentException if solrSortFields is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSolrSortFieldsIsNull() throws Exception {
        SolrSearchIndex.getSolrSortFieldsAsList(null, ";", ",");
    }

    /**
     * @see SolrSearchIndex#getSolrSortFieldsAsList(String,String,String)
     * @verifies throw IllegalArgumentException if splitFieldsBy is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSplitFieldsByIsNull() throws Exception {
        SolrSearchIndex.getSolrSortFieldsAsList("bla,blup", null, ",");
    }

    /**
     * @see SolrSearchIndex#getSolrSortFieldsAsList(String,String,String)
     * @verifies throw IllegalArgumentException if splitNameOrderBy is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSplitNameOrderByIsNull() throws Exception {
        SolrSearchIndex.getSolrSortFieldsAsList("bla,blup", ";", null);
    }

    /**
     * @see SolrSearchIndex#getFirstDoc(String,List)
     * @verifies return correct doc
     */
    @Test
    public void getFirstDoc_shouldReturnCorrectDoc() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(
                ":PPN517154005 AND ").append(SolrConstants.DOCTYPE).append(":PAGE").toString(), Collections.singletonList(SolrConstants.ORDER));
        Assert.assertNotNull(doc);
        Assert.assertEquals(1, doc.getFieldValue(SolrConstants.ORDER));
    }

    /**
     * @see SolrSearchIndex#getDocumentByIddoc(long)
     * @verifies return correct doc
     */
    @Test
    public void getDocumentByIddoc_shouldReturnCorrectDoc() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByIddoc("1387459019047");
        Assert.assertNotNull(doc);
        Assert.assertEquals("1387459019047", doc.getFieldValue(SolrConstants.IDDOC));
    }

    /**
     * @see SolrSearchIndex#getIddocFromIdentifier(String)
     * @verifies retrieve correct IDDOC
     */
    @Test
    public void getIddocFromIdentifier_shouldRetrieveCorrectIDDOC() throws Exception {
        Assert.assertEquals(1387459019047L, DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("PPN517154005"));
    }

    /**
     * @see SolrSearchIndex#getIdentifierFromIddoc(long)
     * @verifies retrieve correct identifier
     */
    @Test
    public void getIdentifierFromIddoc_shouldRetrieveCorrectIdentifier() throws Exception {
        Assert.assertEquals("PPN517154005", DataManager.getInstance().getSearchIndex().getIdentifierFromIddoc(1387459019047L));
    }

    /**
     * @see SolrSearchIndex#getIddocByLogid(String,String)
     * @verifies retrieve correct IDDOC
     */
    @Test
    public void getIddocByLogid_shouldRetrieveCorrectIDDOC() throws Exception {
        Assert.assertEquals(1387459019066L, DataManager.getInstance().getSearchIndex().getIddocByLogid("PPN517154005", "LOG_0001"));
    }

    @Test
    public void testGetMetadataValuesForLanguage() {
        SolrDocument doc = new SolrDocument();
        doc.addField("field_A", "value_A");
        doc.addField("field_B_LANG_EN", "field_B_en");
        doc.addField("field_B_LANG_DE", "field_B_de");
        doc.addField("field_B_LANG_EN", "field_B_en_2");
        
        Map<String, List<String>> mapA = SolrSearchIndex.getMetadataValuesForLanguage(doc, "field_A");
        Assert.assertEquals(1, mapA.size());
        Assert.assertEquals(1, mapA.get(MultiLanguageMetadataValue.DEFAULT_LANGUAGE).size());
        Assert.assertEquals("value_A", mapA.get(MultiLanguageMetadataValue.DEFAULT_LANGUAGE).get(0));
        
        Map<String, List<String>> mapB = SolrSearchIndex.getMetadataValuesForLanguage(doc, "field_B");
        Assert.assertEquals(2, mapB.size());
        Assert.assertEquals(mapB.get("en").size(), 2);
        Assert.assertEquals(mapB.get("de").size(), 1);
        Assert.assertEquals("field_B_de", mapB.get("de").get(0));
        Assert.assertEquals("field_B_en", mapB.get("en").get(0));
        Assert.assertEquals("field_B_en_2", mapB.get("en").get(1));

    }
    
    @Test
    public void testGetMultiLanguageFieldValueMap() {
        SolrDocument doc = new SolrDocument();
        doc.addField("field_A", "value_A");
        doc.addField("field_B", "value_B");
        doc.addField("field_B_LANG_EN", "field_B_en");
        doc.addField("field_B_LANG_DE", "field_B_de");
        doc.addField("field_B_LANG_EN", "field_B_en_2");
        
        Map<String, List<IMetadataValue>> map = SolrSearchIndex.getMultiLanguageFieldValueMap(doc);
        Assert.assertEquals(2, map.keySet().size());
        Assert.assertEquals("value_A", map.get("field_A").get(0).getValue().get());
        Assert.assertEquals("value_B", map.get("field_B").get(0).getValue().get());
        Assert.assertEquals("field_B_de", map.get("field_B").get(0).getValue("de").get());
        Assert.assertEquals("field_B_en", map.get("field_B").get(0).getValue("en").get());
        Assert.assertEquals("field_B_en_2", map.get("field_B").get(1).getValue("en").get());
        
        Assert.assertEquals("value_B", map.get("field_B").get(0).getValue("fr").orElse(map.get("field_B").get(0).getValue().orElse("")));

    }
    
}