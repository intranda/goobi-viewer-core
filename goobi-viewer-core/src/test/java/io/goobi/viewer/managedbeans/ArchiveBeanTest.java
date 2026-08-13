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
package io.goobi.viewer.managedbeans;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.exceptions.ArchiveException;
import io.goobi.viewer.model.archives.ArchiveEntry;
import io.goobi.viewer.model.archives.ArchiveManager;
import io.goobi.viewer.model.archives.ArchiveResource;
import io.goobi.viewer.model.archives.ArchiveTree;

class ArchiveBeanTest extends AbstractSolrEnabledTest {

    /**
     * @verifies clear current resource search string archive tree and database loaded to defaults
     */
    @Test
    void reset_shouldClearCurrentResourceSearchStringArchiveTreeAndDatabaseLoadedToDefaults() {
        ArchiveBean bean = new ArchiveBean();
        bean.setCurrentResource("resource");
        Assertions.assertEquals("resource", bean.getCurrentResource());
        bean.setSearchString("foo");
        Assertions.assertEquals("foo", bean.getSearchString());
        bean.setArchiveTree(new ArchiveTree());
        Assertions.assertNotNull(bean.getArchiveTree());
        bean.setDatabaseLoaded(true);
        Assertions.assertTrue(bean.isDatabaseLoaded());

        bean.reset();
        Assertions.assertEquals("", bean.getCurrentResource());
        Assertions.assertEquals("", bean.getSearchString());
        Assertions.assertNull(bean.getArchiveTree());
        Assertions.assertFalse(bean.isDatabaseLoaded());
    }

    /**
     * @see ArchiveBean#initializeArchiveTree(String)
     * @verifies not throw NullPointerException when archive becomes unresolvable after the null check
     */
    @Test
    void initializeArchiveTree_shouldNotThrowNullPointerExceptionWhenArchiveBecomesUnresolvableAfterTheNullCheck() throws Exception {
        ArchiveEntry rootEntry = new ArchiveEntry(0, 0, null);
        ArchiveTree tree = new ArchiveTree();
        tree.update(rootEntry);

        ArchiveResource resource = new ArchiveResource("resource 1", "r1",
                ZonedDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.systemDefault()).format(ArchiveResource.DATE_TIME_FORMATTER), "10");

        // The archive resolves once and is gone from the shared archive list afterwards, simulating a concurrent
        // reload of that list in another request thread
        ArchiveManager archiveManager = Mockito.mock(ArchiveManager.class);
        Mockito.when(archiveManager.getArchive(Mockito.anyString())).thenReturn(resource, (ArchiveResource) null);
        Mockito.when(archiveManager.getArchiveTree(Mockito.anyString())).thenReturn(tree);

        ArchiveBean bean = new ArchiveBean(archiveManager);
        bean.setCurrentResource("r1");

        try {
            bean.initializeArchiveTree();
        } catch (ArchiveException e) {
            // The CMS archive config lookup needs a database, which is not part of this test setup. Only an
            // unchecked NullPointerException would indicate the regression under test.
        }
        Assertions.assertNotNull(bean.getArchiveTree());
    }

}
