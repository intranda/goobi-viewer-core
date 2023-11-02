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

public interface WidgetContentType {

    public String getIconClass();

    /**
     * Get a message key providing a label for this widget
     * 
     * @return
     */
    public String getLabel();

    /**
     * The filename of the xhtml component for this widget
     * 
     * @return
     */
    public String getFilename();

    /**
     * The enum value of this type, taken from the classes implementing this interface
     * 
     * @return
     */
    public String getName();

    /**
     * Get the WidgetContentType for which {@link #getName()} method matches the given name
     * 
     * @param name
     * @return null if no matching type exists
     */
    public static WidgetContentType valueOf(String name) {
        try {
            return DefaultWidgetType.valueOf(name);
        } catch (IllegalArgumentException e) {
            try {
                return AutomaticWidgetType.valueOf(name);
            } catch (IllegalArgumentException e2) {
                try {
                    return CustomWidgetType.valueOf(name);
                } catch (IllegalArgumentException e3) {
                    return null;
                }
            }
        }
    }

    /**
     * Get the corresponding generation type. Widgets of class {@link DefaultWidgetType} have the generation type {@link WidgetGenerationType.DEFAULT}
     * Widgets of class {@link AutomaticWidgetType} have the generation type {@link WidgetGenerationType.AUTOMATIC} Widgets of class
     * {@link CustomWidgetType} have the generation type {@link WidgetGenerationType.CUSTOM}
     * 
     * @param type
     * @return
     */
    public static WidgetGenerationType getGenerationType(WidgetContentType type) {
        switch (type.getClass().getSimpleName()) {
            case "DefaultWidgetType":
                return WidgetGenerationType.DEFAULT;
            case "AutomaticWidgetType":
                return WidgetGenerationType.AUTOMATIC;
            case "CustomWidgetType":
                return WidgetGenerationType.CUSTOM;
            default:
                throw new IllegalArgumentException("Generation type for WidgetContentType " + type + " not known");
        }
    }
    
    public boolean isAllowedForPage(CMSPage page);
}
