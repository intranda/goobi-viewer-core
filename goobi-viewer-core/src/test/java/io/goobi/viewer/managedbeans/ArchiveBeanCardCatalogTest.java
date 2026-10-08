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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans;

import java.lang.reflect.Field;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.model.archives.ArchiveManager;
import io.goobi.viewer.model.archives.ArchiveResource;
import io.goobi.viewer.model.cms.CMSArchiveConfig;

class ArchiveBeanCardCatalogTest {

    @Test
    void cardCatalogBootstrapJson_shouldBeSafeInsideAnInlineScript() {
        ArchiveBean bean = new ArchiveBean(Mockito.mock(ArchiveManager.class));
        String identifier = "archive\"</script>\u2028&";
        bean.setCurrentResource(identifier);

        String json = bean.getCardCatalogBootstrapJson("/viewer", "{\"label\":\"</script> & \\\"quoted\\\"\"}");

        Assertions.assertFalse(json.toLowerCase().contains("</script>"));
        Assertions.assertTrue(json.contains("\\u003c"));
        Assertions.assertTrue(json.contains("\\u2028"));
        JSONObject parsed = new JSONObject(json);
        Assertions.assertEquals(identifier, parsed.getString("pi"));
        Assertions.assertEquals("/viewer/api/v1", parsed.getString("apiUrl"));
        Assertions.assertEquals("/viewer", parsed.getString("rootUrl"));
        Assertions.assertEquals("</script> & \"quoted\"", parsed.getJSONObject("msg").getString("label"));
    }

    @Test
    void initializeArchiveTree_shouldClearCardCatalogConfigWhenArchiveIsUnknown() throws Exception {
        ArchiveBean bean = new ArchiveBean(Mockito.mock(ArchiveManager.class));
        bean.setCurrentResource("catalog");
        CMSArchiveConfig config = Mockito.mock(CMSArchiveConfig.class);
        Mockito.when(config.getPi()).thenReturn("catalog");
        Mockito.when(config.isCardCatalog()).thenReturn(true);
        Field archiveConfig = ArchiveBean.class.getDeclaredField("archiveConfig");
        archiveConfig.setAccessible(true);
        archiveConfig.set(bean, config);
        bean.setDatabaseLoaded(true);
        Assertions.assertTrue(bean.isCardCatalog());

        bean.setCurrentResource("other");
        Assertions.assertFalse(bean.isCardCatalog());
        bean.initializeArchiveTree();

        Assertions.assertNull(bean.getArchiveConfig());
        Assertions.assertFalse(bean.isDatabaseLoaded());
        Assertions.assertFalse(bean.isCardCatalog());
    }

    @Test
    void archiveDisplayLabel_shouldUseResourceNameWithoutLoadingAnArchiveTree() {
        ArchiveManager manager = Mockito.mock(ArchiveManager.class);
        Mockito.when(manager.getArchive("catalog")).thenReturn(new ArchiveResource("Card catalog", "catalog", null, "0"));
        ArchiveBean bean = new ArchiveBean(manager);
        bean.setCurrentResource("catalog");

        Assertions.assertNull(bean.getArchiveTree());
        Assertions.assertEquals("Card catalog", bean.getArchiveDisplayLabel());
        Assertions.assertEquals("Card catalog", bean.getBreadcrumbTitle());
    }
}
