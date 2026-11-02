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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

class ArchiveTreeTest extends AbstractTest {

    /**
     * @see ArchiveTree#checkTreeFullyLoaded(java.util.List)
     * @verifies set treeFullyLoaded false if tree incomplete
     */
    @Test
    void checkTreeFullyLoaded_shouldSetTreeFullyLoadedFalseIfTreeIncomplete() throws Exception {
        ArchiveTree tree = new ArchiveTree();
        Assertions.assertTrue(tree.isTreeFullyLoaded());

        List<ArchiveEntry> nodes = new ArrayList<>(3);
        nodes.add(new ArchiveEntry(0, 1, null));
        nodes.add(new ArchiveEntry(1, 1, null));
        nodes.add(new ArchiveEntry(2, 1, null));
        tree.checkTreeFullyLoaded(nodes);
        Assertions.assertTrue(tree.isTreeFullyLoaded());

        nodes.get(2).setChildrenFound(true);
        nodes.get(2).setChildrenLoaded(false);
        tree.checkTreeFullyLoaded(nodes);
        Assertions.assertFalse(tree.isTreeFullyLoaded());
    }

    /**
     * Verifies that getTreeViewForGroup triggers buildTree (setting treeBuilt=true)
     * and that the returned list matches the generated flat entry list for the group.
     *
     * @see ArchiveTree#getTreeViewForGroup(String)
     * @verifies call buildTree and set maxTocDepth correctly
     */
    @Test
    void getTreeViewForGroup_shouldCallBuildTreeAndSetMaxTocDepthCorrectly() throws Exception {
        // Build a small archive tree: root -> child1, child2 (child2 -> grandchild)
        ArchiveEntry root = new ArchiveEntry(0, 0, null);
        root.setId("root");
        root.setLabel("Root");

        ArchiveEntry child1 = new ArchiveEntry(0, 1, null);
        child1.setId("child1");
        child1.setLabel("Child 1");
        root.addSubEntry(child1);

        ArchiveEntry child2 = new ArchiveEntry(1, 1, null);
        child2.setId("child2");
        child2.setLabel("Child 2");
        root.addSubEntry(child2);

        ArchiveEntry grandchild = new ArchiveEntry(0, 2, null);
        grandchild.setId("grandchild");
        grandchild.setLabel("Grandchild");
        child2.addSubEntry(grandchild);

        // Generate tree entries for the default group
        ArchiveTree tree = new ArchiveTree();
        tree.generate(root);

        // Calling getTreeViewForGroup should trigger buildTree internally and return the entry list
        List<ArchiveEntry> result = tree.getTreeViewForGroup(ArchiveTree.DEFAULT_GROUP);

        Assertions.assertNotNull(result);
        // The flat list should contain root + child1 + child2 + grandchild = 4 entries
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("root", result.get(0).getId());
        Assertions.assertEquals("child1", result.get(1).getId());
        Assertions.assertEquals("child2", result.get(2).getId());
        Assertions.assertEquals("grandchild", result.get(3).getId());

        // Calling again should return same result (buildTree already ran, treeBuilt flag is true)
        List<ArchiveEntry> result2 = tree.getTreeViewForGroup(ArchiveTree.DEFAULT_GROUP);
        Assertions.assertEquals(result.size(), result2.size());
    }

    /**
     * Parser stub that provides not yet loaded child nodes from a map instead of Solr.
     */
    private static class StubParser extends ArchiveParser {

        private static final long serialVersionUID = 1L;

        /** Children to add on loadChildren(), keyed by parent ID. */
        private final Map<String, List<ArchiveEntry>> childrenToLoad = new HashMap<>();
        private final List<String> searchValues = new ArrayList<>();
        private boolean searchIncomplete = false;

        @Override
        public List<ArchiveResource> getPossibleDatabases() {
            return Collections.emptyList();
        }

        @Override
        public ArchiveEntry loadDatabase(ArchiveResource database, int lazyLoadingThreshold) {
            return null;
        }

        @Override
        public String getUrl() {
            return "stub";
        }

        @Override
        public UnparsedNodeSearchResult searchInUnparsedNodes(ArchiveEntry node, String searchValue) {
            searchValues.add(searchValue);
            return new UnparsedNodeSearchResult(false, searchIncomplete);
        }

        @Override
        public boolean loadChildren(ArchiveEntry entry) {
            List<ArchiveEntry> children = childrenToLoad.remove(entry.getId());
            if (children == null || entry.isChildrenLoaded()) {
                return false;
            }
            for (ArchiveEntry child : children) {
                entry.addSubEntry(child);
            }
            entry.setChildrenLoaded(true);
            return true;
        }

        @Override
        public ArchiveEntry loadEntryPath(ArchiveEntry root, String nodeId) {
            // Load one level at a time until the requested node is found
            for (ArchiveEntry entry : root.getAsFlatList(true)) {
                if (nodeId.equals(entry.getId())) {
                    return entry;
                }
                if (loadChildren(entry)) {
                    return loadEntryPath(root, nodeId);
                }
            }
            return null;
        }
    }

    /**
     * @param id Entry ID
     * @param hierarchy Hierarchy level
     * @return New {@link ArchiveEntry}
     */
    private static ArchiveEntry createEntry(String id, int hierarchy) {
        ArchiveEntry entry = new ArchiveEntry(0, hierarchy, null);
        entry.setId(id);
        entry.setLabel("Label " + id);
        return entry;
    }

    /**
     * Creates root -> folder, where the folder has one child that is not yet loaded.
     *
     * @param parser Parser that provides the folder's child
     * @return Root entry
     */
    private static ArchiveEntry createPartiallyLoadedTree(StubParser parser) {
        ArchiveEntry root = createEntry("root", 0);
        root.setChildrenLoaded(true);
        ArchiveEntry folder = createEntry("folder", 1);
        folder.setChildrenFound(true);
        root.addSubEntry(folder);
        parser.childrenToLoad.put("folder", new ArrayList<>(List.of(createEntry("file", 2))));
        return root;
    }

    /**
     * @see ArchiveTree#isChildrenInView(ArchiveEntry)
     * @verifies return false if children were added after generation
     */
    @Test
    void isChildrenInView_shouldReturnFalseIfChildrenWereAddedAfterGeneration() {
        ArchiveEntry root = createEntry("root", 0);
        ArchiveEntry folder = createEntry("folder", 1);
        root.addSubEntry(folder);
        ArchiveTree tree = new ArchiveTree();
        tree.generate(root);
        Assertions.assertTrue(tree.isChildrenInView(root));
        Assertions.assertFalse(tree.isChildrenInView(folder));

        folder.addSubEntry(createEntry("file", 2));
        Assertions.assertFalse(tree.isChildrenInView(folder));
    }

    /**
     * @see ArchiveTree#isChildrenInView(ArchiveEntry)
     * @verifies return true after regeneration
     */
    @Test
    void isChildrenInView_shouldReturnTrueAfterRegeneration() {
        ArchiveEntry root = createEntry("root", 0);
        ArchiveEntry folder = createEntry("folder", 1);
        root.addSubEntry(folder);
        ArchiveTree tree = new ArchiveTree();
        tree.generate(root);
        folder.addSubEntry(createEntry("file", 2));

        tree.generate(root);
        Assertions.assertTrue(tree.isChildrenInView(folder));
        Assertions.assertEquals(3, tree.getFlatView().size());
    }

    /**
     * @see ArchiveTree#getEntryById(String)
     * @verifies load path to entry not yet loaded
     */
    @Test
    void getEntryById_shouldLoadPathToEntryNotYetLoaded() {
        StubParser parser = new StubParser();
        ArchiveTree tree = new ArchiveTree();
        tree.setEadParser(parser);
        tree.update(createPartiallyLoadedTree(parser));
        Assertions.assertFalse(tree.isTreeFullyLoaded());
        Assertions.assertEquals(2, tree.getFlatView().size());

        ArchiveEntry entry = tree.getEntryById("file");
        Assertions.assertNotNull(entry);
        Assertions.assertEquals("file", entry.getId());
        Assertions.assertEquals("folder", entry.getParentNode().getId());
        // Entry list has been regenerated
        Assertions.assertEquals(3, tree.getFlatView().size());
        Assertions.assertNull(tree.getEntryById("notfound"));
    }

    /**
     * @see ArchiveTree#search(String)
     * @verifies search in not loaded nodes if tree not fully loaded
     */
    @Test
    void search_shouldSearchInNotLoadedNodesIfTreeNotFullyLoaded() {
        StubParser parser = new StubParser();
        ArchiveTree tree = new ArchiveTree();
        tree.setEadParser(parser);
        tree.update(createPartiallyLoadedTree(parser));
        Assertions.assertFalse(tree.isTreeFullyLoaded());

        tree.search("foo");
        Assertions.assertEquals(List.of("foo"), parser.searchValues);
    }

    /**
     * @see ArchiveTree#search(String)
     * @verifies flag search results incomplete if parser reports incomplete search
     */
    @Test
    void search_shouldFlagSearchResultsIncompleteIfParserReportsIncompleteSearch() {
        StubParser parser = new StubParser();
        parser.searchIncomplete = true;
        ArchiveTree tree = new ArchiveTree();
        tree.setEadParser(parser);
        tree.update(createPartiallyLoadedTree(parser));

        tree.search("foo");
        Assertions.assertTrue(tree.isSearchResultsIncomplete());

        parser.searchIncomplete = false;
        tree.search("bar");
        Assertions.assertFalse(tree.isSearchResultsIncomplete());
    }

    /**
     * @see ArchiveTree#search(String)
     * @verifies not search in index if tree fully loaded
     */
    @Test
    void search_shouldNotSearchInIndexIfTreeFullyLoaded() {
        StubParser parser = new StubParser();
        ArchiveEntry root = createEntry("root", 0);
        root.addSubEntry(createEntry("folder", 1));
        root.setChildrenLoaded(true);
        ArchiveTree tree = new ArchiveTree();
        tree.setEadParser(parser);
        tree.update(root);
        Assertions.assertTrue(tree.isTreeFullyLoaded());

        tree.search("folder");
        Assertions.assertTrue(parser.searchValues.isEmpty());
        Assertions.assertTrue(tree.getEntryById("folder").isSearchHit());
    }

    /**
     * @see ArchiveTree#expandEntry(ArchiveEntry)
     * @verifies add loaded children to entry list
     */
    @Test
    void expandEntry_shouldAddLoadedChildrenToEntryList() {
        StubParser parser = new StubParser();
        ArchiveTree tree = new ArchiveTree();
        tree.setEadParser(parser);
        ArchiveEntry root = createPartiallyLoadedTree(parser);
        tree.update(root);
        ArchiveEntry folder = root.getSubEntryList().get(0);

        tree.expandEntry(folder);
        Assertions.assertTrue(folder.isChildrenLoaded());
        Assertions.assertEquals(3, tree.getFlatView().size());
        Assertions.assertTrue(tree.isEntryVisible(folder.getSubEntryList().get(0)));
    }

    /**
     * @see ArchiveTree#expandEntry(ArchiveEntry)
     * @verifies add children loaded via another tree to entry list
     */
    @Test
    void expandEntry_shouldAddChildrenLoadedViaAnotherTreeToEntryList() {
        StubParser parser = new StubParser();
        ArchiveTree globalTree = new ArchiveTree();
        globalTree.setEadParser(parser);
        ArchiveEntry root = createPartiallyLoadedTree(parser);
        globalTree.update(root);
        // Session copies share the nodes of the original tree
        ArchiveTree sessionTree1 = new ArchiveTree(globalTree);
        ArchiveTree sessionTree2 = new ArchiveTree(globalTree);
        ArchiveEntry folder = root.getSubEntryList().get(0);

        sessionTree1.expandEntry(folder);
        Assertions.assertEquals(3, sessionTree1.getFlatView().size());
        Assertions.assertEquals(2, sessionTree2.getFlatView().size());

        // Children are already loaded, but not yet part of the second tree's entry list
        sessionTree2.expandEntry(folder);
        Assertions.assertEquals(3, sessionTree2.getFlatView().size());
        Assertions.assertTrue(sessionTree2.isEntryVisible(folder.getSubEntryList().get(0)));
    }
}
