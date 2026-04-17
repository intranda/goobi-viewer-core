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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;

class SolrDocXmlExportTest {

    /**
     * @see SolrDocXmlExport#toXmlString(SolrDocumentList)
     * @verifies serialise single-valued fields correctly
     */
    @Test
    void toXmlString_shouldSerialiseSingleValuedFieldsCorrectly() throws Exception {
        SolrDocumentList docs = new SolrDocumentList();
        docs.setNumFound(1);
        docs.setStart(0);

        SolrDocument doc = new SolrDocument();
        doc.addField("PI", "PPN12345");
        doc.addField("MD_TITLE", "Test Title");
        docs.add(doc);

        String xml = SolrDocXmlExport.toXmlString(docs);
        assertNotNull(xml);
        assertTrue(xml.contains("numFound=\"1\""));
        assertTrue(xml.contains("name=\"PI\""));
        assertTrue(xml.contains("PPN12345"));
        assertTrue(xml.contains("name=\"MD_TITLE\""));
        assertTrue(xml.contains("Test Title"));
    }

    /**
     * @see SolrDocXmlExport#toXmlString(SolrDocumentList)
     * @verifies serialise multi-valued fields correctly
     */
    @Test
    void toXmlString_shouldSerialiseMultiValuedFieldsCorrectly() throws Exception {
        SolrDocumentList docs = new SolrDocumentList();
        docs.setNumFound(1);
        docs.setStart(0);

        SolrDocument doc = new SolrDocument();
        doc.addField("MD_AUTHOR", Arrays.asList("Author One", "Author Two"));
        docs.add(doc);

        String xml = SolrDocXmlExport.toXmlString(docs);
        assertNotNull(xml);
        assertTrue(xml.contains("<arr name=\"MD_AUTHOR\">"));
        assertTrue(xml.contains("Author One"));
        assertTrue(xml.contains("Author Two"));
    }

    /**
     * @see SolrDocXmlExport#toXmlString(SolrDocumentList)
     * @verifies return empty result element for empty list
     */
    @Test
    void toXmlString_shouldReturnEmptyResultElementForEmptyList() throws Exception {
        SolrDocumentList docs = new SolrDocumentList();
        docs.setNumFound(0);
        docs.setStart(0);

        String xml = SolrDocXmlExport.toXmlString(docs);
        assertNotNull(xml);
        assertTrue(xml.contains("numFound=\"0\""));
        assertTrue(xml.contains("<result"));
    }

    /**
     * @see SolrDocXmlExport#toXmlDocument(SolrDocumentList)
     */
    @Test
    void toXmlDocument_shouldThrowOnNull() {
        assertThrows(IllegalArgumentException.class, () -> SolrDocXmlExport.toXmlDocument(null));
    }
}
