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
package de.intranda.digiverso.presentation.managedbeans;

import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.search.SearchQueryGroup;
import de.intranda.digiverso.presentation.model.search.SearchQueryGroup.SearchQueryGroupOperator;
import de.intranda.digiverso.presentation.model.search.SearchQueryItem;
import de.intranda.digiverso.presentation.model.search.SearchQueryItem.SearchItemOperator;

public class SearchBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see SearchBean#cleanUpSearchTerm(String)
     * @verifies remove illegal chars correctly
     */
    @Test
    public void cleanUpSearchTerm_shouldRemoveIllegalCharsCorrectly() throws Exception {
        Assert.assertEquals("a", SearchBean.cleanUpSearchTerm("(a)"));
    }

    /**
     * @see SearchBean#cleanUpSearchTerm(String)
     * @verifies preserve truncation
     */
    @Test
    public void cleanUpSearchTerm_shouldPreserveTruncation() throws Exception {
        Assert.assertEquals("*a*", SearchBean.cleanUpSearchTerm("*a*"));
    }

    /**
     * @see SearchBean#cleanUpSearchTerm(String)
     * @verifies preserve negation
     */
    @Test
    public void cleanUpSearchTerm_shouldPreserveNegation() throws Exception {
        Assert.assertEquals("-a", SearchBean.cleanUpSearchTerm("-a"));
    }

    /**
     * @see SearchBean#resetSimpleSearchParameters()
     * @verifies reset variables correctly
     */
    @Test
    public void resetSimpleSearchParameters_shouldResetVariablesCorrectly() throws Exception {
        SearchBean searchBean = new SearchBean();
        searchBean.setSearchString("test");
        Assert.assertEquals("test", searchBean.getSearchString());
        Assert.assertEquals("test", searchBean.getSearchStringForUrl());

        searchBean.resetSimpleSearchParameters();
        Assert.assertEquals("", searchBean.getSearchString());
        Assert.assertEquals("-", searchBean.getSearchStringForUrl());
    }

    /**
     * @see SearchBean#resetAdvancedSearchParameters()
     * @verifies reset variables correctly
     */
    @Test
    public void resetAdvancedSearchParameters_shouldResetVariablesCorrectly() throws Exception {
        SearchBean searchBean = new SearchBean();
        searchBean.setAdvancedSearchGroupOperator(1);
        Assert.assertEquals(1, searchBean.getAdvancedSearchGroupOperator());
        searchBean.getAdvancedQueryGroups().add(new SearchQueryGroup(Locale.GERMAN, 1));
        Assert.assertEquals(1, searchBean.getAdvancedQueryGroups().size());

        searchBean.resetAdvancedSearchParameters(2, 3);
        Assert.assertEquals(0, searchBean.getAdvancedSearchGroupOperator());
        Assert.assertEquals(2, searchBean.getAdvancedQueryGroups().size());
        Assert.assertEquals(3, searchBean.getAdvancedQueryGroups().get(0).getQueryItems().size());
    }

    /**
     * @see SearchBean#resetAdvancedSearchParameters()
     * @verifies re-select collection correctly
     */
    @Test
    public void resetAdvancedSearchParameters_shouldReselectCollectionCorrectly() throws Exception {
        SearchBean searchBean = new SearchBean();
        searchBean.getFacets().setCurrentHierarchicalFacetString("DC:col");

        searchBean.resetAdvancedSearchParameters(1, 1);
        Assert.assertEquals(1, searchBean.getAdvancedQueryGroups().size());
        SearchQueryGroup group = searchBean.getAdvancedQueryGroups().get(0);
        Assert.assertEquals(1, group.getQueryItems().size());
        SearchQueryItem item = group.getQueryItems().get(0);
        Assert.assertEquals(SolrConstants.DC, item.getField());
        Assert.assertEquals(SearchItemOperator.IS, item.getOperator());
        Assert.assertEquals("col", item.getValue());
    }

    /**
     * @see SearchBean#resetSearchAction()
     * @verifies return correct Pretty URL ID
     */
    @Test
    public void resetSearchAction_shouldReturnCorrectPrettyURLID() throws Exception {
        SearchBean searchBean = new SearchBean();

        searchBean.setActiveSearchType(0);
        Assert.assertEquals("pretty:search", searchBean.resetSearchAction());
        searchBean.setActiveSearchType(1);
        //        Assert.assertEquals("pretty:extendedsearch", searchBean.resetSearchAction());
        //        searchBean.setActiveSearchType(2);
        //        Assert.assertEquals("pretty:timelinesearch", searchBean.resetSearchAction());
        //        searchBean.setActiveSearchType(3);
        //        Assert.assertEquals("pretty:calendarsearch", searchBean.resetSearchAction());
    }

    /**
     * @see SearchBean#mirrorAdvancedSearchCurrentCollection()
     * @verifies set collection item correctly
     */
    @Test
    public void mirrorAdvancedSearchCurrentCollection_shouldSetCollectionItemCorrectly() throws Exception {
        SearchBean sb = new SearchBean();
        sb.resetAdvancedSearchParameters(2, 3);
        Assert.assertEquals(2, sb.getAdvancedQueryGroups().size());
        SearchQueryGroup group = sb.getAdvancedQueryGroups().get(0);
        Assert.assertEquals(3, group.getQueryItems().size());
        SearchQueryItem item = group.getQueryItems().get(0);
        Assert.assertNull(item.getField());

        sb.getFacets().setCurrentHierarchicalFacetString("DC:a");
        sb.mirrorAdvancedSearchCurrentHierarchicalFacets();
        Assert.assertEquals(SolrConstants.DC, item.getField());
        Assert.assertEquals("a", item.getValue());
    }

    /**
     * @see SearchBean#mirrorAdvancedSearchCurrentCollection()
     * @verifies reset collection item correctly
     */
    @Test
    public void mirrorAdvancedSearchCurrentCollection_shouldResetCollectionItemCorrectly() throws Exception {
        SearchBean sb = new SearchBean();
        sb.resetAdvancedSearchParameters(2, 3);
        Assert.assertEquals(2, sb.getAdvancedQueryGroups().size());
        SearchQueryGroup group = sb.getAdvancedQueryGroups().get(0);
        Assert.assertEquals(3, group.getQueryItems().size());
        SearchQueryItem item = group.getQueryItems().get(0);
        Assert.assertNull(item.getField());

        sb.getFacets().setCurrentHierarchicalFacetString("DC:a");
        sb.mirrorAdvancedSearchCurrentHierarchicalFacets();
        Assert.assertEquals(SolrConstants.DC, item.getField());
        Assert.assertEquals("a", item.getValue());
        sb.getFacets().setCurrentHierarchicalFacetString("-");
        sb.mirrorAdvancedSearchCurrentHierarchicalFacets();
        Assert.assertNull(item.getField());
        Assert.assertNull(item.getValue());
    }

    /**
     * @see SearchBean#mirrorAdvancedSearchCurrentHierarchicalFacets()
     * @verifies mirror facet items to search query items correctly
     */
    @Test
    public void mirrorAdvancedSearchCurrentHierarchicalFacets_shouldMirrorFacetItemsToSearchQueryItemsCorrectly() throws Exception {
        SearchBean sb = new SearchBean();
        sb.resetAdvancedSearchParameters(2, 3);
        Assert.assertEquals(2, sb.getAdvancedQueryGroups().size());
        SearchQueryGroup group = sb.getAdvancedQueryGroups().get(0);
        Assert.assertEquals(3, group.getQueryItems().size());
        SearchQueryItem item1 = group.getQueryItems().get(0);
        Assert.assertNull(item1.getField());
        SearchQueryItem item2 = group.getQueryItems().get(1);
        Assert.assertNull(item2.getField());

        item1.setField(SolrConstants.DC);
        sb.getFacets().setCurrentHierarchicalFacetString("DC:a;;DC:b");
        sb.mirrorAdvancedSearchCurrentHierarchicalFacets();
        Assert.assertEquals(2, sb.getFacets().getCurrentHierarchicalFacets().size());
        Assert.assertEquals(SolrConstants.DC, item1.getField());
        Assert.assertEquals("a", item1.getValue());
        Assert.assertEquals(SolrConstants.DC, item2.getField());
        Assert.assertEquals("b", item2.getValue());
    }

    /**
     * @see SearchBean#mirrorAdvancedSearchCurrentHierarchicalFacets()
     * @verifies not replace query items already in use
     */
    @Test
    public void mirrorAdvancedSearchCurrentHierarchicalFacets_shouldNotReplaceQueryItemsAlreadyInUse() throws Exception {
        SearchBean sb = new SearchBean();
        sb.resetAdvancedSearchParameters(2, 3);
        Assert.assertEquals(2, sb.getAdvancedQueryGroups().size());
        SearchQueryGroup group = sb.getAdvancedQueryGroups().get(0);
        SearchQueryItem item1 = group.getQueryItems().get(0);
        SearchQueryItem item2 = group.getQueryItems().get(1);
        SearchQueryItem item3 = group.getQueryItems().get(2);

        item1.setField("MD_TITLE");
        item1.setValue("text");
        sb.getFacets().setCurrentHierarchicalFacetString("DC:a;;DC:b");
        sb.mirrorAdvancedSearchCurrentHierarchicalFacets();
        Assert.assertEquals("MD_TITLE", item1.getField());
        Assert.assertEquals("text", item1.getValue());
        Assert.assertEquals(SolrConstants.DC, item2.getField());
        Assert.assertEquals("a", item2.getValue());
        Assert.assertEquals(SolrConstants.DC, item3.getField());
        Assert.assertEquals("b", item3.getValue());
    }

    /**
     * @see SearchBean#generateAdvancedSearchString()
     * @verifies construct query correctly
     */
    @Test
    public void generateAdvancedSearchString_shouldConstructQueryCorrectly() throws Exception {
        SearchBean sb = new SearchBean();
        sb.resetAdvancedSearchParameters(2, 2);
        sb.setAdvancedSearchGroupOperator(1);
        {
            // First group
            SearchQueryGroup group = sb.getAdvancedQueryGroups().get(0);
            group.setOperator(SearchQueryGroupOperator.OR);
            {
                // OR-operator, search in all fields
                SearchQueryItem item = group.getQueryItems().get(0);
                item.setOperator(SearchItemOperator.OR);
                item.setField(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS);
                item.setValue("foo bar");
            }
            {
                // AND-operator, search in MD_TITLE with negation
                SearchQueryItem item = group.getQueryItems().get(1);
                item.setOperator(SearchItemOperator.AND);
                item.setField("MD_TITLE");
                item.setValue("bla \"blup\" -nein");
            }
        }
        {
            // Second group
            SearchQueryGroup group = sb.getAdvancedQueryGroups().get(1);
            group.setOperator(SearchQueryGroupOperator.AND);
            {
                // PHASE-operator
                SearchQueryItem item = group.getQueryItems().get(0);
                item.setOperator(SearchItemOperator.PHRASE);
                item.setField(SolrConstants.FULLTEXT);
                item.setValue("lorem ipsum dolor sit amet");
            }
        }
        Assert.assertEquals("((SUPERDEFAULT:(foo OR bar) OR SUPERFULLTEXT:(foo OR bar) OR DEFAULT:(foo OR bar)"
                + " OR FULLTEXT:(foo OR bar) OR NORMDATATERMS:(foo OR bar) OR UGCTERMS:(foo OR bar) OR OVERVIEWPAGE_DESCRIPTION:(foo OR bar) OR OVERVIEWPAGE_PUBLICATIONTEXT:(foo OR bar))"
                + " OR (MD_TITLE:(bla AND \\\"blup\\\" -nein))) OR (((SUPERFULLTEXT:\"lorem ipsum dolor sit amet\" OR FULLTEXT:\"lorem ipsum dolor sit amet\")))",
                sb.generateAdvancedSearchString(true));
    }

    /**
     * @see SearchBean#generateAdvancedSearchString(boolean)
     * @verifies construct query info correctly
     */
    @Test
    public void generateAdvancedSearchString_shouldConstructQueryInfoCorrectly() throws Exception {
        SearchBean sb = new SearchBean();
        sb.resetAdvancedSearchParameters(2, 2);
        sb.setAdvancedSearchGroupOperator(1);
        {
            // First group
            SearchQueryGroup group = sb.getAdvancedQueryGroups().get(0);
            group.setOperator(SearchQueryGroupOperator.OR);
            {
                // OR-operator, search in all fields
                SearchQueryItem item = group.getQueryItems().get(0);
                item.setOperator(SearchItemOperator.OR);
                item.setField(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS);
                item.setValue("foo bar");
            }
            {
                // AND-operator, search in MD_TITLE with negation
                SearchQueryItem item = group.getQueryItems().get(1);
                item.setOperator(SearchItemOperator.AND);
                item.setField("MD_TITLE");
                item.setValue("bla \"blup\" -nein");
            }
        }
        {
            // Second group
            SearchQueryGroup group = sb.getAdvancedQueryGroups().get(1);
            group.setOperator(SearchQueryGroupOperator.AND);
            {
                // PHASE-operator
                SearchQueryItem item = group.getQueryItems().get(0);
                item.setOperator(SearchItemOperator.PHRASE);
                item.setField(SolrConstants.FULLTEXT);
                item.setValue("lorem ipsum dolor sit amet");
            }
        }
        sb.generateAdvancedSearchString(true);
        Assert.assertEquals("(All fields: foo bar OR Title: bla \"blup\" -nein) OR\n<br />(Full text: \"lorem ipsum dolor sit amet\")",
                sb.getAdvancedSearchQueryInfo());
    }

    /**
     * @see SearchBean#getSearchUrl()
     * @verifies return correct url
     */
    @Test
    public void getSearchUrl_shouldReturnCorrectUrl() throws Exception {
        SearchBean sb = new SearchBean();
        NavigationHelper nh = new NavigationHelper();
        sb.setNavigationHelper(nh);

        sb.activeSearchType = SearchHelper.SEARCH_TYPE_ADVANCED;
        Assert.assertEquals(nh.getAdvancedSearchUrl(), sb.getSearchUrl());

        sb.activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
        Assert.assertEquals(nh.getSearchUrl(), sb.getSearchUrl());
    }

    /**
     * @see SearchBean#getSearchUrl()
     * @verifies return null if navigationHelper is null
     */
    @Test
    public void getSearchUrl_shouldReturnNullIfNavigationHelperIsNull() throws Exception {
        SearchBean sb = new SearchBean();
        Assert.assertNull(sb.getSearchUrl());
    }
}