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
package de.intranda.digiverso.presentation.model.search;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;

public class SearchFacetsTest extends AbstractSolrEnabledTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractSolrEnabledTest.setUpClass();
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see SearchFacets#generateFacetPrefix(List,boolean)
     * @verifies encode slashed and backslashes
     */
    @Test
    public void generateFacetPrefix_shouldEncodeSlashedAndBackslashes() throws Exception {
        List<FacetItem> list = new ArrayList<>();
        list.add(new FacetItem("FIELD:a/b\\c", false));
        Assert.assertEquals("FIELD:a/b\\c;;", SearchFacets.generateFacetPrefix(list, false));
        Assert.assertEquals("FIELD:aU002FbU005Cc;;", SearchFacets.generateFacetPrefix(list, true));
    }

    /**
     * @see SearchFacets#parseFacetString(String,List,boolean)
     * @verifies fill list correctly
     */
    @Test
    public void parseFacetString_shouldFillListCorrectly() throws Exception {
        List<FacetItem> facetItems = new ArrayList<>();
        SearchFacets.parseFacetString("DC:a;;DC:b;;MD_TITLE:word;;", facetItems, true);
        Assert.assertEquals(3, facetItems.size());
        Assert.assertEquals("DC", facetItems.get(0).getField());
        Assert.assertEquals("a", facetItems.get(0).getValue());
        Assert.assertEquals("DC", facetItems.get(1).getField());
        Assert.assertEquals("b", facetItems.get(1).getValue());
        Assert.assertEquals("MD_TITLE", facetItems.get(2).getField());
        Assert.assertEquals("word", facetItems.get(2).getValue());
    }

    /**
     * @see SearchFacets#parseFacetString(String,List,boolean)
     * @verifies empty list before filling
     */
    @Test
    public void parseFacetString_shouldEmptyListBeforeFilling() throws Exception {
        List<FacetItem> facetItems = new ArrayList<>();
        SearchFacets.parseFacetString("DC:a;;", facetItems, true);
        Assert.assertEquals(1, facetItems.size());
        SearchFacets.parseFacetString("DC:b;;MD_TITLE:word;;", facetItems, true);
        Assert.assertEquals(2, facetItems.size());
        Assert.assertEquals("DC", facetItems.get(0).getField());
        Assert.assertEquals("b", facetItems.get(0).getValue());
        Assert.assertEquals("MD_TITLE", facetItems.get(1).getField());
        Assert.assertEquals("word", facetItems.get(1).getValue());
    }

    /**
     * @see SearchFacets#parseFacetString(String,List,boolean)
     * @verifies add DC field prefix if no field name is given
     */
    @Test
    public void parseFacetString_shouldAddDCFieldPrefixIfNoFieldNameIsGiven() throws Exception {
        List<FacetItem> facetItems = new ArrayList<>();
        SearchFacets.parseFacetString("collection", facetItems, true);
        Assert.assertEquals(1, facetItems.size());
        Assert.assertEquals(SolrConstants.DC, facetItems.get(0).getField());
        Assert.assertEquals("collection", facetItems.get(0).getValue());
    }

    /**
     * @see SearchFacets#parseFacetString(String,List,boolean)
     * @verifies set hierarchical status correctly
     */
    @Test
    public void parseFacetString_shouldSetHierarchicalStatusCorrectly() throws Exception {
        {
            List<FacetItem> facetItems = new ArrayList<>();
            SearchFacets.parseFacetString("DC:a;;", facetItems, true);
            Assert.assertEquals(1, facetItems.size());
            Assert.assertTrue(facetItems.get(0).isHierarchial());
        }
        {
            List<FacetItem> facetItems = new ArrayList<>();
            SearchFacets.parseFacetString("DC:a;;", facetItems, false);
            Assert.assertEquals(1, facetItems.size());
            Assert.assertFalse(facetItems.get(0).isHierarchial());
        }
    }

    /**
     * @see SearchFacets#getCurrentFacetString()
     * @verifies contain queries from all FacetItems
     */
    @Test
    public void getCurrentFacetString_shouldContainQueriesFromAllFacetItems() throws Exception {
        SearchFacets facets = new SearchFacets();
        for (int i = 0; i < 3; ++i) {
            facets.getCurrentFacets().add(new FacetItem(new StringBuilder().append("FIELD").append(i).append(":value").append(i).toString(), false));
        }
        Assert.assertEquals(3, facets.getCurrentFacets().size());
        String facetString = facets.getCurrentFacetString();
        try {
            facetString = URLDecoder.decode(facetString, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
        }
        Assert.assertNotSame("-", facetString);
        String[] facetStringSplit = facetString.split(";;");
        Assert.assertEquals(3, facetStringSplit.length);
        for (int i = 0; i < 3; ++i) {
            Assert.assertEquals("FIELD" + i + ":value" + i, facetStringSplit[i]);
        }
    }

    /**
     * @see SearchFacets#getCurrentFacetString()
     * @verifies return hyphen if currentFacets empty
     */
    @Test
    public void getCurrentFacetString_shouldReturnHyphenIfCurrentFacetsEmpty() throws Exception {
        SearchFacets facets = new SearchFacets();
        String facetString = facets.getCurrentFacetString();
        Assert.assertEquals("-", facetString);
    }

    /**
     * @see SearchFacets#removeFacetAction(String,String)
     * @verifies remove facet correctly
     */
    @Test
    public void removeFacetAction_shouldRemoveFacetCorrectly() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setCurrentFacetString("DOCSTRCT:a;;MD_TITLE:bob;;MD_TITLE:b;;");
        Assert.assertEquals(3, facets.getCurrentFacets().size());
        facets.removeFacetAction("MD_TITLE:b", null);
        Assert.assertEquals(2, facets.getCurrentFacets().size());
        // Make sure only "MD_TITLE:b" is removed but not facets starting with "MD_TITLE:b"
        Assert.assertEquals("DOCSTRCT%3Aa%3B%3BMD_TITLE%3Abob%3B%3B", facets.getCurrentFacetString());
    }

    /**
     * @see SearchFacets#removeFacetAction(String,String)
     * @verifies remove facet containing reserved chars
     */
    @Test
    public void removeFacetAction_shouldRemoveFacetContainingReservedChars() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setCurrentFacetString("DOCSTRCT:a;;MD_TITLE:bob;;MD_TITLE:{[b]};;");
        Assert.assertEquals(3, facets.getCurrentFacets().size());
        facets.removeFacetAction("MD_TITLE:{[b]}", null);
        Assert.assertEquals(2, facets.getCurrentFacets().size());
        Assert.assertEquals("DOCSTRCT%3Aa%3B%3BMD_TITLE%3Abob%3B%3B", facets.getCurrentFacetString());
    }

    /**
     * @see SearchFacets#removeHierarchicalFacetAction(String,String)
     * @verifies remove facet correctly
     */
    @Test
    public void removeHierarchicalFacetAction_shouldRemoveFacetCorrectly() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setCurrentHierarchicalFacetString("DC:a;;DC:aa;;");
        Assert.assertEquals(2, facets.getCurrentHierarchicalFacets().size());
        facets.removeHierarchicalFacetAction("DC:a", null);
        Assert.assertEquals(1, facets.getCurrentHierarchicalFacets().size());
        // Make sure only "DC:a" is removed but not facets starting with "DC:a"
        Assert.assertEquals("DC:aa;;", facets.getCurrentCollection());
    }

    /**
     * @see SearchFacets#setCurrentFacetString(String)
     * @verifies create FacetItems from all links
     */
    @Test
    public void setCurrentFacetString_shouldCreateFacetItemsFromAllLinks() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setCurrentFacetString("FIELD1:a;;FIELD2:b;;FIELD3:c");
        Assert.assertEquals(3, facets.getCurrentFacets().size());
        Assert.assertEquals("FIELD1", facets.getCurrentFacets().get(0).getField());
        Assert.assertEquals("a", facets.getCurrentFacets().get(0).getValue());
        Assert.assertEquals("FIELD2", facets.getCurrentFacets().get(1).getField());
        Assert.assertEquals("b", facets.getCurrentFacets().get(1).getValue());
        Assert.assertEquals("FIELD3", facets.getCurrentFacets().get(2).getField());
        Assert.assertEquals("c", facets.getCurrentFacets().get(2).getValue());
    }

    /**
     * @see SearchFacets#setCurrentFacetString(String)
     * @verifies decode slashes and backslashes
     */
    @Test
    public void setCurrentFacetString_shouldDecodeSlashesAndBackslashes() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setCurrentFacetString("FIELD:aU002FbU005Cc");
        Assert.assertEquals(1, facets.getCurrentFacets().size());
        Assert.assertEquals("a/b\\c", facets.getCurrentFacets().get(0).getValue());
    }

    /**
     * @see SearchFacets#generateFacetFilterQuery()
     * @verifies generate query correctly
     */
    @Test
    public void generateFacetFilterQuery_shouldGenerateQueryCorrectly() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setCurrentFacetString("FIELD1:a;;FIELD2:b;;FIELD3:[c TO d]");
        Assert.assertEquals("FIELD1:a AND FIELD2:b AND FIELD3:[c TO d]", facets.generateFacetFilterQuery());
    }

    /**
     * @see SearchFacets#generateFacetFilterQuery()
     * @verifies return null if facet list is empty
     */
    @Test
    public void generateFacetFilterQuery_shouldReturnNullIfFacetListIsEmpty() throws Exception {
        SearchFacets facets = new SearchFacets();
        Assert.assertNull(facets.generateFacetFilterQuery());
    }

    /**
     * @see SearchFacets#generateHierarchicalFacetFilterQuery(int)
     * @verifies generate query correctly
     */
    @Test
    public void generateHierarchicalFacetFilterQuery_shouldGenerateQueryCorrectly() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setCurrentHierarchicalFacetString("DC:a;;DC:aa;;");
        Assert.assertEquals("(FACET_DC:\"a\" OR FACET_DC:a.*) AND (FACET_DC:\"aa\" OR FACET_DC:aa.*)",
                facets.generateHierarchicalFacetFilterQuery(0));
        Assert.assertEquals("(FACET_DC:\"a\" OR FACET_DC:a.*) OR (FACET_DC:\"aa\" OR FACET_DC:aa.*)", facets.generateHierarchicalFacetFilterQuery(1));
    }

    /**
     * @see SearchFacets#generateHierarchicalFacetFilterQuery(int)
     * @verifies return null if facet list is empty
     */
    @Test
    public void generateHierarchicalFacetFilterQuery_shouldReturnNullIfFacetListIsEmpty() throws Exception {
        SearchFacets facets = new SearchFacets();
        Assert.assertNull(facets.generateHierarchicalFacetFilterQuery(1));
    }

    /**
     * @see SearchFacets#isHasWrongLanguageCode(String,String)
     * @verifies return true if language code different
     */
    @Test
    public void isHasWrongLanguageCode_shouldReturnTrueIfLanguageCodeDifferent() throws Exception {
        SearchFacets facets = new SearchFacets();
        Assert.assertTrue(facets.isHasWrongLanguageCode("MD_TITLE_LANG_DE", "en"));
    }

    /**
     * @see SearchFacets#isHasWrongLanguageCode(String,String)
     * @verifies return false if language code same
     */
    @Test
    public void isHasWrongLanguageCode_shouldReturnFalseIfLanguageCodeSame() throws Exception {
        SearchFacets facets = new SearchFacets();
        Assert.assertFalse(facets.isHasWrongLanguageCode("MD_TITLE_LANG_DE", "de"));
    }

    /**
     * @see SearchFacets#isHasWrongLanguageCode(String,String)
     * @verifies return false if no language code
     */
    @Test
    public void isHasWrongLanguageCode_shouldReturnFalseIfNoLanguageCode() throws Exception {
        SearchFacets facets = new SearchFacets();
        Assert.assertFalse(facets.isHasWrongLanguageCode("MD_TITLE", "en"));
    }

    /**
     * @see SearchFacets#updateFacetItem(String,String,List,boolean)
     * @verifies update facet item correctly
     */
    @Test
    public void updateFacetItem_shouldUpdateFacetItemCorrectly() throws Exception {
        List<FacetItem> items = new ArrayList<>(2);
        items.add(new FacetItem("FIELD1:foo", false));
        items.add(new FacetItem("FIELD2:bar", false));
        SearchFacets.updateFacetItem("FIELD2", "[foo TO bar]", items, false);
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("FIELD2", items.get(1).getField());
        Assert.assertEquals("foo", items.get(1).getValue());
        Assert.assertEquals("bar", items.get(1).getValue2());
    }

    /**
     * @see SearchFacets#updateFacetItem(String,String,List,boolean)
     * @verifies add new item correctly
     */
    @Test
    public void updateFacetItem_shouldAddNewItemCorrectly() throws Exception {
        List<FacetItem> items = new ArrayList<>(2);
        items.add(new FacetItem("FIELD1:foo", false));
        SearchFacets.updateFacetItem("FIELD2", "bar", items, false);
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("FIELD2", items.get(1).getField());
        Assert.assertEquals("bar", items.get(1).getValue());
    }

    /**
     * @see SearchFacets#populateAbsoluteMinMaxValuesForField(String)
     * @verifies populate values correctly
     */
    @Test
    public void populateAbsoluteMinMaxValuesForField_shouldPopulateValuesCorrectly() throws Exception {
        SearchFacets facets = new SearchFacets();
        List<FacetItem> facetItems = new ArrayList<>(4);
        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":-20", false));
        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":-10", false));
        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":10", false));
        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":2018", false));
        facets.getAvailableFacets().put(SolrConstants._CALENDAR_YEAR, facetItems);
        
        facets.populateAbsoluteMinMaxValuesForField(SolrConstants._CALENDAR_YEAR);
        Assert.assertEquals("-20", facets.getAbsoluteMinRangeValue(SolrConstants._CALENDAR_YEAR));
        Assert.assertEquals("2018", facets.getAbsoluteMaxRangeValue(SolrConstants._CALENDAR_YEAR));

    }


    /**
     * @see SearchFacets#populateAbsoluteMinMaxValuesForField(String)
     * @verifies add all values to list
     */
    @Test
    public void populateAbsoluteMinMaxValuesForField_shouldAddAllValuesToList() throws Exception {
        SearchFacets facets = new SearchFacets();
        List<FacetItem> facetItems = new ArrayList<>(4);
        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":2018", false));
        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":-20", false));
        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":-10", false));
        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":10", false));
        facets.getAvailableFacets().put(SolrConstants._CALENDAR_YEAR, facetItems);
        
        facets.populateAbsoluteMinMaxValuesForField(SolrConstants._CALENDAR_YEAR);
        List<Integer> values = facets.getValueRange(SolrConstants._CALENDAR_YEAR);
        Assert.assertNotNull(values);
        Assert.assertEquals(4, values.size());
        Assert.assertArrayEquals(new Integer[]{-20,  -10, 10, 2018}, values.toArray());
    }
}