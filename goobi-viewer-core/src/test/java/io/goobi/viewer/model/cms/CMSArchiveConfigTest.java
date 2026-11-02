/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CMSArchiveConfigTest {

    @Test
    void copyConstructor_shouldCopyCatalogViewSettings() {
        CMSArchiveConfig original = new CMSArchiveConfig("catalog");
        original.setCardCatalog(true);
        original.setCatalogType(CMSArchiveConfig.CatalogType.VOLUME);

        CMSArchiveConfig copy = new CMSArchiveConfig(original);

        assertTrue(copy.isCardCatalog());
        assertEquals(CMSArchiveConfig.CatalogType.VOLUME, copy.getCatalogType());
    }

    @Test
    void getCatalogType_shouldDefaultToCardCatalog() {
        CMSArchiveConfig config = new CMSArchiveConfig("catalog");
        assertEquals(CMSArchiveConfig.CatalogType.CARD, config.getCatalogType());

        config.setCatalogType(null);
        assertEquals(CMSArchiveConfig.CatalogType.CARD, config.getCatalogType());
    }
}
