package io.goobi.viewer.model.metadata;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.solr.common.SolrDocument;
import org.junit.Test;

import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

class ComplexMetadataTest {

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
