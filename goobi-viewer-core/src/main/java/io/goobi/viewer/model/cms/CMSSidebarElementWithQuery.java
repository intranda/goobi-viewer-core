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
package io.goobi.viewer.model.cms;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DataManager;

/**
 * <p>
 * CMSSidebarElementWithQuery class.
 * </p>
 */
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
     * <p>
     * Constructor for CMSSidebarElementWithQuery.
     * </p>
     */
    public CMSSidebarElementWithQuery() {

    }

    /**
     * <p>
     * Constructor for CMSSidebarElementWithQuery.
     * </p>
     *
     * @param original a {@link io.goobi.viewer.model.cms.CMSSidebarElementWithQuery} object.
     * @param owner a {@link io.goobi.viewer.model.cms.CMSPage} object.
     */
    public CMSSidebarElementWithQuery(CMSSidebarElementWithQuery original, CMSPage owner) {
        super(original, owner);
        this.searchField = original.searchField;
        this.resultDisplayLimit = original.resultDisplayLimit;
        this.additionalQuery = original.additionalQuery;
        this.descendingOrder = original.descendingOrder;
    }

    /**
     * <p>
     * Getter for the field <code>searchField</code>.
     * </p>
     *
     * @return the searchField
     */
    public String getSearchField() {
        return searchField;
    }

    /**
     * <p>
     * Setter for the field <code>searchField</code>.
     * </p>
     *
     * @param searchField the searchField to set
     */
    public void setSearchField(String searchField) {
        this.searchField = searchField;
    }

    /**
     * <p>
     * Getter for the field <code>resultDisplayLimit</code>.
     * </p>
     *
     * @return the resultDisplayLimit
     */
    public Integer getResultDisplayLimit() {
        return resultDisplayLimit;
    }

    /**
     * <p>
     * Setter for the field <code>resultDisplayLimit</code>.
     * </p>
     *
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
     * <p>
     * getAdditionalQuerySuffix.
     * </p>
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
     * <p>
     * Getter for the field <code>additionalQuery</code>.
     * </p>
     *
     * @return the additionalQuery
     */
    public String getAdditionalQuery() {
        return additionalQuery;
    }

    /**
     * <p>
     * Setter for the field <code>additionalQuery</code>.
     * </p>
     *
     * @param additionalQuery the additionalQuery to set
     */
    public void setAdditionalQuery(String additionalQuery) {
        this.additionalQuery = additionalQuery;
    }

    /**
     * <p>
     * isDescendingOrder.
     * </p>
     *
     * @return the descendingOrder
     */
    public boolean isDescendingOrder() {
        return descendingOrder;
    }

    /**
     * <p>
     * Setter for the field <code>descendingOrder</code>.
     * </p>
     *
     * @param descendingOrder the descendingOrder to set
     */
    public void setDescendingOrder(boolean descendingOrder) {
        this.descendingOrder = descendingOrder;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSSidebarElement#hashCode()
     */
    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        return o.getClass().equals(CMSSidebarElementWithQuery.class) && bothNullOrEqual(getType(), ((CMSSidebarElement) o).getType())
                && bothNullOrEqual(getWidgetTitle(), ((CMSSidebarElementWithQuery) o).getWidgetTitle())
                && bothNullOrEqual(getSearchField(), ((CMSSidebarElementWithQuery) o).getSearchField());
    }

    /** {@inheritDoc} */
    @Override
    public PageList getLinkedPages() {
        if (super.getLinkedPages() == null) {
            setLinkedPages(new PageList());
        }
        return super.getLinkedPages();
    }
    
    /**
     * Currently only used for configurable search drillDown. Return the configured drillDown fields
     * 
     * @return a list of possible values for searchField for this widget
     */
    public List<String> getSearchFieldOptions() {
        return DataManager.getInstance().getConfiguration().getAllDrillDownFields();
    }

}
