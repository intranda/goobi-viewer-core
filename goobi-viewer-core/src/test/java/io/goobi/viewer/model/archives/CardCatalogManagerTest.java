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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class CardCatalogManagerTest {

    /**
     * @verifies return single position for empty interval
     * @see CardCatalogManager#computeBoundaryPositions(int,int,int)
     */
    @Test
    void computeBoundaryPositions_shouldReturnSinglePositionForEmptyInterval() {
        assertEquals(List.of(5), CardCatalogManager.computeBoundaryPositions(5, 5, 9));
    }

    /**
     * @verifies distribute positions evenly
     * @see CardCatalogManager#computeBoundaryPositions(int,int,int)
     */
    @Test
    void computeBoundaryPositions_shouldDistributePositionsEvenly() {
        List<Integer> positions = CardCatalogManager.computeBoundaryPositions(1, 1022, 9);
        assertEquals(9, positions.size());
        assertEquals(1, positions.get(0));
        assertEquals(1022, positions.get(positions.size() - 1));
        // Every step covers roughly the same number of cards
        for (int i = 1; i < positions.size(); i++) {
            int step = positions.get(i) - positions.get(i - 1);
            assertTrue(step >= 127 && step <= 129, "Unexpected step width: " + step);
        }
    }

    /**
     * @verifies not return duplicate positions for narrow intervals
     * @see CardCatalogManager#computeBoundaryPositions(int,int,int)
     */
    @Test
    void computeBoundaryPositions_shouldNotReturnDuplicatePositionsForNarrowIntervals() {
        List<Integer> positions = CardCatalogManager.computeBoundaryPositions(1, 4, 9);
        assertEquals(List.of(1, 2, 3, 4), positions);
    }

    /**
     * @verifies chunk entries into ranges of given size
     * @see CardCatalogManager#computeRegisterRanges(List,int)
     */
    @Test
    void computeRegisterRanges_shouldChunkEntriesIntoRangesOfGivenSize() {
        List<CardCatalogEntry> entries = new ArrayList<>();
        for (int i = 1; i <= 250; i++) {
            entries.add(new CardCatalogEntry("id" + i, "title" + i, null, false));
        }
        List<CardCatalogRegisterRange> ranges = CardCatalogManager.computeRegisterRanges(entries, 100);
        assertEquals(3, ranges.size());
        assertEquals(1, ranges.get(0).getFrom());
        assertEquals(100, ranges.get(0).getTo());
        assertEquals("title1", ranges.get(0).getFromLabel());
        assertEquals("title100", ranges.get(0).getToLabel());
        assertEquals(201, ranges.get(2).getFrom());
        assertEquals(250, ranges.get(2).getTo());
        assertEquals("title250", ranges.get(2).getToLabel());
    }

    /**
     * @verifies fall back to nearest titled entry for blank labels
     * @see CardCatalogManager#computeRegisterRanges(List,int)
     */
    @Test
    void computeRegisterRanges_shouldFallBackToNearestTitledEntryForBlankLabels() {
        List<CardCatalogEntry> entries = new ArrayList<>();
        entries.add(new CardCatalogEntry("id1", null, null, false));
        entries.add(new CardCatalogEntry("id2", "Abel", null, false));
        entries.add(new CardCatalogEntry("id3", " ", null, false));
        entries.add(new CardCatalogEntry("id4", "Adler", null, false));
        entries.add(new CardCatalogEntry("id5", "", null, false));
        List<CardCatalogRegisterRange> ranges = CardCatalogManager.computeRegisterRanges(entries, 5);
        assertEquals(1, ranges.size());
        assertEquals("Abel", ranges.get(0).getFromLabel());
        assertEquals("Adler", ranges.get(0).getToLabel());
    }

    /**
     * @verifies return empty list for empty entries
     * @see CardCatalogManager#computeRegisterRanges(List,int)
     */
    @Test
    void computeRegisterRanges_shouldReturnEmptyListForEmptyEntries() {
        assertTrue(CardCatalogManager.computeRegisterRanges(Collections.emptyList(), 100).isEmpty());
        assertTrue(CardCatalogManager.computeRegisterRanges(null, 100).isEmpty());
    }
}
