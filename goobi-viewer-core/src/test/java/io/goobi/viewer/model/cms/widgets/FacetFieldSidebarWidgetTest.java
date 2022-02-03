package io.goobi.viewer.model.cms.widgets;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.solr.SolrConstants;

public class FacetFieldSidebarWidgetTest extends AbstractDatabaseEnabledTest {

    @Test
    public void testPersist() throws DAOException {
        FacetFieldSidebarWidget widget = new FacetFieldSidebarWidget();
        widget.getDescription().setValue("Beschreibung", Locale.GERMAN);
        widget.getDescription().setValue("Description", Locale.ENGLISH);
        widget.setFacetField(SolrConstants.LABEL);
        widget.setFilterQuery("+DC:all");
        
        IDAO dao = DataManager.getInstance().getDao();
        
        dao.addCustomWidget(widget);
        FacetFieldSidebarWidget copy = (FacetFieldSidebarWidget) dao.getCustomWidget(widget.getId());
        assertNotNull(copy);
        assertEquals(widget.getFacetField(), copy.getFacetField());
        assertEquals(widget.getFilterQuery(), copy.getFilterQuery());
        
    }
    
    @Test
    public void testClone() {
        FacetFieldSidebarWidget widget = new FacetFieldSidebarWidget();
        widget.getDescription().setValue("Beschreibung", Locale.GERMAN);
        widget.getDescription().setValue("Description", Locale.ENGLISH);
        widget.setFacetField(SolrConstants.LABEL);
        widget.setFilterQuery("+DC:all");
        widget.setId(2l);
        widget.setNumEntries(11);
        widget.setCollapsed(true);
        widget.setStyleClass("testcssstyle");
        
        FacetFieldSidebarWidget clone = new FacetFieldSidebarWidget(widget);
        assertEquals(widget.getFacetField(), clone.getFacetField());
        assertEquals(widget.getFilterQuery(), clone.getFilterQuery());
        assertEquals(widget.getId(), clone.getId());
        assertEquals(widget.getNumEntries(), clone.getNumEntries());
        assertEquals(widget.isCollapsed(), clone.isCollapsed());
        assertEquals(widget.getStyleClass(), clone.getStyleClass());
    }
    
    @Test
    public void testType() {
        assertEquals(CustomWidgetType.WIDGET_FIELDFACETS, new FacetFieldSidebarWidget().getType());
    }
}
