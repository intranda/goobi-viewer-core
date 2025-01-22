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
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.goobi.viewer.AbstractTest;

class FileToolsTest extends AbstractTest {

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
    void getStringFromFile_shouldThrowFileNotFoundExceptionIfFileNotFound() {
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
    void getStringFromFilePath_shouldThrowFileNotFoundExceptionIfFileNotFound() {
        File file = new File("notfound.txt");
        Assertions.assertFalse(file.exists());
        Assertions.assertThrows(FileNotFoundException.class, () -> FileTools.getStringFromFilePath(file.getPath()));
    }

    /**
     * @see FileTools#compressGzipFile(File,File)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test
    void compressGzipFile_shouldThrowFileNotFoundExceptionIfFileNotFound() {
        File file = new File("notfound.txt");
        Assertions.assertFalse(file.exists());
        Assertions.assertThrows(FileNotFoundException.class, () -> FileTools.compressGzipFile(file, new File("target/test.tar.gz")));
    }

    /**
     * @see FileTools#decompressGzipFile(File,File)
     * @verifies throw FileNotFoundException if file not found
     */
    @Test
    void decompressGzipFile_shouldThrowFileNotFoundExceptionIfFileNotFound() {
        File gzipFile = new File("notfound.tar.gz");
        Assertions.assertFalse(gzipFile.exists());
        Assertions.assertThrows(FileNotFoundException.class, () -> FileTools.decompressGzipFile(gzipFile, new File("target/target.bla")));
    }

    /**
     * @see FileTools#getFileFromString(String,File,String,boolean)
     * @verifies write file correctly
     */
    @Test
    void getFileFromString_shouldWriteFileCorrectly(@TempDir File tempDir) throws Exception {
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
    void getFileFromString_shouldAppendToFileCorrectly(@TempDir File tempDir) throws Exception {
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
     * @see FileTools#getBottomFolderFromPathString(String)
     * @verifies return folder name correctly
     */
    @Test
    void getBottomFolderFromPathString_shouldReturnFolderNameCorrectly() {
        Assertions.assertEquals("PPN123", FileTools.getBottomFolderFromPathString("data/1/alto/PPN123/00000001.xml"));
    }

    /**
     * @see FileTools#getBottomFolderFromPathString(String)
     * @verifies return empty string if no folder in path
     */
    @Test
    void getBottomFolderFromPathString_shouldReturnEmptyStringIfNoFolderInPath() {
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

    @Test
    void testIsYounger(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");

        Files.createFile(file1);
        Thread.sleep(100);
        Files.createFile(file2);

        Assertions.assertTrue(FileTools.isYoungerThan(file2, file1));
        Assertions.assertFalse(FileTools.isYoungerThan(file1, file2));
        Assertions.assertFalse(FileTools.isYoungerThan(file1, file1));
    }
}
