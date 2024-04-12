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
package io.goobi.viewer.model.cms.pages.content.types;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_content_rss")
@DiscriminatorValue("rss")
public class CMSRSSContent extends CMSContent {

    private static final String COMPONENT_NAME = "rssfeed";

    @Column(name = "items_per_view")
    private int itemsPerView = 10;
    @Column(name = "filter_query")
    private String filterQuery = "";

    public CMSRSSContent() {
        super();
    }

    private CMSRSSContent(CMSRSSContent orig) {
        super(orig);
        this.itemsPerView = orig.itemsPerView;
        this.filterQuery = orig.filterQuery;
    }

    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    @Override
    public CMSContent copy() {
        return new CMSRSSContent(this);
    }

    public int getItemsPerView() {
        return itemsPerView;
    }

    public void setItemsPerView(int itemsPerView) {
        this.itemsPerView = itemsPerView;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) throws PresentationException {
        return "";
    }

    @Override
    public String getData(Integer w, Integer h) {
        return "";
    }

    /**
     * Alias for {@link #getItemsPerView()}. Used in legacy cms-templates
     * 
     * @return an int
     */
    public int getElementsPerPage() {
        return getItemsPerView();
    }

    public void setElementsPerPage(int num) {
        setItemsPerView(num);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
