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
package de.intranda.digiverso.presentation.model.cms;

import de.intranda.digiverso.presentation.model.cms.CMSSidebarElement.WidgetMode;
import de.intranda.digiverso.presentation.model.misc.GeoLocationInfo;

public enum SidebarElementType {

    searchField("widgetSearchField", Category.search),
    searchDrillDown("widgetSearchDrillDown"),
//    chronology("widgetChronology"),
    browsing("widgetBrowsing"),
    bookshelves("widgetBookshelfList"),
    crowdsourcing("widgetCrowdsourcing"),
    mySearches("widgetMySearches"),
    fieldDrillDown("widgetFieldDrillDown", Category.fieldQuery),
    rssFeed("widgetRssFeed"),
    user("widgetUser"),
    workCount("widgetWorkCount"),
    searchDrillDownTopics("widgetSearchDrillDownTopics"),
    cmsPageLinks("widgetCmsPageLinks", Category.pageLinks),
    geoLocations("widgetGeoLocations", Category.geoLocations);

    private final String label;
    private final Category type;

    private SidebarElementType(String label) {
        this.label = label;
        this.type = Category.standard;
    }

    private SidebarElementType(String label, Category type) {
        this.label = label;
        this.type = type;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the type
     */
    public Category getType() {
        return type;
    }

    /**
     * @return
     */
    public CMSSidebarElement createSidebarElement() {
        CMSSidebarElement element;
        switch (this.type) {
            case fieldQuery:
                element = new CMSSidebarElementWithQuery();
                break;
            case search:
                element = new CMSSidebarElementWithSearch();
                element.setLinkedPages(new PageList());
                break;
            case pageLinks:
                element = new CMSSidebarElement();
                element.setLinkedPages(new PageList());
                break;
            case geoLocations:
                element = new CMSSidebarElement();
                element.initGeolocations(new GeoLocationInfo());
                break;
            default:
                element = new CMSSidebarElement();
        }
        element.setType(getLabel());
        element.setWidgetMode(WidgetMode.STANDARD);
        return element;
    }

    public enum Category {
        standard,
        custom,
        fieldQuery,
        pageLinks, 
        geoLocations, search;
    }
}
