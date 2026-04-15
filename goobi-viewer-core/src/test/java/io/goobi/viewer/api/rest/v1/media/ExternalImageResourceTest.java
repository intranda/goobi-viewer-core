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
package io.goobi.viewer.api.rest.v1.media;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;

class ExternalImageResourceTest {

    /**
     * validateImageUrl() must reject non-ASCII characters because they cause URI.create() to
     * throw IllegalArgumentException (→ HTTP 500) inside the ImageResource constructor.
     * @verifies reject non ascii characters
     */
    @Test
    void validateImageUrl_shouldRejectNonAsciiCharacters() {
        // Non-ASCII characters (outside printable ASCII range 0x20–0x7E)
        assertThrows(BadRequestException.class, () -> ExternalImageResource.validateImageUrl("image\u00e9.jpg"),
                "Non-ASCII character should be rejected");
        assertThrows(BadRequestException.class, () -> ExternalImageResource.validateImageUrl("\u00c4\u00f6\u00fc.png"),
                "German umlauts should be rejected");
    }

    /**
     * validateImageUrl() must reject bare '%' characters that result from double-encoding
     * (e.g. %2B%254 → +%4). A literal '%' in the decoded filename causes URI.create() to
     * throw IllegalArgumentException inside the ImageResource constructor.
     * @verifies reject bare percent sign
     */
    @Test
    void validateImageUrl_shouldRejectBarePercentSign() {
        assertThrows(BadRequestException.class, () -> ExternalImageResource.validateImageUrl("file%name.jpg"),
                "Filename with bare '%' should be rejected");
        assertThrows(BadRequestException.class, () -> ExternalImageResource.validateImageUrl("%"),
                "Bare '%' should be rejected");
    }

    /**
     * validateImageUrl() must accept normal printable ASCII filenames including common
     * image filename characters.
     * @verifies accept valid ascii filenames
     */
    @Test
    void validateImageUrl_shouldAcceptValidAsciiFilenames() {
        assertDoesNotThrow(() -> ExternalImageResource.validateImageUrl("image.jpg"),
                "Plain filename should be accepted");
        assertDoesNotThrow(() -> ExternalImageResource.validateImageUrl("path/to/image.tif"),
                "Path with slashes should be accepted");
        assertDoesNotThrow(() -> ExternalImageResource.validateImageUrl("file-name_01.png"),
                "Filename with hyphens and underscores should be accepted");
        assertDoesNotThrow(() -> ExternalImageResource.validateImageUrl(null),
                "null should be accepted (handled by JAX-RS before validation)");
    }
}
