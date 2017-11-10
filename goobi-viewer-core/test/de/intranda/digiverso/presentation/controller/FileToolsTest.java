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
package de.intranda.digiverso.presentation.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class FileToolsTest {

    private File tempDir = new File("build/temp");

    @After
    public void tearDown() throws Exception {
        if (tempDir.exists()) {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    /**
     * @see FileTools#readXmlFile(String)
     * @verifies build document correctly
     */
    @Test
    public void readXmlFile_shouldBuildDocumentCorrectly() throws Exception {
        Document doc = FileTools.readXmlFile("resources/test/config_viewer.test.xml");
        Assert.assertNotNull(doc);
        Assert.assertNotNull(doc.getRootElement());
    }

    /**
     * @see FileTools#readXmlFile(String)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test(expected = FileNotFoundException.class)
    public void readXmlFile_shouldThrowFileNotFoundExceptionIfFileNotFound() throws Exception {
        FileTools.readXmlFile("notfound.xml");
    }

    /**
     * @see FileTools#writeXmlFile(Document,String)
     * @verifies write file correctly and return true
     */
    @Test
    public void writeXmlFile_shouldWriteFileCorrectlyAndReturnTrue() throws Exception {
        String filePath = "build/test.xml";
        Document doc = new Document();
        doc.setRootElement(new Element("root"));
        Assert.assertTrue(FileTools.writeXmlFile(doc, "build/test.xml"));
        File xmlFile = new File(filePath);
        Assert.assertTrue(xmlFile.isFile());
    }

    /**
     * @see FileTools#writeXmlFile(Document,String)
     * @verifies throw FileNotFoundException if file is directory
     */
    @Test(expected = FileNotFoundException.class)
    public void writeXmlFile_shouldThrowFileNotFoundExceptionIfFileIsDirectory() throws Exception {
        Document doc = new Document();
        doc.setRootElement(new Element("root"));
        FileTools.writeXmlFile(doc, "build");
    }

    /**
     * @see FileTools#writeXmlFile(Document,String)
     * @verifies return false if doc is null
     */
    @Test
    public void writeXmlFile_shouldReturnFalseIfDocIsNull() throws Exception {
        Assert.assertFalse(FileTools.writeXmlFile(null, "build/test.xml"));
    }

    /**
     * @see FileTools#getStringFromFile(File,String)
     * @verifies read text file correctly
     */
    @Test
    public void getStringFromFile_shouldReadTextFileCorrectly() throws Exception {
        File file = new File("resources/test/stopwords.txt");
        Assert.assertTrue(file.isFile());
        String contents = FileTools.getStringFromFile(file, null);
        Assert.assertTrue(StringUtils.isNotBlank(contents));
    }

    /**
     * @see FileTools#getStringFromFile(File,String)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test(expected = FileNotFoundException.class)
    public void getStringFromFile_shouldThrowFileNotFoundExceptionIfFileNotFound() throws Exception {
        File file = new File("notfound.txt");
        Assert.assertFalse(file.exists());
        FileTools.getStringFromFile(file, null);
    }

    /**
     * @see FileTools#getStringFromFilePath(String)
     * @verifies read text file correctly
     */
    @Test
    public void getStringFromFilePath_shouldReadTextFileCorrectly() throws Exception {
        String contents = FileTools.getStringFromFilePath("resources/test/stopwords.txt");
        Assert.assertTrue(StringUtils.isNotBlank(contents));
    }

    /**
     * @see FileTools#getStringFromFilePath(String)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test(expected = FileNotFoundException.class)
    public void getStringFromFilePath_shouldThrowFileNotFoundExceptionIfFileNotFound() throws Exception {
        File file = new File("notfound.txt");
        Assert.assertFalse(file.exists());
        FileTools.getStringFromFilePath(file.getPath());
    }

    /**
     * @see FileTools#compressGzipFile(File,File)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test(expected = FileNotFoundException.class)
    public void compressGzipFile_shouldThrowFileNotFoundExceptionIfFileNotFound() throws Exception {
        File file = new File("notfound.txt");
        Assert.assertFalse(file.exists());
        FileTools.compressGzipFile(file, new File("build/test.tar.gz"));
    }

    /**
     * @see FileTools#decompressGzipFile(File,File)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test(expected = FileNotFoundException.class)
    public void decompressGzipFile_shouldThrowFileNotFoundExceptionIfFileNotFound() throws Exception {
        File gzipFile = new File("notfound.tar.gz");
        Assert.assertFalse(gzipFile.exists());
        FileTools.decompressGzipFile(gzipFile, new File("build/target.bla"));
    }

    /**
     * @see FileTools#getDocumentFromFile(File)
     * @verifies build document correctly
     */
    @Test
    public void getDocumentFromFile_shouldBuildDocumentCorrectly() throws Exception {
        File file = new File("resources/test/data/sample_alto.xml");
        Assert.assertTrue(file.isFile());
        Document doc = FileTools.getDocumentFromFile(file);
        Assert.assertNotNull(doc);
        Assert.assertNotNull(doc.getRootElement());
        Assert.assertEquals("alto", doc.getRootElement().getName());
    }

    /**
     * @see FileTools#getDocumentFromString(String,String)
     * @verifies build document correctly
     */
    @Test
    public void getDocumentFromString_shouldBuildDocumentCorrectly() throws Exception {
        String xml = "<root><child>child1</child><child>child2</child></root>";
        Document doc = FileTools.getDocumentFromString(xml, null);
        Assert.assertNotNull(doc);
        Assert.assertNotNull(doc.getRootElement());
        Assert.assertEquals("root", doc.getRootElement().getName());
        Assert.assertNotNull(doc.getRootElement().getChildren("child"));
        Assert.assertEquals(2, doc.getRootElement().getChildren("child").size());
    }

    /**
     * @see FileTools#getStringFromElement(Object,String)
     * @verifies return XML string correctly for documents
     */
    @Test
    public void getStringFromElement_shouldReturnXMLStringCorrectlyForDocuments() throws Exception {
        Document doc = new Document();
        doc.setRootElement(new Element("root"));
        String xml = FileTools.getStringFromElement(doc, null);
        Assert.assertNotNull(xml);
        Assert.assertTrue(xml.contains("<root></root>"));
    }

    /**
     * @see FileTools#getStringFromElement(Object,String)
     * @verifies return XML string correctly for elements
     */
    @Test
    public void getStringFromElement_shouldReturnXMLStringCorrectlyForElements() throws Exception {
        String xml = FileTools.getStringFromElement(new Element("root"), null);
        Assert.assertNotNull(xml);
        Assert.assertTrue(xml.contains("<root></root>"));
    }

    /**
     * @see FileTools#getFileFromDocument(File,Document)
     * @verifies write file correctly
     */
    @Test
    public void getFileFromDocument_shouldWriteFileCorrectly() throws Exception {
        Assert.assertTrue(tempDir.mkdirs());
        Document doc = new Document();
        doc.setRootElement(new Element("root"));
        File file = new File(tempDir, "temp.xml");
        FileTools.getFileFromDocument(file.getAbsolutePath(), doc);
        Assert.assertTrue(file.isFile());
    }

    /**
     * @see FileTools#getFileFromString(String,File,String,boolean)
     * @verifies write file correctly
     */
    @Test
    public void getFileFromString_shouldWriteFileCorrectly() throws Exception {
        Assert.assertTrue(tempDir.mkdirs());
        File file = new File(tempDir, "temp.txt");
        String text = "Lorem ipsum dolor sit amet";
        FileTools.getFileFromString(text, file.getAbsolutePath(), null, false);
        Assert.assertTrue(file.isFile());
    }

    /**
     * @see FileTools#getFileFromString(String,File,String,boolean)
     * @verifies append to file correctly
     */
    @Test
    public void getFileFromString_shouldAppendToFileCorrectly() throws Exception {
        Assert.assertTrue(tempDir.mkdirs());
        File file = new File(tempDir, "temp.txt");
        String text = "XY";
        String text2 = "Z";
        FileTools.getFileFromString(text, file.getAbsolutePath(), null, false);
        FileTools.getFileFromString(text2, file.getAbsolutePath(), null, true);
        String concat = FileTools.getStringFromFile(file, null);
        Assert.assertEquals("XYZ", concat);
    }

    /**
     * @see FileTools#getCharset(InputStream)
     * @verifies detect charset correctly
     */
    @Test
    public void getCharset_shouldDetectCharsetCorrectly() throws Exception {
        File file = new File("resources/test/stopwords.txt");
        try (FileInputStream fis = new FileInputStream(file)) {
            Assert.assertEquals("UTF-8", FileTools.getCharset(fis));
        }
    }

}