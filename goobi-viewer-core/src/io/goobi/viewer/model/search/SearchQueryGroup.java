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
package io.goobi.viewer.model.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * Search query group for the advanced search.
 */
public class SearchQueryGroup implements Serializable {

    private static final long serialVersionUID = 609291421340747521L;

    public enum SearchQueryGroupOperator {
        AND,
        OR;

        public String getLabel() {
            return Helper.getTranslation(this.name(), null);
        }
    }

    /** List of query items in this group. */
    private final List<SearchQueryItem> queryItems = new ArrayList<>();

    private SearchQueryGroupOperator operator = SearchQueryGroupOperator.AND;
    private final Locale locale;

    /**
     * <p>Constructor for SearchQueryGroup.</p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @param initSize a int.
     */
    public SearchQueryGroup(Locale locale, int initSize) {
        this.locale = locale;
        for (int i = 0; i < initSize; ++i) {
            queryItems.add(new SearchQueryItem(locale));
        }
    }

    /**
     * <p>getAvailableOperators.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<SearchQueryGroupOperator> getAvailableOperators() {
        return Arrays.asList(SearchQueryGroupOperator.values());
    }

    /**
     * <p>Getter for the field <code>queryItems</code>.</p>
     *
     * @return the queryItems
     */
    public List<SearchQueryItem> getQueryItems() {
        return queryItems;
    }

    /**
     * <p>addNewQueryItem.</p>
     *
     * @should add item correctly
     * @return a boolean.
     */
    public boolean addNewQueryItem() {
        return queryItems.add(new SearchQueryItem(BeanUtils.getLocale()));
    }

    /**
     * <p>removeQueryItem.</p>
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
     * <p>Getter for the field <code>operator</code>.</p>
     *
     * @return the operator
     */
    public SearchQueryGroupOperator getOperator() {
        return operator;
    }

    /**
     * <p>Setter for the field <code>operator</code>.</p>
     *
     * @param operator the operator to set
     */
    public void setOperator(SearchQueryGroupOperator operator) {
        this.operator = operator;
    }

    /**
     * <p>Getter for the field <code>locale</code>.</p>
     *
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }
}
