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
package io.goobi.viewer.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.AbstractTest;

public class FileToolsTest extends AbstractTest {

    private File tempDir = new File("target/temp");

    @After
    public void tearDown() throws Exception {
        if (tempDir.exists()) {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    /**
     * @see FileTools#getStringFromFile(File,String)
     * @verifies read text file correctly
     */
    @Test
    public void getStringFromFile_shouldReadTextFileCorrectly() throws Exception {
        File file = new File("src/test/resources/stopwords.txt");
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
        String contents = FileTools.getStringFromFilePath("src/test/resources/stopwords.txt");
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
        FileTools.compressGzipFile(file, new File("target/test.tar.gz"));
    }

    /**
     * @see FileTools#decompressGzipFile(File,File)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test(expected = FileNotFoundException.class)
    public void decompressGzipFile_shouldThrowFileNotFoundExceptionIfFileNotFound() throws Exception {
        File gzipFile = new File("notfound.tar.gz");
        Assert.assertFalse(gzipFile.exists());
        FileTools.decompressGzipFile(gzipFile, new File("target/target.bla"));
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
        File file = new File("src/test/resources/stopwords.txt");
        try (FileInputStream fis = new FileInputStream(file)) {
            Assert.assertEquals("UTF-8", FileTools.getCharset(fis));
        }
    }

    @Test
    public void testProbeContentType() throws FileNotFoundException, IOException {
        Path resourceFolder = Paths.get("src/test/resources/data/viewer/fulltext");

        Assert.assertEquals("text/plain",
                FileTools.probeContentType(FileTools.getStringFromFilePath(resourceFolder.resolve("ascii.txt").toString())));
        Assert.assertEquals("text/html",
                FileTools.probeContentType(FileTools.getStringFromFilePath(resourceFolder.resolve("html_ascii_crlf.txt").toString())));
        Assert.assertEquals("text/html",
                FileTools.probeContentType(FileTools.getStringFromFilePath(resourceFolder.resolve("html_ascii.txt").toString())));
        Assert.assertEquals("application/xml",
                FileTools.probeContentType(FileTools.getStringFromFilePath(resourceFolder.resolve("xml_utf8_crlf.txt").toString())));
        Assert.assertEquals("text/plain",
                FileTools.probeContentType(FileTools.getStringFromFilePath(resourceFolder.resolve("IZT_Text_4-2018_Fairphone.txt").toString())));

        Assert.assertEquals("text/plain", FileTools.probeContentType((PathConverter.toURI(resourceFolder.resolve("ascii.txt")))));
        Assert.assertEquals("text/html", FileTools.probeContentType((PathConverter.toURI(resourceFolder.resolve("html_ascii_crlf.txt")))));
        Assert.assertEquals("text/html", FileTools.probeContentType((PathConverter.toURI(resourceFolder.resolve("html_ascii.txt")))));
        Assert.assertEquals("application/xml", FileTools.probeContentType((PathConverter.toURI(resourceFolder.resolve("xml_utf8_crlf.txt")))));
        Assert.assertEquals("text/plain", FileTools.probeContentType((PathConverter.toURI(resourceFolder.resolve("IZT_Text_4-2018_Fairphone.txt")))));

        //        Assert.assertEquals("text/plain", FileTools.probeContentType(URI.create("https://viewer.goobi.io/rest/content/fulltext/AC03343066/00000001.txt")));
        //        Assert.assertEquals("application/xml", FileTools.probeContentType(URI.create("https://viewer.goobi.io/rest/content/alto/AC03343066/00000001.xml")));
        //        
        //        Assert.assertEquals("text/plain", FileTools.probeContentType(URI.create("http://localhost:8082/viewer/rest/content/document/fulltext/02008070428708/00000013.txt")));
        //        Assert.assertEquals("application/xml", FileTools.probeContentType(URI.create("http://localhost:8082/viewer/rest/content/document/alto/AC03343066/00000012.xml")));

    }

    /**
     * @see FileTools#getBottomFolderFromPathString(String)
     * @verifies return folder name correctly
     */
    @Test
    public void getBottomFolderFromPathString_shouldReturnFolderNameCorrectly() throws Exception {
        Assert.assertEquals("PPN123", FileTools.getBottomFolderFromPathString("data/1/alto/PPN123/00000001.xml"));
    }

    /**
     * @see FileTools#getBottomFolderFromPathString(String)
     * @verifies return empty string if no folder in path
     */
    @Test
    public void getBottomFolderFromPathString_shouldReturnEmptyStringIfNoFolderInPath() throws Exception {
        Assert.assertEquals("", FileTools.getBottomFolderFromPathString("00000001.xml"));
    }

    /**
     * @see FileTools#getFilenameFromPathString(String)
     * @verifies return file name correctly
     */
    @Test
    public void getFilenameFromPathString_shouldReturnFileNameCorrectly() throws Exception {
        Assert.assertEquals("00000001.xml", FileTools.getFilenameFromPathString("data/1/alto/PPN123/00000001.xml"));
    }
}
