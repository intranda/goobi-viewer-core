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

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.model.ResumptionToken;

class FormatTest extends AbstractSolrEnabledTest {

    /**
     * @see Format#getIdentifyXML(String)
     * @verifies construct element correctly
     */
    @Test
    void getIdentifyXML_shouldConstructElementCorrectly() throws Exception {
        Element eleIdentify = Format.getIdentifyXML(null);
        Assertions.assertNotNull(eleIdentify);
        Assertions.assertEquals("Identify", eleIdentify.getName());
        Assertions.assertEquals("OAI Frontend", eleIdentify.getChildText("repositoryName", null));
        Assertions.assertEquals("http://localhost:8080/viewer/oai", eleIdentify.getChildText("baseURL", null));
        Assertions.assertEquals("2.0", eleIdentify.getChildText("protocolVersion", null));
        Assertions.assertEquals("admin@example.com", eleIdentify.getChildText("adminEmail", null));
        Assertions.assertEquals("2012-10-10T12:07:32Z", eleIdentify.getChildText("earliestDatestamp", null));
        Assertions.assertEquals("transient", eleIdentify.getChildText("deletedRecord", null));
        Assertions.assertEquals("YYYY-MM-DDThh:mm:ssZ", eleIdentify.getChildText("granularity", null));
        Element eleDescription = eleIdentify.getChild("description", null);
        Assertions.assertNotNull(eleDescription);
        Assertions.assertEquals("Lorem ipsum dolor sit amet", eleDescription.getChildText("description", Format.DC_NS));
    }

    /**
     * @see Format#createMetadataFormats()
     * @verifies construct element correctly
     */
    @Test
    void createMetadataFormats_shouldConstructElementCorrectly() {
        Element ele = Format.createMetadataFormats();
        Assertions.assertNotNull(ele);
        Assertions.assertEquals("ListMetadataFormats", ele.getName());
        List<Element> eleListMetadataFormat = ele.getChildren("metadataFormat", null);
        Assertions.assertNotNull(eleListMetadataFormat);
        Assertions.assertEquals(9, eleListMetadataFormat.size());
        for (Element eleMetadataFormat : eleListMetadataFormat) {
            Assertions.assertNotNull(eleMetadataFormat.getChildText("metadataPrefix", null));
            Assertions.assertNotNull(eleMetadataFormat.getChildText("metadataNamespace", null));
            Assertions.assertNotNull(eleMetadataFormat.getChildText("schema", null));
        }
    }

    /**
     * @see Format#createListSets(Locale)
     * @verifies construct element correctly
     */
    @Test
    void createListSets_shouldConstructElementCorrectly() throws Exception {
        Element eleListSets = Format.createListSets(Locale.ENGLISH);
        Assertions.assertNotNull(eleListSets);
        Assertions.assertEquals("ListSets", eleListSets.getName());
        List<Element> eleListSet = eleListSets.getChildren("set", null);
        Assertions.assertNotNull(eleListSet);
        Assertions.assertEquals(55, eleListSet.size());
    }

    /**
     * @see Format#getOaiPmhElement(String)
     * @verifies construct element correctly
     */
    @Test
    void getOaiPmhElement_shouldConstructElementCorrectly() {
        Element ele = Format.getOaiPmhElement("oai");
        Assertions.assertNotNull(ele);
        Assertions.assertEquals("oai", ele.getName());
        Assertions.assertEquals("http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd",
                ele.getAttributeValue("schemaLocation", Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")));
    }

    /**
     * @see Format#createResumptionTokenAndElement(long,int,Namespace,RequestHandler)
     * @verifies construct element correctly
     */
    @Test
    void createResumptionTokenAndElement_shouldConstructElementCorrectly() throws Exception {
        File tokenFolder = new File(DataManager.getInstance().getConfiguration().getResumptionTokenFolder());
        try {
            if (!tokenFolder.exists()) {
                tokenFolder.mkdirs();
            }
            Element ele = Format.createResumptionTokenAndElement(100, 10, 0, new RequestHandler());
            Assertions.assertNotNull(ele);
            Assertions.assertEquals("resumptionToken", ele.getName());
            Assertions.assertNotNull(ele.getAttributeValue("expirationDate"));
            Assertions.assertEquals("100", ele.getAttributeValue("completeListSize"));
            Assertions.assertEquals("0", ele.getAttributeValue("cursor"));
        } finally {
            if (tokenFolder.isDirectory()) {
                FileUtils.deleteDirectory(tokenFolder);
            }
        }
    }

    /**
     * @see Format#handleToken(String)
     * @verifies return error if resumption token name illegal
     */
    @Test
    void handleToken_shouldReturnErrorIfResumptionTokenNameIllegal() {
        Element result = Format.handleToken("foo", "");
        Assertions.assertEquals("error", result.getName());
        Assertions.assertEquals("badResumptionToken", result.getAttributeValue("code"));
    }

    /**
     * @see Format#deserializeResumptionToken(File)
     * @verifies deserialize token correctly
     */
    @Test
    void deserializeResumptionToken_shouldDeserializeTokenCorrectly() throws Exception {
        File f = new File("src/test/resources/token/oai_1634822246437");
        Assertions.assertTrue(f.isFile());

        ResumptionToken token = Format.deserializeResumptionToken(f);
        Assertions.assertNotNull(token);
    }
}