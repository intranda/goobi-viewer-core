package io.goobi.viewer.model.cms.widgets;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.translations.TranslatedText;

@Entity
@DiscriminatorValue("FacetFieldSidebarWidget")
public class FacetFieldSidebarWidget extends CustomSidebarWidget {

    @Column(name = "facet_field", nullable = true, columnDefinition = "TINYTEXT")
    private String facetField = "";
    @Column(name = "filter_query", nullable = true, columnDefinition = "MEDIUMTEXT")
    private String filterQuery = "";
    @Column(name = "num_entries")
    private int numEntries = 5;
    
    public FacetFieldSidebarWidget() {
        
    }
    
    public FacetFieldSidebarWidget(FacetFieldSidebarWidget o) {
        super(o);
        this.facetField = o.facetField;
        this.filterQuery = o.filterQuery;
        this.numEntries = o.numEntries;
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
    
    @Override
    public CustomWidgetTypes getType() {
        return CustomWidgetTypes.WIDGET_FIELDFACETS;
    }
    
    public int getNumEntries() {
        return numEntries;
    }
    
    public void setNumEntries(int numEntries) {
        this.numEntries = numEntries;
    }
    
    /**
     * Override default title to always show the selected facetField
     */
    @Override
    public TranslatedText getTitle() {
        return new TranslatedText(ViewerResourceBundle.getTranslations(this.facetField, true));
    }
    
}
