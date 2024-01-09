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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.administration.legal.DisplayScope.PageScope;

public class DisclaimerTest extends AbstractDatabaseEnabledTest {

    private IDAO dao;

    @BeforeEach
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
