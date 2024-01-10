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
package io.goobi.viewer.model.cms.widgets.embed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.cms.widgets.HtmlSidebarWidget;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;

class CMSSidebarElementCustomTest extends AbstractDatabaseEnabledTest {

    @Test
    void test() throws DAOException {
        CustomSidebarWidget widget = new HtmlSidebarWidget();
        widget.setId(11l);
        widget.getTitle().setText("Titel", Locale.GERMAN);
        CMSPage owner = DataManager.getInstance().getDao().getCMSPage(1l);
        Assertions.assertNotNull(owner);

        CMSSidebarElementCustom element = new CMSSidebarElementCustom(widget, owner);
        assertEquals(owner, element.getOwnerPage());
        assertEquals(widget, element.getWidget());
        assertEquals(WidgetGenerationType.CUSTOM, element.getGenerationType());
        assertEquals(CustomWidgetType.WIDGET_HTML, element.getContentType());
        assertEquals(widget.getTitle(), element.getTitle());
    }

}
