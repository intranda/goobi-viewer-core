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
package io.goobi.viewer.model.security.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;

class IpRangeCacheTest extends AbstractDatabaseEnabledTest {

    // H2 fixture contains 2 IpRange rows (see JPADAOTest#getAllIpRanges_... baseline).
    private static final int NUM_IP_RANGES = 2;

    private IpRangeCache cache;

    @BeforeEach
    void initCache() {
        cache = new IpRangeCache();
    }

    /**
     * @see IpRangeCache#getAllIpRanges()
     * @verifies return all IP ranges from dao
     */
    @Test
    void getAllIpRanges_shouldReturnAllIpRangesFromDao() throws Exception {
        List<IpRange> result = cache.getAllIpRanges();
        assertNotNull(result);
        assertEquals(NUM_IP_RANGES, result.size());
    }

    /**
     * @see IpRangeCache#getAllIpRanges()
     * @verifies return same instance on second call
     */
    @Test
    void getAllIpRanges_shouldReturnSameInstanceOnSecondCall() throws Exception {
        assertSame(cache.getAllIpRanges(), cache.getAllIpRanges());
    }

    /**
     * @see IpRangeCache#getAllIpRanges()
     * @verifies return immutable list
     */
    @Test
    void getAllIpRanges_shouldReturnImmutableList() throws Exception {
        List<IpRange> result = cache.getAllIpRanges();
        assertThrows(UnsupportedOperationException.class, () -> result.add(new IpRange()));
    }

    /**
     * @see IpRangeCache#invalidate()
     * @verifies force reload on next read
     */
    @Test
    void invalidate_shouldForceReloadOnNextRead() throws Exception {
        List<IpRange> first = cache.getAllIpRanges();
        cache.invalidate();
        List<IpRange> second = cache.getAllIpRanges();
        assertEquals(first.size(), second.size());
        assertNotSame(first, second, "expected fresh snapshot after invalidate()");
    }
}
