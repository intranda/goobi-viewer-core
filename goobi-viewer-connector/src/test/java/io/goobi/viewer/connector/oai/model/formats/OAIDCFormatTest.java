/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.connector.oai.model.formats;

import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.solr.SolrConstants;

class OAIDCFormatTest extends AbstractSolrEnabledTest {

    /**
     * @see OAIDCFormat#generateSingleDCRecord(SolrDocument,RequestHandler,String,Namespace,Namespace,List,String)
     * @verifies generate element correctly
     */
    @Test
    void generateSingleDCRecord_shouldGenerateElementCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.PI, "PPN123456789");
        doc.setField(SolrConstants.ACCESSCONDITION, "restricted");

        RequestHandler handler = new RequestHandler();

        Namespace nsOaiDoc = Namespace.getNamespace(Metadata.OAI_DC.getMetadataNamespacePrefix(), Metadata.OAI_DC.getMetadataNamespaceUri());

        OAIDCFormat format = new OAIDCFormat();
        Element eleRecord = format.generateSingleDCRecord(doc, handler, null, Format.OAI_NS, nsOaiDoc, null, null);
        Assertions.assertNotNull(eleRecord);

        Element eleHeader = eleRecord.getChild("header", Format.OAI_NS);
        Assertions.assertNotNull(eleHeader);
        Assertions.assertEquals("repoPPN123456789", eleHeader.getChildText("identifier", Format.OAI_NS));

        Element eleMetadata = eleRecord.getChild("metadata", Format.OAI_NS);
        Assertions.assertNotNull(eleMetadata);
        Element eleOaiDc = eleMetadata.getChild("dc", nsOaiDoc);
        Assertions.assertNotNull(eleOaiDc);
    }

    /**
     * @see OAIDCFormat#generateDcSource(SolrDocument,SolrDocument,SolrDocument,Namespace)
     * @verifies throw IllegalArgumentException if topstructDoc null
     */
    @Test
    void generateDcSource_shouldThrowIllegalArgumentExceptionIfTopstructDocNull() {
        Namespace ns = Namespace.getNamespace(Metadata.DC.getMetadataNamespacePrefix(), Metadata.DC.getMetadataNamespaceUri());
        Assertions.assertThrows(IllegalArgumentException.class, () -> OAIDCFormat.generateDcSource(null, null, null, ns));
    }

    /**
     * @see OAIDCFormat#generateDcSource(SolrDocument,SolrDocument,SolrDocument,Namespace)
     * @verifies create element correctly
     */
    @Test
    void generateDcSource_shouldCreateElementCorrectly() {
        SolrDocument doc = new SolrDocument();
        doc.addField("MD_CREATOR", "Doe, John");
        doc.addField("MD_CREATOR", StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED);
        doc.addField(SolrConstants.TITLE, "Foo Bar");
        doc.addField(SolrConstants.TITLE, StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED);
        doc.addField("MD_PLACEPUBLISH", "Somewhere");
        doc.addField("MD_PLACEPUBLISH", StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED);
        doc.addField("MD_PUBLISHER", "Indie");
        doc.addField("MD_PUBLISHER", StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED);
        doc.addField("MD_YEARPUBLISH", "2023");
        doc.addField("MD_YEARPUBLISH", StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED);

        Element ele = OAIDCFormat.generateDcSource(doc, doc, null,
                Namespace.getNamespace(Metadata.DC.getMetadataNamespacePrefix(), Metadata.DC.getMetadataNamespaceUri()));
        Assertions.assertNotNull(ele);
        Assertions.assertTrue(ele.getText().contains("Doe, John"));
        Assertions.assertTrue(ele.getText().contains("Foo Bar"));
        Assertions.assertTrue(ele.getText().contains("Somewhere"));
        Assertions.assertTrue(ele.getText().contains("Indie"));
        Assertions.assertTrue(ele.getText().contains("2023"));
        Assertions.assertFalse(ele.getText().contains(StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED));
    }
}