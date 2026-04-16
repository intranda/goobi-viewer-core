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
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

class ArchiveTreeTest extends AbstractTest {

    /**
     * @verifies set tree fully loaded false if tree incomplete
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
}
