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

import java.util.function.Predicate;

import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;

/**
 * Types of widgets that are always available for CMS pages and cannot be configured
 *
 * @author florian
 *
 */
public enum DefaultWidgetType implements WidgetContentType {

    /**
     * Browsing or "Stöbern" widget, containing all browse terms which are configured in the viewer-config
     */
    WIDGET_BROWSING("browseTitle", "cms_widget__browse__description", "fa fa-list-alt", "widget_browsing.xhtml"),
    /**
     * Displays search facetting for a page with search functionality. Always displays the facet fields configured in viewer-config Also includes
     * chronology-facetting (by year) and geospatial facetting (on a map) which are displayed as independent widgets in the GUI
     */
    WIDGET_FACETTING("faceting", "cms_widget__faceting__description", "fa fa-list-ul", "widget_searchFacets.xhtml", CMSPage::hasSearchFunctionality),
    /**
     * Displays a search input field and link to advanced search
     */
    WIDGET_SEARCH("navigationSearch", "cms_widget__search__description", "fa fa-search", "widget_searchField.xhtml"),
    /**
     * Display the total number of records available in the viewer
     */
    WIDGET_WORKCOUNT("totalNumberOfVolumes", "cms_widget__total_number_of_volumes__description", "fa fa-circle-o", "widget_workCount.xhtml"),

    WIDGET_HIGHLIGHT("cms_widget__highlight__label", "cms_widget__highlight__description", "fa fa-star", "widget_highlight.xhtml");

    private final String label;
    private final String description;
    private final String filename;
    private final String iconClass;
    private final Predicate<CMSPage> allowedForPage;

    private DefaultWidgetType(String label, String description, String iconClass, String filename) {
        this(label, description, iconClass, filename, p -> true);
    }

    private DefaultWidgetType(String label, String description, String iconClass, String filename, Predicate<CMSPage> allowedForPage) {
        this.label = label;
        this.description = description;
        this.filename = filename;
        this.iconClass = iconClass;
        this.allowedForPage = allowedForPage;
    }

    @Override
    public String getIconClass() {
        return this.iconClass;
    }

    public String getLabel() {
        return label;
    }

    /**
     * A message key for a description of this widget type
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public boolean isAllowedForPage(CMSPage page) {
        return this.allowedForPage.test(page);
    }

    @Override
    public boolean isAllowedForPage(CMSPageTemplate template) {
        CMSPage page = new CMSPage(template);
        return this.allowedForPage.test(page);
    }
}
