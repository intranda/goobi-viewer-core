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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.AbstractTest;

class FileToolsTest extends AbstractTest {

    private File tempDir = new File("target/temp");

    @AfterEach
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
    void getStringFromFile_shouldReadTextFileCorrectly() throws Exception {
        File file = new File("src/test/resources/stopwords.txt");
        Assertions.assertTrue(file.isFile());
        String contents = FileTools.getStringFromFile(file, null);
        Assertions.assertTrue(StringUtils.isNotBlank(contents));
    }

    /**
     * @see FileTools#getStringFromFile(File,String)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test
    void getStringFromFile_shouldThrowFileNotFoundExceptionIfFileNotFound() throws Exception {
        File file = new File("notfound.txt");
        Assertions.assertFalse(file.exists());
        Assertions.assertThrows(FileNotFoundException.class, () -> FileTools.getStringFromFile(file, null));
    }

    /**
     * @see FileTools#getStringFromFilePath(String)
     * @verifies read text file correctly
     */
    @Test
    void getStringFromFilePath_shouldReadTextFileCorrectly() throws Exception {
        String contents = FileTools.getStringFromFilePath("src/test/resources/stopwords.txt");
        Assertions.assertTrue(StringUtils.isNotBlank(contents));
    }

    /**
     * @see FileTools#getStringFromFilePath(String)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test
    void getStringFromFilePath_shouldThrowFileNotFoundExceptionIfFileNotFound() throws Exception {
        File file = new File("notfound.txt");
        Assertions.assertFalse(file.exists());
        Assertions.assertThrows(FileNotFoundException.class, () -> FileTools.getStringFromFilePath(file.getPath()));
    }

    /**
     * @see FileTools#compressGzipFile(File,File)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test
    void compressGzipFile_shouldThrowFileNotFoundExceptionIfFileNotFound() throws Exception {
        File file = new File("notfound.txt");
        Assertions.assertFalse(file.exists());
        Assertions.assertThrows(FileNotFoundException.class, () -> FileTools.compressGzipFile(file, new File("target/test.tar.gz")));
    }

    /**
     * @see FileTools#decompressGzipFile(File,File)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test
    void decompressGzipFile_shouldThrowFileNotFoundExceptionIfFileNotFound() throws Exception {
        File gzipFile = new File("notfound.tar.gz");
        Assertions.assertFalse(gzipFile.exists());
        Assertions.assertThrows(FileNotFoundException.class, () -> FileTools.decompressGzipFile(gzipFile, new File("target/target.bla")));
    }

    /**
     * @see FileTools#getFileFromString(String,File,String,boolean)
     * @verifies write file correctly
     */
    @Test
    void getFileFromString_shouldWriteFileCorrectly() throws Exception {
        Assertions.assertTrue(tempDir.mkdirs());
        File file = new File(tempDir, "temp.txt");
        String text = "Lorem ipsum dolor sit amet";
        FileTools.getFileFromString(text, file.getAbsolutePath(), null, false);
        Assertions.assertTrue(file.isFile());
    }

    /**
     * @see FileTools#getFileFromString(String,File,String,boolean)
     * @verifies append to file correctly
     */
    @Test
    void getFileFromString_shouldAppendToFileCorrectly() throws Exception {
        Assertions.assertTrue(tempDir.mkdirs());
        File file = new File(tempDir, "temp.txt");
        String text = "XY";
        String text2 = "Z";
        FileTools.getFileFromString(text, file.getAbsolutePath(), null, false);
        FileTools.getFileFromString(text2, file.getAbsolutePath(), null, true);
        String concat = FileTools.getStringFromFile(file, null);
        Assertions.assertEquals("XYZ", concat);
    }

    /**
     * @see FileTools#getCharset(InputStream)
     * @verifies detect charset correctly
     */
    @Test
    void getCharset_shouldDetectCharsetCorrectly() throws Exception {
        File file = new File("src/test/resources/stopwords.txt");
        try (FileInputStream fis = new FileInputStream(file)) {
            Assertions.assertEquals("UTF-8", FileTools.getCharset(fis));
        }
    }

    /**
     * @see FileTools#getCharset(InputStream)
     * @verifies not close stream
     */
    @SuppressWarnings("resource")
    @Test
    void getCharset_shouldNotCloseStream() throws Exception {
        File file = new File("src/test/resources/stopwords.txt");
        FileInputStream fis = new FileInputStream(file);
        try {
            Assertions.assertEquals("UTF-8", FileTools.getCharset(fis));
            try {
                fis.available();
            } catch (IOException e) {
                Assertions.fail("Stream closed");
            }
        } finally {
            fis.close();
        }
    }

    @Test
    void testProbeContentType() throws FileNotFoundException, IOException {
        Path resourceFolder = Paths.get("src/test/resources/data/viewer/fulltext");

        //        Assertions.assertEquals("text/plain",
        //                FileTools.probeContentType(FileTools.getStringFromFilePath(resourceFolder.resolve("ascii.txt").toString())));
        //        Assertions.assertEquals("text/html",
        //                FileTools.probeContentType(FileTools.getStringFromFilePath(resourceFolder.resolve("html_ascii_crlf.txt").toString())));
        //        Assertions.assertEquals("text/html",
        //                FileTools.probeContentType(FileTools.getStringFromFilePath(resourceFolder.resolve("html_ascii.txt").toString())));
        //        Assertions.assertEquals("application/xml",
        //                FileTools.probeContentType(FileTools.getStringFromFilePath(resourceFolder.resolve("xml_utf8_crlf.txt").toString())));
        //        Assertions.assertEquals("text/plain",
        //                FileTools.probeContentType(FileTools.getStringFromFilePath(resourceFolder.resolve("IZT_Text_4-2018_Fairphone.txt").toString())));

        //        Assertions.assertEquals("text/plain", FileTools.probeContentType((PathConverter.toURI(resourceFolder.resolve("ascii.txt")))));
        Assertions.assertEquals("text/html", FileTools.probeContentType((PathConverter.toURI(resourceFolder.resolve("html_ascii_crlf.txt")))));
        //        Assertions.assertEquals("text/html", FileTools.probeContentType((PathConverter.toURI(resourceFolder.resolve("html_ascii.txt")))));
        //        Assertions.assertEquals("application/xml", FileTools.probeContentType((PathConverter.toURI(resourceFolder.resolve("xml_utf8_crlf.txt")))));
        //        Assertions.assertEquals("text/plain", FileTools.probeContentType((PathConverter.toURI(resourceFolder.resolve("IZT_Text_4-2018_Fairphone.txt")))));

        //        Assertions.assertEquals("text/plain", FileTools.probeContentType(URI.create("https://viewer.goobi.io/rest/content/fulltext/AC03343066/00000001.txt")));
        //        Assertions.assertEquals("application/xml", FileTools.probeContentType(URI.create("https://viewer.goobi.io/rest/content/alto/AC03343066/00000001.xml")));
        //
        //        Assertions.assertEquals("text/plain", FileTools.probeContentType(URI.create("http://localhost:8082/viewer/rest/content/document/fulltext/02008070428708/00000013.txt")));
        //        Assertions.assertEquals("application/xml", FileTools.probeContentType(URI.create("http://localhost:8082/viewer/rest/content/document/alto/AC03343066/00000012.xml")));

    }

    /**
     * @see FileTools#getBottomFolderFromPathString(String)
     * @verifies return folder name correctly
     */
    @Test
    void getBottomFolderFromPathString_shouldReturnFolderNameCorrectly() throws Exception {
        Assertions.assertEquals("PPN123", FileTools.getBottomFolderFromPathString("data/1/alto/PPN123/00000001.xml"));
    }

    /**
     * @see FileTools#getBottomFolderFromPathString(String)
     * @verifies return empty string if no folder in path
     */
    @Test
    void getBottomFolderFromPathString_shouldReturnEmptyStringIfNoFolderInPath() throws Exception {
        Assertions.assertEquals("", FileTools.getBottomFolderFromPathString("00000001.xml"));
    }

    /**
     * @see FileTools#getFilenameFromPathString(String)
     * @verifies return file name correctly
     */
    @Test
    void getFilenameFromPathString_shouldReturnFileNameCorrectly() throws Exception {
        Assertions.assertEquals("00000001.xml", FileTools.getFilenameFromPathString("data/1/alto/PPN123/00000001.xml"));
    }
}
