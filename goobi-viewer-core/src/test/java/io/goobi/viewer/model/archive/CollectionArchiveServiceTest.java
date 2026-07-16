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
package io.goobi.viewer.model.archive;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CollectionArchiveServiceTest {

    /**
     * @see CollectionArchiveService#slugify(String)
     * @verifies replace unsafe characters and reject traversal
     */
    @Test
    void slugify_shouldProduceSafeSegment() {
        assertEquals("varia", CollectionArchiveService.slugify("varia"));
        assertEquals("a.b.c", CollectionArchiveService.slugify("a.b.c"));
        // Path separators and traversal sequences must not survive
        assertFalse(CollectionArchiveService.slugify("../../etc/passwd").contains("/"));
        assertFalse(CollectionArchiveService.slugify("../../etc").startsWith("."));
        assertEquals("a_b_c", CollectionArchiveService.slugify("a b/c"));
        assertEquals("_", CollectionArchiveService.slugify(""));
    }

    /**
     * @see CollectionArchiveService#buildFileName(String, long, long)
     * @see CollectionArchiveService#parseFileName(String)
     * @verifies round-trip record count and timestamp
     */
    @Test
    void fileName_shouldRoundTrip() {
        String name = CollectionArchiveService.buildFileName("varia", 1234, 1720953600000L);
        assertEquals("varia__n1234__u1720953600000.zip", name);
        assertArrayEquals(new long[] { 1234L, 1720953600000L }, CollectionArchiveService.parseFileName(name));
    }

    /**
     * @see CollectionArchiveService#parseFileName(String)
     * @verifies return null for non-matching names
     */
    @Test
    void parseFileName_shouldReturnNullForNonMatching() {
        assertNull(CollectionArchiveService.parseFileName("varia.zip"));
        assertNull(CollectionArchiveService.parseFileName("varia__nX__u5.zip"));
        assertNull(CollectionArchiveService.parseFileName("random.txt"));
    }

    /**
     * @see CollectionArchiveService#escapePhrase(String)
     * @verifies escape backslash and double quote only
     */
    @Test
    void escapePhrase_shouldEscapeQuotesAndBackslashes() {
        // A collection name attempting to break out of the Solr phrase must be neutralised
        assertEquals("a\\\"b", CollectionArchiveService.escapePhrase("a\"b"));
        assertEquals("a\\\\b", CollectionArchiveService.escapePhrase("a\\b"));
        // Spaces and colons are legal inside a phrase and must be left untouched
        assertEquals("a b:c", CollectionArchiveService.escapePhrase("a b:c"));
        assertTrue(CollectionArchiveService.escapePhrase("plain").equals("plain"));
    }
}
