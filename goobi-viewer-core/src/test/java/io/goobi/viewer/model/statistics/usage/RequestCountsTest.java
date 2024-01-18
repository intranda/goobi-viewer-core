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
package io.goobi.viewer.model.statistics.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author florian
 *
 */
class RequestCountsTest {

    @Test
    void test_serialize() {
        
        SessionRequestCounts counts = new SessionRequestCounts();
        counts.setCount(RequestType.FILE_DOWNLOAD, 4);
        counts.setCount(RequestType.MEDIA_RESOURCE, 3);
        counts.setCount(RequestType.RECORD_VIEW, 23);
    
        String s = counts.toJsonArray();
        assertEquals("[23,4,3]", s);
    }

    @Test
    void test_serialize_empty() {
        SessionRequestCounts counts = new SessionRequestCounts();
    
        String s = counts.toJsonArray();
        assertEquals("[0,0,0]", s);
    }
    
    @Test
    void test_serialize_partially_empty() {
        SessionRequestCounts counts = new SessionRequestCounts();
        counts.setCount(RequestType.MEDIA_RESOURCE, 3);

        String s = counts.toJsonArray();
        assertEquals("[0,0,3]", s);
    }
    
    @Test
    void test_deserialize() {
        String s = "[23,4,3]";
        SessionRequestCounts counts = new SessionRequestCounts(s);
        assertEquals(Long.valueOf(23), counts.getCount(RequestType.RECORD_VIEW));
        assertEquals(Long.valueOf(4), counts.getCount(RequestType.FILE_DOWNLOAD));
        assertEquals(Long.valueOf(3), counts.getCount(RequestType.MEDIA_RESOURCE));
    }
}
