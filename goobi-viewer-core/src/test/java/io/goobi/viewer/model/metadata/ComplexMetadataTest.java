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

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

public class ComplexMetadataTest {

    private static final String YEAR = "1972";
    private static final String TITLE_EN = "The title";
    private static final String TITLE_DE = "Der Titel";
    private static final String MDTYPE = "PERSON";
    private static final String PI = "PI12345";
    private static final Long IDDOC = 456l;
    private static final Long IDDOC_OWNER = 123l;

    @Test
    void testSingleDoc() {
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

    @Test
    void testMultiDoc() {
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

    @Test
    void testMultiMetadata() {
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
