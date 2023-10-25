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
package io.goobi.viewer.api.rest.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class MediaDeliveryServiceTest {

    @Test
    public void test_matchRangeHeader() {
        {
            String range = "n-n";
            assertFalse(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=13";
            assertFalse(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=14-";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=14-53";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=14-15,165-23,52-";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=14-34,15-6346";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=-123";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=14-34, -6346";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            //test catastrophic backtracking
            String range =
                    "bytes=14-34,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            //test catastrophic backtracking
            String range =
                    "bytes=14-34,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            //test catastrophic backtracking
            String range =
                    "bytes=14-34,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,dfsdfsd";
            assertFalse(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=-";
            assertFalse(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
    }

}
