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
     * @see SolrEADParser#checkTreeFullyLoaded()
     * @verifies set treeFullyLoaded false if tree incomplete
     */
    @Test
    void checkTreeFullyLoaded_setTreeFullyLoadedFalseIfTreeIncomplete() throws Exception {
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
}
