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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.search.AdvancedSearchFieldConfiguration;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchQueryGroup;
import io.goobi.viewer.model.search.SearchQueryItem;
import io.goobi.viewer.model.search.SearchQueryItem.SearchItemOperator;
import io.goobi.viewer.model.search.SearchSortingOption;
import io.goobi.viewer.solr.SolrConstants;

class SearchBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    private SearchBean searchBean;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        searchBean = new SearchBean();
    }

    /**
     * @see SearchBean#clearSearchItemLists()
     * @verifies clear map correctly
     */
    @Test
    void clearSearchItemLists_shouldClearMapCorrectly() throws Exception {
        searchBean.getAdvancedSearchSelectItems(SolrConstants.DOCSTRCT, "en", false);
        Assertions.assertFalse(searchBean.getAdvancedSearchSelectItems().isEmpty());
        searchBean.clearSearchItemLists();
        Assertions.assertTrue(searchBean.getAdvancedSearchSelectItems().isEmpty());
    }

    /**
     * @see SearchBean#resetSimpleSearchParameters()
     * @verifies reset variables correctly
     */
    @Test
    void resetSimpleSearchParameters_shouldResetVariablesCorrectly() {
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
    void resetAdvancedSearchParameters_shouldResetVariablesCorrectly() {
        searchBean.resetAdvancedSearchParameters();
        assertEquals(3, searchBean.getAdvancedSearchQueryGroup().getQueryItems().size());
    }

    /**
     * @see SearchBean#resetAdvancedSearchParameters()
     * @verifies re-select collection correctly
     */
    @Test
    void resetAdvancedSearchParameters_shouldReselectCollectionCorrectly() {
        searchBean.getFacets().setActiveFacetString("DC:col");

        searchBean.resetAdvancedSearchParameters();
        assertEquals(3, searchBean.getAdvancedSearchQueryGroup().getQueryItems().size());
        SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
        assertEquals(SolrConstants.DC, item.getField());
        assertEquals(SearchItemOperator.OR, item.getOperator()); // OR is configured for the first line of the default template
        assertEquals("col", item.getValue());
    }

    /**
     * @see SearchBean#resetSearchAction()
     * @verifies return correct Pretty URL ID
     */
    @Test
    void resetSearchAction_shouldReturnCorrectPrettyURLID() {
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
    void mirrorAdvancedSearchCurrentCollection_shouldSetCollectionItemCorrectly() {
        searchBean.resetAdvancedSearchParameters();
        assertEquals(3, searchBean.getAdvancedSearchQueryGroup().getQueryItems().size());
        SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
        assertEquals(SearchHelper.SEARCH_FILTER_ALL.getField(), item.getField());

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
    void mirrorAdvancedSearchCurrentCollection_shouldResetCollectionItemCorrectly() {
        searchBean.resetAdvancedSearchParameters();
        assertEquals(3, searchBean.getAdvancedSearchQueryGroup().getQueryItems().size());
        SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
        assertEquals(SearchHelper.SEARCH_FILTER_ALL.getField(), item.getField());

        searchBean.getFacets().setActiveFacetString("DC:a");
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        assertEquals(SolrConstants.DC, item.getField());
        assertEquals("a", item.getValue());
        searchBean.getFacets().setActiveFacetString("-");
        searchBean.mirrorAdvancedSearchCurrentHierarchicalFacets();
        assertEquals(SolrConstants.DC, item.getField());
        Assertions.assertNull(item.getValue());
    }

    /**
     * @see SearchBean#mirrorAdvancedSearchCurrentHierarchicalFacets()
     * @verifies mirror facet items to search query items correctly
     */
    @Test
    void mirrorAdvancedSearchCurrentHierarchicalFacets_shouldMirrorFacetItemsToSearchQueryItemsCorrectly() {
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
    void mirrorAdvancedSearchCurrentHierarchicalFacets_shouldNotReplaceQueryItemsAlreadyInUse() {
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
    void mirrorAdvancedSearchCurrentHierarchicalFacets_shouldNotAddIdenticalHierarchicalQueryItems() {
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
    void generateSimpleSearchString_shouldGeneratePhraseSearchQueryWithoutFilterCorrectly() {
        searchBean.generateSimpleSearchString("\"foo bar\"");
        assertEquals(
                "SUPERDEFAULT:(\"foo bar\") OR SUPERFULLTEXT:(\"foo bar\") OR SUPERUGCTERMS:(\"foo bar\") OR SUPERSEARCHTERMS_ARCHIVE:(\"foo bar\")"
                        + " OR DEFAULT:(\"foo bar\") OR FULLTEXT:(\"foo bar\") OR NORMDATATERMS:(\"foo bar\") OR UGCTERMS:(\"foo bar\")"
                        + " OR SEARCHTERMS_ARCHIVE:(\"foo bar\") OR CMS_TEXT_ALL:(\"foo bar\")",
                searchBean.getSearchStringInternal());
    }

    /**
     * @see SearchBean#generateSimpleSearchString(String)
     * @verifies generate phrase search query with specific filter correctly
     */
    @Test
    void generateSimpleSearchString_shouldGeneratePhraseSearchQueryWithSpecificFilterCorrectly() {
        searchBean.setCurrentSearchFilterString("filter_FULLTEXT");
        searchBean.generateSimpleSearchString("\"foo bar\"");
        assertEquals("SUPERFULLTEXT:(\"foo bar\") OR FULLTEXT:(\"foo bar\")", searchBean.getSearchStringInternal());
    }

    /**
     * @see SearchBean#generateSimpleSearchString(String)
     * @verifies generate non-phrase search query without filter correctly
     */
    @Test
    void generateSimpleSearchString_shouldGenerateNonphraseSearchQueryWithoutFilterCorrectly() {
        searchBean.generateSimpleSearchString("foo bar");
        assertEquals(
                "SUPERDEFAULT:(foo AND bar) SUPERFULLTEXT:(foo AND bar) SUPERUGCTERMS:(foo AND bar) SUPERSEARCHTERMS_ARCHIVE:(foo AND bar)"
                        + " DEFAULT:(foo AND bar) FULLTEXT:(foo AND bar) NORMDATATERMS:(foo AND bar) UGCTERMS:(foo AND bar)"
                        + " SEARCHTERMS_ARCHIVE:(foo AND bar) CMS_TEXT_ALL:(foo AND bar)",
                searchBean.getSearchStringInternal());
    }

    /**
     * @see SearchBean#generateSimpleSearchString(String)
     * @verifies generate non-phrase search query with specific filter correctly
     */
    @Test
    void generateSimpleSearchString_shouldGenerateNonphraseSearchQueryWithSpecificFilterCorrectly() {
        searchBean.setCurrentSearchFilterString("filter_FULLTEXT");
        searchBean.generateSimpleSearchString("foo bar");
        assertEquals("SUPERFULLTEXT:(foo AND bar) OR FULLTEXT:(foo AND bar)", searchBean.getSearchStringInternal());
    }

    /**
     * @see SearchBean#generateSimpleSearchString(String)
     * @verifies add proximity search token correctly
     */
    @Test
    void generateSimpleSearchString_shouldAddProximitySearchTokenCorrectly() {
        // All
        searchBean.generateSimpleSearchString("\"foo bar\"~20");
        Assertions.assertTrue(searchBean.getSearchStringInternal().contains("SUPERFULLTEXT:(\"foo bar\"~20)"));
        Assertions.assertTrue(searchBean.getSearchStringInternal().contains(" FULLTEXT:(\"foo bar\"~20)"));

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
    void generateSimpleSearchString_shouldResetExactSearchStringIfInputEmpty() {
        searchBean.setExactSearchString("PI:*");
        assertEquals("PI%3A*", searchBean.getExactSearchString());
        searchBean.generateSimpleSearchString("");
        assertEquals("-", searchBean.getExactSearchString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchMainQuery()
     * @verifies construct query correctly
     */
    @Test
    void generateAdvancedSearchMainQuery_shouldConstructQueryCorrectly() {
        searchBean.resetAdvancedSearchParameters();

        // First group
        {
            // OR-operator, search in all fields
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setOperator(SearchItemOperator.OR);
            item.setField(SearchHelper.SEARCH_FILTER_ALL.getField());
            item.setValue("foo bar");
        }
        {
            // AND-operator, search in MD_TITLE with negation
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setOperator(SearchItemOperator.AND);
            item.setField("MD_TITLE");
            item.setValue("bla \"blup\" -nein");
        }

        assertEquals("((SUPERDEFAULT:(foo bar) SUPERFULLTEXT:(foo bar) SUPERUGCTERMS:(foo bar) SUPERSEARCHTERMS_ARCHIVE:(foo bar)"
                + " DEFAULT:(foo bar) FULLTEXT:(foo bar) NORMDATATERMS:(foo bar) UGCTERMS:(foo bar) SEARCHTERMS_ARCHIVE:(foo bar)"
                + " CMS_TEXT_ALL:(foo bar)) +(MD_TITLE:(bla AND \\\"blup\\\" -nein)))",
                searchBean.generateAdvancedSearchMainQuery());
    }

    /**
     * @see SearchBean#generateAdvancedSearchMainQuery(boolean)
     * @verifies construct query info correctly
     */
    @Test
    void generateAdvancedSearchMainQuery_shouldConstructQueryInfoCorrectly() {
        searchBean.resetAdvancedSearchParameters();

        // First group
        {
            // OR-operator, search in all fields
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setOperator(SearchItemOperator.OR);
            item.setField(SearchHelper.SEARCH_FILTER_ALL.getField());
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

        searchBean.generateAdvancedSearchMainQuery();
        assertEquals("OR (Global search: monograph) AND (Title: bla &quot;blup&quot; -nein) NOT (Structure type: Monograph)",
                searchBean.getAdvancedSearchQueryInfo());
    }

    /**
     * @see SearchBean#generateAdvancedSearchMainQuery(boolean)
     * @verifies add multiple facets for the same field correctly
     */
    @Test
    void generateAdvancedSearchMainQuery_shouldAddMultipleFacetsForTheSameFieldCorrectly() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assertions.assertTrue(item.isHierarchical());
        }
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setField(SolrConstants.DC);
            item.setValue("bar");
            Assertions.assertTrue(item.isHierarchical());
        }
        searchBean.generateAdvancedSearchMainQuery();

        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;" + SolrConstants.DC + ":bar;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchMainQuery(boolean)
     * @verifies add multiple facets for the same field correctly if field already in current facets
     */
    @Test
    void generateAdvancedSearchMainQuery_shouldAddMultipleFacetsForTheSameFieldCorrectlyIfFieldAlreadyInActiveFacets() throws Exception {
        searchBean.resetAdvancedSearchParameters();
        searchBean.getFacets().setActiveFacetString(SolrConstants.DC + ":foo;;"); // current facet string already contains this field

        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assertions.assertTrue(item.isHierarchical());
        }
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setField(SolrConstants.DC);
            item.setValue("bar");
            Assertions.assertTrue(item.isHierarchical());
        }
        searchBean.generateAdvancedSearchMainQuery();

        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;" + SolrConstants.DC + ":bar;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchMainQuery(boolean)
     * @verifies only add identical facets once
     */
    @Test
    void generateAdvancedSearchMainQuery_shouldOnlyAddIdenticalFacetsOnce() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assertions.assertTrue(item.isHierarchical());
        }
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assertions.assertTrue(item.isHierarchical());
        }
        searchBean.generateAdvancedSearchMainQuery();

        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchMainQuery(boolean)
     * @verifies not add more facets if field value combo already in current facets
     */
    @Test
    void generateAdvancedSearchMainQuery_shouldNotAddMoreFacetsIfFieldValueComboAlreadyInActiveFacets() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        searchBean.getFacets().setActiveFacetString(SolrConstants.DC + ":foo;;");

        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assertions.assertTrue(item.isHierarchical());
        }
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assertions.assertTrue(item.isHierarchical());
        }
        searchBean.generateAdvancedSearchMainQuery();

        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchMainQuery(boolean)
     * @verifies not replace obsolete facets with duplicates
     */
    @Test
    void generateAdvancedSearchMainQuery_shouldNotReplaceObsoleteFacetsWithDuplicates() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        // Current facets are DC:foo and DC:bar
        searchBean.getFacets().setActiveFacetString(SolrConstants.DC + ":foo;;" + SolrConstants.DC + ":bar;;");

        // Passing DC:foo and DC:foo from the advanced search
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assertions.assertTrue(item.isHierarchical());
        }
        {
            SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(1);
            item.setField(SolrConstants.DC);
            item.setValue("foo");
            Assertions.assertTrue(item.isHierarchical());
        }
        searchBean.generateAdvancedSearchMainQuery();

        // Only one DC:foo should be in the facets
        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#generateAdvancedSearchMainQuery(boolean)
     * @verifies remove facets that are not matched among query items
     */
    @Test
    void generateAdvancedSearchMainQuery_shouldRemoveFacetsThatAreNotMatchedAmongQueryItems() throws Exception {
        searchBean.resetAdvancedSearchParameters();

        searchBean.getFacets().setActiveFacetString(SolrConstants.DC + ":foo;;" + SolrConstants.DC + ":bar;;");
        assertEquals(2, searchBean.getFacets().getActiveFacets().size());
        Assertions.assertTrue(searchBean.getFacets().getActiveFacets().get(0).isHierarchial());

        SearchQueryItem item = searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0);
        item.setField(SolrConstants.DC);
        item.setValue("foo");

        searchBean.generateAdvancedSearchMainQuery();

        assertEquals(URLEncoder.encode(SolrConstants.DC + ":foo;;", StringTools.DEFAULT_ENCODING),
                searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#getSearchUrl()
     * @verifies return correct url
     */
    @Test
    void getSearchUrl_shouldReturnCorrectUrl() {
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
    void getSearchUrl_shouldReturnNullIfNavigationHelperIsNull() {
        Assertions.assertNull(searchBean.getSearchUrl());
    }

    /**
     * @see SearchBean#increaseCurrentHitIndex()
     * @verifies increase index correctly
     */
    @Test
    void increaseCurrentHitIndex_shouldIncreaseIndexCorrectly() {
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
    void increaseCurrentHitIndex_shouldDecreaseIndexCorrectly() {
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
    void increaseCurrentHitIndex_shouldResetOperandAfterwards() {
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
    void increaseCurrentHitIndex_shouldDoNothingIfHitIndexAtTheLastHit() {
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
    void increaseCurrentHitIndex_shouldDoNothingIfHitIndexAt0() {
        searchBean.setCurrentSearch(new Search());
        searchBean.getCurrentSearch().setHitsCount(10);
        searchBean.setHitIndexOperand(-1);
        searchBean.setCurrentHitIndex(0);

        searchBean.increaseCurrentHitIndex();
        assertEquals(0, searchBean.getCurrentHitIndex());
    }

    @Test
    void testGetHierarchicalFacets() {
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
    void getAdvancedSearchAllowedFields_shouldOmitLanguagedFieldsForOtherLanguages() {
        List<AdvancedSearchFieldConfiguration> fields = SearchBean.getAdvancedSearchAllowedFields("en", StringConstants.DEFAULT_NAME, false);
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
                default:
                    break;
            }
        }
        Assertions.assertTrue(en);
        Assertions.assertFalse(de);
        Assertions.assertFalse(es);
    }

    /**
     * @see SearchBean#getAdvancedSearchAllowedFields()
     * @verifies addSearchFilters
     */
    @Test
    void getAdvancedSearchAllowedFields_shouldAddSearchFilters() {
        List<AdvancedSearchFieldConfiguration> fields = SearchBean.getAdvancedSearchAllowedFields("en", StringConstants.DEFAULT_NAME, true);
        assertEquals(17, fields.size());
        assertEquals(SearchHelper.SEARCH_FILTER_ALL.getField(), fields.get(0).getField());
        assertEquals(SolrConstants.DEFAULT, fields.get(1).getField());
        assertEquals(SolrConstants.FULLTEXT, fields.get(2).getField());
        assertEquals(SolrConstants.NORMDATATERMS, fields.get(3).getField());
        assertEquals(SolrConstants.UGCTERMS, fields.get(4).getField());
        assertEquals(SolrConstants.SEARCHTERMS_ARCHIVE, fields.get(5).getField());
        assertEquals(SolrConstants.CMS_TEXT_ALL, fields.get(6).getField());
    }

    /**
     * @see SearchBean#findCurrentHitIndex(String,int,boolean)
     * @verifies set currentHitIndex to minus one if no search hits
     */
    @Test
    void findCurrentHitIndex_shouldSetCurrentHitIndexToMinusOneIfNoSearchHits() {
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
    void findCurrentHitIndex_shouldSetCurrentHitIndexCorrectly() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("search.resultGroups[@enabled]", false);
        searchBean.setCurrentSearch(new Search());
        searchBean.getCurrentSearch().setPage(1);
        searchBean.getCurrentSearch().setQuery("+DC:dcimage* +ISWORK:true -IDDOC_PARENT:*");
        searchBean.getCurrentSearch().setSortString("SORT_TITLE");
        searchBean.getCurrentSearch().execute(new SearchFacets(), null, 10, null, false, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        assertEquals(19, searchBean.getCurrentSearch().getHitsCount());

        searchBean.findCurrentHitIndex("AC03456323", 1, true);
        assertEquals(0, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("PPN9462", 1, true);
        assertEquals(1, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("633114553", 1, true);
        assertEquals(2, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("PPN407465633d27302e312e312e27_40636c6173736e756d3d27312e27_407369673d27313527", 1, true);
        assertEquals(3, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("808996762", 1, true);
        assertEquals(4, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("AC16139576", 1, true);
        assertEquals(5, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("02008011811811", 1, true);
        assertEquals(6, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("ARVIErdm5", 1, true);
        assertEquals(7, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("iiif_test_image", 1, true);
        assertEquals(8, searchBean.getCurrentHitIndex());

        searchBean.findCurrentHitIndex("339471409", 1, true);
        assertEquals(9, searchBean.getCurrentHitIndex());
    }

    /**
     * @see SearchBean#searchSimple()
     * @verifies not reset facets
     */
    @Test
    void searchSimple_shouldNotResetFacets() throws Exception {
        searchBean.getFacets().setActiveFacetString("foo:bar");
        searchBean.searchSimple();
        assertEquals(URLEncoder.encode("foo:bar;;", SearchBean.URL_ENCODING), searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#searchSimple(boolean,boolean)
     * @verifies not reset facets if resetFacets false
     */
    @Test
    void searchSimple_shouldNotResetFacetsIfResetFacetsFalse() throws Exception {
        searchBean.getFacets().setActiveFacetString("foo:bar");
        searchBean.searchSimple(true, false);
        assertEquals(URLEncoder.encode("foo:bar;;", SearchBean.URL_ENCODING), searchBean.getFacets().getActiveFacetString());
    }

    /**
     * @see SearchBean#searchSimple(boolean,boolean)
     * @verifies not produce results if search terms not in index
     */
    @Test
    void searchSimple_shouldNotProduceResultsIfSearchTermsNotInIndex() throws Exception {
        searchBean.setNavigationHelper(new NavigationHelper());

        // Simulate search execution via the quick search widget
        searchBean.setInvisibleSearchString("1234xyz");
        searchBean.searchSimple(true, false);
        // TODO The double escaping that breaks the search cannot be reproduced with way, unfortunately - this test always passes
        searchBean.setExactSearchString(searchBean.getExactSearchString());
        searchBean.search();

        assertEquals(0, searchBean.getCurrentSearch().getHitsCount());
    }

    /**
     * @see SearchBean#getExactSearchString()
     * @verifies url escape string
     */
    @Test
    void getExactSearchString_shouldUrlEscapeString() {
        searchBean.setExactSearchString("PI:*");
        assertEquals("PI%3A*", searchBean.getExactSearchString());
    }

    /**
     * @see SearchBean#getExactSearchString()
     * @verifies escape critical chars
     */
    @Test
    void getExactSearchString_shouldEscapeCriticalChars() {
        searchBean.setExactSearchString("PI:foo/bar");
        assertEquals("PI%3Afoo" + StringTools.SLASH_REPLACEMENT + "bar", searchBean.getExactSearchString());
    }

    /**
     * @see SearchBean#setExactSearchString(String)
     * @verifies perform double unescaping if necessary
     */
    @Test
    void setExactSearchString_shouldPerformDoubleUnescapingIfNecessary() {
        searchBean.setExactSearchString("SUPERDEFAULT%25253A%2525281234xyz%252529");
        assertEquals("SUPERDEFAULT%3A%281234xyz%29", searchBean.getExactSearchString()); // getter should return single encoding
    }

    /**
     * @see SearchBean#getSearchSortingOptions()
     * @verifies return options correctly
     */
    @Test
    void getSearchSortingOptions_shouldReturnOptionsCorrectly() {
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
    void getSearchSortingOptions_shouldUseCurrentRandomSeedOptionInsteadOfDefault() {
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
    void searchAdvanced_shouldGenerateSearchStringCorrectly() {
        Assertions.assertTrue(StringUtils.isEmpty(searchBean.getSearchStringInternal()));
        searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0).setField(SolrConstants.PI);
        searchBean.getAdvancedSearchQueryGroup().getQueryItems().get(0).setValue(PI_KLEIUNIV);
        searchBean.searchAdvanced(false);
        assertEquals("((PI:(PPN517154005)))", searchBean.getSearchStringInternal()); // OR is configured for the first line of the default template
    }

    /**
     * @see SearchBean#searchAdvanced(boolean)
     * @verifies reset search parameters
     */
    @Test
    void searchAdvanced_shouldResetSearchParameters() {
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
    void searchToday_shouldSetSearchStringCorrectly() {
        searchBean.searchToday();
        Assertions.assertTrue(searchBean.getSearchStringInternal().startsWith(SolrConstants.MONTHDAY));
    }

    /**
     * @see SearchBean#setActiveResultGroupName(String)
     * @verifies select result group correctly
     */
    @Test
    void setActiveResultGroupName_shouldSelectResultGroupCorrectly() {
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
    void setActiveResultGroupName_shouldResetResultGroupIfNewNameNotConfigured() {
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
    void setActiveResultGroupName_shouldResetResultGroupIfEmptyNameGiven() {
        SearchBean sb = new SearchBean();
        sb.setActiveResultGroupName("stories");
        assertEquals("stories", sb.getActiveResultGroupName());

        sb.setActiveResultGroupName("-");
        assertEquals("-", sb.getActiveResultGroupName());
    }

    /**
     * @see SearchBean#setActiveResultGroupName(String)
     * @verifies not change hitsPerPageSetterCalled value
     */
    @Test
    void setHitsPerPageNoTrigger_shouldNotChangeHitsPerPageSetterCalledValue() {
        SearchBean sb = new SearchBean();
        Assertions.assertFalse(sb.isHitsPerPageSetterCalled());

        sb.setHitsPerPageNoTrigger(5);
        Assertions.assertFalse(sb.isHitsPerPageSetterCalled());
    }
}
