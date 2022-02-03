package io.goobi.viewer.model.cms.widgets.embed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;

import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.type.AutomaticWidgetType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;
import io.goobi.viewer.model.maps.GeoMap;

public class CMSSidebarElementAutomaticTest extends AbstractDatabaseEnabledTest {

    @Test
    public void test() throws DAOException {
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
