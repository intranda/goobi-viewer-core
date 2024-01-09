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
import static org.junit.Assume.assumeNotNull;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.widgets.type.AutomaticWidgetType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;
import io.goobi.viewer.model.maps.GeoMap;

class CMSSidebarElementAutomaticTest extends AbstractDatabaseEnabledTest {

    @Test
    void test() throws DAOException {
        GeoMap map = DataManager.getInstance().getDao().getGeoMap(1l);
        assumeNotNull(map);
        CMSPage owner = DataManager.getInstance().getDao().getCMSPage(1l);
        assumeNotNull(owner);

        CMSSidebarElementAutomatic widget = new CMSSidebarElementAutomatic(map, owner);
        assertEquals(owner, widget.getOwnerPage());
        assertEquals(map, widget.getMap());
        assertEquals(WidgetGenerationType.AUTOMATIC, widget.getGenerationType());
        assertEquals(AutomaticWidgetType.WIDGET_CMSGEOMAP, widget.getContentType());
        assertEquals(map.getTitle(), widget.getTitle().getText());
    }

}
