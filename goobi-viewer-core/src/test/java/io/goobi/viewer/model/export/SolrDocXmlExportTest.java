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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.controller.StringConstants;

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

    /**
     * @see SolrDocXmlExport#sanitize(SolrDocumentList)
     * @verifies remove access-restricted values from multi-valued fields
     */
    @Test
    void sanitize_shouldRemoveRestrictedValuesFromMultiValuedField() {
        SolrDocumentList docs = new SolrDocumentList();
        docs.setNumFound(1);
        SolrDocument doc = new SolrDocument();
        doc.addField("MD_AUTHOR", Arrays.asList("Doe, John", StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED, "Smith, Jane"));
        docs.add(doc);

        SolrDocumentList result = SolrDocXmlExport.sanitize(docs);

        assertEquals(1, result.size());
        @SuppressWarnings("unchecked")
        Collection<Object> authors = (Collection<Object>) result.get(0).getFieldValue("MD_AUTHOR");
        assertNotNull(authors);
        assertEquals(2, authors.size());
        assertTrue(authors.stream().noneMatch(v -> StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(v)));
        assertTrue(authors.stream().anyMatch(v -> "Doe, John".equals(v)));
        assertTrue(authors.stream().anyMatch(v -> "Smith, Jane".equals(v)));
    }

    /**
     * @see SolrDocXmlExport#sanitize(SolrDocumentList)
     * @verifies remove fields whose single value is access-restricted
     */
    @Test
    void sanitize_shouldRemoveSingleValuedFieldIfRestricted() {
        SolrDocumentList docs = new SolrDocumentList();
        docs.setNumFound(1);
        SolrDocument doc = new SolrDocument();
        doc.addField("MD_TITLE", "A Title");
        doc.addField("MD_AUTHOR", StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED);
        docs.add(doc);

        SolrDocumentList result = SolrDocXmlExport.sanitize(docs);

        assertEquals(1, result.size());
        SolrDocument cleanDoc = result.get(0);
        assertNotNull(cleanDoc.getFieldValue("MD_TITLE"));
        assertFalse(cleanDoc.containsKey("MD_AUTHOR"), "Restricted single-value field should be removed");
    }

    /**
     * @see SolrDocXmlExport#sanitize(SolrDocumentList)
     * @verifies deduplicate multi-valued fields preserving insertion order
     */
    @Test
    void sanitize_shouldDeduplicateMultiValuedFields() {
        SolrDocumentList docs = new SolrDocumentList();
        docs.setNumFound(1);
        SolrDocument doc = new SolrDocument();
        doc.addField("MD_SUBJECT", Arrays.asList("History", "Science", "History", "Art", "Science"));
        docs.add(doc);

        SolrDocumentList result = SolrDocXmlExport.sanitize(docs);

        @SuppressWarnings("unchecked")
        Collection<Object> subjects = (Collection<Object>) result.get(0).getFieldValue("MD_SUBJECT");
        assertNotNull(subjects);
        assertEquals(3, subjects.size(), "Duplicates should be removed");
        // First occurrence of each value must be retained
        Object[] arr = subjects.toArray();
        assertEquals("History", arr[0]);
        assertEquals("Science", arr[1]);
        assertEquals("Art", arr[2]);
    }

    /**
     * @see SolrDocXmlExport#sanitize(SolrDocumentList)
     * @verifies leave documents with no restricted or duplicate values unchanged
     */
    @Test
    void sanitize_shouldLeaveCleanDocumentUnchanged() {
        SolrDocumentList docs = new SolrDocumentList();
        docs.setNumFound(1);
        SolrDocument doc = new SolrDocument();
        doc.addField("PI", "PPN12345");
        doc.addField("MD_TITLE", "Clean Title");
        doc.addField("MD_AUTHOR", Arrays.asList("Author One", "Author Two"));
        docs.add(doc);

        SolrDocumentList result = SolrDocXmlExport.sanitize(docs);

        assertEquals(1, result.size());
        SolrDocument cleanDoc = result.get(0);
        assertEquals("PPN12345", cleanDoc.getFieldValue("PI"));
        assertEquals("Clean Title", cleanDoc.getFieldValue("MD_TITLE"));
        @SuppressWarnings("unchecked")
        Collection<Object> authors = (Collection<Object>) cleanDoc.getFieldValue("MD_AUTHOR");
        assertEquals(2, authors.size());
    }
}
