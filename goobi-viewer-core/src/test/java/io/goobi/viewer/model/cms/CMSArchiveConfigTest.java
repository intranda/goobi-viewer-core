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
    void copyConstructor_shouldCopyCardCatalogFlagAndLinks() {
        CMSArchiveConfig original = new CMSArchiveConfig("catalog");
        original.setCardCatalog(true);
        original.setCatalogType(CMSArchiveConfig.CatalogType.VOLUME);
        original.addLink();
        original.getLinks().get(0).setUrl("https://example.org/help");

        CMSArchiveConfig copy = new CMSArchiveConfig(original);

        assertTrue(copy.isCardCatalog());
        assertEquals(CMSArchiveConfig.CatalogType.VOLUME, copy.getCatalogType());
        assertEquals(1, copy.getLinks().size());
        assertEquals("https://example.org/help", copy.getLinks().get(0).getUrl());
        copy.getLinks().get(0).setUrl("https://example.org/other");
        assertEquals("https://example.org/help", original.getLinks().get(0).getUrl());
    }

    @Test
    void getCatalogType_shouldDefaultToCardCatalog() {
        CMSArchiveConfig config = new CMSArchiveConfig("catalog");
        assertEquals(CMSArchiveConfig.CatalogType.CARD, config.getCatalogType());

        config.setCatalogType(null);
        assertEquals(CMSArchiveConfig.CatalogType.CARD, config.getCatalogType());
    }

    @Test
    void normalizeLinks_shouldDropLinksWithoutUrlAndNormalizeTheOthers() {
        CMSArchiveConfig config = new CMSArchiveConfig("catalog");
        config.addLink();
        config.addLink();
        config.getLinks().get(0).setUrl("https://example.org/help");
        config.addLink();
        config.getLinks().get(2).setUrl("   ");
        config.addLink();
        config.getLinks().get(3).setUrl(" www.gei.de ");

        config.normalizeLinks();

        assertEquals(2, config.getLinks().size());
        assertEquals("https://example.org/help", config.getLinks().get(0).getUrl());
        assertEquals("https://www.gei.de", config.getLinks().get(1).getUrl());
    }
}
