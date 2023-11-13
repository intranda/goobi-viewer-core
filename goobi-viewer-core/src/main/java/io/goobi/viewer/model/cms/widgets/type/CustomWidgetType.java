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
package io.goobi.viewer.model.cms.widgets.type;

import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;

/**
 * Types of sidebar widgets that contain individual configuration and must be created by a user
 *
 * @author florian
 *
 */
public enum CustomWidgetType implements WidgetContentType {

    /**
     * Displays an RSS feed. Number and sorting of feed item may be configured, as well as a search query to filter the feed items
     */
    WIDGET_RSSFEED("cms__add_widget__select_rss_title", "cms__add_widget__select_rss_desc", "fa fa-rss", "widget_rssFeed.xhtml"),
    /**
     * Display facets for a search field of type 'FACET_'. A filter query for facet results may be configured, as well as the order of facets
     */
    WIDGET_FIELDFACETS("cms__add_widget__field_content__title", "cms__add_widget__field_content__desc", "fa fa-list-ul", "widget_fieldFacets.xhtml"),
    /**
     * Displays links to CMS pages. The linked pages can be selected when creating the widget
     */
    WIDGET_CMSPAGES("cms__add_widget__select_pages_title", "cms__add_widget__select_pages_desc", "fa fa-clone", "widget_cmsPageLinks.xhtml"),
    /**
     * Display an html text
     */
    WIDGET_HTML("cms__add_widget__select_html_title", "cms__add_widget__select_html_desc", "fa fa-code", "widget_custom.xhtml");

    private final String label;
    private final String description;
    private final String iconClass;
    private final String filename;

    private CustomWidgetType(String label, String description, String iconClass, String filename) {
        this.label = label;
        this.description = description;
        this.iconClass = iconClass;
        this.filename = filename;
    }

    @Override
    public String getIconClass() {
        return iconClass;
    }

    public String getLabel() {
        return label;
    }

    /**
     * A message key for a description of ths widget type
     * 
     * @return
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public boolean isAllowedForPage(CMSPage page) {
        return true;
    }

    @Override
    public boolean isAllowedForPage(CMSPageTemplate template) {
        return true;
    }
}
