/**
 * This file is part of the Goobi Viewer - a content presentation and management application for digitized objects.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.model.search.SearchQueryItem.SearchItemOperator;

public class SearchQueryItemTest {

    /**
     * @see SearchQueryItem#getAvailableOperators()
     * @verifies return IS if displaySelectItems true
     */
    @Test
    public void getAvailableOperators_shouldReturnISIfDisplaySelectItemsTrue() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        item.setField(SolrConstants.DC);
        Assert.assertTrue(item.isDisplaySelectItems());
        List<SearchItemOperator> operators = item.getAvailableOperators();
        Assert.assertEquals(1, operators.size());
        Assert.assertEquals(SearchItemOperator.IS, operators.get(0));
    }

    /**
     * @see SearchQueryItem#getAvailableOperators()
     * @verifies return AND, OR, PHRASE if displaySelectItems false
     */
    @Test
    public void getAvailableOperators_shouldReturnANDORPHRASEIfDisplaySelectItemsFalse() throws Exception {
        SearchQueryItem item = new SearchQueryItem(null);
        Assert.assertFalse(item.isDisplaySelectItems());
        List<SearchItemOperator> operators = item.getAvailableOperators();
        Assert.assertEquals(3, operators.size());
        Assert.assertEquals(SearchItemOperator.AND, operators.get(0));
        Assert.assertEquals(SearchItemOperator.OR, operators.get(1));
        Assert.assertEquals(SearchItemOperator.PHRASE, operators.get(2));
    }

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
                    "SUPERDEFAULT:(foo OR bar) OR SUPERFULLTEXT:(foo OR bar) OR NORMDATATERMS:(foo OR bar) OR UGCTERMS:(foo OR bar) OR OVERVIEWPAGE_DESCRIPTION:(foo OR bar) OR OVERVIEWPAGE_PUBLICATIONTEXT:(foo OR bar)",
                    item.generateQuery(searchTerms, true));
            Assert.assertTrue(searchTerms.contains("foo"));
            Assert.assertTrue(searchTerms.contains("bar"));
        }
        {
            SearchQueryItem item = new SearchQueryItem(null);
            item.setOperator(SearchItemOperator.AND);
            item.setField("MD_TITLE");
            item.setValue("bla \"blup\" -nein");
            Set<String> searchTerms = new HashSet<>(0);
            Assert.assertEquals("MD_TITLE:(bla AND \"blup\" -nein)", item.generateQuery(searchTerms, true));
            Assert.assertTrue(searchTerms.isEmpty());
        }
        {
            SearchQueryItem item = new SearchQueryItem(null);
            item.setOperator(SearchItemOperator.PHRASE);
            item.setField(SolrConstants.FULLTEXT);
            item.setValue("lorem ipsum dolor sit amet");
            Set<String> searchTerms = new HashSet<>(1);
            Assert.assertEquals("SUPERFULLTEXT:\"lorem ipsum dolor sit amet\"", item.generateQuery(searchTerms, true));
            Assert.assertTrue(searchTerms.contains("lorem ipsum dolor sit amet"));
        }
    }
}