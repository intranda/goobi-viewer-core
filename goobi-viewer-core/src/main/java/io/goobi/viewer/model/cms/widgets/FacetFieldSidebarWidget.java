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

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSRecordListContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSSearchContent;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A subtype of {@link CustomSidebarWidget} to display a list of possible values of a given SOLR field and link to a search listing of items with a
 * specific value
 *
 * @author florian
 *
 */
@Entity
@DiscriminatorValue("FacetFieldSidebarWidget")
public class FacetFieldSidebarWidget extends CustomSidebarWidget {

    private static final long serialVersionUID = 265864701646294713L;

    @Column(name = "facet_field", nullable = true, columnDefinition = "TINYTEXT")
    private String facetField = "";
    @Column(name = "filter_query", nullable = true, columnDefinition = "MEDIUMTEXT")
    private String filterQuery = "";
    @Column(name = "num_entries")
    private int numEntries = 5;

    /**
     * Empty default constructor
     */
    public FacetFieldSidebarWidget() {

    }

    /**
     * Cloning constructor
     *
     * @param o
     */
    public FacetFieldSidebarWidget(FacetFieldSidebarWidget o) {
        super(o);
        this.facetField = o.facetField;
        this.filterQuery = o.filterQuery;
        this.numEntries = o.numEntries;
    }

    /**
     * Contains the SOLR field holding the values to list
     * 
     * @return the facetField
     */
    public String getFacetField() {
        return facetField;
    }

    /**
     * Set the SOLR field for which to list values
     * 
     * @param facetField the facetField to set
     */
    public void setFacetField(String facetField) {
        this.facetField = facetField;
    }

    /**
     * An additional SOLR query. If this is not empty, only values of the {@link #getFacetField()} are listed that are contained in documents meeting
     * this query. This is also true for the linked result lists
     * 
     * @return the filterQuery
     */
    public String getFilterQuery() {
        return filterQuery;
    }

    public String getCombinedFilterQuery(CMSPage page) {
        if (page != null && page.hasSearchFunctionality()) {
            Optional<CMSContent> searchContent = page.getComponents()
                    .stream()
                    .flatMap(c -> c.getContentItems().stream())
                    .map(c -> c.getContent())
                    .filter(content -> content instanceof CMSSearchContent || content instanceof CMSRecordListContent)
                    .findAny();
            String searchPrefix = searchContent.map(content -> {
                if (content instanceof CMSSearchContent) {
                    return ((CMSSearchContent) content).getSearchPrefix();
                } else {
                    return ((CMSRecordListContent) content).getSolrQuery();
                }
            })
                    .orElse("");
            if(StringUtils.isNoneBlank(searchPrefix, this.filterQuery)) {
                return String.format("+(%s) +(%s)", searchPrefix, this.filterQuery);
            } else if(StringUtils.isNotBlank(searchPrefix)) {
                return searchPrefix;
            } else {
                return filterQuery;
            }
        } else {
            return filterQuery;
        }
    }

    /**
     * Set the value of {@link #getFilterQuery()}
     * 
     * @param filterQuery the filterQuery to set
     */
    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    @Override
    public CustomWidgetType getType() {
        return CustomWidgetType.WIDGET_FIELDFACETS;
    }

    /**
     *
     * @return the number of field values displayed in the widget
     */
    public int getNumEntries() {
        return numEntries;
    }

    /**
     * Set the number of field values displayed in the widget
     * 
     * @param numEntries
     */
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
