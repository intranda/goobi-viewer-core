package io.goobi.viewer.model.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

class ComplexMetadataTest {

    private static final String PI = "PI12345";
    private static final Object IDDOC = null;
    private static final Long IDDOC_OWNER = 123l;
    private static final Object PI_TOPSTRUCT = null;

    @Test
    void test() {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.PI_TOPSTRUCT, PI);
        doc.setField(SolrConstants.IDDOC, IDDOC);
        doc.setField(SolrConstants.IDDOC_OWNER, IDDOC_OWNER);
        doc.setField(SolrConstants.PI_TOPSTRUCT, PI_TOPSTRUCT);
        doc.setField(SolrConstants.DOCTYPE, DocType.METADATA.name());
        doc.setField(SolrConstants.METADATATYPE, "PERSON");
        doc.setField(SolrConstants.DOCTYPE, DocType.METADATA.name());
    }

}
