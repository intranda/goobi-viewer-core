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

import javax.xml.transform.TransformerException;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

class XsltSearchExportTest extends AbstractTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractTest.setUpClass();
    }

    private static SolrDocumentList createTestDocs() {
        SolrDocumentList docs = new SolrDocumentList();
        docs.setNumFound(1);
        docs.setStart(0);

        SolrDocument doc = new SolrDocument();
        doc.addField("PI", "PPN12345");
        doc.addField("MD_TITLE", "A Sample Monograph");
        doc.addField("MD_AUTHOR", "Doe, John");
        doc.addField("MD_YEARPUBLISH", "2024");
        doc.addField("MD_PUBLISHER", "Test Publisher");
        doc.addField("MD_PLACEPUBLISH", "Berlin");
        doc.addField("DOCSTRCT", "monograph");
        docs.add(doc);

        return docs;
    }

    /**
     * @see XsltSearchExport#transform(SolrDocumentList, String)
     * @verifies transform documents using the given stylesheet
     */
    @Test
    void transform_shouldTransformDocumentsUsingEndnoteStylesheet() throws Exception {
        SolrDocumentList docs = createTestDocs();

        String result = XsltSearchExport.transform(docs, "solr2endnote.xsl");
        assertNotNull(result);
        assertTrue(result.contains("<records>"));
        assertTrue(result.contains("<record>"));
        assertTrue(result.contains("A Sample Monograph"));
        assertTrue(result.contains("Doe, John"));
        assertTrue(result.contains("2024"));
        assertTrue(result.contains("Test Publisher"));
    }

    /**
     * @see XsltSearchExport#transform(SolrDocumentList, String)
     * @verifies transform documents using the given stylesheet
     */
    @Test
    void transform_shouldTransformDocumentsUsingBibtexStylesheet() throws Exception {
        SolrDocumentList docs = createTestDocs();

        String result = XsltSearchExport.transform(docs, "solr2bibtex.xsl");
        assertNotNull(result);
        assertTrue(result.contains("@book{PPN12345,"));
        assertTrue(result.contains("author = {Doe, John}"));
        assertTrue(result.contains("title = {A Sample Monograph}"));
        assertTrue(result.contains("year = {2024}"));
        assertTrue(result.contains("publisher = {Test Publisher}"));
        assertTrue(result.contains("address = {Berlin}"));
    }

    /**
     * @see XsltSearchExport#transform(SolrDocumentList, String)
     * @verifies transform documents using the given stylesheet
     */
    @Test
    void transform_shouldTransformDocumentsUsingRisStylesheet() throws Exception {
        SolrDocumentList docs = createTestDocs();

        String result = XsltSearchExport.transform(docs, "solr2ris.xsl");
        assertNotNull(result);
        assertTrue(result.contains("TY  - BOOK"));
        assertTrue(result.contains("TI  - A Sample Monograph"));
        assertTrue(result.contains("AU  - Doe, John"));
        assertTrue(result.contains("PY  - 2024"));
        assertTrue(result.contains("PB  - Test Publisher"));
        assertTrue(result.contains("CY  - Berlin"));
        assertTrue(result.contains("ER  - "));
    }

    /**
     * @see XsltSearchExport#transform(SolrDocumentList, String)
     * @verifies throw TransformerException for missing stylesheet
     */
    @Test
    void transform_shouldThrowTransformerExceptionForMissingStylesheet() {
        SolrDocumentList docs = createTestDocs();
        assertThrows(TransformerException.class, () -> XsltSearchExport.transform(docs, "nonexistent.xsl"));
    }

    /**
     * @see XsltSearchExport#transform(SolrDocumentList, String)
     */
    @Test
    void transform_shouldThrowOnNullDocs() {
        assertThrows(IllegalArgumentException.class, () -> XsltSearchExport.transform(null, "solr2endnote.xsl"));
    }

    /**
     * @see XsltSearchExport#transform(SolrDocumentList, String)
     */
    @Test
    void transform_shouldThrowOnBlankXsltFileName() {
        SolrDocumentList docs = createTestDocs();
        assertThrows(IllegalArgumentException.class, () -> XsltSearchExport.transform(docs, ""));
    }
}
