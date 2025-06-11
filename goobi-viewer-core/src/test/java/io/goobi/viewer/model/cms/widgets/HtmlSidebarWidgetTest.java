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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;

class HtmlSidebarWidgetTest extends AbstractDatabaseEnabledTest {

    private static final String LOREM_IPSUM =
            "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";
    private static final String LOREM_IPSUM_SHORT = "Lorem ipsum dolor sit amet,";

    @Test
    void testPersist() throws DAOException {
        HtmlSidebarWidget widget = new HtmlSidebarWidget();
        widget.getDescription().setValue("Beschreibung", Locale.GERMAN);
        widget.getDescription().setValue("Description", Locale.ENGLISH);
        widget.getTitle().setValue("Titel", Locale.GERMAN);
        widget.getTitle().setValue("Title", Locale.ENGLISH);
        widget.getHtmlText().setValue("<h1>Text</h1>", Locale.GERMAN);
        widget.getHtmlText().setValue("<h1>Some Text</h1>", Locale.ENGLISH);

        IDAO dao = DataManager.getInstance().getDao();

        dao.addCustomWidget(widget);
        HtmlSidebarWidget copy = (HtmlSidebarWidget) dao.getCustomWidget(widget.getId());
        assertNotNull(copy);
        assertFalse(copy.isEmpty(Locale.GERMAN));
        assertEquals(widget.getHtmlText().getText(Locale.GERMAN), copy.getHtmlText().getText(Locale.GERMAN));

    }

    @Test
    void testClone() {
        HtmlSidebarWidget widget = new HtmlSidebarWidget();
        widget.getDescription().setValue("Beschreibung", Locale.GERMAN);
        widget.getDescription().setValue("Description", Locale.ENGLISH);
        widget.getTitle().setValue("Titel", Locale.GERMAN);
        widget.getTitle().setValue("Title", Locale.ENGLISH);
        widget.getHtmlText().setValue("<h1>Text</h1>", Locale.GERMAN);
        widget.getHtmlText().setValue("<h1>Some Text</h1>", Locale.ENGLISH);
        widget.setId(2l);
        widget.setCollapsed(true);
        widget.setStyleClass("testcssstyle");

        HtmlSidebarWidget copy = new HtmlSidebarWidget(widget);
        assertEquals(widget.getTitle(), copy.getTitle());
        assertFalse(widget.getTitle() == copy.getTitle());
        assertEquals(widget.getHtmlText(), copy.getHtmlText());
        assertFalse(widget.getHtmlText() == copy.getHtmlText());
        assertEquals(widget.getDescription(), copy.getDescription());
        assertFalse(widget.getDescription() == copy.getDescription());
        assertEquals(widget.getId(), copy.getId());
        assertEquals(widget.isCollapsed(), copy.isCollapsed());
        assertEquals(widget.getStyleClass(), copy.getStyleClass());
    }

    @Test
    void testType() {
        assertEquals(CustomWidgetType.WIDGET_HTML, new HtmlSidebarWidget().getType());
    }

    @Test
    void test_shortDescriptionFromDescription() {
        HtmlSidebarWidget widget = new HtmlSidebarWidget();
        Assertions.assertFalse(widget.isHasShortDescription());
        widget.getDescription().setText(LOREM_IPSUM, Locale.GERMAN);
        Assertions.assertTrue(widget.isHasShortDescription());
        Assertions.assertEquals(LOREM_IPSUM_SHORT + "...", widget.getShortDescription(35).getValueOrFallback(Locale.GERMAN));

        widget.getHtmlText().setText("Some other text", Locale.GERMAN);

        Assertions.assertTrue(widget.isHasShortDescription());
        Assertions.assertEquals(LOREM_IPSUM_SHORT + "...", widget.getShortDescription(35).getValueOrFallback(Locale.GERMAN));
    }

    @Test
    void test_shortDescriptionFromText() {
        HtmlSidebarWidget widget = new HtmlSidebarWidget();
        Assertions.assertFalse(widget.isHasShortDescription());
        widget.getHtmlText().setText(LOREM_IPSUM, Locale.GERMAN);
        Assertions.assertTrue(widget.isHasShortDescription());
        Assertions.assertEquals(LOREM_IPSUM_SHORT + "...", widget.getShortDescription(35).getValueOrFallback(Locale.GERMAN));
    }

}
