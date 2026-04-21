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

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;

class WidgetDisplayElementTest {

    private static final String LOREM_IPSUM =
            "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";
    private static final String LOREM_IPSUM_SHORT = "Lorem ipsum dolor sit amet,";

    /**
     * @verifies return description from type
     */
    @Test
    void getDescriptionOrTypeDescription_shouldReturnDescriptionFromType() {
        CustomSidebarWidget widget = new HtmlSidebarWidget();
        WidgetDisplayElement element = new WidgetDisplayElement(widget);
        Assertions.assertEquals(CustomWidgetType.WIDGET_HTML.getDescription(), element.getDescriptionOrTypeDescription().getText(Locale.GERMAN));
    }

    /**
     * @verifies return description from html text
     */
    @Test
    void getDescriptionOrTypeDescription_shouldReturnDescriptionFromHtmlText() {
        HtmlSidebarWidget widget = new HtmlSidebarWidget();
        widget.getHtmlText().setText(LOREM_IPSUM, Locale.GERMAN);
        WidgetDisplayElement element = new WidgetDisplayElement(widget);
        // Assertions.assertEquals(LOREM_IPSUM_SHORT + "...", element.getDescriptionOrTypeDescription().getText(Locale.GERMAN));
        Assertions.assertEquals(LOREM_IPSUM, element.getDescriptionOrTypeDescription().getText(Locale.GERMAN));
    }

    /**
     * @verifies return description from description text
     */
    @Test
    void getDescriptionOrTypeDescription_shouldReturnDescriptionFromDescriptionText() {
        CustomSidebarWidget widget = new HtmlSidebarWidget();
        widget.getDescription().setText(LOREM_IPSUM, Locale.GERMAN);
        WidgetDisplayElement element = new WidgetDisplayElement(widget);
        // Assertions.assertEquals(LOREM_IPSUM_SHORT + "...", element.getDescriptionOrTypeDescription().getText(Locale.GERMAN));
        Assertions.assertEquals(LOREM_IPSUM, element.getDescriptionOrTypeDescription().getText(Locale.GERMAN));
    }

    /**
     * @see WidgetDisplayElement#getDescription()
     * @verifies description from type
     */
    @Test
    void getDescription_shouldDescriptionFromType() {
        // Verify that getDescription returns the description set via the constructor
        CustomSidebarWidget widget = new HtmlSidebarWidget();
        widget.getDescription().setText("Test description", Locale.ENGLISH);
        WidgetDisplayElement element = new WidgetDisplayElement(widget);
        Assertions.assertNotNull(element.getDescription());
        Assertions.assertFalse(element.getDescription().isEmpty());
    }

}
