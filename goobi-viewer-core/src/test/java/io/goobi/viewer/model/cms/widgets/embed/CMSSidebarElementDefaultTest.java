package io.goobi.viewer.model.cms.widgets.embed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;

import java.util.Locale;

import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.type.DefaultWidgetType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;

public class CMSSidebarElementDefaultTest extends AbstractDatabaseEnabledTest  {

    @Test
    public void test() throws DAOException {
        CMSPage owner = DataManager.getInstance().getDao().getCMSPage(1l);
        assumeNotNull(owner);
        
        CMSSidebarElementDefault element = new CMSSidebarElementDefault(DefaultWidgetType.WIDGET_SEARCH, owner);
        assertEquals(owner, element.getOwnerPage());
        assertEquals(WidgetGenerationType.DEFAULT, element.getGenerationType());
        assertEquals(DefaultWidgetType.WIDGET_SEARCH, element.getContentType());
        assertEquals(ViewerResourceBundle.getTranslation(DefaultWidgetType.WIDGET_SEARCH.getLabel(),Locale.GERMAN), element.getTitle().getValue(Locale.GERMAN).orElse(""));
    }

}
