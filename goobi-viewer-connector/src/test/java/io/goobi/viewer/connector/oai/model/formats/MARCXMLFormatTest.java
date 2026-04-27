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

import java.nio.charset.StandardCharsets;

import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.utils.XmlConstants;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.solr.SolrConstants;

class MARCXMLFormatTest extends AbstractSolrEnabledTest {

    /**
     * @see MARCXMLFormat#generateMarc(Element,String,String)
     * @verifies generate element correctly
     */
    @Test
    void createGetRecord_shouldGenerateElementCorrectly() throws Exception {
        Document metsDoc = XmlTools.readXmlFile("src/test/resources/viewer/indexed_mets/PPN517154005.xml");
        Assertions.assertNotNull(metsDoc);
        Element eleMets = metsDoc.getRootElement();
        Assertions.assertEquals("mets", eleMets.getName());

        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.PI_TOPSTRUCT, "PPN517154005");

        Element eleRecordMets =
                METSFormat.generateMetsRecord(XmlTools.getStringFromElement(metsDoc, StandardCharsets.UTF_8.name()), doc, new RequestHandler(),
                        null, null);
        Assertions.assertNotNull(eleRecordMets);

        Element eleGetRecordWrapper = new Element("GetRecord", Format.OAI_NS);
        eleGetRecordWrapper.addContent(eleRecordMets);

        Element eleGetRecordMarc = MARCXMLFormat.generateMarc(eleGetRecordWrapper, "PPN517154005", "GetRecord");
        Assertions.assertNotNull(eleGetRecordMarc);

        Element eleRecord = eleGetRecordMarc.getChild(XmlConstants.ELE_NAME_RECORD, Format.OAI_NS);
        Assertions.assertNotNull(eleRecord);
        Assertions.assertEquals(XmlConstants.ELE_NAME_RECORD, eleRecord.getName());
        Assertions.assertNotNull(eleRecord.getChild(XmlConstants.ELE_NAME_HEADER, Format.OAI_NS));
        Element eleMetadata = eleRecord.getChild(XmlConstants.ELE_NAME_METADATA, Format.OAI_NS);
        Assertions.assertNotNull(eleMetadata);

        Element eleMarcRecord = eleMetadata.getChild(XmlConstants.ELE_NAME_RECORD, MARCXMLFormat.NAMESPACE_MARC);
        Assertions.assertNotNull(eleMarcRecord);
        Assertions.assertEquals(XmlConstants.ELE_NAME_RECORD, eleMarcRecord.getName());
    }
}