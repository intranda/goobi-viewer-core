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
package io.goobi.viewer.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.solr.SolrConstants;

public class SearchFacetsTest extends AbstractSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractSolrEnabledTest.setUpClass();
    }

    /**
     * @see SearchFacets#resetActiveFacets()
     * @verifies reset facets correctly
     */
    @Test
    public void resetActiveFacets_shouldResetFacetsCorrectly() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("foo:bar;;");
        Assertions.assertEquals("foo%3Abar%3B%3B", facets.getActiveFacetString());
        facets.resetActiveFacets();
        Assertions.assertEquals("-", facets.getActiveFacetString());
    }

    /**
     * @see SearchFacets#generateFacetPrefix(List,boolean)
     * @verifies encode slashed and backslashes
     */
    @Test
    public void generateFacetPrefix_shouldEncodeSlashedAndBackslashes() throws Exception {
        List<IFacetItem> list = new ArrayList<>();
        list.add(new FacetItem("FIELD:a/b\\c", false));
        Assertions.assertEquals("FIELD:a/b\\c;;", SearchFacets.generateFacetPrefix(list, false));
        Assertions.assertEquals("FIELD:aU002FbU005Cc;;", SearchFacets.generateFacetPrefix(list, true));
    }

    /**
     * @see SearchFacets#parseFacetString(String,List,boolean)
     * @verifies fill list correctly
     */
    @Test
    public void parseFacetString_shouldFillListCorrectly() throws Exception {
        List<IFacetItem> facetItems = new ArrayList<>();
        SearchFacets.parseFacetString("DC:a;;DC:b;;MD_TITLE:word;;", facetItems, null);
        Assertions.assertEquals(3, facetItems.size());
        Assertions.assertEquals("DC", facetItems.get(0).getField());
        Assertions.assertEquals("DC", facetItems.get(1).getField());
        Assertions.assertEquals("b", facetItems.get(1).getValue());
        Assertions.assertEquals("MD_TITLE", facetItems.get(2).getField());
        Assertions.assertEquals("word", facetItems.get(2).getValue());
    }

    /**
     * @see SearchFacets#parseFacetString(String,List,boolean)
     * @verifies empty list before filling
     */
    @Test
    public void parseFacetString_shouldEmptyListBeforeFilling() throws Exception {
        List<IFacetItem> facetItems = new ArrayList<>();
        SearchFacets.parseFacetString("DC:a;;", facetItems, null);
        Assertions.assertEquals(1, facetItems.size());
        SearchFacets.parseFacetString("DC:b;;MD_TITLE:word;;", facetItems, null);
        Assertions.assertEquals(2, facetItems.size());
        Assertions.assertEquals("DC", facetItems.get(0).getField());
        Assertions.assertEquals("b", facetItems.get(0).getValue());
        Assertions.assertEquals("MD_TITLE", facetItems.get(1).getField());
        Assertions.assertEquals("word", facetItems.get(1).getValue());
    }

    /**
     * @see SearchFacets#parseFacetString(String,List,boolean)
     * @verifies add DC field prefix if no field name is given
     */
    @Test
    public void parseFacetString_shouldAddDCFieldPrefixIfNoFieldNameIsGiven() throws Exception {
        List<IFacetItem> facetItems = new ArrayList<>(1);
        SearchFacets.parseFacetString("collection", facetItems, null);
        Assertions.assertEquals(1, facetItems.size());
        Assertions.assertEquals(SolrConstants.DC, facetItems.get(0).getField());
        Assertions.assertEquals("collection", facetItems.get(0).getValue());
    }

    /**
     * @see SearchFacets#parseFacetString(String,List,boolean)
     * @verifies set hierarchical status correctly
     */
    @Test
    public void parseFacetString_shouldSetHierarchicalStatusCorrectly() throws Exception {
        List<IFacetItem> facetItems = new ArrayList<>(1);
        SearchFacets.parseFacetString("DC:a;;", facetItems, null);
        Assertions.assertEquals(1, facetItems.size());
        Assertions.assertTrue(facetItems.get(0).isHierarchial());
    }

    /**
     * @see SearchFacets#parseFacetString(String,List,Map)
     * @verifies use label from labelMap if available
     */
    @Test
    public void parseFacetString_shouldUseLabelFromLabelMapIfAvailable() throws Exception {
        List<IFacetItem> facetItems = new ArrayList<>(1);
        SearchFacets.parseFacetString("FOO:bar;;", facetItems, Collections.singletonMap("FOO:bar", "new label"));
        Assertions.assertEquals(1, facetItems.size());
        Assertions.assertEquals("new label", facetItems.get(0).getLabel());
    }

    /**
     * @see SearchFacets#parseFacetString(String,List,Map)
     * @verifies parse wildcard facets correctly
     */
    @Test
    public void parseFacetString_shouldParseWildcardFacetsCorrectly() throws Exception {
        List<IFacetItem> facetItems = new ArrayList<>(1);
        SearchFacets.parseFacetString("FOO:A*;;", facetItems, null);
        Assertions.assertEquals(1, facetItems.size());
        Assertions.assertEquals("A", facetItems.get(0).getLabel());
        Assertions.assertEquals("A*", facetItems.get(0).getValue());
    }

    /**
     * @see SearchFacets#parseFacetString(String,List,Map)
     * @verifies create multiple items from multiple instances of same field
     */
    @Test
    public void parseFacetString_shouldCreateMultipleItemsFromMultipleInstancesOfSameField() throws Exception {
        List<IFacetItem> facetItems = new ArrayList<>();
        SearchFacets.parseFacetString("YEAR:[a TO b];;YEAR:[c TO d]", facetItems, null);
        Assertions.assertEquals(2, facetItems.size());
        Assertions.assertEquals("YEAR", facetItems.get(0).getField());
        Assertions.assertEquals("YEAR", facetItems.get(1).getField());
        Assertions.assertEquals("a", facetItems.get(0).getValue());
        Assertions.assertEquals("b", facetItems.get(0).getValue2());
        Assertions.assertEquals("c", facetItems.get(1).getValue());
        Assertions.assertEquals("d", facetItems.get(1).getValue2());
    }

    /**
     * @see SearchFacets#getActiveFacetString()
     * @verifies contain queries from all FacetItems
     */
    @Test
    public void getActiveFacetString_shouldContainQueriesFromAllFacetItems() throws Exception {
        SearchFacets facets = new SearchFacets();
        for (int i = 0; i < 3; ++i) {
            facets.getActiveFacets().add(new FacetItem(new StringBuilder().append("FIELD").append(i).append(":value").append(i).toString(), false));
        }
        Assertions.assertEquals(3, facets.getActiveFacets().size());
        String facetString = facets.getActiveFacetString();
        try {
            facetString = URLDecoder.decode(facetString, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            //
        }
        Assertions.assertNotSame("-", facetString);
        String[] facetStringSplit = facetString.split(";;");
        Assertions.assertEquals(3, facetStringSplit.length);
        for (int i = 0; i < 3; ++i) {
            Assertions.assertEquals("FIELD" + i + ":value" + i, facetStringSplit[i]);
        }
    }

    /**
     * @see SearchFacets#getActiveFacetString()
     * @verifies return hyphen if currentFacets empty
     */
    @Test
    public void getActiveFacetString_shouldReturnHyphenIfActiveFacetsEmpty() throws Exception {
        SearchFacets facets = new SearchFacets();
        String facetString = facets.getActiveFacetString();
        Assertions.assertEquals("-", facetString);
    }

    /**
     * @see SearchFacets#removeFacetAction(String,String)
     * @verifies remove facet correctly
     */
    @Test
    public void removeFacetAction_shouldRemoveFacetCorrectly() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("DOCSTRCT:a;;MD_TITLE:bob;;MD_TITLE:b;;");
        Assertions.assertEquals(3, facets.getActiveFacets().size());
        facets.removeFacetAction("MD_TITLE:b", null);
        Assertions.assertEquals(2, facets.getActiveFacets().size());
        // Make sure only "MD_TITLE:b" is removed but not facets starting with "MD_TITLE:b"
        Assertions.assertEquals("DOCSTRCT%3Aa%3B%3BMD_TITLE%3Abob%3B%3B", facets.getActiveFacetString());
    }

    /**
     * @see SearchFacets#removeFacetAction(String,String)
     * @verifies remove facet containing reserved chars
     */
    @Test
    public void removeFacetAction_shouldRemoveFacetContainingReservedChars() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("DOCSTRCT:a;;MD_TITLE:bob;;MD_TITLE:{[b]};;");
        Assertions.assertEquals(3, facets.getActiveFacets().size());
        facets.removeFacetAction("MD_TITLE:{[b]}", null);
        Assertions.assertEquals(2, facets.getActiveFacets().size());
        Assertions.assertEquals("DOCSTRCT%3Aa%3B%3BMD_TITLE%3Abob%3B%3B", facets.getActiveFacetString());
    }

    /**
     * @see SearchFacets#setActiveFacetString(String)
     * @verifies create FacetItems from all links
     */
    @Test
    public void setActiveFacetString_shouldCreateFacetItemsFromAllLinks() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("FIELD1:a;;FIELD2:b;;FIELD3:c");
        Assertions.assertEquals(3, facets.getActiveFacets().size());
        Assertions.assertEquals("FIELD1", facets.getActiveFacets().get(0).getField());
        Assertions.assertEquals("a", facets.getActiveFacets().get(0).getValue());
        Assertions.assertEquals("FIELD2", facets.getActiveFacets().get(1).getField());
        Assertions.assertEquals("b", facets.getActiveFacets().get(1).getValue());
        Assertions.assertEquals("FIELD3", facets.getActiveFacets().get(2).getField());
        Assertions.assertEquals("c", facets.getActiveFacets().get(2).getValue());
    }

    /**
     * @see SearchFacets#setActiveFacetString(String)
     * @verifies decode slashes and backslashes
     */
    @Test
    public void setActiveFacetString_shouldDecodeSlashesAndBackslashes() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("FIELD:aU002FbU005Cc");
        Assertions.assertEquals(1, facets.getActiveFacets().size());
        Assertions.assertEquals("a/b\\c", facets.getActiveFacets().get(0).getValue());
    }

    /**
     * @see SearchFacets#generateSimpleFacetFilterQueries(boolean)
     * @verifies generate query correctly
     */
    @Test
    public void generateSimpleFacetFilterQueries_shouldGenerateQueriesCorrectly() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("MD_FIELD1:a;;FIELD2:b;;YEAR:[c TO d]");
        List<String> result = facets.generateSimpleFacetFilterQueries(true);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("FACET_FIELD1:a", result.get(0));
        Assertions.assertEquals("FIELD2:b", result.get(1));
        Assertions.assertEquals("YEAR:[c TO d]", result.get(2));
    }

    /**
     * @see SearchFacets#generateSimpleFacetFilterQueries(boolean)
     * @verifies return empty list if facet list empty
     */
    @Test
    public void generateSimpleFacetFilterQueries_shouldReturnEmptyListIfFacetListEmpty() throws Exception {
        SearchFacets facets = new SearchFacets();
        Assertions.assertTrue(facets.generateSimpleFacetFilterQueries(true).isEmpty());
    }

    /**
     * @see SearchFacets#generateSimpleFacetFilterQueries(boolean)
     * @verifies skip range facet fields if so requested
     */
    @Test
    public void generateSimpleFacetFilterQueries_shouldSkipRangeFacetFieldsIfSoRequested() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("FIELD1:a;;FIELD2:b;;YEAR:[c TO d]");
        List<String> result = facets.generateSimpleFacetFilterQueries(false);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("FIELD1:a", result.get(0));
        Assertions.assertEquals("FIELD2:b", result.get(1));
    }

    /**
     * @see SearchFacets#generateSimpleFacetFilterQueries(boolean)
     * @verifies skip subelement fields
     */
    @Test
    public void generateSimpleFacetFilterQueries_shouldSkipSubelementFields() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("FIELD1:a;;FIELD2:b;;" + SolrConstants.DOCSTRCT_SUB + ":figure");
        List<String> result = facets.generateSimpleFacetFilterQueries(false);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("FIELD1:a", result.get(0));
        Assertions.assertEquals("FIELD2:b", result.get(1));
    }

    /**
     * @see SearchFacets#generateSimpleFacetFilterQueries(boolean)
     * @verifies skip hierarchical fields
     */
    @Test
    public void generateSimpleFacetFilterQueries_shouldSkipHierarchicalFields() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("FIELD1:a;;FIELD2:b;;DC:foo.bar;;");
        List<String> result = facets.generateSimpleFacetFilterQueries(false);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("FIELD1:a", result.get(0));
        Assertions.assertEquals("FIELD2:b", result.get(1));
    }

    /**
     * @see SearchFacets#generateSimpleFacetFilterQueries(boolean)
     * @verifies combine facet queries if field name same
     */
    @Test
    public void generateSimpleFacetFilterQueries_shouldCombineFacetQueriesIfFieldNameSame() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("FIELD1:a;;FIELD2:b;;YEAR:[c TO d];;YEAR:[e TO f]");
        List<String> result = facets.generateSimpleFacetFilterQueries(true);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("FIELD1:a", result.get(0));
        Assertions.assertEquals("FIELD2:b", result.get(1));
        Assertions.assertEquals("YEAR:[c TO d] AND YEAR:[e TO f]", result.get(2));

        DataManager.getInstance().getConfiguration().overrideValue("search.facets.field(1)[@multiValueOperator]", "OR");
        Assertions.assertEquals("OR", DataManager.getInstance().getConfiguration().getMultiValueOperatorForField("YEAR"));

        result = facets.generateSimpleFacetFilterQueries(true);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("FIELD1:a", result.get(0));
        Assertions.assertEquals("FIELD2:b", result.get(1));
        Assertions.assertEquals("YEAR:[c TO d] YEAR:[e TO f]", result.get(2));
    }

    /**
     * @see SearchFacets#generateSubElementFacetFilterQuery()
     * @verifies generate query correctly
     */
    @Test
    public void generateSubElementFacetFilterQuery_shouldGenerateQueryCorrectly() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("FIELD1:a;;FIELD2:b;;" + SolrConstants.DOCSTRCT_SUB + ":article;;" + SolrConstants.DOCSTRCT_SUB + ":cover;;");
        Assertions.assertEquals("FACET_" + SolrConstants.DOCSTRCT_SUB + ":article AND " + "FACET_" + SolrConstants.DOCSTRCT_SUB + ":cover",
                facets.generateSubElementFacetFilterQuery());
    }

    /**
     * @see SearchFacets#getActiveFacetsForField(String)
     * @verifies return correct items
     */
    @Test
    public void getActiveFacetsForField_shouldReturnCorrectItems() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("FIELD1:a;;FIELD2:b;;");
        List<IFacetItem> result = facets.getActiveFacetsForField("FIELD1");
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("FIELD1", result.get(0).getField());
        Assertions.assertEquals("a", result.get(0).getValue());
    }

    /**
     * @see SearchFacets#isFacetCurrentlyUsed(IFacetItem)
     * @verifies return correct value
     */
    @Test
    public void isFacetCurrentlyUsed_shouldReturnCorrectValue() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("FIELD1:a;;");
        Assertions.assertTrue(facets.isFacetCurrentlyUsed(new FacetItem("FIELD1:a", false)));
        Assertions.assertFalse(facets.isFacetCurrentlyUsed(new FacetItem("FIELD1:b", false)));
    }

    /**
     * @see SearchFacets#isHasRangeFacets()
     * @verifies return correct value
     */
    @Test
    public void isHasRangeFacets_shouldReturnCorrectValue() throws Exception {
        SearchFacets facets = new SearchFacets();
        Assertions.assertFalse(facets.isHasRangeFacets());
        facets.getMinValues().put(SolrConstants.YEAR, "1");
        facets.getMaxValues().put(SolrConstants.YEAR, "10");
        Assertions.assertTrue(facets.isHasRangeFacets());
    }

    /**
     * @see SearchFacets#generateHierarchicalFacetFilterQuery()
     * @verifies generate query correctly
     */
    @Test
    public void generateHierarchicalFacetFilterQuery_shouldGenerateQueryCorrectly() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString("MD_FOO:bar;;DC:a.b;;MD_CREATOR:bob;;");

        String result = facets.generateHierarchicalFacetFilterQuery();
        Assertions.assertEquals("(FACET_DC:\"a.b\" OR FACET_DC:a.b.*) AND (FACET_CREATOR:\"bob\" OR FACET_CREATOR:bob.*)", result);

    }

    /**
     * @see SearchFacets#generateHierarchicalFacetFilterQuery(int)
     * @verifies return null if facet list is empty
     */
    @Test
    public void generateHierarchicalFacetFilterQuery_shouldReturnNullIfFacetListIsEmpty() throws Exception {
        SearchFacets facets = new SearchFacets();
        Assertions.assertNull(facets.generateHierarchicalFacetFilterQuery());
    }

    /**
     * @see SearchFacets#isHasWrongLanguageCode(String,String)
     * @verifies return true if language code different
     */
    @Test
    public void isHasWrongLanguageCode_shouldReturnTrueIfLanguageCodeDifferent() throws Exception {
        SearchFacets facets = new SearchFacets();
        Assertions.assertTrue(facets.isHasWrongLanguageCode("MD_TITLE_LANG_DE", "en"));
    }

    /**
     * @see SearchFacets#isHasWrongLanguageCode(String,String)
     * @verifies return false if language code same
     */
    @Test
    public void isHasWrongLanguageCode_shouldReturnFalseIfLanguageCodeSame() throws Exception {
        SearchFacets facets = new SearchFacets();
        Assertions.assertFalse(facets.isHasWrongLanguageCode("MD_TITLE_LANG_DE", "de"));
    }

    /**
     * @see SearchFacets#isHasWrongLanguageCode(String,String)
     * @verifies return false if no language code
     */
    @Test
    public void isHasWrongLanguageCode_shouldReturnFalseIfNoLanguageCode() throws Exception {
        SearchFacets facets = new SearchFacets();
        Assertions.assertFalse(facets.isHasWrongLanguageCode("MD_TITLE", "en"));
    }

    /**
     * @see SearchFacets#updateFacetItem(String,String,List,boolean)
     * @verifies update facet item correctly
     */
    @Test
    public void updateFacetItem_shouldUpdateFacetItemCorrectly() throws Exception {
        List<IFacetItem> items = new ArrayList<>(2);
        items.add(new FacetItem("FIELD1:foo", false));
        items.add(new FacetItem("FIELD2:bar", false));
        SearchFacets.updateFacetItem("FIELD2", "[foo TO bar]", items, false);
        Assertions.assertEquals(2, items.size());
        Assertions.assertEquals("FIELD2", items.get(1).getField());
        Assertions.assertEquals("foo", items.get(1).getValue());
        Assertions.assertEquals("bar", items.get(1).getValue2());
    }

    /**
     * @see SearchFacets#updateFacetItem(String,String,List,boolean)
     * @verifies add new item correctly
     */
    @Test
    public void updateFacetItem_shouldAddNewItemCorrectly() throws Exception {
        List<IFacetItem> items = new ArrayList<>(2);
        items.add(new FacetItem("FIELD1:foo", false));
        SearchFacets.updateFacetItem("FIELD2", "bar", items, false);
        Assertions.assertEquals(2, items.size());
        Assertions.assertEquals("FIELD2", items.get(1).getField());
        Assertions.assertEquals("bar", items.get(1).getValue());
    }

    /**
     * @see SearchFacets#populateAbsoluteMinMaxValuesForField(String)
     * @verifies populate values correctly
     */
    @Test
    public void populateAbsoluteMinMaxValuesForField_shouldPopulateValuesCorrectly() throws Exception {
        SearchFacets facets = new SearchFacets();
        //        List<FacetItem> facetItems = new ArrayList<>(4);
        //        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":-20", false));
        //        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":-10", false));
        //        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":10", false));
        //        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":2018", false));
        //        facets.getAvailableFacets().put(SolrConstants._CALENDAR_YEAR, facetItems);

        //        String[] values = { "-20", "-10", "10", "2018" };
        SortedMap<String, Long> values = new TreeMap<>(Map.of("-20", 1l, "-10", 1l, "10", 1l, "2018", 1l));
        facets.populateAbsoluteMinMaxValuesForField(SolrConstants.CALENDAR_YEAR, values);
        Assertions.assertEquals("-20", facets.getAbsoluteMinRangeValue(SolrConstants.CALENDAR_YEAR));
        Assertions.assertEquals("2018", facets.getAbsoluteMaxRangeValue(SolrConstants.CALENDAR_YEAR));

    }

    /**
     * @see SearchFacets#populateAbsoluteMinMaxValuesForField(String)
     * @verifies add all values to list
     */
    @Test
    public void populateAbsoluteMinMaxValuesForField_shouldAddAllValuesToList() throws Exception {
        SearchFacets facets = new SearchFacets();
        //        List<FacetItem> facetItems = new ArrayList<>(4);
        //        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":2018", false));
        //        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":-20", false));
        //        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":-10", false));
        //        facetItems.add(new FacetItem(SolrConstants._CALENDAR_YEAR + ":10", false));
        //        facets.getAvailableFacets().put(SolrConstants.CALENDAR_YEAR, facetItems);

        SortedMap<String, Long> values = new TreeMap<>(Map.of("-20", 1l, "-10", 1l, "10", 1l, "2018", 1l));
        facets.populateAbsoluteMinMaxValuesForField(SolrConstants.CALENDAR_YEAR, values);
        List<Integer> valueRange = facets.getValueRange(SolrConstants.CALENDAR_YEAR);
        Assertions.assertNotNull(valueRange);
        Assertions.assertEquals(4, valueRange.size());
        Assertions.assertArrayEquals(new Integer[] { -20, -10, 10, 2018 }, valueRange.toArray());
    }

    @Test
    public void testFacetEscaping()
            throws UnsupportedEncodingException, PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        //original geojson string received from geomap
        String geoJson =
                "{\"type\":\"rectangle\",\"vertices\":[[52.27468490157105,12.831527289994273],[52.78227376368535,12.831527289994273],[52.78227376368535,13.864873763618117],[52.27468490157105,13.864873763618117],[52.27468490157105,12.831527289994273]]}";

        //create SearchFacets with GeoFacetItem
        GeoFacetItem item = new GeoFacetItem("WKT_COORDS");
        SearchFacets facets = new SearchFacets();
        facets.getActiveFacets().add(item);
        facets.setGeoFacetFeature(geoJson);

        //facet string written to url (escaped in browser)
        String urlFacetString = facets.getActiveFacetString();
        String browserFacetString = URLEncoder.encode(urlFacetString, "utf-8");
        //facet string set from url
        facets.setActiveFacetString(browserFacetString);

        String filterQueryString = facets.generateFacetFilterQueries(true).get(0);
        List<SearchHit> hits = SearchHelper.searchWithAggregation("BOOL_WKT_COORDS:*", 0, 100, null, null,
                Collections.singletonList(filterQueryString), null, null, null, null, Locale.GERMANY, false, 0);
        assertEquals(2, hits.size());

    }

    /**
     * @see SearchFacets#isUnselectedValuesAvailable()
     * @verifies return true if a facet field has selectable values
     */
    @Test
    public void isUnselectedValuesAvailable_shouldReturnTrueIfAFacetFieldHasSelectableValues() throws Exception {
        SearchFacets facets = new SearchFacets();
        List<IFacetItem> facetItems = new ArrayList<>(2);
        facetItems.add(new FacetItem("FIELD:foo", "Foo", false));
        facetItems.add(new FacetItem("FIELD:bar", "Bar", false));
        facets.getAvailableFacets().put("FIELD", facetItems);
        Assertions.assertEquals(2, facets.getAvailableFacetsForField("FIELD", true).size());

        facets.setActiveFacetString("FIELD:foo;;");
        Assertions.assertEquals(1, facets.getAvailableFacetsForField("FIELD", true).size());
        Assertions.assertTrue(facets.isUnselectedValuesAvailable());
    }

    /**
     * @see SearchFacets#isUnselectedValuesAvailable()
     * @verifies return false of no selectable values found
     */
    @Test
    public void isUnselectedValuesAvailable_shouldReturnFalseOfNoSelectableValuesFound() throws Exception {
        SearchFacets facets = new SearchFacets();
        List<IFacetItem> facetItems = new ArrayList<>(2);
        facetItems.add(new FacetItem("FIELD:foo", "Foo", false));
        facetItems.add(new FacetItem("FIELD:bar", "Bar", false));
        facets.getAvailableFacets().put("FIELD", facetItems);
        Assertions.assertEquals(2, facets.getAvailableFacetsForField("FIELD", true).size());

        facets.setActiveFacetString("FIELD:foo;;FIELD:bar;;");
        Assertions.assertFalse(facets.isUnselectedValuesAvailable());
    }

    /**
     * @see SearchFacets#isUnselectedValuesAvailable()
     * @verifies return false if only range facets available
     */
    @Test
    public void isUnselectedValuesAvailable_shouldReturnFalseIfOnlyRangeFacetsAvailable() throws Exception {
        SearchFacets facets = new SearchFacets();
        facets.getAvailableFacets().put("FIELD", new ArrayList<>(Collections.singletonList(new FacetItem("FIELD:foo", "Foo", false))));
        facets.getAvailableFacets().put(SolrConstants.YEAR, new ArrayList<>(Collections.singletonList(new FacetItem("YEAR:1980", false))));

        facets.setActiveFacetString("FIELD:foo;;");
        Assertions.assertFalse(facets.isUnselectedValuesAvailable());
    }
}
