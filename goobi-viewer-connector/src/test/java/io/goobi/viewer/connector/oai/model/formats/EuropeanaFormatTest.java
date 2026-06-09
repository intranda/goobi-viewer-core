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
import io.goobi.viewer.solr.SolrConstants;

class EuropeanaFormatTest extends AbstractSolrEnabledTest {

    /**
     * @see EuropeanaFormat#generateSingleESERecord(SolrDocument,RequestHandler,String)
     * @verifies generate element correctly
     */
    @Test
    void ggenerateSingleESERecord_shouldGenerateElementCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.PI, "PPN123456789");
        doc.setField(SolrConstants.ACCESSCONDITION, "restricted");
        doc.setField(SolrConstants.ISWORK, true);
        doc.setField("MD_LANGUAGE", "en");
        doc.setField(SolrConstants.TITLE, "Lorem ipsum");
        doc.setField("MD_INFORMATION", "info");
        doc.setField("MD_YEARPUBLISH", "2024");
        doc.addField("MD_CREATOR", "Doe, John");
        doc.addField("MD_DATECREATED", "2024-12-20");
        doc.addField("MD_DATEISSUED", "2024-12-21");
        doc.addField(SolrConstants.DC, "varia");
        doc.addField("MD_PUBLISHER", "Indie");
        doc.addField("MD_PLACEPUBLISH", "Somewhere");
        doc.addField(SolrConstants.DOCSTRCT, "monograph");

        RequestHandler handler = new RequestHandler();

        Namespace nsDc = Namespace.getNamespace(Metadata.DC.getMetadataNamespacePrefix(), Metadata.DC.getMetadataNamespaceUri());
        Namespace nsEuropeana = Namespace.getNamespace(Metadata.ESE.getMetadataNamespacePrefix(), Metadata.ESE.getMetadataNamespaceUri());

        EuropeanaFormat format = new EuropeanaFormat();
        Element eleRecord = format.generateSingleESERecord(doc, handler, null);
        Assertions.assertNotNull(eleRecord);

        Element eleHeader = eleRecord.getChild("header", Format.OAI_NS);
        Assertions.assertNotNull(eleHeader);
        Assertions.assertEquals("repoPPN123456789", eleHeader.getChildText("identifier", Format.OAI_NS));

        Element eleMetadata = eleRecord.getChild("metadata", Format.OAI_NS);
        Assertions.assertNotNull(eleMetadata);
        Element eleEuropeanaRecord = eleMetadata.getChild("record", nsEuropeana);
        Assertions.assertNotNull(eleEuropeanaRecord);

        // dc:language
        Element eleDcLanguage = eleEuropeanaRecord.getChild("language", nsDc);
        Assertions.assertNotNull(eleDcLanguage);
        Assertions.assertEquals("en", eleDcLanguage.getText());

        // dc:title
        Element eleDcTitle = eleEuropeanaRecord.getChild("title", nsDc);
        Assertions.assertNotNull(eleDcTitle);
        Assertions.assertEquals("Lorem ipsum", eleDcTitle.getText());

        // dc:description
        Element eleDcDescription = eleEuropeanaRecord.getChild("description", nsDc);
        Assertions.assertNotNull(eleDcDescription);
        Assertions.assertEquals("info", eleDcDescription.getText());

        // dc:created
        Element eleDcDateCreated = eleEuropeanaRecord.getChild("created", nsDc);
        Assertions.assertNotNull(eleDcDateCreated);
        Assertions.assertEquals("2024-12-20", eleDcDateCreated.getText());

        // dc:issued
        Element eleDcDateIssued = eleEuropeanaRecord.getChild("issued", nsDc);
        Assertions.assertNotNull(eleDcDateIssued);
        Assertions.assertEquals("2024-12-21", eleDcDateIssued.getText());

        // dc:subject
        Element eleDcSubject = eleEuropeanaRecord.getChild("subject", nsDc);
        Assertions.assertNotNull(eleDcSubject);
        Assertions.assertEquals("varia", eleDcSubject.getText());

        // dc:publisher
        Element eleDcPublisher = eleEuropeanaRecord.getChild("publisher", nsDc);
        Assertions.assertNotNull(eleDcPublisher);
        Assertions.assertEquals("Indie", eleDcPublisher.getText());

        // dc:type
        Element eleDcType = eleEuropeanaRecord.getChild("type", nsDc);
        Assertions.assertNotNull(eleDcType);
        Assertions.assertEquals("monograph", eleDcType.getText());

        // dc:format
        List<Element> eleListDcFormat = eleEuropeanaRecord.getChildren("format", nsDc);
        Assertions.assertNotNull(eleListDcFormat);
        Assertions.assertEquals(2, eleListDcFormat.size());
        Assertions.assertEquals("image/jpeg", eleListDcFormat.get(0).getText());
        Assertions.assertEquals("application/pdf", eleListDcFormat.get(1).getText());

        // dc:source
        Element eleDcSource = eleEuropeanaRecord.getChild("source", nsDc);
        Assertions.assertNotNull(eleDcSource);

        // europeana:provider
        Element eleEuropeanaProvider = eleEuropeanaRecord.getChild("provider", nsEuropeana);
        Assertions.assertNotNull(eleEuropeanaProvider);
        Assertions.assertEquals("Institution XYZ", eleEuropeanaProvider.getText());
        
        // europeana:type
        Element eleEuropeanaType = eleEuropeanaRecord.getChild("type", nsEuropeana);
        Assertions.assertNotNull(eleEuropeanaType);
        Assertions.assertEquals("TEXT", eleEuropeanaType.getText());
        
        // europeana:rights
        Element eleEuropeanaRights = eleEuropeanaRecord.getChild("rights", nsEuropeana);
        Assertions.assertNotNull(eleEuropeanaRights);
        Assertions.assertEquals("http://www.example.com/rights", eleEuropeanaRights.getText());
        
        // europeana:dataProvider
        Element eleEuropeanaDataProvider = eleEuropeanaRecord.getChild("dataProvider", nsEuropeana);
        Assertions.assertNotNull(eleEuropeanaDataProvider);
        Assertions.assertEquals("Institution XYZ", eleEuropeanaDataProvider.getText());
        
        // europeana:isShownAt
        Element eleEuropeanaIsShownAt = eleEuropeanaRecord.getChild("isShownAt", nsEuropeana);
        Assertions.assertNotNull(eleEuropeanaIsShownAt);
        Assertions.assertEquals("http://localhost/viewer/piresolver?id=PPN123456789", eleEuropeanaIsShownAt.getText());
    }
}