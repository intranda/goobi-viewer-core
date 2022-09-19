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

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.search.SearchQueryItem.SearchItemOperator;
import io.goobi.viewer.solr.SolrConstants;

public class SearchQueryItemTest extends AbstractSolrEnabledTest {

    /**
     * @see SearchQueryItem#generateQuery(Set)
     * @verifies generate query correctly
     */
    @Test
    public void generateQuery_shouldGenerateQueryCorrectly() throws Exception {
        {
            SearchQueryItem item = new SearchQueryItem(null);
            item.setOperator(SearchItemOperator.OR);
            item.setField(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS);
            item.setValue("foo bar");
            Set<String> searchTerms = new HashSet<>(2);
            Assert.assertEquals(
                    "(SUPERDEFAULT:(foo OR bar) OR SUPERFULLTEXT:(foo OR bar) OR SUPERUGCTERMS:(foo OR bar) OR DEFAULT:(foo OR bar) OR FULLTEXT:(foo OR bar) OR NORMDATATERMS:(foo OR bar) OR UGCTERMS:(foo OR bar) OR CMS_TEXT_ALL:(foo OR bar))",
                    item.generateQuery(searchTerms, true, false));
            Assert.assertTrue(searchTerms.contains("foo"));
            Assert.assertTrue(searchTerms.contains("bar"));
        }
        {
            SearchQueryItem item = new SearchQueryItem(null);
            item.setOperator(SearchItemOperator.AND);
            item.setField("MD_TITLE");
            item.setValue("bla \"blup\" -nein");
            Set<String> searchTerms = new HashSet<>(0);
            Assert.assertEquals("+(MD_TITLE:(bla AND \\\"blup\\\" -nein))", item.generateQuery(searchTerms, true, false));
            Assert.assertTrue(searchTerms.isEmpty());
        }
        {
            SearchQueryItem item = new SearchQueryItem(null);
            item.setField(SolrConstants.FULLTEXT);
            item.setValue("\"lorem ipsum dolor sit amet\"");
            Set<String> searchTerms = new HashSet<>(1);
            Assert.assertEquals("+(SUPERFULLTEXT:\"lorem ipsum dolor sit amet\" OR FULLTEXT:\"lorem ipsum dolor sit amet\")",
                    item.generateQuery(searchTerms, true, false));
            Assert.assertTrue(searchTerms.contains("lorem ipsum dolor sit amet"));
        }
        // Auto-tokenize phrase search field if so configured
        {
            SearchQueryItem item = new SearchQueryItem(null);
            item.setField("MD_TITLE");
            item.setValue("\"lorem ipsum dolor sit amet\"");
            Set<String> searchTerms = new HashSet<>(0);
            Assert.assertEquals("+(MD_TITLE" + SolrConstants._UNTOKENIZED + ":\"lorem ipsum dolor sit amet\")",
                    item.generateQuery(searchTerms, true, false));
            Assert.assertTrue(searchTerms.isEmpty());
        }
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean)
     * @verifies escape reserved characters
     */
    @Test
    public void generateQuery_shouldEscapeReservedCharacters() throws Exception {
        {
            SearchQueryItem item = new SearchQueryItem(null);
            item.setOperator(SearchItemOperator.OR);
            item.setField(SolrConstants.DEFAULT);
            item.setValue("[foo] :bar:");
            Set<String> searchTerms = new HashSet<>(2);
            Assert.assertEquals("(SUPERDEFAULT:(\\[foo\\] OR \\:bar\\:) OR DEFAULT:(\\[foo\\] OR \\:bar\\:))",
                    item.generateQuery(searchTerms, true, false));
        }
        {
            // Phrase searches should NOT have escaped terms
            SearchQueryItem item = new SearchQueryItem(null);
            item.setField(SolrConstants.DEFAULT);
            item.setValue("\"[foo] :bar:\"");
            Set<String> searchTerms = new HashSet<>(2);
            Assert.assertEquals("+(SUPERDEFAULT:\"[foo] :bar:\" OR DEFAULT:\"[foo] :bar:\")", item.generateQuery(searchTerms, true, false));
        }
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean)
     * @verifies always use OR operator if searching in all fields
     */
    @Test
    public void generateQuery_shouldAlwaysUseOROperatorIfSearchingInAllFields() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setOperator(SearchItemOperator.AND);
        item.setField(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS);
        item.setValue("foo bar");
        Set<String> searchTerms = new HashSet<>(2);
        Assert.assertEquals(
                "+(SUPERDEFAULT:(foo OR bar) OR SUPERFULLTEXT:(foo OR bar) OR SUPERUGCTERMS:(foo OR bar) OR DEFAULT:(foo OR bar) OR FULLTEXT:(foo OR bar) OR NORMDATATERMS:(foo OR bar) OR UGCTERMS:(foo OR bar) OR CMS_TEXT_ALL:(foo OR bar))",
                item.generateQuery(searchTerms, true, false));
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean)
     * @verifies preserve truncation
     */
    @Test
    public void generateQuery_shouldPreserveTruncation() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setOperator(SearchItemOperator.AND);
        item.setField(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS);
        item.setValue("*foo*");
        Set<String> searchTerms = new HashSet<>(2);
        Assert.assertEquals(
                "+(SUPERDEFAULT:(*foo*) OR SUPERFULLTEXT:(*foo*) OR SUPERUGCTERMS:(*foo*) OR DEFAULT:(*foo*) OR FULLTEXT:(*foo*) OR NORMDATATERMS:(*foo*) OR UGCTERMS:(*foo*) OR CMS_TEXT_ALL:(*foo*))",
                item.generateQuery(searchTerms, true, false));
    }

    @Test
    public void generateQuery_shouldAddFuzzySearchOperator() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setOperator(SearchItemOperator.AND);
        item.setField("MD_TITLE");
        item.setValue("fooo bar");
        Set<String> searchTerms = new HashSet<>(2);
        Assert.assertEquals("+(MD_TITLE:((fooo fooo~1) AND (bar)))", item.generateQuery(searchTerms, true, true));
    }

    @Test
    public void generateQuery_shouldAddFuzzySearchOperatorWithWildcards() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setOperator(SearchItemOperator.AND);
        item.setField("MD_TITLE");
        item.setValue("*fooo* *bar*");
        Set<String> searchTerms = new HashSet<>(2);
        Assert.assertEquals("+(MD_TITLE:((*fooo* fooo~1) AND (*bar*)))", item.generateQuery(searchTerms, true, true));
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean)
     * @verifies preserve truncation
     */
    @Test
    public void generateQuery_shouldAddFuzzySearchOperatorWithHyphen() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setOperator(SearchItemOperator.AND);
        item.setField("MD_TITLE");
        item.setValue("foo-bar");
        Set<String> searchTerms = new HashSet<>(2);
        Assert.assertEquals("+(MD_TITLE:((foo\\-bar foo\\-bar~1)))", item.generateQuery(searchTerms, true, true));
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean)
     * @verifies generate range query correctly
     */
    @Test
    public void generateQuery_shouldGenerateRangeQueryCorrectly() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setField(SolrConstants.YEAR);
        item.setValue(" 1900 ");
        item.setValue2(" 2020 ");
        Assert.assertEquals("+(YEAR:([1900 TO 2020]))", item.generateQuery(new HashSet<>(), true, false));
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean,boolean,int)
     * @verifies add proximity search token correctly
     */
    @Test
    public void generateQuery_shouldAddProximitySearchTokenCorrectly() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setField(SolrConstants.FULLTEXT);
        item.setValue("\"foo bar\"~10");
        Set<String> searchTerms = new HashSet<>(2);
        Assert.assertEquals("+(" + SolrConstants.SUPERFULLTEXT + ":\"foo bar\"~10 OR " + SolrConstants.FULLTEXT + ":\"foo bar\"~10)",
                item.generateQuery(searchTerms, true, false));
    }

    /**
     * @see SearchQueryItem#toggleDisplaySelectItems()
     * @verifies set displaySelectItems false if searching in all fields
     */
    @Test
    public void toggleDisplaySelectItems_shouldSetDisplaySelectItemsFalseIfSearchingInAllFields() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setField(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS);
        item.displaySelectItems = true;
        item.toggleDisplaySelectItems();
        Assert.assertFalse(item.isDisplaySelectItems());
    }

    /**
     * @see SearchQueryItem#toggleDisplaySelectItems()
     * @verifies set displaySelectItems false if searching in fulltext
     */
    @Test
    public void toggleDisplaySelectItems_shouldSetDisplaySelectItemsFalseIfSearchingInFulltext() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setField(SolrConstants.FULLTEXT);
        item.displaySelectItems = true;
        item.toggleDisplaySelectItems();
        Assert.assertFalse(item.isDisplaySelectItems());
    }

    /**
     * @see SearchQueryItem#toggleDisplaySelectItems()
     * @verifies set displaySelectItems true if value count below threshold
     */
    @Test
    public void toggleDisplaySelectItems_shouldSetDisplaySelectItemsTrueIfValueCountBelowThreshold() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setField("MD_FOO");
        item.toggleDisplaySelectItems();
        Assert.assertTrue(item.isDisplaySelectItems());
    }

    /**
     * @see SearchQueryItem#toggleDisplaySelectItems()
     * @verifies set displaySelectItems false if value count above threshold
     */
    @Test
    public void toggleDisplaySelectItems_shouldSetDisplaySelectItemsFalseIfValueCountAboveThreshold() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setField(SolrConstants.PI);
        item.toggleDisplaySelectItems();
        Assert.assertFalse(item.isDisplaySelectItems());
    }
}
