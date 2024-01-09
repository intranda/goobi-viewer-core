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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;

public class PageListSidebarWidgetTest extends AbstractDatabaseEnabledTest {

    @Test
    void testPersist() throws DAOException {
        PageListSidebarWidget widget = new PageListSidebarWidget();
        widget.getDescription().setValue("Beschreibung", Locale.GERMAN);
        widget.getDescription().setValue("Description", Locale.ENGLISH);
        widget.getTitle().setValue("Titel", Locale.GERMAN);
        widget.getTitle().setValue("Title", Locale.ENGLISH);
        widget.setPageIds(List.of(23l, 93l, 1023l, 2l));

        IDAO dao = DataManager.getInstance().getDao();

        dao.addCustomWidget(widget);
        PageListSidebarWidget copy = (PageListSidebarWidget) dao.getCustomWidget(widget.getId());
        assertNotNull(copy);
        assertFalse(copy.isEmpty(Locale.GERMAN));
        assertTrue(CollectionUtils.isEqualCollection(widget.getPageIds(), copy.getPageIds()));
    }

    @Test
    void testClone() {
        PageListSidebarWidget widget = new PageListSidebarWidget();
        widget.getTitle().setValue("Titel", Locale.GERMAN);
        widget.setPageIds(List.of(23l, 93l, 1023l, 2l));

        PageListSidebarWidget clone = new PageListSidebarWidget(widget);
        assertEquals(widget.getTitle(), clone.getTitle());
        assertTrue(CollectionUtils.isEqualCollection(widget.getPageList().getPages(), clone.getPageList().getPages()));
    }

    @Test
    void testType() {
        assertEquals(CustomWidgetType.WIDGET_CMSPAGES, new PageListSidebarWidget().getType());
    }
}
