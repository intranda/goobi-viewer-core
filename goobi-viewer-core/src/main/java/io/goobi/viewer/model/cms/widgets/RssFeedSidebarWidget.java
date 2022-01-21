package io.goobi.viewer.model.cms.widgets;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

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
    public CustomWidgetTypes getType() {
        return CustomWidgetTypes.WIDGET_RSSFEED;
    }
    
}
