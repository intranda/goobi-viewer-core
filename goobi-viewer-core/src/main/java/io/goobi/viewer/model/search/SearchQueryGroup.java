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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Search query group for the advanced search.
 */
public class SearchQueryGroup implements Serializable {

    private static final long serialVersionUID = 609291421340747521L;

    public enum SearchQueryGroupOperator {
        AND,
        OR;

        public String getLabel() {
            return ViewerResourceBundle.getTranslation(this.name(), null);
        }
    }

    /** List of query items in this group. */
    private final List<SearchQueryItem> queryItems = new ArrayList<>();

    private SearchQueryGroupOperator operator = SearchQueryGroupOperator.AND;

    /**
     * <p>
     * Constructor for SearchQueryGroup.
     * </p>
     *
     * @param fieldConfigs
     * @param template
     */
    public SearchQueryGroup(List<AdvancedSearchFieldConfiguration> fieldConfigs, String template) {
        init(fieldConfigs, template);
    }

    /**
     * 
     * @param fieldConfigs
     * @param template
     * @should create and preselect visible fields
     * @should only create allfields item if fieldConfigs null
     */
    public void init(List<AdvancedSearchFieldConfiguration> fieldConfigs, String template) {
        queryItems.clear();
        operator = SearchQueryGroupOperator.AND;

        if (template == null || StringConstants.DEFAULT_NAME.equals(template)) {
            SearchQueryItem firstItem = new SearchQueryItem(template);
            firstItem.setField(SearchHelper.SEARCH_FILTER_ALL_LABEL);
            queryItems.add(firstItem);
        }
        if (fieldConfigs != null) {
            for (AdvancedSearchFieldConfiguration fieldConfig : fieldConfigs) {
                if (fieldConfig.isVisible()) {
                    SearchQueryItem item = new SearchQueryItem(template);
                    item.setField(fieldConfig.getField());
                    item.setLabel(fieldConfig.getLabel());
                    queryItems.add(item);
                }
            }
        }
    }

    /**
     * Replaces query items in this group with the given list.
     * 
     * @param items
     * @should replace existing items with given
     */
    public void injectItems(List<SearchQueryItem> items) {
        queryItems.clear();
        queryItems.addAll(items);
    }

    /**
     * 
     * @return true if none of the items has any value input; false otherwise
     * @should return true if all items without value
     * @should return false if at least one item has value
     */
    public boolean isBlank() {
        for (SearchQueryItem item : queryItems) {
            if (StringUtils.isNotBlank(item.getValue()) || StringUtils.isNotBlank(item.getValue2())) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * getAvailableOperators.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @should return all enum values
     */
    public List<SearchQueryGroupOperator> getAvailableOperators() {
        return Arrays.asList(SearchQueryGroupOperator.values());
    }

    /**
     * <p>
     * Getter for the field <code>queryItems</code>.
     * </p>
     *
     * @return the queryItems
     */
    public List<SearchQueryItem> getQueryItems() {
        return queryItems;
    }

    /**
     * <p>
     * addNewQueryItem.
     * </p>
     *
     * @return true if operation successful; false otherwise
     * @should add item correctly
     */
    public boolean addNewQueryItem() {
        return queryItems.add(new SearchQueryItem());
    }

    /**
     * <p>
     * removeQueryItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.search.SearchQueryItem} object.
     * @should remove item correctly
     * @should not remove last remaining item
     * @return a boolean.
     */
    public boolean removeQueryItem(SearchQueryItem item) {
        if (queryItems.size() > 1) {
            return queryItems.remove(item);
        }

        return false;
    }

    /**
     * <p>
     * Getter for the field <code>operator</code>.
     * </p>
     *
     * @return the operator
     */
    public SearchQueryGroupOperator getOperator() {
        return operator;
    }

    /**
     * <p>
     * Setter for the field <code>operator</code>.
     * </p>
     *
     * @param operator the operator to set
     */
    public void setOperator(SearchQueryGroupOperator operator) {
        this.operator = operator;
    }
}
