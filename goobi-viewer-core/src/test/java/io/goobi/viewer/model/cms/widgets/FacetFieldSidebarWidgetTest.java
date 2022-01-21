package io.goobi.viewer.model.cms.widgets;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.solr.SolrConstants;

public class FacetFieldSidebarWidgetTest extends AbstractDatabaseEnabledTest {

    @Test
    public void testPersist() throws DAOException {
        FacetFieldSidebarWidget widget = new FacetFieldSidebarWidget();
        widget.getDescription().setValue("Beschreibung", Locale.GERMAN);
        widget.getDescription().setValue("Description", Locale.ENGLISH);
        widget.getTitle().setValue("Titel", Locale.GERMAN);
        widget.getTitle().setValue("Title", Locale.ENGLISH);
        widget.setFacetField(SolrConstants.LABEL);
        widget.setFilterQuery("+DC:all");
        
        IDAO dao = DataManager.getInstance().getDao();
        
        dao.addCustomWidget(widget);
        FacetFieldSidebarWidget copy = (FacetFieldSidebarWidget) dao.getCustomWidget(widget.getId());
        assertNotNull(copy);
        assertFalse(copy.isEmpty(Locale.GERMAN));
        assertEquals(widget.getFacetField(), copy.getFacetField());
        assertEquals(widget.getFilterQuery(), copy.getFilterQuery());
        
    }

}
