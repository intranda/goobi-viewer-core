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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import jakarta.servlet.http.HttpServletResponse;

class MediaDeliveryServiceTest {

    /**
     * @verifies match range header
     * @see MediaDeliveryService#matchesRangeHeaderPattern
     */
    @Test
    void matchesRangeHeaderPattern_shouldMatchRangeHeader() {
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

    /**
     * @see MediaDeliveryService#initResponse(HttpServletResponse, String, long, String, String)
     * @verifies not call reset or setBufferSize on the response
     */
    @Test
    void initResponse_shouldNotCallResetOrSetBufferSizeOnTheResponse() {
        // Regression guard: initResponse previously invoked reset() and setBufferSize() which
        // triggered "Skipping attempt to set buffer size on a committed response" warnings under
        // Tomcat/Rewrite wrappers. Both calls have been removed; this test pins that behaviour.
        HttpServletResponse response = mock(HttpServletResponse.class);

        MediaDeliveryService.initResponse(response, "movie.mp4", 1_700_000_000_000L, "movie.mp4_1234_1700000000000", "inline");

        verify(response, never()).reset();
        verify(response, never()).setBufferSize(ArgumentMatchers.anyInt());
    }

    /**
     * @see MediaDeliveryService#initResponse(HttpServletResponse, String, long, String, String)
     * @verifies set content-disposition, accept-ranges, etag, last-modified and expires headers
     */
    @Test
    void initResponse_shouldSetContentDispositionAcceptRangesEtagLastModifiedAndExpiresHeaders() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        long lastModified = 1_700_000_000_000L;
        String eTag = "movie.mp4_1234_1700000000000";

        MediaDeliveryService.initResponse(response, "movie.mp4", lastModified, eTag, "inline");

        verify(response).setHeader("Content-Disposition", "inline;filename=\"movie.mp4\"");
        verify(response).setHeader("Accept-Ranges", "bytes");
        verify(response).setHeader("ETag", eTag);
        verify(response).setDateHeader(ArgumentMatchers.eq("Last-Modified"), ArgumentMatchers.eq(lastModified));
        verify(response).setDateHeader(ArgumentMatchers.eq("Expires"), ArgumentMatchers.anyLong());
    }
}
