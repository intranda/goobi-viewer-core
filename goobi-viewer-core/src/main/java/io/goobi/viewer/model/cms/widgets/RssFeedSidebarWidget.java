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
package io.goobi.viewer.model.cms.widgets;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.solr.SolrConstants;

@Entity
@DiscriminatorValue("RssFeedSidebarWidget")
public class RssFeedSidebarWidget extends CustomSidebarWidget {

    @Column(name = "filter_query", nullable = true, columnDefinition = "MEDIUMTEXT")
    private String filterQuery = "";
    @Column(name = "sort_field", nullable = true, columnDefinition = "TEXT")
    private String sortField = "SORT_DATECREATED";
    @Column(name = "descending_sorting")
    private boolean descendingSorting = true;
    @Column(name = "num_entries")
    private int numEntries = 5;
    
    public RssFeedSidebarWidget() {
        
    }
    
    public RssFeedSidebarWidget(RssFeedSidebarWidget o) {
        super(o);
        this.filterQuery = o.filterQuery;
        this.sortField = o.sortField;
        this.descendingSorting = o.descendingSorting;
        this.numEntries = o.numEntries;
    }
    
    /**
     * @return the filterQuery
     */
    public String getFilterQuery() {
        return filterQuery;
    }
    /**
     * @param filterQuery the filterQuery to set
     */
    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }
    /**
     * @return the sortField
     */
    public String getSortField() {
        return sortField;
    }
    /**
     * @param sortField the sortField to set
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }
    /**
     * @return the descendingSorting
     */
    public boolean isDescendingSorting() {
        return descendingSorting;
    }
    /**
     * @param descendingSorting the descendingSorting to set
     */
    public void setDescendingSorting(boolean descendingSorting) {
        this.descendingSorting = descendingSorting;
    }
    /**
     * @return the numEntries
     */
    public int getNumEntries() {
        return numEntries;
    }
    /**
     * @param numEntries the numEntries to set
     */
    public void setNumEntries(int numEntries) {
        this.numEntries = numEntries;
    }
    
    @Override
    public CustomWidgetType getType() {
        return CustomWidgetType.WIDGET_RSSFEED;
    }

    
}
