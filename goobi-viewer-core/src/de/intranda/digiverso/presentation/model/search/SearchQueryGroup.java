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
package de.intranda.digiverso.presentation.model.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

/**
 * Search query group for the advanced search.
 */
public class SearchQueryGroup {

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
     * 
     * @param locale
     * @param initSize
     */
    public SearchQueryGroup(Locale locale, int initSize) {
        this.locale = locale;
        for (int i = 0; i < initSize; ++i) {
            queryItems.add(new SearchQueryItem(locale));
        }
    }

    public List<SearchQueryGroupOperator> getAvailableOperators() {
        return Arrays.asList(SearchQueryGroupOperator.values());
    }

    /**
     * @return the queryItems
     */
    public List<SearchQueryItem> getQueryItems() {
        return queryItems;
    }

    /**
     * 
     * @return
     * @should add item correctly
     */
    public boolean addNewQueryItem() {
        return queryItems.add(new SearchQueryItem(BeanUtils.getLocale()));
    }

    /**
     * 
     * @param item
     * @return
     * @should remove item correctly
     * @should not remove last remaining item
     */
    public boolean removeQueryItem(SearchQueryItem item) {
        if (queryItems.size() > 1) {
            return queryItems.remove(item);
        }

        return false;
    }

    /**
     * @return the operator
     */
    public SearchQueryGroupOperator getOperator() {
        return operator;
    }

    /**
     * @param operator the operator to set
     */
    public void setOperator(SearchQueryGroupOperator operator) {
        this.operator = operator;
    }

    /**
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }
}
