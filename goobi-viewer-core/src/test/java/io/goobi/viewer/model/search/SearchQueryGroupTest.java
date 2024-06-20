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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.search.SearchQueryGroup.SearchQueryGroupOperator;
import io.goobi.viewer.solr.SolrConstants;

class SearchQueryGroupTest extends AbstractSolrEnabledTest {

    /**
     * @see SearchQueryGroup#init(List)
     * @verifies create and preselect visible fields
     */
    @Test
    void init_shouldCreateAndPreselectVisibleFields() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "en"), null);
        Assertions.assertEquals(3, group.getQueryItems().size());
        Assertions.assertEquals(SearchHelper.SEARCH_FILTER_ALL.getField(), group.getQueryItems().get(0).getField());
        Assertions.assertEquals(SolrConstants.DC, group.getQueryItems().get(1).getField());
        Assertions.assertEquals("MD_TITLE", group.getQueryItems().get(2).getField());
    }

    /**
     * @see SearchQueryGroup#init(List)
     * @verifies only create allfields item if fieldConfigs null
     */
    @Test
    void init_shouldOnlyCreateAllfieldsItemIfFieldConfigsNull() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(null, null);
        Assertions.assertEquals(1, group.getQueryItems().size());
        Assertions.assertEquals(SearchHelper.SEARCH_FILTER_ALL.getField(), group.getQueryItems().get(0).getField());
    }

    /**
     * @see SearchQueryGroup#injectItems(List)
     * @verifies replace existing items with given
     */
    @Test
    void injectItems_shouldReplaceExistingItemsWithGiven() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "en"), null);
        Assertions.assertEquals(3, group.getQueryItems().size());

        SearchQueryItem item = new SearchQueryItem();
        item.setValue("foobar");
        group.injectItems(Collections.singletonList(item));
        Assertions.assertEquals(1, group.getQueryItems().size());
        Assertions.assertEquals("foobar", group.getQueryItems().get(0).getValue());
    }

    /**
     * @see SearchQueryGroup#isBlank()
     * @verifies return true if all items without value
     */
    @Test
    void isBlank_shouldReturnTrueIfAllItemsWithoutValue() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "en"), null);
        Assertions.assertTrue(group.isBlank());
    }

    /**
     * @see SearchQueryGroup#isBlank()
     * @verifies return false if at least one item has value
     */
    @Test
    void isBlank_shouldReturnFalseIfAtLeastOneItemHasValue() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "en"), null);
        Assertions.assertEquals(3, group.getQueryItems().size());
        group.getQueryItems().get(0).setValue("foobar");
        Assertions.assertFalse(group.isBlank());
    }

    /**
     * @see SearchQueryGroup#getAvailableOperators()
     * @verifies return all enum values
     */
    @Test
    void getAvailableOperators_shouldReturnAllEnumValues() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "en"), null);
        List<SearchQueryGroupOperator> operators = group.getAvailableOperators();
        Assertions.assertTrue(operators.contains(SearchQueryGroupOperator.AND));
        Assertions.assertTrue(operators.contains(SearchQueryGroupOperator.OR));
    }

    /**
     * @see SearchQueryGroup#addNewQueryItem()
     * @verifies add item correctly
     */
    @Test
    void addNewQueryItem_shouldAddItemCorrectly() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(null, null);
        Assertions.assertEquals(1, group.getQueryItems().size());
        Assertions.assertTrue(group.addNewQueryItem());
        Assertions.assertEquals(2, group.getQueryItems().size());
    }

    /**
     * @see SearchQueryGroup#removeQueryItem(SearchQueryItem)
     * @verifies remove item correctly
     */
    @Test
    void removeQueryItem_shouldRemoveItemCorrectly() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "en"), null);
        Assertions.assertEquals(3, group.getQueryItems().size());
        Assertions.assertTrue(group.removeQueryItem(group.getQueryItems().get(0)));
        Assertions.assertEquals(2, group.getQueryItems().size());
    }

    /**
     * @see SearchQueryGroup#removeQueryItem(SearchQueryItem)
     * @verifies not remove last remaining item
     */
    @Test
    void removeQueryItem_shouldNotRemoveLastRemainingItem() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(null, null);
        Assertions.assertEquals(1, group.getQueryItems().size());
        Assertions.assertFalse(group.removeQueryItem(group.getQueryItems().get(0)));
        Assertions.assertEquals(1, group.getQueryItems().size());
    }
}