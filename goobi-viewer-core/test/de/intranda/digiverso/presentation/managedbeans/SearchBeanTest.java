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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.model.search.FacetItem;
import de.intranda.digiverso.presentation.model.search.SearchQueryGroup;
import de.intranda.digiverso.presentation.model.search.SearchQueryGroup.SearchQueryGroupOperator;
import de.intranda.digiverso.presentation.model.search.SearchQueryItem;
import de.intranda.digiverso.presentation.model.search.SearchQueryItem.SearchItemOperator;

public class SearchBeanTest {
    /**
     * @see SearchBean#getCurrentFacetString()
     * @verifies contain queries from all FacetItems
     */
    @Test
    public void getCurrentFacetString_shouldContainQueriesFromAllFacetItems() throws Exception {
        SearchBean sb = new SearchBean();
        for (int i = 0; i < 3; ++i) {
            sb.getCurrentFacets().add(new FacetItem(new StringBuilder().append("FIELD").append(i).append(":value").append(i).toString(), false));
        }
        Assert.assertEquals(3, sb.getCurrentFacets().size());
        String facetString = sb.getCurrentFacetString();
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
     * @see SearchBean#getCurrentFacetString()
     * @verifies return hyphen if currentFacets empty
     */
    @Test
    public void getCurrentFacetString_shouldReturnHyphenIfCurrentFacetsEmpty() throws Exception {
        SearchBean sb = new SearchBean();
        String facetString = sb.getCurrentFacetString();
        Assert.assertEquals("-", facetString);
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
        searchBean.setCurrentCollection("DC:col");

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

        sb.setCurrentCollection("DC:a");
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

        sb.setCurrentCollection("DC:a");
        sb.mirrorAdvancedSearchCurrentHierarchicalFacets();
        Assert.assertEquals(SolrConstants.DC, item.getField());
        Assert.assertEquals("a", item.getValue());
        sb.setCurrentCollection("-");
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
        sb.setCurrentCollection("DC:a;;DC:b");
        sb.mirrorAdvancedSearchCurrentHierarchicalFacets();
        Assert.assertEquals(2, sb.getCurrentHierarchicalFacets().size());
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
        sb.setCurrentCollection("DC:a;;DC:b");
        sb.mirrorAdvancedSearchCurrentHierarchicalFacets();
        Assert.assertEquals("MD_TITLE", item1.getField());
        Assert.assertEquals("text", item1.getValue());
        Assert.assertEquals(SolrConstants.DC, item2.getField());
        Assert.assertEquals("a", item2.getValue());
        Assert.assertEquals(SolrConstants.DC, item3.getField());
        Assert.assertEquals("b", item3.getValue());
    }

    /**
     * @see SearchBean#setSortString(String)
     * @verifies split value correctly
     */
    @Test
    public void setSortString_shouldSplitValueCorrectly() throws Exception {
        SearchBean sb = new SearchBean();
        String sortString = "!SORT_1;SORT_2;SORT_3";
        sb.setSortString(sortString);
        Assert.assertEquals(3, sb.sortFields.size());
    }

    /**
     * @see SearchBean#generateFacetPrefix(List,boolean)
     * @verifies encode slashed and backslashes
     */
    @Test
    public void generateFacetPrefix_shouldEncodeSlashedAndBackslashes() throws Exception {
        List<FacetItem> list = new ArrayList<>();
        list.add(new FacetItem("FIELD:a/b\\c", false));
        Assert.assertEquals("FIELD:a/b\\c;;", SearchBean.generateFacetPrefix(list, false));
        Assert.assertEquals("FIELD:aU002FbU005Cc;;", SearchBean.generateFacetPrefix(list, true));
    }

    /**
     * @see SearchBean#setCurrentFacetString(String)
     * @verifies decode slashes and backslashes
     */
    @Test
    public void setCurrentFacetString_shouldDecodeSlashesAndBackslashes() throws Exception {
        SearchBean sb = new SearchBean();
        sb.setCurrentFacetString("FIELD:aU002FbU005Cc");
        Assert.assertEquals(1, sb.getCurrentFacets().size());
        Assert.assertEquals("a/b\\c", sb.getCurrentFacets().get(0).getValue());
    }

    /**
     * @see SearchBean#parseFacetString(String,List)
     * @verifies fill list correctly
     */
    @Test
    public void parseFacetString_shouldFillListCorrectly() throws Exception {
        List<FacetItem> facetItems = new ArrayList<>();
        SearchBean.parseFacetString("DC:a;;DC:b;;MD_TITLE:word;;", facetItems, true);
        Assert.assertEquals(3, facetItems.size());
        Assert.assertEquals("DC", facetItems.get(0).getField());
        Assert.assertEquals("a", facetItems.get(0).getValue());
        Assert.assertEquals("DC", facetItems.get(1).getField());
        Assert.assertEquals("b", facetItems.get(1).getValue());
        Assert.assertEquals("MD_TITLE", facetItems.get(2).getField());
        Assert.assertEquals("word", facetItems.get(2).getValue());
    }

    /**
     * @see SearchBean#parseFacetString(String,List)
     * @verifies empty list before filling
     */
    @Test
    public void parseFacetString_shouldEmptyListBeforeFilling() throws Exception {
        List<FacetItem> facetItems = new ArrayList<>();
        SearchBean.parseFacetString("DC:a;;", facetItems, true);
        Assert.assertEquals(1, facetItems.size());
        SearchBean.parseFacetString("DC:b;;MD_TITLE:word;;", facetItems, true);
        Assert.assertEquals(2, facetItems.size());
        Assert.assertEquals("DC", facetItems.get(0).getField());
        Assert.assertEquals("b", facetItems.get(0).getValue());
        Assert.assertEquals("MD_TITLE", facetItems.get(1).getField());
        Assert.assertEquals("word", facetItems.get(1).getValue());
    }

    /**
     * @see SearchBean#parseFacetString(String,List)
     * @verifies add DC field prefix if no field name is given
     */
    @Test
    public void parseFacetString_shouldAddDCFieldPrefixIfNoFieldNameIsGiven() throws Exception {
        List<FacetItem> facetItems = new ArrayList<>();
        SearchBean.parseFacetString("collection", facetItems, true);
        Assert.assertEquals(1, facetItems.size());
        Assert.assertEquals(SolrConstants.DC, facetItems.get(0).getField());
        Assert.assertEquals("collection", facetItems.get(0).getValue());
    }

    /**
     * @see SearchBean#parseFacetString(String,List,boolean)
     * @verifies set hierarchical status correctly
     */
    @Test
    public void parseFacetString_shouldSetHierarchicalStatusCorrectly() throws Exception {
        {
            List<FacetItem> facetItems = new ArrayList<>();
            SearchBean.parseFacetString("DC:a;;", facetItems, true);
            Assert.assertEquals(1, facetItems.size());
            Assert.assertTrue(facetItems.get(0).isHierarchial());
        }
        {
            List<FacetItem> facetItems = new ArrayList<>();
            SearchBean.parseFacetString("DC:a;;", facetItems, false);
            Assert.assertEquals(1, facetItems.size());
            Assert.assertFalse(facetItems.get(0).isHierarchial());
        }
    }

    /**
     * @see SearchBean#removeHierarchicalFacetAction(String)
     * @verifies remove facet correctly
     */
    @Test
    public void removeHierarchicalFacetAction_shouldRemoveFacetCorrectly() throws Exception {
        SearchBean sb = new SearchBean();
        sb.setCurrentCollection("DC:a;;DC:aa;;");
        Assert.assertEquals(2, sb.getCurrentHierarchicalFacets().size());
        sb.removeHierarchicalFacetAction("DC:a");
        Assert.assertEquals(1, sb.getCurrentHierarchicalFacets().size());
        // Make sure only "DC:a" is removed but not facets starting with "DC:a"
        Assert.assertEquals("DC:aa;;", sb.getCurrentCollection());
        ;
    }

    /**
     * @see SearchBean#removeFacetAction(String)
     * @verifies remove facet correctly
     */
    @Test
    public void removeFacetAction_shouldRemoveFacetCorrectly() throws Exception {
        SearchBean sb = new SearchBean();
        sb.setCurrentFacetString("DOCSTRCT:a;;MD_TITLE:bob;;MD_TITLE:b;;");
        Assert.assertEquals(3, sb.getCurrentFacets().size());
        sb.removeFacetAction("MD_TITLE:b");
        Assert.assertEquals(2, sb.getCurrentFacets().size());
        // Make sure only "MD_TITLE:b" is removed but not facets starting with "MD_TITLE:b"
        Assert.assertEquals("DOCSTRCT%3Aa%3B%3BMD_TITLE%3Abob%3B%3B", sb.getCurrentFacetString());
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
        Assert.assertEquals(
                "((SUPERDEFAULT:(foo OR bar) OR SUPERFULLTEXT:(foo OR bar) OR NORMDATATERMS:(foo OR bar) OR UGCTERMS:(foo OR bar) OR OVERVIEWPAGE_DESCRIPTION:(foo OR bar) OR OVERVIEWPAGE_PUBLICATIONTEXT:(foo OR bar))"
                        + " OR (MD_TITLE:(bla AND \"blup\" -nein))) OR ((SUPERFULLTEXT:\"lorem ipsum dolor sit amet\"))", sb
                                .generateAdvancedSearchString(true));
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
        Assert.assertEquals("(All fields: foo bar OR Title: bla \"blup\" -nein) OR\n<br />(Full text: \"lorem ipsum dolor sit amet\")", sb
                .getAdvancedSearchQueryInfo());
    }
}