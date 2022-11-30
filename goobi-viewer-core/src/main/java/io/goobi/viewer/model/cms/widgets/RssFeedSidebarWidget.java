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
package io.goobi.viewer.model.cms.widgets;

import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A subtype of {@link CustomSidebarWidget} to display a RSS feed of selected records. The default settings show a list of the five last imported
 * records
 *
 * @author florian
 *
 */
@Entity
@DiscriminatorValue("RssFeedSidebarWidget")
public class RssFeedSidebarWidget extends CustomSidebarWidget {

    private static final long serialVersionUID = 6410849010435447708L;

    @Column(name = "filter_query", nullable = true, columnDefinition = "MEDIUMTEXT")
    private String filterQuery = "";
    @Column(name = "sort_field", nullable = true, columnDefinition = "TEXT")
    private String sortField = "DATECREATED";
    @Column(name = "descending_sorting")
    private boolean descendingSorting = true;
    @Column(name = "num_entries")
    private int numEntries = 5;

    /**
     * Empty default constructor
     */
    public RssFeedSidebarWidget() {

    }

    /**
     * Cloning constructor
     * 
     * @param o
     */
    public RssFeedSidebarWidget(RssFeedSidebarWidget o) {
        super(o);
        this.filterQuery = o.filterQuery;
        this.sortField = o.sortField;
        this.descendingSorting = o.descendingSorting;
        this.numEntries = o.numEntries;
    }

    /**
     * If this query is not empty only records matching this SOLR query are listed
     * 
     * @return the filterQuery
     */
    public String getFilterQuery() {
        return filterQuery;
    }

    /**
     * Set the {@link #getFilterQuery() filter query}
     * 
     * @param filterQuery the filterQuery to set
     */
    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    /**
     * A SOLR field to select and sort the listed entries by.
     * 
     * @return the sortField
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * Set the {@link #getSortField() sort field}
     * 
     * @param sortField the sortField to set
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    /**
     * Whether the records are selected and sorted ins descending order
     * 
     * @return the descendingSorting
     */
    public boolean isDescendingSorting() {
        return descendingSorting;
    }

    /**
     * Set the {@link #isDescendingSorting() descending sorting}
     * 
     * @param descendingSorting the descendingSorting to set
     */
    public void setDescendingSorting(boolean descendingSorting) {
        this.descendingSorting = descendingSorting;
    }

    /**
     * The number of entries to display
     * 
     * @return The number of entries to display
     */
    public int getNumEntries() {
        return numEntries;
    }

    /**
     * Set the number of entries to display
     * 
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
