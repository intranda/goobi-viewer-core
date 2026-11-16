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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jdom2.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DateTools;

class SolrEADParserTest extends AbstractSolrEnabledTest {

    private SolrEADParser eadParser;

    @BeforeEach
    void before() {
        eadParser = new SolrEADParser();
    }

    /**
     * @see SolrEADParser#getPossibleDatabases()
     * @verifies return all resources
     */
    @Test
    void getPossibleDatabases_shouldReturnAllResources() throws Exception {
        List<ArchiveResource> resources = eadParser.getPossibleDatabases();
        assertNotNull(resources);
        assertEquals(1, resources.size());
        ArchiveResource resource = resources.get(0);
        assertEquals("Akte_Koch_-_Humboldt_Universitaet", resource.getResourceId());
        assertEquals("Koch, Robert", resource.getResourceName());
        assertEquals(0, resource.getSize());
    }

    /**
     * @verifies format timestamp correctly
     */
    @Test
    void formatDate_shouldFormatTimestampCorrectly() {
        Assertions.assertNull(SolrEADParser.formatDate(null));
        Assertions.assertEquals("2024-02-28T14:29:55Z",
                SolrEADParser.formatDate(DateTools.getMillisFromLocalDateTime(LocalDateTime.of(2024, 02, 28, 14, 29, 55), false)));
    }

    /** Archive node at hierarchy level 3 of the test archive: root / "Koch, Robert" / "1 Werke" / this node. */
    private static final String DEEP_NODE_ID = "A91x16199082136154120181205135958491";

    /**
     * @return Test archive resource
     */
    private ArchiveResource getTestArchive() throws Exception {
        List<ArchiveResource> resources = eadParser.getPossibleDatabases();
        Assertions.assertFalse(resources.isEmpty(), "No EAD record in the index.");
        return resources.get(0);
    }

    /**
     * @see SolrEADParser#loadDatabase(ArchiveResource,int)
     * @verifies load entire tree if below threshold
     */
    @Test
    void loadDatabase_shouldLoadEntireTreeIfBelowThreshold() throws Exception {
        ArchiveResource archive = getTestArchive();
        ArchiveEntry root = eadParser.loadDatabase(archive, Integer.MAX_VALUE);
        assertNotNull(root);
        Assertions.assertFalse(eadParser.isLoadedOnDemand(archive.getResourceId()));
        List<ArchiveEntry> entries = root.getAsFlatList(true);
        Assertions.assertTrue(entries.size() > 100);
        for (ArchiveEntry entry : entries) {
            Assertions.assertTrue(!entry.isHasChildren() || entry.isChildrenLoaded(), "Children not loaded: " + entry.getLabel());
        }
    }

    /**
     * @see SolrEADParser#loadDatabase(ArchiveResource,int)
     * @verifies only load root and its children if at or above threshold
     */
    @Test
    void loadDatabase_shouldOnlyLoadRootAndItsChildrenIfAtOrAboveThreshold() throws Exception {
        ArchiveResource archive = getTestArchive();
        ArchiveEntry root = eadParser.loadDatabase(archive, 1);
        assertNotNull(root);
        Assertions.assertTrue(eadParser.isLoadedOnDemand(archive.getResourceId()));
        Assertions.assertTrue(root.isChildrenLoaded());
        assertEquals(1, root.getSubEntryList().size());

        ArchiveEntry child = root.getSubEntryList().get(0);
        assertEquals("Koch, Robert", child.getLabel());
        assertEquals(1, child.getHierarchyLevel());
        Assertions.assertSame(root, child.getParentNode());
        Assertions.assertTrue(child.isChildrenFound());
        Assertions.assertFalse(child.isChildrenLoaded());
        Assertions.assertTrue(child.getSubEntryList().isEmpty());
    }

    /**
     * @see SolrEADParser#loadChildren(ArchiveEntry)
     * @verifies load only immediate children
     */
    @Test
    void loadChildren_shouldLoadOnlyImmediateChildren() throws Exception {
        ArchiveEntry root = eadParser.loadDatabase(getTestArchive(), 1);
        ArchiveEntry entry = root.getSubEntryList().get(0);

        Assertions.assertTrue(eadParser.loadChildren(entry));
        Assertions.assertTrue(entry.isChildrenLoaded());
        assertEquals(5, entry.getSubEntryList().size());
        // Sorted by archive order
        assertEquals("1 Werke", entry.getSubEntryList().get(0).getLabel());
        assertEquals("5 Sammlungen / Objekte", entry.getSubEntryList().get(4).getLabel());
        for (ArchiveEntry child : entry.getSubEntryList()) {
            assertEquals(2, child.getHierarchyLevel());
            Assertions.assertSame(entry, child.getParentNode());
            Assertions.assertTrue(child.isChildrenFound());
            Assertions.assertFalse(child.isChildrenLoaded());
        }
    }

    /**
     * @see SolrEADParser#loadChildren(ArchiveEntry)
     * @verifies not load children twice
     */
    @Test
    void loadChildren_shouldNotLoadChildrenTwice() throws Exception {
        ArchiveEntry root = eadParser.loadDatabase(getTestArchive(), 1);
        ArchiveEntry entry = root.getSubEntryList().get(0);

        // Load the same node concurrently, as two sessions sharing the node might do
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> task = () -> eadParser.loadChildren(entry);
            List<Future<Boolean>> results = executor.invokeAll(List.of(task, task));
            int loadCount = 0;
            for (Future<Boolean> result : results) {
                if (Boolean.TRUE.equals(result.get())) {
                    loadCount++;
                }
            }
            assertEquals(1, loadCount);
        } finally {
            executor.shutdown();
        }
        List<ArchiveEntry> children = entry.getSubEntryList();
        assertEquals(5, children.size());

        Assertions.assertFalse(eadParser.loadChildren(entry));
        Assertions.assertSame(children, entry.getSubEntryList());
    }

    /**
     * @see SolrEADParser#loadEntryPath(ArchiveEntry,String)
     * @verifies load path to deep node
     */
    @Test
    void loadEntryPath_shouldLoadPathToDeepNode() throws Exception {
        ArchiveEntry root = eadParser.loadDatabase(getTestArchive(), 1);

        ArchiveEntry entry = eadParser.loadEntryPath(root, DEEP_NODE_ID);
        assertNotNull(entry);
        assertEquals(DEEP_NODE_ID, entry.getId());
        assertEquals(3, entry.getHierarchyLevel());
        assertEquals("1 Werke", entry.getParentNode().getLabel());
        Assertions.assertSame(root, entry.getRootNode());
        // Only the levels along the path have been loaded
        Assertions.assertFalse(root.getSubEntryList().get(0).getSubEntryList().get(1).isChildrenLoaded());
    }

    /**
     * @see SolrEADParser#loadEntryPath(ArchiveEntry,String)
     * @verifies return null if node not found
     */
    @Test
    void loadEntryPath_shouldReturnNullIfNodeNotFound() throws Exception {
        ArchiveEntry root = eadParser.loadDatabase(getTestArchive(), 1);
        Assertions.assertNull(eadParser.loadEntryPath(root, "notfound"));
    }

    /**
     * @see SolrEADParser#searchInUnparsedNodes(ArchiveEntry,String)
     * @verifies load path to matching nodes
     */
    @Test
    void searchInUnparsedNodes_shouldLoadPathToMatchingNodes() throws Exception {
        ArchiveEntry root = eadParser.loadDatabase(getTestArchive(), 1);
        assertEquals(2, root.getAsFlatList(true).size());

        ArchiveParser.UnparsedNodeSearchResult result = eadParser.searchInUnparsedNodes(root, "Bulawayo");
        Assertions.assertTrue(result.nodesLoaded());
        Assertions.assertFalse(result.incomplete());
        Assertions.assertTrue(root.getAsFlatList(true)
                .stream()
                .anyMatch(e -> "A91x20876127481460520181205135958494".equals(e.getId())));
    }

    /**
     * @see SolrEADParser#searchInUnparsedNodes(ArchiveEntry,String)
     * @verifies match word prefixes
     */
    @Test
    void searchInUnparsedNodes_shouldMatchWordPrefixes() throws Exception {
        ArchiveEntry root = eadParser.loadDatabase(getTestArchive(), 1);

        Assertions.assertTrue(eadParser.searchInUnparsedNodes(root, "Bulaw").nodesLoaded());
        Assertions.assertTrue(root.getAsFlatList(true)
                .stream()
                .anyMatch(e -> "A91x20876127481460520181205135958494".equals(e.getId())));
    }

    /**
     * @see SolrEADParser#searchInUnparsedNodes(ArchiveEntry,String,int,int)
     * @verifies flag result incomplete if more hits than allowed
     */
    @Test
    void searchInUnparsedNodes_shouldFlagResultIncompleteIfMoreHitsThanAllowed() throws Exception {
        ArchiveEntry root = eadParser.loadDatabase(getTestArchive(), 1);

        ArchiveParser.UnparsedNodeSearchResult result = eadParser.searchInUnparsedNodes(root, "Handschriftliche", 1, Integer.MAX_VALUE);
        Assertions.assertTrue(result.nodesLoaded());
        Assertions.assertTrue(result.incomplete());
    }

    /**
     * @see SolrEADParser#searchInUnparsedNodes(ArchiveEntry,String,int,int)
     * @verifies skip hits whose path exceeds the node budget
     */
    @Test
    void searchInUnparsedNodes_shouldSkipHitsWhosePathExceedsTheNodeBudget() throws Exception {
        ArchiveEntry root = eadParser.loadDatabase(getTestArchive(), 1);

        // Loading the path to the hit requires loading the 5 children of "Koch, Robert" plus the children of "1 Werke"
        ArchiveParser.UnparsedNodeSearchResult result = eadParser.searchInUnparsedNodes(root, "Bulawayo", 100, 5);
        Assertions.assertFalse(result.nodesLoaded());
        Assertions.assertTrue(result.incomplete());
        Assertions.assertFalse(root.getSubEntryList().get(0).isChildrenLoaded());
    }

    /**
     * @see SolrEADParser#buildTitleQuery(String)
     * @verifies require all terms
     */
    @Test
    void buildTitleQuery_shouldRequireAllTerms() {
        assertEquals("+(foo foo*) +(bar bar*)", SolrEADParser.buildTitleQuery(" foo  bar "));
    }

    /**
     * @see SolrEADParser#buildTitleQuery(String)
     * @verifies add prefix wildcard to terms with at least three characters
     */
    @Test
    void buildTitleQuery_shouldAddPrefixWildcardToTermsWithAtLeastThreeCharacters() {
        assertEquals("+a +ab +(abc abc*)", SolrEADParser.buildTitleQuery("a ab abc"));
    }

    /**
     * @see SolrEADParser#buildTitleQuery(String)
     * @verifies escape special characters
     */
    @Test
    void buildTitleQuery_shouldEscapeSpecialCharacters() {
        assertEquals("+(a\\:b\\* a\\:b\\**)", SolrEADParser.buildTitleQuery("a:b*"));
    }
}
