package io.goobi.viewer.model.administration.legal;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Locale;

import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;

public class CookieBannerTest extends AbstractDatabaseEnabledTest {

    @Test
    public void testPersistence() throws DAOException {
        CookieBanner banner = new CookieBanner();
        banner.setActive(true);
        banner.setIgnoreList(Arrays.asList(1l,5l,10l));
        banner.getText().setText("Deutscher Text", Locale.GERMAN);
        banner.getText().setText("English text", Locale.ENGLISH);

        DataManager.getInstance().getDao().saveCookieBanner(banner);
        CookieBanner loaded = DataManager.getInstance().getDao().getCookieBanner();
        
        assertTrue(banner == loaded);
        
        CookieBanner copy = new CookieBanner(loaded);
        
        assertEquals(loaded.getId(), copy.getId());
        assertEquals(loaded.getText(), copy.getText());
        assertEquals(loaded.isActive(), copy.isActive());
        assertEquals(loaded.getIgnoreList().size(),copy.getIgnoreList().size());
        assertTrue(loaded.getIgnoreList().containsAll(copy.getIgnoreList()));
    }

}
