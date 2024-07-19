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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;

import org.jdom2.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

class SolrEADParserTest extends AbstractSolrEnabledTest {

    private SolrEADParser eadParser;

    @BeforeEach
    void before() {
        try {
            eadParser = new SolrEADParser();
        } catch (PresentationException | IndexUnreachableException e) {
            Assertions.fail(e.getMessage());
        }
    }

    /**
     * @see SolrEADParser#getPossibleDatabases()
     * @verifies return all resources
     */
    @Test
    void getPossibleDatabases_shouldReturnAllResources() throws Exception {
        List<ArchiveResource> resources = eadParser.getPossibleDatabases();
        assertNotNull(resources);
        assertEquals(1, resources.size());
        ArchiveResource resource = resources.get(0);
        assertEquals("Akte_Koch_-_Humboldt_Universitaet", resource.getResourceId());
        assertEquals("Koch, Robert", resource.getResourceName());
        assertEquals(0, resource.getSize());
    }

    /**
     * @see SolrEADParser#formatDate(Document)
     * @verifies format timestamp correctly
     */
    @Test
    void formatDate_shouldFormatTimestampCorrectly() throws Exception {
        Assertions.assertNull(SolrEADParser.formatDate(null));
        Assertions.assertEquals("2024-02-28T14:29:55Z",
                SolrEADParser.formatDate(DateTools.getMillisFromLocalDateTime(LocalDateTime.of(2024, 02, 28, 14, 29, 55), false)));
    }
}
