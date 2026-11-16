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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class CardCatalogDrawerTest {

    /**
     * @see CardCatalogDrawer#getOrLoadRegisterRanges(int, CardCatalogDrawer.RegisterLoader)
     * @verifies load ranges only once for the same chunk size
     */
    @Test
    void getOrLoadRegisterRanges_shouldLoadRangesOnlyOnceForTheSameChunkSize() throws Exception {
        CardCatalogDrawer drawer = new CardCatalogDrawer("drawer", "iddoc", "A", 10);
        List<CardCatalogRegisterRange> ranges = List.of(new CardCatalogRegisterRange(1, 10, "A", "Z"));
        AtomicInteger calls = new AtomicInteger();
        CardCatalogDrawer.RegisterLoader loader = () -> {
            calls.incrementAndGet();
            return ranges;
        };

        assertSame(ranges, drawer.getOrLoadRegisterRanges(10, loader));
        assertSame(ranges, drawer.getOrLoadRegisterRanges(10, loader));
        assertEquals(1, calls.get());
    }

    /**
     * @see CardCatalogDrawer#getOrLoadRegisterRanges(int, CardCatalogDrawer.RegisterLoader)
     * @verifies reload ranges for a different chunk size
     */
    @Test
    void getOrLoadRegisterRanges_shouldReloadRangesForADifferentChunkSize() throws Exception {
        CardCatalogDrawer drawer = new CardCatalogDrawer("drawer", "iddoc", "A", 10);
        List<CardCatalogRegisterRange> ranges10 = List.of(new CardCatalogRegisterRange(1, 10, "A", "Z"));
        List<CardCatalogRegisterRange> ranges5 =
                List.of(new CardCatalogRegisterRange(1, 5, "A", "M"), new CardCatalogRegisterRange(6, 10, "N", "Z"));

        assertSame(ranges10, drawer.getOrLoadRegisterRanges(10, () -> ranges10));
        assertSame(ranges5, drawer.getOrLoadRegisterRanges(5, () -> ranges5));
    }
}
