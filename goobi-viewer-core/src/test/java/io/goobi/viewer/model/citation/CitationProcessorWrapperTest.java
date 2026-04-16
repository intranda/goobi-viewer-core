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
package io.goobi.viewer.model.citation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.undercouch.citeproc.CSL;
import io.goobi.viewer.AbstractTest;

class CitationProcessorWrapperTest extends AbstractTest {

    /**
     * Verifies that getCitationProcessor creates a non-null CSL processor for the "apa" style,
     * caches it, and returns the same instance on subsequent calls with the same style.
     *
     * @see CitationProcessorWrapper#getCitationProcessor(String)
     * @verifies create citation processor correctly
     */
    @Test
    void getCitationProcessor_shouldCreateCitationProcessorCorrectly() throws Exception {
        CitationProcessorWrapper wrapper = new CitationProcessorWrapper();

        // First call should create and return a new CSL processor
        CSL processor = wrapper.getCitationProcessor("apa");
        Assertions.assertNotNull(processor);

        // Second call with the same style should return the cached instance
        CSL processor2 = wrapper.getCitationProcessor("apa");
        Assertions.assertSame(processor, processor2);

        // A different style should produce a separate processor
        CSL chicagoProcessor = wrapper.getCitationProcessor("chicago-notes-bibliography");
        Assertions.assertNotNull(chicagoProcessor);
        Assertions.assertNotSame(processor, chicagoProcessor);

        // Null style should throw IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class, () -> wrapper.getCitationProcessor(null));
    }
}
