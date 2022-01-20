package io.goobi.viewer.model.cms.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;

import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;

public class HtmlSidebarWidgetTest extends AbstractDatabaseEnabledTest{

    @Test
    public void testPersist() throws DAOException {
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

}
