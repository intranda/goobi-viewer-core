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
 * Indicates how data for a sidebar widget is created and stored.
 * 
 * @author florian
 *
 */
public enum WidgetGenerationType {
    /**
     * Static widgets with no underlying data, just a xhtml component
     */
    DEFAULT(""),
    /**
     * Widgets provided automatically by some other kind of user generated data. The widget simply displays the providing data in some form
     */
    AUTOMATIC("cms_widgets__type_automatic"),
    /**
     * Widgets created manually with individual settings
     */
    CUSTOM("cms_widgets__type_custom");

    private final String label;

    private WidgetGenerationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
