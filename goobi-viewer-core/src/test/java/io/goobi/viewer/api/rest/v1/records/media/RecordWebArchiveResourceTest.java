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
package io.goobi.viewer.api.rest.v1.records.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RecordWebArchiveResourceTest {

    /**
     * @verifies return identifier unchanged when it has no source param
     * @see RecordWebArchiveResource#resolveWebArchiveUrl(String)
     */
    @Test
    void resolveWebArchiveUrl_shouldReturnIdentifierWhenNoSourceParam() {
        String identifier = "https://archive.example.org/site1.wacz";
        assertEquals(identifier, RecordWebArchiveResource.resolveWebArchiveUrl(identifier));
    }

    /**
     * @verifies return decoded source param value when present
     * @see RecordWebArchiveResource#resolveWebArchiveUrl(String)
     */
    @Test
    void resolveWebArchiveUrl_shouldReturnDecodedSourceParamValue() {
        String identifier = "https://archive.example.org/replay?source=https%3A%2F%2Fexample.org%2Ffile.json&other=1";
        assertEquals("https://example.org/file.json", RecordWebArchiveResource.resolveWebArchiveUrl(identifier));
    }

    /**
     * @verifies return null for malformed url
     * @see RecordWebArchiveResource#resolveWebArchiveUrl(String)
     */
    @Test
    void resolveWebArchiveUrl_shouldReturnNullForMalformedUrl() {
        assertNull(RecordWebArchiveResource.resolveWebArchiveUrl("http://exa mple.org/broken"));
    }

    /**
     * @verifies return null for blank input
     * @see RecordWebArchiveResource#resolveWebArchiveUrl(String)
     */
    @Test
    void resolveWebArchiveUrl_shouldReturnNullForBlankInput() {
        assertNull(RecordWebArchiveResource.resolveWebArchiveUrl(""));
        assertNull(RecordWebArchiveResource.resolveWebArchiveUrl(null));
    }

    /**
     * @verifies return last path segment
     * @see RecordWebArchiveResource#deriveExternalResourceName(String)
     */
    @Test
    void deriveExternalResourceName_shouldReturnLastPathSegment() {
        assertEquals("site1.wacz", RecordWebArchiveResource.deriveExternalResourceName("https://example.org/archive/site1.wacz"));
    }

    /**
     * @verifies return last path segment for json manifest urls
     * @see RecordWebArchiveResource#deriveExternalResourceName(String)
     */
    @Test
    void deriveExternalResourceName_shouldReturnLastPathSegmentForJsonManifest() {
        assertEquals("index.json", RecordWebArchiveResource.deriveExternalResourceName("https://example.org/manifests/index.json"));
    }

    /**
     * @verifies fall back to the full url when it has no path segment
     * @see RecordWebArchiveResource#deriveExternalResourceName(String)
     */
    @Test
    void deriveExternalResourceName_shouldFallBackToFullUrlWhenNoPath() {
        String url = "https://example.org";
        assertEquals(url, RecordWebArchiveResource.deriveExternalResourceName(url));
    }

    /**
     * @verifies fall back to the full url on malformed input
     * @see RecordWebArchiveResource#deriveExternalResourceName(String)
     */
    @Test
    void deriveExternalResourceName_shouldFallBackToFullUrlOnMalformedUrl() {
        String url = "http://exa mple.org/broken";
        assertEquals(url, RecordWebArchiveResource.deriveExternalResourceName(url));
    }
}
