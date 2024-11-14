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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.search.SearchQueryItem.SearchItemOperator;
import io.goobi.viewer.solr.SolrConstants;

class SearchQueryItemTest extends AbstractSolrEnabledTest {

    /**
     * @see SearchQueryItem#generateQuery(Set)
     * @verifies generate query correctly
     */
    @Test
    void generateQuery_shouldGenerateQueryCorrectly() {
        {
            SearchQueryItem item = new SearchQueryItem();
            item.setOperator(SearchItemOperator.OR);
            item.setField(SearchHelper.SEARCH_FILTER_ALL.getField());
            item.setValue("foo bar");
            Set<String> searchTerms = new HashSet<>(2);
            Assertions.assertEquals(
                    "(SUPERDEFAULT:(foo bar) SUPERFULLTEXT:(foo bar) SUPERUGCTERMS:(foo bar) SUPERSEARCHTERMS_ARCHIVE:(foo bar) DEFAULT:(foo bar)"
                            + " FULLTEXT:(foo bar) NORMDATATERMS:(foo bar) UGCTERMS:(foo bar) SEARCHTERMS_ARCHIVE:(foo bar) CMS_TEXT_ALL:(foo bar))",
                    item.generateQuery(searchTerms, true, false));
            Assertions.assertTrue(searchTerms.contains("foo"));
            Assertions.assertTrue(searchTerms.contains("bar"));
        }
        {
            SearchQueryItem item = new SearchQueryItem();
            item.setOperator(SearchItemOperator.AND);
            item.setField("MD_TITLE");
            item.setValue("bla \"blup\" -nein");
            Set<String> searchTerms = new HashSet<>(0);
            Assertions.assertEquals("+(MD_TITLE:(bla AND \\\"blup\\\" -nein))", item.generateQuery(searchTerms, true, false));
            Assertions.assertTrue(searchTerms.isEmpty());
        }
        {
            SearchQueryItem item = new SearchQueryItem();
            item.setField(SolrConstants.FULLTEXT);
            item.setValue("\"lorem ipsum dolor sit amet\"");
            Set<String> searchTerms = new HashSet<>(1);
            Assertions.assertEquals("+(SUPERFULLTEXT:\"lorem ipsum dolor sit amet\" FULLTEXT:\"lorem ipsum dolor sit amet\")",
                    item.generateQuery(searchTerms, true, false));
            Assertions.assertTrue(searchTerms.contains("lorem ipsum dolor sit amet"));
        }
        // Auto-tokenize phrase search field if so configured
        {
            SearchQueryItem item = new SearchQueryItem();
            item.setField("MD_TITLE");
            item.setValue("\"lorem ipsum dolor sit amet\"");
            Set<String> searchTerms = new HashSet<>(0);
            Assertions.assertEquals("+(MD_TITLE" + SolrConstants.SUFFIX_UNTOKENIZED + ":\"lorem ipsum dolor sit amet\")",
                    item.generateQuery(searchTerms, true, false));
            Assertions.assertTrue(searchTerms.isEmpty());
        }
        // Multiple values
        {
            SearchQueryItem item = new SearchQueryItem();
            item.setField("DOCSTRCT"); // selected field must be configured in a way that will return SolrQueryItem.isDisplaySelectItems() == true
            item.getValues().add("foo bar");
            item.getValues().add("lorem ipsum");
            Set<String> searchTerms = new HashSet<>(0);
            Assertions.assertEquals("+(DOCSTRCT:\"foo bar\" DOCSTRCT:\"lorem ipsum\")", item.generateQuery(searchTerms, true, false));
            Assertions.assertTrue(searchTerms.isEmpty());
        }
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean)
     * @verifies escape reserved characters
     */
    @Test
    void generateQuery_shouldEscapeReservedCharacters() {
        {
            SearchQueryItem item = new SearchQueryItem();
            item.setOperator(SearchItemOperator.OR);
            item.setField(SolrConstants.DEFAULT);
            item.setValue("[foo] :bar:");
            Set<String> searchTerms = new HashSet<>(2);
            Assertions.assertEquals("(SUPERDEFAULT:(\\[foo\\] AND \\:bar\\:) DEFAULT:(\\[foo\\] AND \\:bar\\:))",
                    item.generateQuery(searchTerms, true, false));
        }
        {
            // Phrase searches should NOT have escaped terms
            SearchQueryItem item = new SearchQueryItem();
            item.setField(SolrConstants.DEFAULT);
            item.setValue("\"[foo] :bar:\"");
            Set<String> searchTerms = new HashSet<>(2);
            Assertions.assertEquals("+(SUPERDEFAULT:\"[foo] :bar:\" DEFAULT:\"[foo] :bar:\")", item.generateQuery(searchTerms, true, false));
        }
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean)
     * @verifies always use OR operator if searching in all fields
     */
    @Test
    void generateQuery_shouldAlwaysUseOROperatorIfSearchingInAllFields() {
        SearchQueryItem item = new SearchQueryItem();
        item.setOperator(SearchItemOperator.AND);
        item.setField(SearchHelper.SEARCH_FILTER_ALL.getField());
        item.setValue("foo bar");
        Set<String> searchTerms = new HashSet<>(2);
        Assertions.assertEquals(
                "+(SUPERDEFAULT:(foo bar) SUPERFULLTEXT:(foo bar) SUPERUGCTERMS:(foo bar) SUPERSEARCHTERMS_ARCHIVE:(foo bar) DEFAULT:(foo bar)"
                        + " FULLTEXT:(foo bar) NORMDATATERMS:(foo bar) UGCTERMS:(foo bar) SEARCHTERMS_ARCHIVE:(foo bar) CMS_TEXT_ALL:(foo bar))",
                item.generateQuery(searchTerms, true, false));
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean)
     * @verifies preserve truncation
     */
    @Test
    void generateQuery_shouldPreserveTruncation() {
        SearchQueryItem item = new SearchQueryItem();
        item.setOperator(SearchItemOperator.AND);
        item.setField(SearchHelper.SEARCH_FILTER_ALL.getField());
        item.setValue("*foo*");
        Set<String> searchTerms = new HashSet<>(2);
        Assertions.assertEquals(
                "+(SUPERDEFAULT:(*foo*) SUPERFULLTEXT:(*foo*) SUPERUGCTERMS:(*foo*) SUPERSEARCHTERMS_ARCHIVE:(*foo*) DEFAULT:(*foo*)"
                        + " FULLTEXT:(*foo*) NORMDATATERMS:(*foo*) UGCTERMS:(*foo*) SEARCHTERMS_ARCHIVE:(*foo*) CMS_TEXT_ALL:(*foo*))",
                item.generateQuery(searchTerms, true, false));
    }

    @Test
    void generateQuery_shouldAddFuzzySearchOperator() {
        SearchQueryItem item = new SearchQueryItem();
        item.setOperator(SearchItemOperator.AND);
        item.setField("MD_TITLE");
        item.setValue("fooo bar");
        Set<String> searchTerms = new HashSet<>(2);
        Assertions.assertEquals("+(MD_TITLE:((fooo fooo~1) AND (bar)))", item.generateQuery(searchTerms, true, true));
    }

    @Test
    void generateQuery_shouldAddFuzzySearchOperatorWithWildcards() {
        SearchQueryItem item = new SearchQueryItem();
        item.setOperator(SearchItemOperator.AND);
        item.setField("MD_TITLE");
        item.setValue("*fooo* *bar*");
        Set<String> searchTerms = new HashSet<>(2);
        Assertions.assertEquals("+(MD_TITLE:((*fooo* fooo~1) AND (*bar*)))", item.generateQuery(searchTerms, true, true));
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean)
     * @verifies preserve truncation
     */
    @Test
    void generateQuery_shouldAddFuzzySearchOperatorWithHyphen() {
        SearchQueryItem item = new SearchQueryItem();
        item.setOperator(SearchItemOperator.AND);
        item.setField("MD_TITLE");
        item.setValue("foo-bar");
        Set<String> searchTerms = new HashSet<>(2);
        Assertions.assertEquals("+(MD_TITLE:((foo\\-bar foo\\-bar~1)))", item.generateQuery(searchTerms, true, true));
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean)
     * @verifies generate range query correctly
     */
    @Test
    void generateQuery_shouldGenerateRangeQueryCorrectly() {
        SearchQueryItem item = new SearchQueryItem();
        item.setField("MD_YEARPUBLISH");
        item.setValue(" 1900 ");
        item.setValue2(" 2020 ");
        Assertions.assertEquals("+(MD_YEARPUBLISH:([1900 TO 2020]))", item.generateQuery(new HashSet<>(), true, false));
    }

    /**
     * @see SearchQueryItem#generateQuery(Set,boolean,boolean,int)
     * @verifies add proximity search token correctly
     */
    @Test
    void generateQuery_shouldAddProximitySearchTokenCorrectly() {
        SearchQueryItem item = new SearchQueryItem();
        item.setField(SolrConstants.FULLTEXT);
        item.setValue("\"foo bar\"~10");
        Set<String> searchTerms = new HashSet<>(2);
        Assertions.assertEquals("+(" + SolrConstants.SUPERFULLTEXT + ":\"foo bar\"~10 " + SolrConstants.FULLTEXT + ":\"foo bar\"~10)",
                item.generateQuery(searchTerms, true, false));
    }

    /**
     * @see SearchQueryItem#toggleDisplaySelectItems()
     * @verifies set displaySelectItems false if searching in all fields
     */
    @Test
    void toggleDisplaySelectItems_shouldSetDisplaySelectItemsFalseIfSearchingInAllFields() {
        SearchQueryItem item = new SearchQueryItem();
        item.setField(SearchHelper.SEARCH_FILTER_ALL.getField());
        item.setDisplaySelectItems(true);
        item.toggleDisplaySelectItems();
        Assertions.assertFalse(item.isDisplaySelectItems());
    }

    /**
     * @see SearchQueryItem#toggleDisplaySelectItems()
     * @verifies set displaySelectItems false if searching in fulltext
     */
    @Test
    void toggleDisplaySelectItems_shouldSetDisplaySelectItemsFalseIfSearchingInFulltext() {
        SearchQueryItem item = new SearchQueryItem();
        item.setField(SolrConstants.FULLTEXT);
        item.setDisplaySelectItems(true);
        item.toggleDisplaySelectItems();
        Assertions.assertFalse(item.isDisplaySelectItems());
    }

    /**
     * @see SearchQueryItem#toggleDisplaySelectItems()
     * @verifies set displaySelectItems true if value count below threshold
     */
    @Test
    void toggleDisplaySelectItems_shouldSetDisplaySelectItemsTrueIfValueCountBelowThreshold() {
        SearchQueryItem item = new SearchQueryItem();
        item.setField(SolrConstants.EVENTTYPE);
        item.toggleDisplaySelectItems();
        Assertions.assertTrue(item.isDisplaySelectItems());
    }

    /**
     * @see SearchQueryItem#toggleDisplaySelectItems()
     * @verifies set displaySelectItems false if value count above threshold
     */
    @Test
    void toggleDisplaySelectItems_shouldSetDisplaySelectItemsFalseIfValueCountAboveThreshold() {
        SearchQueryItem item = new SearchQueryItem();
        item.setField(SolrConstants.PI);
        item.toggleDisplaySelectItems();
        Assertions.assertFalse(item.isDisplaySelectItems());
    }

    /**
     * @see SearchQueryItem#toggleDisplaySelectItems()
     * @verifies set displaySelectItems false if value count zero
     */
    @Test
    void toggleDisplaySelectItems_shouldSetDisplaySelectItemsFalseIfValueCountZero() {
        SearchQueryItem item = new SearchQueryItem();
        item.setField("MD_NO_SUCH_FIELD");
        item.toggleDisplaySelectItems();
        Assertions.assertFalse(item.isDisplaySelectItems());
    }

    /**
     * @see SearchQueryItem#getLabel()
     * @verifies return field if label empty
     */
    @Test
    void getLabel_shouldReturnFieldIfLabelEmpty() {
        SearchQueryItem item = new SearchQueryItem();
        item.setField("MD_FIELD");
        Assertions.assertEquals("MD_FIELD", item.getLabel());
    }
}
