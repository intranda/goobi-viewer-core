package io.goobi.viewer.model.cms.widgets;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("FacetFieldSidebarWidget")
public class FacetFieldSidebarWidget extends CustomSidebarWidget {

    @Column(name = "facet_field", nullable = true, columnDefinition = "TINYTEXT")
    private String facetField = "";
    @Column(name = "filter_query", nullable = true, columnDefinition = "MEDIUMTEXT")
    private String filterQuery = "";
    @Column(name = "numeric_sorting", nullable = true)
    private boolean numericSorting = false;
    @Column(name = "descending_sorting", nullable = true)
    private boolean descendingSorting = false;
    
    public FacetFieldSidebarWidget() {
        
    }
    
    public FacetFieldSidebarWidget(FacetFieldSidebarWidget o) {
        super(o);
        this.facetField = o.facetField;
        this.filterQuery = o.filterQuery;
        this.numericSorting = o.numericSorting;
        this.descendingSorting = o.descendingSorting;
    }
    
    /**
     * @return the facetField
     */
    public String getFacetField() {
        return facetField;
    }
    /**
     * @param facetField the facetField to set
     */
    public void setFacetField(String facetField) {
        this.facetField = facetField;
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
     * @return the numericSorting
     */
    public boolean isNumericSorting() {
        return numericSorting;
    }
    /**
     * @param numericSorting the numericSorting to set
     */
    public void setNumericSorting(boolean numericSorting) {
        this.numericSorting = numericSorting;
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
    
    @Override
    public CustomWidgetTypes getType() {
        return CustomWidgetTypes.WIDGET_FIELDFACETS;
    }
    
    
}
