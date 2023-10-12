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

/**
 * All types of sidebar widgets which are generated automatically if certain conditions are met, usually if certain CMS content exists Currently
 * (22-01) only CMS-Geomaps provide automatic widgets
 *
 * @author florian
 *
 */
public enum AutomaticWidgetType implements WidgetContentType {

    /**
     * Widget displaying a geomap created in CMS
     */
    WIDGET_CMSGEOMAP("widgetGeoMap", "fa fa-map-o", "widget_geoMap.xhtml");

    private final String label;
    private final String filename;
    private final String iconClass;

    private AutomaticWidgetType(String label, String iconClass, String filename) {
        this.label = label;
        this.filename = filename;
        this.iconClass = iconClass;
    }

    @Override
    public String getIconClass() {
        return this.iconClass;
    }

    public String getLabel() {
        return this.label;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public String getName() {
        return name();
    }

}
