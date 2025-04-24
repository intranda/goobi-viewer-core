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
package io.goobi.viewer.model.archives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

class ArchiveManagerTest extends AbstractSolrEnabledTest {

    SolrEADParser eadParser;
    List<ArchiveResource> possibleDatabases;

    @BeforeEach
    void before() {
        try {
            SolrEADParser tempParser = new SolrEADParser();
            tempParser.updateAssociatedRecordMap();
            List<ArchiveResource> tempDatabases = tempParser.getPossibleDatabases();
            if (!tempDatabases.isEmpty()) {
                ArchiveEntry root =
                        tempParser.loadDatabase(tempDatabases.get(0), DataManager.getInstance().getConfiguration().getArchivesLazyLoadingThreshold());

                possibleDatabases = new ArrayList<>();
                possibleDatabases.add(new ArchiveResource("resource 1", "r1",
                        ZonedDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.systemDefault()).format(ArchiveResource.DATE_TIME_FORMATTER), "10"));
                possibleDatabases
                        .add(new ArchiveResource("resource 2", "r2", ZonedDateTime.now().format(ArchiveResource.DATE_TIME_FORMATTER), "10"));

                eadParser = new SolrEADParser() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public List<ArchiveResource> getPossibleDatabases() {
                        return possibleDatabases;
                    }

                    @Override
                    public ArchiveEntry loadDatabase(ArchiveResource database, int lazyLoadingThreshold) {
                        return root;
                    }
                };
            }
        } catch (PresentationException | IndexUnreachableException e) {
            fail(e.toString());
        }
    }

    @Test
    void testGetDatabases() {
        ArchiveManager archiveManager = new ArchiveManager(eadParser);
        assertEquals(2, archiveManager.getDatabases().size());
    }

    @Test
    void testGetNodeTypes() {
        ArchiveManager archiveManager = new ArchiveManager(eadParser);
        assertEquals("collection", archiveManager.getNodeType("collection").getName());
        assertEquals("folder", archiveManager.getNodeType("folder").getName());
        assertEquals("folder", archiveManager.getNodeType("notmapped").getName());
        assertEquals("folder", archiveManager.getNodeType("").getName());
        assertEquals("folder", archiveManager.getNodeType(null).getName());
        assertEquals("fa fa-folder-open-o", archiveManager.getNodeType(null).getIconClass());
        assertEquals("fa fa-file-video-o", archiveManager.getNodeType("video").getIconClass());
        assertEquals("video", archiveManager.getNodeType("video").getName());
    }

    @Test
    void testGetDatabase() throws Exception {
        {
            ArchiveManager archiveManager = Mockito.spy(new ArchiveManager(eadParser));
            ArchiveTree tree = archiveManager.getArchiveTree("r1");
            assertNotNull(tree);
        }
        {
            ArchiveManager archiveManager = Mockito.spy(new ArchiveManager(eadParser));
            ArchiveTree tree = archiveManager.getArchiveTree("r2");
            assertNotNull(tree);
        }
        {
            ArchiveManager archiveManager = Mockito.spy(new ArchiveManager(eadParser));
            ArchiveTree tree = archiveManager.getArchiveTree("r3");
            assertNull(tree);
        }
    }

    @Test
    void testUpdateDatabase() throws Exception {
        ArchiveManager archiveManager = Mockito.spy(new ArchiveManager(eadParser));
        archiveManager.getArchiveTree("r1");
        archiveManager.getArchiveTree("r1");
        Mockito.verify(archiveManager, Mockito.times(1)).loadDatabase(Mockito.any(), Mockito.any());
    }

    @Test
    void testAddNewArchive() {
        assertNotNull(possibleDatabases);
        ArchiveManager archiveManager = new ArchiveManager(eadParser);

        ArchiveResource newArchive = new ArchiveResource("resource 3", "r3",
                ZonedDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.systemDefault()).format(ArchiveResource.DATE_TIME_FORMATTER), "10");
        possibleDatabases.add(newArchive);
        assertNull(archiveManager.getArchive("r3"));
        archiveManager.updateArchiveList();
        assertNotNull(archiveManager.getArchive("r3"));
    }

    @Test
    void testRemoveArchive() {
        assertNotNull(possibleDatabases);
        ArchiveManager archiveManager = new ArchiveManager(eadParser);
        possibleDatabases.remove(1);
        assertNotNull(archiveManager.getArchive("r2"));
        archiveManager.updateArchiveList();
        assertNull(archiveManager.getArchive("r2"));
    }

    /**
     * @see ArchiveManager#loadTree(ArchiveEntry)
     * @verifies load tree correctly
     */
    @Test
    void loadTree_shouldLoadTreeCorrectly() throws Exception {
        assertNotNull(possibleDatabases, "No EAD record in the index.");
        ArchiveEntry entry =
                eadParser.loadDatabase(possibleDatabases.get(0), DataManager.getInstance().getConfiguration().getArchivesLazyLoadingThreshold());
        assertNotNull(entry);
        ArchiveTree tree = ArchiveManager.loadTree(entry);
        assertNotNull(tree);
    }
}
