package io.goobi.viewer.model.cms.widgets.embed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;

import java.util.Locale;

import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.cms.widgets.HtmlSidebarWidget;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;

public class CMSSidebarElementCustomTest extends AbstractDatabaseEnabledTest {

    @Test
    public void test() throws DAOException {
        CustomSidebarWidget widget = new HtmlSidebarWidget();
        widget.setId(11l);
        widget.getTitle().setText("Titel", Locale.GERMAN);
        CMSPage owner = DataManager.getInstance().getDao().getCMSPage(1l);
        assumeNotNull(owner);
        
        CMSSidebarElementCustom element = new CMSSidebarElementCustom(widget, owner);
        assertEquals(owner, element.getOwnerPage());
        assertEquals(widget, element.getWidget());
        assertEquals(WidgetGenerationType.CUSTOM, element.getGenerationType());
        assertEquals(CustomWidgetType.WIDGET_HTML, element.getContentType());
        assertEquals(widget.getTitle(), element.getTitle());
    }

}
