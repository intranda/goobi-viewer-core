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