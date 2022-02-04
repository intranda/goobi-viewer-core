package io.goobi.viewer.model.cms.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;

public class PageListSidebarWidgetTest extends AbstractDatabaseEnabledTest {
    
    @Test
    public void testPersist() throws DAOException {
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
    public void testClone() {
        PageListSidebarWidget widget = new PageListSidebarWidget();
        widget.getTitle().setValue("Titel", Locale.GERMAN);
        widget.setPageIds(List.of(23l, 93l, 1023l, 2l));
        
        PageListSidebarWidget clone = new PageListSidebarWidget(widget);
        assertEquals(widget.getTitle(), clone.getTitle());
        assertTrue(CollectionUtils.isEqualCollection(widget.getPageList().getPages(), clone.getPageList().getPages()));
    }

    @Test
    public void testType() {
        assertEquals(CustomWidgetType.WIDGET_CMSPAGES, new PageListSidebarWidget().getType());
    }
}
