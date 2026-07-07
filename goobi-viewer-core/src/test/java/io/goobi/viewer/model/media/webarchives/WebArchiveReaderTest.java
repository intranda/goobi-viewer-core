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

import org.junit.jupiter.api.Test;

class WebArchiveReaderTest {

    /**
     * @verifies return the value of the named param
     * @see WebArchiveReader#extractParamValue(String, String)
     */
    @Test
    void extractParamValue_shouldReturnValueOfNamedParam() {
        assertEquals("https://example.org/file.json",
                WebArchiveReader.extractParamValue("source=https%3A%2F%2Fexample.org%2Ffile.json&other=1", "source"));
    }

    /**
     * @verifies return null when the named param is absent
     * @see WebArchiveReader#extractParamValue(String, String)
     */
    @Test
    void extractParamValue_shouldReturnNullWhenParamAbsent() {
        assertNull(WebArchiveReader.extractParamValue("other=1", "source"));
    }

    /**
     * @verifies return null when rawParams is null
     * @see WebArchiveReader#extractParamValue(String, String)
     */
    @Test
    void extractParamValue_shouldReturnNullWhenRawParamsIsNull() {
        assertNull(WebArchiveReader.extractParamValue(null, "source"));
    }

    /**
     * @verifies find the requested param among several
     * @see WebArchiveReader#extractParamValue(String, String)
     */
    @Test
    void extractParamValue_shouldFindParamAmongSeveral() {
        assertEquals("https://example.org/start",
                WebArchiveReader.extractParamValue("a=1&url=https%3A%2F%2Fexample.org%2Fstart&b=2", "url"));
    }
}
