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
package de.intranda.digiverso.presentation.model.cms;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.apache.commons.lang3.StringUtils;

@Entity
public class CMSSidebarElementWithQuery extends CMSSidebarElement {

    @Column(name = "search_field")
    private String searchField = null;

    @Column(name = "result_display_limit")
    private int resultDisplayLimit = 20;

    @Column(name = "additional_query")
    private String additionalQuery = "";

    @Column(name = "descending_order")
    private boolean descendingOrder = false;

    /**
     * @return the searchField
     */
    public String getSearchField() {
        return searchField;
    }

    /**
     * @param searchField the searchField to set
     */
    public void setSearchField(String searchField) {
        this.searchField = searchField;
    }

    /**
     * @return the resultDisplayLimit
     */
    public Integer getResultDisplayLimit() {
        return resultDisplayLimit;
    }

    /**
     * @param resultDisplayLimit the resultDisplayLimit to set
     */
    public void setResultDisplayLimit(Integer resultDisplayLimit) {
        if (resultDisplayLimit != null) {
            this.resultDisplayLimit = resultDisplayLimit;
        } else {
            this.resultDisplayLimit = 0;
        }
    }

    /**
     * 
     * @return additionalQuery with an AND() wrapper
     * @should build suffix correctly
     */
    public String getAdditionalQuerySuffix() {
        if (StringUtils.isNotBlank(additionalQuery)) {
            return " AND (" + additionalQuery + ")";
        }

        return "";
    }

    /**
     * @return the additionalQuery
     */
    public String getAdditionalQuery() {
        return additionalQuery;
    }

    /**
     * @param additionalQuery the additionalQuery to set
     */
    public void setAdditionalQuery(String additionalQuery) {
        this.additionalQuery = additionalQuery;
    }

    /**
     * @return the descendingOrder
     */
    public boolean isDescendingOrder() {
        return descendingOrder;
    }

    /**
     * @param descendingOrder the descendingOrder to set
     */
    public void setDescendingOrder(boolean descendingOrder) {
        this.descendingOrder = descendingOrder;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.cms.CMSSidebarElement#hashCode()
     */
    @Override
    public int hashCode() {
        int code = super.hashCode();
        if (StringUtils.isNotBlank(getWidgetTitle())) {
            code += HASH_MULTIPLIER * getWidgetTitle().hashCode();
        }
        if (StringUtils.isNotBlank(getSearchField())) {
            code *= HASH_MULTIPLIER * getSearchField().hashCode();
        }
        return code;
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass().equals(CMSSidebarElementWithQuery.class) && bothNullOrEqual(getType(), ((CMSSidebarElement) o).getType())
                && bothNullOrEqual(getWidgetTitle(), ((CMSSidebarElementWithQuery) o).getWidgetTitle())
                && bothNullOrEqual(getSearchField(), ((CMSSidebarElementWithQuery) o).getSearchField());
    }

}
