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
package io.goobi.viewer.model.cms.widgets.type;

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
    WIDGET_BROWSING("browseTitle", "cms_widget__browse__description", "widget_browsing.xhtml"),
    /**
     * Displays search facetting for a page with search functionality. Always displays the facet fields configured in viewer-config 
     * Also includes chronology-facetting (by year) and geospatial facetting (on a map) which are displayed as independent widgets in the GUI
     */
    WIDGET_FACETTING("faceting", "cms_widget__faceting__description", "widget_searchFacets.xhtml"),
    /**
     * Displays a search input field and link to advanced search
     */
    WIDGET_SEARCH("navigationSearch", "cms_widget__search__description", "widget_searchField.xhtml"),
    /**
     * Display the total number of records available in the viewer
     */
    WIDGET_WORKCOUNT("totalNumberOfVolumes", "cms_widget__total_number_of_volumes__description", "widget_workCount.xhtml");
    
    private final String label;
    private final String description;
    private final String filename;
    
    private DefaultWidgetType(String label, String description, String filename) {
        this.label = label;
        this.description = description;
        this.filename = filename;
    }
    
    public String getLabel() {
        return label;
    }
    
    /**
     * A message key for a description of ths widget type
     * @return
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
}
