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

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
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
    public CustomWidgetType getType() {
        return CustomWidgetType.WIDGET_FIELDFACETS;
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
