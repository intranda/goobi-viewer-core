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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * File I/O utilities.
 */
public class FileTools {

    private static final Logger logger = LoggerFactory.getLogger(FileTools.class);

    public static FilenameFilter filenameFilterXML = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return "xml".equals(FilenameUtils.getExtension(name.toLowerCase()));
        }
    };

    /**
     *
     * @param filePath
     * @return
     * @throws IOException in case of errors
     * @throws FileNotFoundException if file not found
     * @should read text file correctly
     * @should throw FileNotFoundException if file not found
     */
    public static String getStringFromFilePath(String filePath) throws FileNotFoundException, IOException {
        return getStringFromFile(new File(filePath), null);
    }

    /**
     * Read a text file and return content as String
     *
     * @param file
     * @param encoding The character encoding to use. If null, a standard utf-8 encoding will be used
     * @return
     * @throws IOException in case of errors
     * @throws FileNotFoundException if file not found
     * @should read text file correctly
     * @should throw FileNotFoundException if file not found
     */
    public static String getStringFromFile(File file, String encoding) throws FileNotFoundException, IOException {
        if (file == null) {
            throw new IllegalArgumentException("file may not be null");
        }

        if (encoding == null) {
            try (FileInputStream fis = new FileInputStream(file)) {
                encoding = getCharset(fis);
                logger.trace("{} encoding detected: {}", file.getName(), encoding);
            }
            if (encoding == null) {
                encoding = Helper.DEFAULT_ENCODING;
            }
        }

        StringBuilder text = new StringBuilder();
        String ls = System.getProperty("line.separator");
        try (FileInputStream fis = new FileInputStream(file); Scanner scanner = new Scanner(fis, encoding)) {
            while (scanner.hasNextLine()) {
                text.append(scanner.nextLine()).append(ls);
            }
        }

        return text.toString().trim();
    }

    /**
     * Uses ICU4J to determine the charset of the given InputStream.
     * 
     * @param input
     * @return Detected charset name; null if not detected.
     * @throws IOException
     * @should detect charset correctly
     */
    public static String getCharset(InputStream input) throws IOException {
        CharsetDetector cd = new CharsetDetector();
        try (BufferedInputStream bis = new BufferedInputStream(input)) {
            cd.setText(bis);
            CharsetMatch cm = cd.detect();
            if (cm != null) {
                return cm.getName();
            }
        }

        return null;
    }

    /**
     * Reads a String from a byte array
     *
     * @param bytes
     * @param encoding
     * @return
     */
    public static String getStringFromByteArray(byte[] bytes, String encoding) {
        String result = "";

        if (encoding == null) {
            encoding = Helper.DEFAULT_ENCODING;
        }

        Scanner scanner = null;
        StringBuilder text = new StringBuilder();
        String NL = System.getProperty("line.separator");
        try {
            scanner = new Scanner(new ByteArrayInputStream(bytes), encoding);
            while (scanner.hasNextLine()) {
                text.append(scanner.nextLine()).append(NL);
            }
        } finally {
            scanner.close();
        }
        result = text.toString();
        return result.trim();
    }

    /**
     * Simply write a String into a text file.
     *
     * @param string The String to write
     * @param filePath The file path to write to (will be created if it doesn't exist)
     * @param encoding The character encoding to use. If null, a standard utf-8 encoding will be used
     * @param append Whether to append the text to an existing file (true), or to overwrite it (false)
     * @return
     * @throws IOException
     * @should write file correctly
     * @should append to file correctly
     */
    public static File getFileFromString(String string, String filePath, String encoding, boolean append) throws IOException {
        if (string == null) {
            throw new IllegalArgumentException("string may not be null");
        }
        if (encoding == null) {
            encoding = Helper.DEFAULT_ENCODING;
        }

        File file = new File(filePath);
        try (FileWriterWithEncoding writer = new FileWriterWithEncoding(file, encoding, append)) {
            writer.write(string);
        }

        return file;
    }

    /**
     *
     * @param gzipFile
     * @param newFile
     * @throws FileNotFoundException
     * @throws IOException if file not found
     * @should throw FileNotFoundException if file not found
     */
    public static void decompressGzipFile(File gzipFile, File newFile) throws FileNotFoundException, IOException {
        try (FileInputStream fis = new FileInputStream(gzipFile); GZIPInputStream gis = new GZIPInputStream(fis);
                FileOutputStream fos = new FileOutputStream(newFile)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
    }

    /**
     *
     * @param file
     * @param gzipFile
     * @throws FileNotFoundException
     * @throws IOException
     * @should throw FileNotFoundException if file not found
     */
    public static void compressGzipFile(File file, File gzipFile) throws FileNotFoundException, IOException {
        try (FileInputStream fis = new FileInputStream(file); FileOutputStream fos = new FileOutputStream(gzipFile);
                GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }
        }
    }

    /**
     *
     * @param files
     * @param zipFile
     * @param level
     * @throws FileNotFoundException
     * @throws IOException
     * @should throw FileNotFoundException if file not found
     */
    public static void compressZipFile(List<File> files, File zipFile, Integer level) throws FileNotFoundException, IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files may not be empty or null");
        }
        if (zipFile == null) {
            throw new IllegalArgumentException("zipFile may not be empty or null");
        }

        try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
            if (level != null) {
                zos.setLevel(level);
            }
            for (File file : files) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
            }
        }
    }

    /**
     *
     * @param files
     * @param zipFile
     * @param level
     * @throws FileNotFoundException
     * @throws IOException
     * @should throw FileNotFoundException if file not found
     */
    public static void compressZipFile(Map<Path, String> contentMap, File zipFile, Integer level) throws FileNotFoundException, IOException {
        if (contentMap == null || contentMap.isEmpty()) {
            throw new IllegalArgumentException("texts may not be empty or null");
        }
        if (zipFile == null) {
            throw new IllegalArgumentException("zipFile may not be empty or null");
        }

        try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
            if (level != null) {
                zos.setLevel(level);
            }
            for (Path path : contentMap.keySet()) {
                try (InputStream in = IOUtils.toInputStream(contentMap.get(path))) {
                    zos.putNextEntry(new ZipEntry(path.getFileName().toString()));
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
            }
        }
    }

    /**
     *
     * @param path
     * @param create
     * @return
     * @throws IOException
     */
    public static boolean checkPathExistance(Path path, boolean create) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path may not be null");
        }

        if (Files.exists(path)) {
            return true;
        }
        if (create) {
            Files.createDirectory(path);
            logger.info("Created folder: {}", path.toAbsolutePath().toString());
            return true;
        }
        logger.error("Folder not found: {}", path.toAbsolutePath().toString());
        return false;
    }

    public static void copyStream(OutputStream output, InputStream input) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = input.read(buf)) > 0) {
            output.write(buf, 0, len);
        }
    }

}
