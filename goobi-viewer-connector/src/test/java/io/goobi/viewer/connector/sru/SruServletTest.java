package io.goobi.viewer.connector.sru;

import java.util.Arrays;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.utils.SolrSearchIndex;
import io.goobi.viewer.solr.SolrConstants;

class SruServletTest extends AbstractSolrEnabledTest {

    /**
     * @see SruServlet#createSchemaInfoElement(String,String,String,String)
     * @verifies create element correctly
     */
    @Test
    void createSchemaInfoElement_shouldCreateElementCorrectly() throws Exception {
        Element ele = SruServlet.createSchemaInfoElement("Schema F", "sf", "info:srw/schema/1/f-1.0", "http://example.com/schemaf.xsd");
        Assertions.assertNotNull(ele);
        Element eleSchema = ele.getChild("schema", SruServlet.EXPLAIN_NAMESPACE);
        Assertions.assertNotNull(eleSchema);
        Assertions.assertEquals("true", eleSchema.getAttributeValue("retrieve"));
        Assertions.assertEquals("false", eleSchema.getAttributeValue("sort"));
        Assertions.assertEquals("info:srw/schema/1/f-1.0", eleSchema.getAttributeValue("identifier"));
        Assertions.assertEquals("http://example.com/schemaf.xsd", eleSchema.getAttributeValue("location"));
        Assertions.assertEquals("sf", eleSchema.getAttributeValue("name"));
        Assertions.assertEquals("Schema F", eleSchema.getChildText("title", SruServlet.EXPLAIN_NAMESPACE));

    }

    /**
     * @see SruServlet#createSupportsElement(String)
     * @verifies create element correctly
     */
    @Test
    void createSupportsElement_shouldCreateElementCorrectly() throws Exception {
        Element ele = SruServlet.createSupportsElement("your mom");
        Assertions.assertNotNull(ele);
        Assertions.assertEquals("your mom", ele.getText());
    }

    /**
     * @see SruServlet#generateSearchRetrieve(SruRequestParameter,SolrSearchIndex,String)
     * @verifies create element correctly
     */
    @Test
    void generateSearchRetrieve_shouldCreateElementCorrectly() throws Exception {
        SruRequestParameter parameter =
                new SruRequestParameter(SruOperation.SEARCHRETRIEVE, "1.2", "identifier=PPN517154005", 1, 10, "recordPacking_value",
                        Metadata.OAI_DC, "recordXPath_value", "resultSetTTL_value", "sortKeys_value", "stylesheet_value", "extraRequestData_value",
                        "scanClause_value", 5, 10);

        Element eleSearchRetrieve = SruServlet.generateSearchRetrieve(parameter, DataManager.getInstance().getSearchIndex(), "-DC:foo");
        Assertions.assertNotNull(eleSearchRetrieve);
        Assertions.assertEquals("1.2", eleSearchRetrieve.getChildText("version", SruServlet.SRU_NAMESPACE));
        Assertions.assertEquals("1", eleSearchRetrieve.getChildText("numberOfRecords", SruServlet.SRU_NAMESPACE));

        Element eleRecords = eleSearchRetrieve.getChild("records", SruServlet.SRU_NAMESPACE);
        Assertions.assertNotNull(eleRecords);

        Assertions.assertNotNull(eleRecords.getChild("record", SruServlet.SRU_NAMESPACE));
        Assertions.assertNotNull(eleSearchRetrieve.getChild("echoedSearchRetrieveRequest", SruServlet.SRU_NAMESPACE));

    }

    /**
     * @see SruServlet#createWrongSchemaDocument(SruRequestParameter,String)
     * @verifies create document correctly
     */
    @Test
    void createWrongSchemaDocument_shouldCreateDocumentCorrectly() throws Exception {
        Document doc = SruServlet.createWrongSchemaDocument("1.2", "sf");
        Assertions.assertNotNull(doc);
        Element eleRoot = doc.getRootElement();
        Assertions.assertNotNull(eleRoot);
        Assertions.assertEquals("1.2", eleRoot.getChildText("version", SruServlet.SRU_NAMESPACE));

        Element eleDiagnostic = eleRoot.getChild("diagnostic", SruServlet.SRU_NAMESPACE);
        Assertions.assertNotNull(eleDiagnostic);
        Element eleUri = eleDiagnostic.getChild("uri", SruServlet.DIAG_NAMESPACE);
        Assertions.assertNotNull(eleUri);
        Assertions.assertEquals("info:srw/diagnostic/1/66", eleUri.getText());
        Assertions.assertEquals("Unknown schema for retrieval / sf", eleDiagnostic.getChildText("message", SruServlet.DIAG_NAMESPACE));
    }

    /**
     * @see SruServlet#createMissingArgumentDocument(String,String)
     * @verifies create document correctly
     */
    @Test
    void createMissingArgumentDocument_shouldCreateDocumentCorrectly() throws Exception {
        Document doc = SruServlet.createMissingArgumentDocument("1.1", "scanClause");
        Assertions.assertNotNull(doc);
        Element eleRoot = doc.getRootElement();
        Assertions.assertNotNull(eleRoot);
        Assertions.assertEquals("1.1", eleRoot.getChildText("version", SruServlet.SRU_NAMESPACE));

        Element eleDiagnostic = eleRoot.getChild("diagnostic", SruServlet.SRU_NAMESPACE);
        Assertions.assertNotNull(eleDiagnostic);
        Element eleUri = eleDiagnostic.getChild("uri", SruServlet.DIAG_NAMESPACE);
        Assertions.assertNotNull(eleUri);
        Assertions.assertEquals("info:srw/diagnostic/1/7", eleUri.getText());
        Assertions.assertEquals("Mandatory parameter not supplied / scanClause", eleDiagnostic.getChildText("message", SruServlet.DIAG_NAMESPACE));
    }

    /**
     * @see SruServlet#createUnsupportedOperationDocument(String,String)
     * @verifies create document correctly
     */
    @Test
    void createUnsupportedOperationDocument_shouldCreateDocumentCorrectly() throws Exception {
        Document doc = SruServlet.createUnsupportedOperationDocument("1.2", "foobar");
        Assertions.assertNotNull(doc);
        Element eleRoot = doc.getRootElement();
        Assertions.assertNotNull(eleRoot);
        Assertions.assertEquals("1.2", eleRoot.getChildText("version", SruServlet.SRU_NAMESPACE));

        Element eleDiagnostic = eleRoot.getChild("diagnostic", SruServlet.SRU_NAMESPACE);
        Assertions.assertNotNull(eleDiagnostic);
        Element eleUri = eleDiagnostic.getChild("uri", SruServlet.DIAG_NAMESPACE);
        Assertions.assertNotNull(eleUri);
        Assertions.assertEquals("info:srw/diagnostic/1/4", eleUri.getText());
        Assertions.assertEquals("Unsupported operation / foobar", eleDiagnostic.getChildText("message", SruServlet.DIAG_NAMESPACE));
    }

    /**
     * @see SruServlet#generateSearchQuery(String,Metadata,String)
     * @verifies throw {@link IllegalArgumentException} if sruQuery null
     */
    @Test
    void generateSearchQuery_shouldThrowLinkIllegalArgumentExceptionIfSruQueryNull() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SruServlet.generateSearchQuery(null, Metadata.LIDO, null));
    }

    /**
     * @see SruServlet#generateSearchQuery(String,Metadata,String)
     * @verifies create query correctly
     */
    @Test
    void generateSearchQuery_shouldCreateQueryCorrectly() throws Exception {
        String result = SruServlet.generateSearchQuery("dc.identifier=urn:nbn:foo;bar:123", Metadata.LIDO, " -DC:a.*");
        Assertions.assertEquals("URN:urn:nbn:foo;bar:123 AND SOURCEDOCFORMAT:LIDO AND (ISWORK:true OR ISANCHOR:true) -DC:a.*", result);

        result = SruServlet.generateSearchQuery("anywhere=foo", Metadata.MARCXML, null);
        Assertions.assertEquals("*:foo AND SOURCEDOCFORMAT:METS AND (ISWORK:true OR ISANCHOR:true)", result);
    }
    
    /**
     * @see SruServlet#generateSolrRecord(SolrDocument,Element)
     * @verifies add correct element types
     */
    @Test
    void generateSolrRecord_shouldAddCorrectElementTypes() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.DOCSTRCT, "monograph");
        doc.addField(SolrConstants.DATECREATED, 123L);
        doc.addField(SolrConstants.NUMPAGES, 10);
        doc.addField(SolrConstants.ISWORK, true);
        doc.addField("MD_FOO", Arrays.asList("one", "two"));
        
        Element ele = new Element("foo");
        
        SruServlet.generateSolrRecord(doc, ele);
        
        Element eleDoc = ele.getChild("doc");
        Assertions.assertNotNull(doc);
        
        Element eleStr = eleDoc.getChild("str");
        Assertions.assertNotNull(eleStr);
        Assertions.assertEquals(SolrConstants.DOCSTRCT, eleStr.getAttributeValue("name"));
        Assertions.assertEquals("monograph", eleStr.getText());
        
        Element eleLong = eleDoc.getChild("long");
        Assertions.assertNotNull(eleLong);
        Assertions.assertEquals(SolrConstants.DATECREATED, eleLong.getAttributeValue("name"));
        Assertions.assertEquals("123", eleLong.getText());
        
        Element eleInt = eleDoc.getChild("int");
        Assertions.assertNotNull(eleInt);
        Assertions.assertEquals(SolrConstants.NUMPAGES, eleInt.getAttributeValue("name"));
        Assertions.assertEquals("10", eleInt.getText());
        
        Element eleBool = eleDoc.getChild("bool");
        Assertions.assertNotNull(eleBool);
        Assertions.assertEquals(SolrConstants.ISWORK, eleBool.getAttributeValue("name"));
        Assertions.assertEquals("true", eleBool.getText());
        
        Element eleArr = eleDoc.getChild("arr");
        Assertions.assertNotNull(eleArr);
        Assertions.assertEquals("MD_FOO", eleArr.getAttributeValue("name"));
        List<Element> eleListArrStr = eleArr.getChildren("str");
        Assertions.assertNotNull(eleListArrStr);
        Assertions.assertEquals(2, eleListArrStr.size());
        Assertions.assertEquals("one", eleListArrStr.get(0).getText());
        Assertions.assertEquals("two", eleListArrStr.get(1).getText());
    }
}