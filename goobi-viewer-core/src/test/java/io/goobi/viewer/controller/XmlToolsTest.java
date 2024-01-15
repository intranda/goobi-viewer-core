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
package io.goobi.viewer.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.FileSystemException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class XmlToolsTest {

    private File tempDir = new File("target/temp");

    @AfterEach
    public void tearDown() throws Exception {
        if (tempDir.exists()) {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    /**
     * @see XmlTools#getDocumentFromString(String,String)
     * @verifies build document correctly
     */
    @Test
    void getDocumentFromString_shouldBuildDocumentCorrectly() throws Exception {
        String xml = "<root><child>child1</child><child>child2</child></root>";
        Document doc = XmlTools.getDocumentFromString(xml, null);
        Assertions.assertNotNull(doc);
        Assertions.assertNotNull(doc.getRootElement());
        Assertions.assertEquals("root", doc.getRootElement().getName());
        Assertions.assertNotNull(doc.getRootElement().getChildren("child"));
        Assertions.assertEquals(2, doc.getRootElement().getChildren("child").size());
    }

    /**
     * @see XmlTools#getStringFromElement(Object,String)
     * @verifies return XML string correctly for documents
     */
    @Test
    void getStringFromElement_shouldReturnXMLStringCorrectlyForDocuments() throws Exception {
        Document doc = new Document();
        doc.setRootElement(new Element("root"));
        String xml = XmlTools.getStringFromElement(doc, null);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("<root></root>"));
    }

    /**
     * @see XmlTools#getStringFromElement(Object,String)
     * @verifies return XML string correctly for elements
     */
    @Test
    void getStringFromElement_shouldReturnXMLStringCorrectlyForElements() throws Exception {
        String xml = XmlTools.getStringFromElement(new Element("root"), null);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("<root></root>"));
    }

    /**
     * @see XmlTools#writeXmlFile(Document,String)
     * @verifies write file correctly
     */
    void writeXmlFile_shouldWriteFileCorrectly() throws Exception {
        String filePath = tempDir + "/test.xml";
        Document doc = new Document();
        doc.setRootElement(new Element("root"));
        File xmlFile = XmlTools.writeXmlFile(doc, filePath);
        Assertions.assertTrue(xmlFile.isFile());
    }

    /**
     * @see XmlTools#writeXmlFile(Document,String)
     * @verifies throw FileSystemException if file is directory
     */
    @Test
    void writeXmlFile_shouldThrowFileSystemExceptionIfFileIsDirectory() throws Exception {
        Document doc = new Document();
        doc.setRootElement(new Element("root"));
        Assertions.assertThrows(FileSystemException.class, () -> XmlTools.writeXmlFile(doc, "target"));
    }

    /**
     * t
     *
     * @see XmlTools#readXmlFile(Path)
     * @verifies build document from path correctly
     */
    @Test
    void readXmlFile_shouldBuildDocumentFromPathCorrectly() throws Exception {
        Document doc = XmlTools.readXmlFile(Paths.get("src/test/resources/config_viewer.test.xml"));
        Assertions.assertNotNull(doc);
        Assertions.assertNotNull(doc.getRootElement());
    }

    /**
     * @see XmlTools#readXmlFile(String)
     * @verifies build document from string correctly
     */
    @Test
    void readXmlFile_shouldBuildDocumentFromStringCorrectly() throws Exception {
        Document doc = XmlTools.readXmlFile("src/test/resources/config_viewer.test.xml");
        Assertions.assertNotNull(doc);
        Assertions.assertNotNull(doc.getRootElement());
    }

    /**
     * @see XmlTools#readXmlFile(URL)
     * @verifies build document from url correctly
     */
    @Test
    void readXmlFile_shouldBuildDocumentFromUrlCorrectly() throws Exception {
        Document doc = XmlTools.readXmlFile(Paths.get("src/test/resources/config_viewer.test.xml").toUri().toURL());
        Assertions.assertNotNull(doc);
        Assertions.assertNotNull(doc.getRootElement());
    }

    /**
     * @see XmlTools#readXmlFile(String)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test
    void readXmlFile_shouldThrowFileNotFoundExceptionIfFileNotFound() throws Exception {
        Assertions.assertThrows(FileNotFoundException.class, () -> XmlTools.readXmlFile("notfound.xml"));
    }

}
