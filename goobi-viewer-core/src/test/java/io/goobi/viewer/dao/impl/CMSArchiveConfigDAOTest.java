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
package io.goobi.viewer.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.cms.CMSArchiveConfig;

class CMSArchiveConfigDAOTest extends AbstractDatabaseEnabledTest {

    /**
     * @verifies persist and reload card catalog flag and catalog type
     */
    @Test
    void saveCMSArchiveConfig_shouldPersistAndReloadCardCatalogFlagAndCatalogType() throws Exception {
        CMSArchiveConfig config = new CMSArchiveConfig("PI_CARDCATALOG");
        config.setDateUpdated(LocalDateTime.now());
        config.setCardCatalog(true);
        config.setCatalogType(CMSArchiveConfig.CatalogType.VOLUME);

        assertNotNull(DataManager.getInstance().getDao().saveCMSArchiveConfig(config));

        Optional<CMSArchiveConfig> loaded = DataManager.getInstance().getDao().getCmsArchiveConfigForArchive("PI_CARDCATALOG");
        assertTrue(loaded.isPresent());
        assertTrue(loaded.get().isCardCatalog());
        assertEquals(CMSArchiveConfig.CatalogType.VOLUME, loaded.get().getCatalogType());
    }

    /**
     * Regression: deleteCMSArchiveConfig used to fetch its reference via the wrong entity class (Role), so deletion never removed the config.
     *
     * @verifies delete config
     */
    @Test
    void deleteCMSArchiveConfig_shouldDeleteConfig() throws Exception {
        CMSArchiveConfig config = new CMSArchiveConfig("PI_CARDCATALOG_DELETE");
        config.setDateUpdated(LocalDateTime.now());
        config.setCardCatalog(true);
        assertNotNull(DataManager.getInstance().getDao().saveCMSArchiveConfig(config));

        CMSArchiveConfig saved =
                DataManager.getInstance().getDao().getCmsArchiveConfigForArchive("PI_CARDCATALOG_DELETE").orElseThrow();
        assertNotNull(saved.getId());

        assertTrue(DataManager.getInstance().getDao().deleteCMSArchiveConfig(saved));
        assertFalse(DataManager.getInstance().getDao().getCmsArchiveConfigForArchive("PI_CARDCATALOG_DELETE").isPresent());
    }
}
