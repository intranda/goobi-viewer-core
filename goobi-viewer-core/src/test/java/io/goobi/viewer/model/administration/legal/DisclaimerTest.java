package io.goobi.viewer.model.administration.legal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.administration.legal.DisplayScope.PageScope;

public class DisclaimerTest extends AbstractDatabaseEnabledTest {

    private IDAO dao;
    
    @Before
    public void setup() throws DAOException {
        this.dao = DataManager.getInstance().getDao();
    }
    
    @Test
    public void test_persist() throws DAOException {
        Disclaimer disclaimer = new Disclaimer();
        disclaimer.setDisplayScope(new DisplayScope(PageScope.RECORD, "PI:X"));
        disclaimer.getAcceptanceScope().setDaysToLive(8);
        dao.saveDisclaimer(disclaimer);
        disclaimer.getDisplayScope().setFilterQuery("PI:Y");
        Disclaimer storage = dao.getDisclaimer();
        assertNotNull(storage);
        assertEquals("PI:X", storage.getDisplayScope().getFilterQuery());
        assertEquals(8, storage.getAcceptanceScope().getDaysToLive());
    }

    @Test
    public void testScope() {
        Disclaimer disclaimer = new Disclaimer();
        assertEquals(14, disclaimer.getAcceptanceScope().getDaysToLive());
        assertEquals(ConsentScope.StorageMode.LOCAL, disclaimer.getAcceptanceScope().getStorageMode());
        disclaimer.getAcceptanceScope().setDaysToLive(7);
        assertEquals(7, disclaimer.getAcceptanceScope().getDaysToLive());
        disclaimer.getAcceptanceScope().setStorageMode(ConsentScope.StorageMode.SESSION);
        assertEquals(ConsentScope.StorageMode.SESSION, disclaimer.getAcceptanceScope().getStorageMode());
    }
}
