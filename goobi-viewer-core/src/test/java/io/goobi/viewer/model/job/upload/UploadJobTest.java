/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
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
package io.goobi.viewer.model.job.upload;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;

public class UploadJobTest extends AbstractTest {

    /**
     * @see UploadJob#buildXmlBody()
     * @verifies create xml document correctly
     */
    @Test
    public void buildXmlBody_shouldCreateXmlDocumentCorrectly() throws Exception {
        UploadJob uj = new UploadJob();
        uj.setEmail("a@b.com");
        uj.setPi("PPN123");
        uj.setTitle("Lorem ipsum");
        uj.setDescription("Lorem ipsum dolor sit amet...");

        Document doc = uj.buildXmlBody();
        Assert.assertNotNull(doc);
        Assert.assertNotNull(doc.getRootElement());
        Assert.assertEquals("PPN123", doc.getRootElement().getChildText("identifier"));
        Assert.assertEquals("manuscript", doc.getRootElement().getChildText("docstruct"));
        Assert.assertEquals("loreip_PPN123", doc.getRootElement().getChildText("processtitle"));

        {
            Element eleMetadataList = doc.getRootElement().getChild("metadataList");
            Assert.assertNotNull(eleMetadataList);
            Assert.assertEquals(2, eleMetadataList.getChildren("metadata").size());
            for (Element eleMetadata : eleMetadataList.getChildren("metadata")) {
                switch (eleMetadata.getAttributeValue("name")) {
                    case "Description":
                        Assert.assertEquals("Lorem ipsum dolor sit amet...", eleMetadata.getText());
                        break;
                    case "TitleDocMain":
                        Assert.assertEquals("Lorem ipsum", eleMetadata.getText());
                        break;
                    default:
                        Assert.fail("Unknown metadata name: " + eleMetadata.getAttributeValue("name"));
                        break;
                }
            }
        }
        {
            Element elePropertyList = doc.getRootElement().getChild("propertyList");
            Assert.assertNotNull(elePropertyList);
            Assert.assertEquals(1, elePropertyList.getChildren("property").size());
            for (Element eleProperty : elePropertyList.getChildren("metadata")) {
                switch (eleProperty.getAttributeValue("name")) {
                    case "email":
                        Assert.assertEquals("a@b.com", eleProperty.getText());
                        break;
                    default:
                        Assert.fail("Unknown property name: " + eleProperty.getAttributeValue("name"));
                        break;
                }
            }
        }

    }
}