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
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;

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
    
    @Test
    public void testClone() {
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
    public void testType() {
        assertEquals(CustomWidgetType.WIDGET_HTML, new HtmlSidebarWidget().getType());
    }

}
