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
package io.goobi.viewer.managedbeans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.search.AdvancedSearchFieldConfiguration;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchQueryItem;
import io.goobi.viewer.model.search.SearchQueryItem.SearchItemOperator;
import io.goobi.viewer.model.search.SearchSortingOption;
import io.goobi.viewer.solr.SolrConstants;

public class SearchBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    private SearchBean searchBean;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        searchBean = new SearchBean();
    }

    /**
     * @see SearchBean#clearSearchItemLists()
     * @verifies clear map correctly
     */
    @Test
    public void clearSearchItemLists_shouldClearMapCorrectly() throws Exception {
        searchBean.getAdvancedSearchSelectItems(SolrConstants.DOCSTRCT, "en", false);
        Assert.assertFalse(searchBean.getAdvancedSearchSelectItems().isEmpty());
        searchBean.clearSearchItemLists();
        Assert.assertTrue(searchBean.getAdvancedSearchSelectItems().isEmpty());
    }

    /**
     * @see SearchBean#resetSimpleSearchParameters()
     * @verifies reset variables correctly
     */
    @Test
    public void resetSimpleSearchParameters_shouldResetVariablesCorrectly() throws Exception {
        searchBean.setSearchString("test");
        assertEquals("test", searchBean.getSearchString());
        assertEquals("test", searchBean.getSearchStringForUrl());

        searchBean.resetSimpleSearchParameters();
        assertEquals("", searchBean.getSearchString());
        assertEquals("-", searchBean.getSearchStringForUrl());
    }

    /**
     * @see SearchBean#resetAdvancedSearchParameters()
     * @verifies reset variables correctly
     */
    @Test
    public void resetAdvancedSearchParameters_shouldResetVariablesCorrectly() throws Exception {
        searchBean.resetAdvancedSearchParameters();
        assertEquals(3, searchBean.getAdvancedSearchQueryGroup().getQueryItems().size());
    }

    /**
     * @see SearchBean#resetAdvancedSearchParameters()
     * @verifies re-select collection correctly
     */
    @Test
    public void resetAdvancedSearchParameters_shouldReselectCollectionCorrectly() throws Exception {
        searchBean.getFacets().setActiveFacetString("DC:col");

        searchBean.resetAdvancedSearchParameters();
        assertEquals(3, searchBean.getAdvancedSearchQueryGroup().getQueryItems().size());
        SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
        assertEquals(SolrConstants.DC, item.getField());
        assertEquals(SearchItemOperator.AND, item.getOperator());
        assertEquals("col", item.getValue());
    }

    /**
     * @see SearchBean#resetSearchAction()
     * @verifies return correct Pretty URL ID
     */
    @Test
    public void resetSearchAction_shouldReturnCorrectPrettyURLID() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("search.advanced[@enabled]", true);
        DataManager.getInstance().getConfiguration().overrideValue("search.calendar[@enabled]", true);

        searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_REGULAR);
        assertEquals("pretty:search", searchBean.resetSearchAction());
        searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_ADVANCED);
        assertEquals("pretty:searchadvanced", searchBean.resetSearchAction());
        searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_CALENDAR);
        assertEquals("pretty:searchcalendar", searchBean.resetSearchAction());
    }

    /**
     * @see SearchBean#mirrorAdvancedSearchCurrentCollection()
     * @verifies set collection item correctly
     */
    @Test
    public void mirrorAdvancedSearchCurrentCollection_shouldSetCollectionItemCorrectly() throws Exception {
        searchBean.resetAdvancedSearchParameters();
        assertEquals(3, searchBean.getAdvancedSearchQueryGroup().getQueryItems().size());
        SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
        assertEquals(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS, item.getField());

        searchBean.getFacets().setActiveFacetString("DC:a");
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        assertEquals(SolrConstants.DC, item.getField());
        assertEquals("a", item.getValue());
    }

    /**
     * @see SearchBean#mirrorAdvancedSearchCurrentCollection()
     * @verifies reset collection item correctly
     */
    @Test
    public void mirrorAdvancedSearchCurrentCollection_shouldResetCollectionItemCorrectly() throws Exception {
        searchBean.resetAdvancedSearchParameters();
        assertEquals(3, searchBean.getAdvancedSearchQueryGroup().getQueryItems().size());
        SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
        assertEquals(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS, item.getField());

        searchBean.getFacets().setActiveFacetString("DC:a");
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        assertEquals(SolrConstants.DC, item.getField());
        assertEquals("a", item.getValue());
        searchBean.getFacets().setActiveFacetString("-");
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        assertEquals(SolrConstants.DC, item.getField());
        Assert.assertNull(item.getValue());
    }

    /**
     * @see SearchBean#mirrorAdvancedSearchCurrentHierarchicalFacets()
     * @verifies mirror facet items to search query items correctly
     */
    @Test
    public void mirrorAdvancedSearchCurrentHierarchicalFacets_shouldMirrorFacetItemsToSearchQueryItemsCorrectly() throws Exception {
        searchBean.resetAdvancedSearchParameters();
        assertEquals(3, searchBean.getAdvancedSearchQueryGroup().getQueryItems().size());
        SearchQueryItem item1 = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
        SearchQueryItem item2 = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);

        item1.setField(SolrConstants.DC);
        searchBean.getFacets().setActiveFacetString("DC:a;;DC:b");
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        assertEquals(2, searchBean.getFacets().getActiveFacets().size());
        assertEquals(SolrConstants.DC, item1.getField());
        assertEquals("a", item1.getValue());
        assertEquals(SolrConstants.DC, item2.getField());
        assertEquals("b", item2.getValue());
    }

    /**
     * @see SearchBean#mirrorAdvancedSearchCurrentHierarchicalFacets()
     * @verifies not replace query items already in use
     */
    @Test
    public void mirrorAdvancedSearchCurrentHierarchicalFacets_shouldNotReplaceQueryItemsAlreadyInUse() throws Exception {
        searchBean.resetAdvancedSearchParameters();
        SearchQueryItem item1 = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
        SearchQueryItem item2 = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
        SearchQueryItem item3 = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(2);

        item1.setField("MD_TITLE");
        item1.setValue("text");
        searchBean.getFacets().setActiveFacetString("DC:a;;DC:b");
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        assertEquals("MD_TITLE", item1.getField());
        assertEquals("text", item1.getValue());
        assertEquals(SolrConstants.DC, item2.getField());
        assertEquals("a", item2.getValue());
        assertEquals(SolrConstants.DC, item3.getField());
        assertEquals("b", item3.getValue());
    }

    /**
     * @see SearchBean#mirrorAdvancedSearchCurrentHierarchicalFacets()
     * @verifies not add identical hierarchical query items
     */
    @Test
    public void mirrorAdvancedSearchCurrentHierarchicalFacets_shouldNotAddIdenticalHierarchicalQueryItems() throws Exception {
        searchBean.resetAdvancedSearchParameters();
        SearchQueryItem item1 = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);

        item1.setField(SolrConstants.DC);
        item1.setValue("foo");
        searchBean.getFacets().setActiveFacetString("DC:foo;;DC:foo");
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        // There should be no second query item generated for the other DC:foo
        assertEquals(3, searchBean.getAdvancedSearchQueryGroup().getQueryItems().size());
        assertEquals(SolrConstants.DC, item1.getField());
        assertEquals("foo", item1.getValue());
    }

    /**
     * @see SearchBean#generateSimpleSearchString(String)
     * @verifies generate phrase search query without filter correctly
     */
    @Test
    public void generateSimpleSearchString_shouldGeneratePhraseSearchQueryWithoutFilterCorrectly() throws Exception {
        searchBean.generateSimpleSearchString("\"foo bar\"");
        assertEquals(
                "SUPERDEFAULT:(\"foo bar\") OR SUPERFULLTEXT:(\"foo bar\") OR SUPERUGCTERMS:(\"foo bar\") OR DEFAULT:(\"foo bar\") OR FULLTEXT:(\"foo bar\") OR NORMDATATERMS:(\"foo bar\") OR UGCTERMS:(\"foo bar\") OR CMS_TEXT_ALL:(\"foo bar\")",
                searchBean.getSearchStringInternal());
    }

    /**
     * @see SearchBean#generateSimpleSearchString(String)
     * @verifies generate phrase search query with specific filter correctly
     */
    @Test
    public void generateSimpleSearchString_shouldGeneratePhraseSearchQueryWithSpecificFilterCorrectly() throws Exception {
        searchBean.setCurrentSearchFilterString("filter_FULLTEXT");
        searchBean.generateSimpleSearchString("\"foo bar\"");
        assertEquals("SUPERFULLTEXT:(\"foo bar\") OR FULLTEXT:(\"foo bar\")", searchBean.getSearchStringInternal());
    }

    /**
     * @see SearchBean#generateSimpleSearchString(String)
     * @verifies generate non-phrase search query without filter correctly
     */
    @Test
    public void generateSimpleSearchString_shouldGenerateNonphraseSearchQueryWithoutFilterCorrectly() throws Exception {
        searchBean.generateSimpleSearchString("foo bar");
        assertEquals(
                "SUPERDEFAULT:(foo AND bar) SUPERFULLTEXT:(foo AND bar) SUPERUGCTERMS:(foo AND bar) DEFAULT:(foo AND bar) FULLTEXT:(foo AND bar) NORMDATATERMS:(foo AND bar) UGCTERMS:(foo AND bar) CMS_TEXT_ALL:(foo AND bar)",
                searchBean.getSearchStringInternal());
    }

    /**
     * @see SearchBean#generateSimpleSearchString(String)
     * @verifies generate non-phrase search query with specific filter correctly
     */
    @Test
    public void generateSimpleSearchString_shouldGenerateNonphraseSearchQueryWithSpecificFilterCorrectly() throws Exception {
        searchBean.setCurrentSearchFilterString("filter_FULLTEXT");
        searchBean.generateSimpleSearchString("foo bar");
        assertEquals("SUPERFULLTEXT:(foo AND bar) OR FULLTEXT:(foo AND bar)", searchBean.getSearchStringInternal());
    }

    /**
     * @see SearchBean#generateSimpleSearchString(String)
     * @verifies add proximity search token correctly
     */
    @Test
    public void generateSimpleSearchString_shouldAddProximitySearchTokenCorrectly() throws Exception {
        // All
        searchBean.generateSimpleSearchString("\"foo bar\"~20");
        Assert.assertTrue(searchBean.getSearchStringInternal().contains("SUPERFULLTEXT:(\"foo bar\"~20)"));
        Assert.assertTrue(searchBean.getSearchStringInternal().contains(" FULLTEXT:(\"foo bar\"~20)"));

        // Just full-text
        searchBean.setCurrentSearchFilterString("filter_FULLTEXT");
        searchBean.generateSimpleSearchString("\"foo bar\"~20");
        assertEquals("SUPERFULLTEXT:(\"foo bar\"~20) OR FULLTEXT:(\"foo bar\"~20)", searchBean.getSearchStringInternal());
    }

    /**
     * @see SearchBean#generateSimpleSearchString(String)
     * @verifies reset exactSearchString if input empty
     */
    @Test
    public void generateSimpleSearchString_shouldResetExactSearchStringIfInputEmpty() throws Exception {
        searchBean.setExactSearchString("PI:*");
        assertEquals("PI%3A*", searchBean.getExactSearchString());
        searchBean.generateSimpleSearchString("");
        assertEquals("-", searchBean.getExactSearchString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchString()
     * @verifies construct query correctly
     */
    @Test
    public void generateAdvancedSearchString_shouldConstructQueryCorrectly() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        // First group
        {
            // OR-operator, search in all fields
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setOperator(SearchItemOperator.OR);
            item.setField(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS);
            item.setValue("foo bar");
        }
        {
            // AND-operator, search in MD_TITLE with negation
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setOperator(SearchItemOperator.AND);
            item.setField("MD_TITLE");
            item.setValue("bla \"blup\" -nein");
        }

        assertEquals("((SUPERDEFAULT:(foo bar) SUPERFULLTEXT:(foo bar) SUPERUGCTERMS:(foo bar) DEFAULT:(foo bar)"
                + " FULLTEXT:(foo bar) NORMDATATERMS:(foo bar) UGCTERMS:(foo bar) CMS_TEXT_ALL:(foo bar)) +(MD_TITLE:(bla AND \\\"blup\\\" -nein)))",
                searchBean.generateAdvancedSearchString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchString(boolean)
     * @verifies construct query info correctly
     */
    @Test
    public void generateAdvancedSearchString_shouldConstructQueryInfoCorrectly() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        // First group
        {
            // OR-operator, search in all fields
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setOperator(SearchItemOperator.OR);
            item.setField(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS);
            item.setValue("monograph"); // should NOT be translated
        }
        {
            // AND-operator, search in MD_TITLE with negation
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setOperator(SearchItemOperator.AND);
            item.setField("MD_TITLE");
            item.setValue("bla \"blup\" -nein");
        }
        {
            // NOT-operator, search in DOCSTRCT with negation
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(2);
            item.setOperator(SearchItemOperator.NOT);
            item.setField("DOCSTRCT");
            item.setValue("monograph"); // should be translated
        }

        searchBean.generateAdvancedSearchString();
        assertEquals("OR (All fields: monograph) AND (Title: bla &quot;blup&quot; -nein) NOT (Structure type: Monograph)",
                searchBean.getAdvancedSearchQueryInfo());
    }

    /**
     * @see SearchBean#generateAdvancedSearchString(boolean)
     * @verifies add multiple facets for the same field correctly
     */
    @Test
    public void generateAdvancedSearchString_shouldAddMultipleFacetsForTheSameFieldCorrectly() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assert.assertTrue(item.isHierarchical());
        }
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setField(SolrConstants.DC);
            item.setValue("bar");
            Assert.assertTrue(item.isHierarchical());
        }
        searchBean.generateAdvancedSearchString();

        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;" + SolrConstants.DC + ":bar;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchString(boolean)
     * @verifies add multiple facets for the same field correctly if field already in current facets
     */
    @Test
    public void generateAdvancedSearchString_shouldAddMultipleFacetsForTheSameFieldCorrectlyIfFieldAlreadyInActiveFacets() throws Exception {
        searchBean.resetAdvancedSearchParameters();
        searchBean.getFacets().setActiveFacetString(SolrConstants.DC + ":foo;;"); // current facet string already contains this field

        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assert.assertTrue(item.isHierarchical());
        }
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setField(SolrConstants.DC);
            item.setValue("bar");
            Assert.assertTrue(item.isHierarchical());
        }
        searchBean.generateAdvancedSearchString();

        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;" + SolrConstants.DC + ":bar;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchString(boolean)
     * @verifies only add identical facets once
     */
    @Test
    public void generateAdvancedSearchString_shouldOnlyAddIdenticalFacetsOnce() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assert.assertTrue(item.isHierarchical());
        }
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assert.assertTrue(item.isHierarchical());
        }
        searchBean.generateAdvancedSearchString();

        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchString(boolean)
     * @verifies not add more facets if field value combo already in current facets
     */
    @Test
    public void generateAdvancedSearchString_shouldNotAddMoreFacetsIfFieldValueComboAlreadyInActiveFacets() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        searchBean.getFacets().setActiveFacetString(SolrConstants.DC + ":foo;;");

        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assert.assertTrue(item.isHierarchical());
        }
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assert.assertTrue(item.isHierarchical());
        }
        searchBean.generateAdvancedSearchString();

        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchString(boolean)
     * @verifies not replace obsolete facets with duplicates
     */
    @Test
    public void generateAdvancedSearchString_shouldNotReplaceObsoleteFacetsWithDuplicates() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        // Current facets are DC:foo and DC:bar
        searchBean.getFacets().setActiveFacetString(SolrConstants.DC + ":foo;;" + SolrConstants.DC + ":bar;;");

        // Passing DC:foo and DC:foo from the advanced search
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assert.assertTrue(item.isHierarchical());
        }
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assert.assertTrue(item.isHierarchical());
        }
        searchBean.generateAdvancedSearchString();

        // Only one DC:foo should be in the facets
        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchString(boolean)
     * @verifies remove facets that are not matched among query items
     */
    @Test
    public void generateAdvancedSearchString_shouldRemoveFacetsThatAreNotMatchedAmongQueryItems() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        searchBean.getFacets().setActiveFacetString(SolrConstants.DC + ":foo;;" + SolrConstants.DC + ":bar;;");
        assertEquals(2, searchBean.getFacets().getActiveFacets().size());
        Assert.assertTrue(searchBean.getFacets().getActiveFacets().get(0).isHierarchial());

        SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
        item.setField(SolrConstants.DC);
        item.setValue("foo");

        searchBean.generateAdvancedSearchString();

        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#getSearchUrl()
     * @verifies return correct url
     */
    @Test
    public void getSearchUrl_shouldReturnCorrectUrl() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        searchBean.setNavigationHelper(nh);

        searchBean.setActiveSearchTypeTest(SearchHelper.SEARCH_TYPE_ADVANCED);
        assertEquals(nh.getAdvancedSearchUrl(), searchBean.getSearchUrl());

        searchBean.setActiveSearchTypeTest(SearchHelper.SEARCH_TYPE_REGULAR);
        assertEquals(nh.getSearchUrl(), searchBean.getSearchUrl());
    }

    /**
     * @see SearchBean#getSearchUrl()
     * @verifies return null if navigationHelper is null
     */
    @Test
    public void getSearchUrl_shouldReturnNullIfNavigationHelperIsNull() throws Exception {
        Assert.assertNull(searchBean.getSearchUrl());
    }

    /**
     * @see SearchBean#increaseCurrentHitIndex()
     * @verifies increase index correctly
     */
    @Test
    public void increaseCurrentHitIndex_shouldIncreaseIndexCorrectly() throws Exception {
        searchBean.setCurrentSearch(new Search());
        searchBean.getCurrentSearch().setHitsCount(10);

        // Regular case
        searchBean.setHitIndexOperand(1);
        searchBean.setCurrentHitIndex(6);
        searchBean.increaseCurrentHitIndex();
        assertEquals(7, searchBean.getCurrentHitIndex());

        // Edge case (min)
        searchBean.setHitIndexOperand(1);
        searchBean.setCurrentHitIndex(0);
        searchBean.increaseCurrentHitIndex();
        assertEquals(1, searchBean.getCurrentHitIndex());

        // Edge case (max)
        searchBean.setHitIndexOperand(1);
        searchBean.setCurrentHitIndex(8);
        searchBean.increaseCurrentHitIndex();
        assertEquals(9, searchBean.getCurrentHitIndex());
    }

    /**
     * @see SearchBean#increaseCurrentHitIndex()
     * @verifies decrease index correctly
     */
    @Test
    public void increaseCurrentHitIndex_shouldDecreaseIndexCorrectly() throws Exception {
        searchBean.setCurrentSearch(new Search());
        searchBean.getCurrentSearch().setHitsCount(10);

        // Regular case
        searchBean.setHitIndexOperand(-1);
        searchBean.setCurrentHitIndex(6);
        searchBean.increaseCurrentHitIndex();
        assertEquals(5, searchBean.getCurrentHitIndex());

        // Edge case (min)
        searchBean.setHitIndexOperand(-1);
        searchBean.setCurrentHitIndex(1);
        searchBean.increaseCurrentHitIndex();
        assertEquals(0, searchBean.getCurrentHitIndex());

        // Edge case (max)
        searchBean.setHitIndexOperand(-1);
        searchBean.setCurrentHitIndex(9);
        searchBean.increaseCurrentHitIndex();
        assertEquals(8, searchBean.getCurrentHitIndex());
    }

    /**
     * @see SearchBean#increaseCurrentHitIndex()
     * @verifies reset operand afterwards
     */
    @Test
    public void increaseCurrentHitIndex_shouldResetOperandAfterwards() throws Exception {
        searchBean.setCurrentSearch(new Search());
        searchBean.getCurrentSearch().setHitsCount(10);

        searchBean.setHitIndexOperand(1);
        searchBean.setCurrentHitIndex(6);
        assertEquals(1, searchBean.getHitIndexOperand());
        searchBean.increaseCurrentHitIndex();
        assertEquals(0, searchBean.getHitIndexOperand());
    }

    /**
     * @see SearchBean#increaseCurrentHitIndex()
     * @verifies do nothing if hit index at the last hit
     */
    @Test
    public void increaseCurrentHitIndex_shouldDoNothingIfHitIndexAtTheLastHit() throws Exception {
        searchBean.setCurrentSearch(new Search());
        searchBean.getCurrentSearch().setHitsCount(10);
        searchBean.setHitIndexOperand(1);
        searchBean.setCurrentHitIndex(9);

        searchBean.increaseCurrentHitIndex();
        assertEquals(9, searchBean.getCurrentHitIndex());
    }

    /**
     * @see SearchBean#increaseCurrentHitIndex()
     * @verifies do nothing if hit index at 0
     */
    @Test
    public void increaseCurrentHitIndex_shouldDoNothingIfHitIndexAt0() throws Exception {
        searchBean.setCurrentSearch(new Search());
        searchBean.getCurrentSearch().setHitsCount(10);
        searchBean.setHitIndexOperand(-1);
        searchBean.setCurrentHitIndex(0);

        searchBean.increaseCurrentHitIndex();
        assertEquals(0, searchBean.getCurrentHitIndex());
    }

    @Test
    public void testGetHierarchicalFacets() {
        String facetString = "DC:sonstiges.ocr.antiqua;;DOCSTRCT:monograph;;MD_TOPICS_UNTOKENIZED:schulbuch";
        List<String> hierarchicalFacetFields = Arrays.asList(new String[] { "A", "MD_TOPICS", "B", "DC", "C" });

        List<String> facets = SearchFacets.getHierarchicalFacets(facetString, hierarchicalFacetFields);
        assertEquals(2, facets.size());
        assertEquals("sonstiges.ocr.antiqua", facets.get(1));
        assertEquals("schulbuch", facets.get(0));
    }

    /**
     * @see SearchBean#getAdvancedSearchAllowedFields()
     * @verifies omit languaged fields for other languages
     */
    @Test
    public void getAdvancedSearchAllowedFields_shouldOmitLanguagedFieldsForOtherLanguages() throws Exception {
        List<AdvancedSearchFieldConfiguration> fields = SearchBean.getAdvancedSearchAllowedFields("en", StringConstants.DEFAULT_NAME);
        boolean en = false;
        boolean de = false;
        boolean es = false;
        for (AdvancedSearchFieldConfiguration field : fields) {
            switch (field.getField()) {
                case "MD_FOO_LANG_EN":
                    en = true;
                    break;
                case "MD_FOO_LANG_DE":
                    de = true;
                    break;
                case "MD_FOO_LANG_ES":
                    es = true;
                    break;
            }
        }
        Assert.assertTrue(en);
        Assert.assertFalse(de);
        Assert.assertFalse(es);
    }

    /**
     * @see SearchBean#findCurrentHitIndex(String,int,boolean)
     * @verifies set currentHitIndex to minus one if no search hits
     */
    @Test
    public void findCurrentHitIndex_shouldSetCurrentHitIndexToMinusOneIfNoSearchHits() throws Exception {
        searchBean.findCurrentHitIndex("PPN123", 1, true);
        assertEquals(-1, searchBean.getCurrentHitIndex());

        searchBean.setCurrentSearch(new Search());
        assertEquals(0, searchBean.getCurrentSearch().getHitsCount());
        searchBean.findCurrentHitIndex("PPN123", 1, true);
        assertEquals(-1, searchBean.getCurrentHitIndex());
    }

    /**
     * @see SearchBean#findCurrentHitIndex(String,int,boolean)
     * @verifies set currentHitIndex correctly
     */
    @Test
    public void findCurrentHitIndex_shouldSetCurrentHitIndexCorrectly() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("search.resultGroups[@enabled]", false);
        searchBean.setCurrentSearch(new Search());
        searchBean.getCurrentSearch().setPage(1);
        searchBean.getCurrentSearch().setQuery("+DC:dcimage* +ISWORK:true -IDDOC_PARENT:*");
        searchBean.getCurrentSearch().setSortString("SORT_TITLE");
        searchBean.getCurrentSearch().execute(new SearchFacets(), null, 10, null, false, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        assertEquals(18, searchBean.getCurrentSearch().getHitsCount());

        searchBean.findCurrentHitIndex("PPN9462", 1, true);
        assertEquals(0, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("633114553", 1, true);
        assertEquals(1, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("PPN407465633d27302e312e312e27_40636c6173736e756d3d27312e27_407369673d27313527", 1, true);
        assertEquals(2, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("808996762", 1, true);
        assertEquals(3, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("02008011811811", 1, true);
        assertEquals(4, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("iiif_test_image", 1, true);
        assertEquals(6, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("339471409", 1, true);
        assertEquals(7, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("02008012412069", 1, true);
        assertEquals(8, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("02008012412076", 1, true);
        assertEquals(9, searchBean.getCurrentHitIndex());
    }

    /**
     * @see SearchBean#searchSimple()
     * @verifies not reset facets
     */
    @Test
    public void searchSimple_shouldNotResetFacets() throws Exception {
        searchBean.getFacets().setActiveFacetString("foo:bar");
        searchBean.searchSimple();
        assertEquals(URLEncoder.encode("foo:bar;;", SearchBean.URL_ENCODING), searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#searchSimple(boolean,boolean)
     * @verifies not reset facets if resetFacets false
     */
    @Test
    public void searchSimple_shouldNotResetFacetsIfResetFacetsFalse() throws Exception {
        searchBean.getFacets().setActiveFacetString("foo:bar");
        searchBean.searchSimple(true, false);
        assertEquals(URLEncoder.encode("foo:bar;;", SearchBean.URL_ENCODING), searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#searchSimple(boolean,boolean)
     * @verifies not produce results if search terms not in index
     */
    @Test
    public void searchSimple_shouldNotProduceResultsIfSearchTermsNotInIndex() throws Exception {
        searchBean.setNavigationHelper(new NavigationHelper());

        // Simulate search execution via the quick search widget
        searchBean.setInvisibleSearchString("1234xyz");
        searchBean.searchSimple(true, false);
        searchBean.setExactSearchString(searchBean.getExactSearchString()); // TODO The double escaping that breaks the search cannot be reproduced with way, unfortunately - this test always passes
        searchBean.search();

        assertEquals(0, searchBean.getCurrentSearch().getHitsCount());
    }

    /**
     * @see SearchBean#getExactSearchString()
     * @verifies url escape string
     */
    @Test
    public void getExactSearchString_shouldUrlEscapeString() throws Exception {
        searchBean.setExactSearchString("PI:*");
        assertEquals("PI%3A*", searchBean.getExactSearchString());
    }

    /**
     * @see SearchBean#getExactSearchString()
     * @verifies escape critical chars
     */
    @Test
    public void getExactSearchString_shouldEscapeCriticalChars() throws Exception {
        searchBean.setExactSearchString("PI:foo/bar");
        assertEquals("PI%3Afoo" + StringTools.SLASH_REPLACEMENT + "bar", searchBean.getExactSearchString());
    }

    /**
     * @see SearchBean#setExactSearchString(String)
     * @verifies perform double unescaping if necessary
     */
    @Test
    public void setExactSearchString_shouldPerformDoubleUnescapingIfNecessary() throws Exception {
        searchBean.setExactSearchString("SUPERDEFAULT%25253A%2525281234xyz%252529");
        assertEquals("SUPERDEFAULT%3A%281234xyz%29", searchBean.getExactSearchString()); // getter should return single encoding
    }

    /**
     * @see SearchBean#getSearchSortingOptions()
     * @verifies return options correctly
     */
    @Test
    public void getSearchSortingOptions_shouldReturnOptionsCorrectly() throws Exception {
        Collection<SearchSortingOption> options = searchBean.getSearchSortingOptions("en");
        String defaultSorting = DataManager.getInstance().getConfiguration().getDefaultSortField("en");
        assertEquals("SORT_TITLE_LANG_EN", defaultSorting);
        List<String> sortStrings = DataManager.getInstance().getConfiguration().getSortFields();
        assertEquals(sortStrings.size() * 2 - 4, options.size());
        Iterator<SearchSortingOption> iterator = options.iterator();
        assertEquals(defaultSorting, iterator.next().getField());
        //        assertEquals("Relevance", iterator.next().getLabel());
        //        assertEquals("Creator ascending", iterator.next().getLabel());
        //        assertEquals("Creator descending", iterator.next().getLabel());
    }

    /**
     * @see SearchBean#getSearchSortingOptions()
     * @verifies use current random seed option instead of default
     */
    @Test
    public void getSearchSortingOptions_shouldUseCurrentRandomSeedOptionInsteadOfDefault() throws Exception {
        searchBean.setSearchSortingOption(new SearchSortingOption("random_12345"));
        Collection<SearchSortingOption> options = searchBean.getSearchSortingOptions(null);
        Iterator<SearchSortingOption> iterator = options.iterator();
        boolean found = false;
        while (iterator.hasNext()) {
            if ("random_12345".equals(iterator.next().getField())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    /**
     * @see SearchBean#searchAdvanced(boolean)
     * @verifies generate search string correctly
     */
    @Test
    public void searchAdvanced_shouldGenerateSearchStringCorrectly() throws Exception {
        Assert.assertTrue(StringUtils.isEmpty(searchBean.getSearchStringInternal()));
        searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0).setField(SolrConstants.PI);
        searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0).setValue(PI_KLEIUNIV);
        searchBean.searchAdvanced(false);
        assertEquals("(+(PI:(PPN517154005)))", searchBean.getSearchStringInternal());
    }

    /**
     * @see SearchBean#searchAdvanced(boolean)
     * @verifies reset search parameters
     */
    @Test
    public void searchAdvanced_shouldResetSearchParameters() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("search.advanced[@enabled]", true);
        searchBean.setActiveSearchType(1);
        assertEquals(1, searchBean.getActiveSearchType());
        searchBean.setCurrentPage(2);
        searchBean.searchAdvanced(true);
        assertEquals(1, searchBean.getCurrentPage());
    }

    /**
     * @see SearchBean#searchToday()
     * @verifies set search string correctly
     */
    @Test
    public void searchToday_shouldSetSearchStringCorrectly() throws Exception {
        searchBean.searchToday();
        Assert.assertTrue(searchBean.getSearchStringInternal().startsWith(SolrConstants.MONTHDAY));
    }

    /**
     * @see SearchBean#setActiveResultGroupName(String)
     * @verifies select result group correctly
     */
    @Test
    public void setActiveResultGroupName_shouldSelectResultGroupCorrectly() throws Exception {
        SearchBean sb = new SearchBean();
        assertEquals("-", sb.getActiveResultGroupName());

        sb.setActiveResultGroupName("stories");
        assertEquals("stories", sb.getActiveResultGroupName());
    }

    /**
     * @see SearchBean#setActiveResultGroupName(String)
     * @verifies reset result group if new name not configured
     */
    @Test
    public void setActiveResultGroupName_shouldResetResultGroupIfNewNameNotConfigured() throws Exception {
        SearchBean sb = new SearchBean();
        sb.setActiveResultGroupName("stories");
        assertEquals("stories", sb.getActiveResultGroupName());

        sb.setActiveResultGroupName("notfound");
        assertEquals("-", sb.getActiveResultGroupName());
    }

    /**
     * @see SearchBean#setActiveResultGroupName(String)
     * @verifies reset result group if empty name given
     */
    @Test
    public void setActiveResultGroupName_shouldResetResultGroupIfEmptyNameGiven() throws Exception {
        SearchBean sb = new SearchBean();
        sb.setActiveResultGroupName("stories");
        assertEquals("stories", sb.getActiveResultGroupName());

        sb.setActiveResultGroupName("-");
        assertEquals("-", sb.getActiveResultGroupName());
    }

    /**
     * @see SearchBean#setActiveResultGroupName(String)
     * @verifies reset advanced search query items if new group used as field template
     */
    @Test
    public void setActiveResultGroupName_shouldResetAdvancedSearchQueryItemsIfNewGroupUsedAsFieldTemplate() throws Exception {
        SearchBean sb = new SearchBean();
        sb.setActiveResultGroupName("stories");

        List<SearchQueryItem> items = sb.getAdvancedSearchQueryGroup().getQueryItems();
        Assert.assertFalse(items.isEmpty());
        items.get(0).setOperator(SearchItemOperator.NOT);
        items.get(0).setValue("foo bar");

        // Same group, no reset
        sb.setActiveResultGroupName("stories");
        assertEquals(SearchItemOperator.NOT, items.get(0).getOperator());
        assertEquals("foo bar", items.get(0).getValue());

        // Non-template group, no reset
        sb.setActiveResultGroupName("monographs");
        assertEquals(SearchItemOperator.NOT, items.get(0).getOperator());
        assertEquals("foo bar", items.get(0).getValue());

        // Template group, reset
        sb.setActiveResultGroupName("lido_objects");
        assertEquals(SearchItemOperator.AND, items.get(0).getOperator());
        Assert.assertNull(items.get(0).getValue());
    }

    /**
     * @see SearchBean#setActiveResultGroupName(String)
     * @verifies reset advanced search query items if old group used as field template
     */
    @Test
    public void setActiveResultGroupName_shouldResetAdvancedSearchQueryItemsIfOldGroupUsedAsFieldTemplate() throws Exception {
        SearchBean sb = new SearchBean();
        sb.setActiveResultGroupName("lido_objects");

        List<SearchQueryItem> items = sb.getAdvancedSearchQueryGroup().getQueryItems();
        Assert.assertFalse(items.isEmpty());
        items.get(0).setOperator(SearchItemOperator.NOT);
        items.get(0).setValue("foo bar");

        // No group, reset
        sb.setActiveResultGroupName("-");
        assertEquals(SearchItemOperator.AND, items.get(0).getOperator());
        Assert.assertNull(items.get(0).getValue());
    }
}
