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
package io.goobi.viewer.model.media.webarchives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;

import org.junit.jupiter.api.Test;

class WebArchiveReaderTest {

    /**
     * @verifies return the value of the named query parameter
     * @see WebArchiveReader#extractQueryParamValue(URI, String)
     */
    @Test
    void extractQueryParamValue_shouldReturnValueOfNamedParam() throws Exception {
        URI uri = new URI("https://example.org/replay?source=https%3A%2F%2Fexample.org%2Ffile.json&other=1");
        assertEquals("https://example.org/file.json", WebArchiveReader.extractQueryParamValue(uri, "source"));
    }

    /**
     * @verifies return null when the named query parameter is absent
     * @see WebArchiveReader#extractQueryParamValue(URI, String)
     */
    @Test
    void extractQueryParamValue_shouldReturnNullWhenParamAbsent() throws Exception {
        URI uri = new URI("https://example.org/replay?other=1");
        assertNull(WebArchiveReader.extractQueryParamValue(uri, "source"));
    }

    /**
     * @verifies return null when the uri has no query
     * @see WebArchiveReader#extractQueryParamValue(URI, String)
     */
    @Test
    void extractQueryParamValue_shouldReturnNullWhenNoQuery() throws Exception {
        URI uri = new URI("https://example.org/replay");
        assertNull(WebArchiveReader.extractQueryParamValue(uri, "source"));
    }

    /**
     * @verifies find the requested param among several
     * @see WebArchiveReader#extractQueryParamValue(URI, String)
     */
    @Test
    void extractQueryParamValue_shouldFindParamAmongSeveral() throws Exception {
        URI uri = new URI("https://example.org/replay?a=1&url=https%3A%2F%2Fexample.org%2Fstart&b=2");
        assertEquals("https://example.org/start", WebArchiveReader.extractQueryParamValue(uri, "url"));
    }
}
