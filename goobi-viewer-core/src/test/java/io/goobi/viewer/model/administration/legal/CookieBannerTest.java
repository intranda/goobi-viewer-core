/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.administration.legal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;

class CookieBannerTest extends AbstractDatabaseEnabledTest {

    @Test
    void testPersistence() throws DAOException {
        CookieBanner banner = new CookieBanner();
        banner.setActive(true);
        banner.setIgnoreList(Arrays.asList(1l,5l,10l));
        banner.getText().setText("Deutscher Text", Locale.GERMAN);
        banner.getText().setText("English text", Locale.ENGLISH);

        DataManager.getInstance().getDao().saveCookieBanner(banner);
        CookieBanner loaded = DataManager.getInstance().getDao().getCookieBanner();

        CookieBanner copy = new CookieBanner(loaded);

        assertEquals(loaded.getId(), copy.getId());
        assertEquals(loaded.getText(), copy.getText());
        assertEquals(loaded.isActive(), copy.isActive());
        assertEquals(loaded.getIgnoreList().size(),copy.getIgnoreList().size());
        assertTrue(loaded.getIgnoreList().containsAll(copy.getIgnoreList()));
    }

}
