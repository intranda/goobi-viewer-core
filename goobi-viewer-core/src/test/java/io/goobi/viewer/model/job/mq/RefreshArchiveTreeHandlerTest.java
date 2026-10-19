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
package io.goobi.viewer.model.job.mq;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RefreshArchiveTreeHandlerTest {

    @AfterEach
    void tearDown() {
        RefreshArchiveTreeHandler.takePendingIdentifiers();
        RefreshArchiveTreeHandler.resetRefreshQueued();
    }

    /**
     * @see RefreshArchiveTreeHandler#addPendingIdentifiers(java.util.Collection)
     * @verifies only request one task until it has started
     */
    @Test
    void addPendingIdentifiers_shouldOnlyRequestOneTaskUntilItHasStarted() {
        Assertions.assertTrue(RefreshArchiveTreeHandler.addPendingIdentifiers(List.of("PPN1")));
        Assertions.assertTrue(RefreshArchiveTreeHandler.isRefreshQueued());
        // Further identifiers are added to the queued task
        Assertions.assertFalse(RefreshArchiveTreeHandler.addPendingIdentifiers(List.of("PPN2", "PPN1")));
        Assertions.assertFalse(RefreshArchiveTreeHandler.addPendingIdentifiers(List.of("PPN3")));

        // The task takes all pending identifiers when it starts; afterwards a new task is requested again
        RefreshArchiveTreeHandler.resetRefreshQueued();
        Set<String> pending = RefreshArchiveTreeHandler.takePendingIdentifiers();
        Assertions.assertEquals(Set.of("PPN1", "PPN2", "PPN3"), pending);
        Assertions.assertTrue(RefreshArchiveTreeHandler.takePendingIdentifiers().isEmpty());
        Assertions.assertTrue(RefreshArchiveTreeHandler.addPendingIdentifiers(List.of("PPN4")));
    }

    /**
     * @see RefreshArchiveTreeHandler#chunks(java.util.Collection)
     * @verifies split values into chunks of at most max size
     */
    @Test
    void chunks_shouldSplitValuesIntoChunksOfAtMostMaxSize() {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < RefreshArchiveTreeHandler.MAX_IDENTIFIERS_PER_QUERY * 2 + 1; i++) {
            values.add("PPN" + i);
        }
        List<List<String>> chunks = RefreshArchiveTreeHandler.chunks(values);
        Assertions.assertEquals(3, chunks.size());
        Assertions.assertEquals(RefreshArchiveTreeHandler.MAX_IDENTIFIERS_PER_QUERY, chunks.get(0).size());
        Assertions.assertEquals(1, chunks.get(2).size());
        Assertions.assertTrue(RefreshArchiveTreeHandler.chunks(List.of()).isEmpty());
    }
}
