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
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.cms.CMSArchiveConfig;
import io.goobi.viewer.model.cms.CMSArchiveLink;

class CMSArchiveConfigDAOTest extends AbstractDatabaseEnabledTest {

    /**
     * @verifies persist and reload card catalog flag and help links
     */
    @Test
    void saveCMSArchiveConfig_shouldPersistAndReloadCardCatalogFlagAndHelpLinks() throws Exception {
        CMSArchiveConfig config = new CMSArchiveConfig("PI_CARDCATALOG");
        config.setDateUpdated(LocalDateTime.now());
        config.setCardCatalog(true);
        config.addLink();
        config.getLinks().get(0).getLabel().setText("Ordnungskriterien", Locale.GERMAN);
        config.getLinks().get(0).setUrl("https://example.org/ordnungskriterien");
        config.addLink();
        config.getLinks().get(1).getLabel().setText("Suchhilfe", Locale.GERMAN);
        config.getLinks().get(1).setUrl("https://example.org/suchhilfe");

        assertTrue(DataManager.getInstance().getDao().saveCMSArchiveConfig(config));

        Optional<CMSArchiveConfig> loaded = DataManager.getInstance().getDao().getCmsArchiveConfigForArchive("PI_CARDCATALOG");
        assertTrue(loaded.isPresent());
        assertTrue(loaded.get().isCardCatalog());
        assertEquals(2, loaded.get().getLinks().size());
        CMSArchiveLink first = loaded.get().getLinks().get(0);
        assertEquals("Ordnungskriterien", first.getLabel().getText(Locale.GERMAN));
        assertEquals("https://example.org/ordnungskriterien", first.getUrl());
        assertEquals("https://example.org/suchhilfe", loaded.get().getLinks().get(1).getUrl());
    }

    /**
     * Regression: deleteCMSArchiveConfig used to fetch its reference via the wrong entity class (Role), so deletion never removed the config.
     *
     * @verifies delete config with its links
     */
    @Test
    void deleteCMSArchiveConfig_shouldDeleteConfigWithItsLinks() throws Exception {
        CMSArchiveConfig config = new CMSArchiveConfig("PI_CARDCATALOG_DELETE");
        config.setDateUpdated(LocalDateTime.now());
        config.setCardCatalog(true);
        config.addLink();
        config.getLinks().get(0).getLabel().setText("Hinweise", Locale.GERMAN);
        config.getLinks().get(0).setUrl("https://example.org/hinweise");
        assertTrue(DataManager.getInstance().getDao().saveCMSArchiveConfig(config));

        CMSArchiveConfig saved =
                DataManager.getInstance().getDao().getCmsArchiveConfigForArchive("PI_CARDCATALOG_DELETE").orElseThrow();
        assertNotNull(saved.getId());

        assertTrue(DataManager.getInstance().getDao().deleteCMSArchiveConfig(saved));
        assertFalse(DataManager.getInstance().getDao().getCmsArchiveConfigForArchive("PI_CARDCATALOG_DELETE").isPresent());
    }
}
