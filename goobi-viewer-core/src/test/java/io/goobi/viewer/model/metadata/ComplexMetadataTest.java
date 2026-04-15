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
package io.goobi.viewer.model.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

class ComplexMetadataTest {

    private static final String YEAR = "1972";
    private static final String TITLE_EN = "The title";
    private static final String TITLE_DE = "Der Titel";
    private static final String MDTYPE = "PERSON";
    private static final String PI = "PI12345";
    private static final String IDDOC = "456";
    private static final String IDDOC_OWNER = "123";

    /**
     * @verifies return correct field values from single document
     */
    @Test
    void getFirstValue_shouldReturnCorrectFieldValuesFromSingleDocument() {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
        doc.setField(SolrConstants.IDDOC, IDDOC.toString());
        doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER.toString());
        doc.setField(SolrConstants.DOCTYPE, DocType.METADATA.name());
        doc.setField(SolrConstants.LABEL, "The label");
        doc.setField(SolrConstants.METADATATYPE, MDTYPE);
        doc.setField("MD_TITLE_LANG_DE", TITLE_DE);
        doc.setField("MD_TITLE_LANG_EN", TITLE_EN);
        doc.setField(SolrConstants.YEAR, YEAR);

        ComplexMetadata md = ComplexMetadata.getFromSolrDoc(doc);

        assertEquals(PI, md.getTopStructIdentifier());
        assertEquals(IDDOC_OWNER, md.getOwnerId());
        assertEquals(IDDOC, md.getId());
        assertEquals(MDTYPE, md.getType());
        assertEquals(YEAR, md.getFirstValue(SolrConstants.YEAR, null));
        assertEquals(TITLE_DE, md.getFirstValue(SolrConstants.TITLE, Locale.GERMAN));
        assertEquals(TITLE_EN, md.getFirstValue(SolrConstants.TITLE, Locale.ENGLISH));
    }

    /**
     * @verifies return correct field values from multilanguage docs
     */
    @Test
    void getFromMultilanganguageDocs_shouldReturnCorrectFieldValuesFromMultilanguageDocs() {
        List<SolrDocument> docs = new ArrayList<>();
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
            doc.setField(SolrConstants.IDDOC, IDDOC.toString());
            doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER.toString());
            doc.setField("MD_REFID", "1");
            doc.setField(SolrConstants.DOCTYPE, DocType.METADATA.name());
            doc.setField(SolrConstants.LABEL, "MD_TITLE_LANG_DE");
            doc.setField(SolrConstants.METADATATYPE, MDTYPE);
            doc.setField("MD_VALUE", TITLE_DE);
            docs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
            doc.setField(SolrConstants.IDDOC, IDDOC.toString());
            doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER.toString());
            doc.setField("MD_REFID", "1");
            doc.setField(SolrConstants.DOCTYPE, DocType.METADATA.name());
            doc.setField(SolrConstants.LABEL, "MD_TITLE_LANG_EN");
            doc.setField(SolrConstants.METADATATYPE, MDTYPE);
            doc.setField("MD_VALUE", TITLE_EN);
            docs.add(doc);
        }

        ComplexMetadata md = ComplexMetadata.getFromMultilanganguageDocs(docs);
        assertEquals(PI, md.getTopStructIdentifier());
        assertEquals(IDDOC_OWNER, md.getOwnerId());
        assertEquals(IDDOC, md.getId());
        assertEquals(MDTYPE, md.getType());
        assertEquals(TITLE_DE, md.getFirstValue(SolrConstants.TITLE, Locale.GERMAN));
        assertEquals(TITLE_EN, md.getFirstValue(SolrConstants.TITLE, Locale.ENGLISH));
    }

    /**
     * Verify that multiple Solr documents sharing the same MD_REFID are grouped into a single
     * translated ComplexMetadata entry, even when the number of duplicates is large.
     * This guards against O(n²) list-copy behaviour in the grouping implementation.
     * @verifies merges documents with same ref id
     */
    @Test
    void getMetadataFromDocuments_shouldMergesDocumentsWithSameRefId() {
        // 100 language-variant docs all belonging to the same logical metadata value (REFID="1")
        List<SolrDocument> docs = IntStream.range(0, 100).mapToObj(i -> {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
            doc.setField(SolrConstants.IDDOC, String.valueOf(400 + i));
            doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER);
            doc.setField("MD_REFID", "1");
            doc.setField(SolrConstants.DOCTYPE, SolrConstants.DocType.METADATA.name());
            doc.setField(SolrConstants.LABEL, "MD_TITLE_LANG_DE");
            doc.setField(SolrConstants.METADATATYPE, MDTYPE);
            doc.setField("MD_VALUE", "Titel " + i);
            return doc;
        }).collect(Collectors.toList());

        // Only one logical metadata entity should result (all docs belong to the same REFID group)
        List<ComplexMetadata> result = ComplexMetadata.getMetadataFromDocuments(docs);
        assertEquals(1, result.size(), "100 docs with the same REFID must yield exactly 1 ComplexMetadata entry");
    }

    /**
     * Verify that documents without MD_REFID (null key) are each treated as an independent
     * untranslated ComplexMetadata entry rather than being merged into a single group.
     * @verifies keeps null ref id documents as separate entries
     */
    @Test
    void getMetadataFromDocuments_shouldKeepsNullRefIdDocumentsAsSeparateEntries() {
        List<SolrDocument> docs = new ArrayList<>();
        for (String value : List.of("Alpha", "Beta", "Gamma")) {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
            doc.setField(SolrConstants.IDDOC, IDDOC);
            doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER);
            // no MD_REFID → null key → untranslated path
            doc.setField(SolrConstants.DOCTYPE, SolrConstants.DocType.METADATA.name());
            doc.setField(SolrConstants.LABEL, "MD_TITLE");
            doc.setField(SolrConstants.METADATATYPE, MDTYPE);
            doc.setField("MD_VALUE", value);
            docs.add(doc);
        }

        List<ComplexMetadata> result = ComplexMetadata.getMetadataFromDocuments(docs);
        // Each null-REFID doc must produce its own ComplexMetadata (not merged into one multilang group)
        assertEquals(3, result.size(), "Each document without MD_REFID must produce a separate ComplexMetadata entry");
    }

    /**
     * @verifies return correct values from multiple metadata documents
     */
    @Test
    void getMetadataFromDocuments_shouldReturnCorrectValuesFromMultipleMetadataDocuments() {
        List<SolrDocument> docs = new ArrayList<>();

        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
            doc.setField(SolrConstants.IDDOC, IDDOC.toString());
            doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER.toString());
            doc.setField("MD_REFID", "1");
            doc.setField(SolrConstants.DOCTYPE, DocType.METADATA.name());
            doc.setField(SolrConstants.LABEL, "MD_TITLE_LANG_DE");
            doc.setField(SolrConstants.METADATATYPE, MDTYPE);
            doc.setField("MD_VALUE", TITLE_DE);
            docs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
            doc.setField(SolrConstants.IDDOC, IDDOC.toString());
            doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER.toString());
            doc.setField("MD_REFID", "1");
            doc.setField(SolrConstants.DOCTYPE, DocType.METADATA.name());
            doc.setField(SolrConstants.LABEL, "MD_TITLE_LANG_EN");
            doc.setField(SolrConstants.METADATATYPE, MDTYPE);
            doc.setField("MD_VALUE", TITLE_EN);
            docs.add(doc);
        }

        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
            doc.setField(SolrConstants.IDDOC, IDDOC.toString());
            doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER.toString());
            doc.setField("MD_REFID", "2");
            doc.setField(SolrConstants.DOCTYPE, DocType.METADATA.name());
            doc.setField(SolrConstants.LABEL, "MD_TITLE_LANG_DE");
            doc.setField(SolrConstants.METADATATYPE, MDTYPE);
            doc.setField("MD_VALUE", TITLE_DE + "_2");
            docs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
            doc.setField(SolrConstants.IDDOC, IDDOC.toString());
            doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER.toString());
            doc.setField("MD_REFID", "2");
            doc.setField(SolrConstants.DOCTYPE, DocType.METADATA.name());
            doc.setField(SolrConstants.LABEL, "MD_TITLE_LANG_EN");
            doc.setField(SolrConstants.METADATATYPE, MDTYPE);
            doc.setField("MD_VALUE", TITLE_EN + "_2");
            docs.add(doc);
        }

        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
            doc.setField(SolrConstants.IDDOC, IDDOC.toString());
            doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER.toString());
            doc.setField(SolrConstants.DOCTYPE, DocType.METADATA.name());
            doc.setField(SolrConstants.LABEL, "MD_RELATIONSHIP_EVENT");
            doc.setField(SolrConstants.METADATATYPE, MDTYPE);
            doc.setField("MD_VALUE", "Oscar");
            docs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
            doc.setField(SolrConstants.IDDOC, IDDOC.toString());
            doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER.toString());
            doc.setField(SolrConstants.DOCTYPE, DocType.METADATA.name());
            doc.setField(SolrConstants.LABEL, "MD_RELATIONSHIP_EVENT");
            doc.setField(SolrConstants.METADATATYPE, MDTYPE);
            doc.setField("MD_VALUE", "Bambi");
            docs.add(doc);
        }

        List<ComplexMetadata> mds = ComplexMetadata.getMetadataFromDocuments(docs);

        assertEquals(4, mds.size());
        assertEquals(1, mds.stream().filter(md -> "Bambi".equals(md.getFirstValue("MD_RELATIONSHIP_EVENT", null))).count());
        assertEquals(1, mds.stream().filter(md -> "Oscar".equals(md.getFirstValue("MD_RELATIONSHIP_EVENT", null))).count());
        assertEquals(1, mds.stream().filter(md -> TITLE_DE.equals(md.getFirstValue("MD_TITLE", Locale.GERMAN))).count());
        assertEquals(1, mds.stream().filter(md -> (TITLE_DE + "_2").equals(md.getFirstValue("MD_TITLE", Locale.GERMAN))).count());

    }

}
