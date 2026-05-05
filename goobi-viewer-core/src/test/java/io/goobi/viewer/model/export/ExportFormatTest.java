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
package io.goobi.viewer.model.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExportFormatTest {

    @Test
    void constructor_shouldSetAllFields() {
        ExportFormat format = new ExportFormat("bibtex", true, "solr2bibtex.xsl", "text/plain", "bib");
        assertEquals("bibtex", format.getName());
        assertTrue(format.isEnabled());
        assertEquals("solr2bibtex.xsl", format.getXslt());
        assertEquals("text/plain", format.getContentType());
        assertEquals("bib", format.getFileExtension());
    }

    @Test
    void constructor_shouldHandleDisabledFormat() {
        ExportFormat format = new ExportFormat("disabled", false, "test.xsl", "text/plain", "txt");
        assertFalse(format.isEnabled());
    }

    @Test
    void toString_shouldContainName() {
        ExportFormat format = new ExportFormat("endnote", true, "solr2endnote.xsl", "application/xml", "xml");
        assertTrue(format.toString().contains("endnote"));
    }
}
