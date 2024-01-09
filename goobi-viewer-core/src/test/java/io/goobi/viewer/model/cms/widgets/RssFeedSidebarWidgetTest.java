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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.solr.SolrConstants;

public class RssFeedSidebarWidgetTest extends AbstractDatabaseEnabledTest {

    @Test
    void testPersist() throws DAOException {
        RssFeedSidebarWidget widget = new RssFeedSidebarWidget();
        widget.getDescription().setValue("Beschreibung", Locale.GERMAN);
        widget.getDescription().setValue("Description", Locale.ENGLISH);
        widget.getTitle().setValue("Titel", Locale.GERMAN);
        widget.getTitle().setValue("Title", Locale.ENGLISH);
        widget.setSortField(SolrConstants.YEARPUBLISH);
        widget.setDescendingSorting(false);
        widget.setFilterQuery("+DC:all");
        widget.setNumEntries(9);

        IDAO dao = DataManager.getInstance().getDao();

        dao.addCustomWidget(widget);
        RssFeedSidebarWidget copy = (RssFeedSidebarWidget) dao.getCustomWidget(widget.getId());
        assertNotNull(copy);
        assertFalse(copy.isEmpty(Locale.GERMAN));
        assertEquals(widget.getSortField(), copy.getSortField());
        assertEquals(widget.getFilterQuery(), copy.getFilterQuery());
        assertEquals(widget.isDescendingSorting(), copy.isDescendingSorting());
        assertEquals(widget.getNumEntries(), copy.getNumEntries());

    }

    @Test
    void testClone() {
        RssFeedSidebarWidget widget = new RssFeedSidebarWidget();
        widget.getDescription().setValue("Beschreibung", Locale.GERMAN);
        widget.getDescription().setValue("Description", Locale.ENGLISH);
        widget.setSortField(SolrConstants.LABEL);
        widget.setFilterQuery("+DC:all");
        widget.setDescendingSorting(false);
        widget.setId(2l);
        widget.setNumEntries(11);
        widget.setCollapsed(true);
        widget.setStyleClass("testcssstyle");

        RssFeedSidebarWidget clone = new RssFeedSidebarWidget(widget);
        assertEquals(widget.getSortField(), clone.getSortField());
        assertEquals(widget.getFilterQuery(), clone.getFilterQuery());
        assertEquals(widget.isDescendingSorting(), clone.isDescendingSorting());
        assertEquals(widget.getId(), clone.getId());
        assertEquals(widget.getNumEntries(), clone.getNumEntries());
        assertEquals(widget.isCollapsed(), clone.isCollapsed());
        assertEquals(widget.getStyleClass(), clone.getStyleClass());
    }

    @Test
    void testType() {
        assertEquals(CustomWidgetType.WIDGET_RSSFEED, new RssFeedSidebarWidget().getType());
    }

}
