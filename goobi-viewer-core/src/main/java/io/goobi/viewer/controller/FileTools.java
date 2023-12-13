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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import de.unigoettingen.sub.commons.util.PathConverter;

/**
 * File I/O utilities.
 */
public final class FileTools {

    private static final Logger logger = LogManager.getLogger(FileTools.class);

    /** Private constructor. */
    private FileTools() {
    }

    public static final DirectoryStream.Filter<Path> IMAGE_NAME_FILTER =
            (Path path) -> path.getFileName().toString().matches("(?i)[^.]+\\.(jpe?g|tiff?|png|jp2)");

    public static final DirectoryStream.Filter<Path> PDF_NAME_FILTER = (Path path) -> path.getFileName().toString().matches("(?i)[^.]+\\.(pdf)");

    /**
     * <p>
     * getStringFromFilePath.
     * </p>
     *
     * @param filePath a {@link java.lang.String} object.
     * @should read text file correctly
     * @should throw FileNotFoundException if file not found
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public static String getStringFromFilePath(String filePath) throws IOException {
        return getStringFromFile(new File(filePath), null);
    }

    /**
     * Read a text file and return content as String
     *
     * @param file a {@link java.io.File} object.
     * @param encoding The character encoding to use. If null, a standard utf-8 encoding will be used
     * @should read text file correctly
     * @should throw FileNotFoundException if file not found
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public static String getStringFromFile(File file, String encoding) throws IOException {
        return getStringFromFile(file, encoding, null);
    }

    /**
     * Read a text file and return content as String
     *
     * @param file a {@link java.io.File} object.
     * @param encoding The character encoding to use. If null, a standard utf-8 encoding will be used
     * @param convertToEncoding Optional target encoding for conversion
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public static String getStringFromFile(File file, final String encoding, String convertToEncoding) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file may not be null");
        }

        String useEncoding = encoding;
        if (useEncoding == null) {
            try (FileInputStream fis = new FileInputStream(file)) {
                useEncoding = getCharset(fis);
                logger.trace("'{}' encoding detected: {}", file.getName(), useEncoding);
            }
            if (useEncoding == null) {
                useEncoding = StringTools.DEFAULT_ENCODING;
            }
        }

        StringBuilder text = new StringBuilder();
        String ls = System.getProperty("line.separator");
        try (FileInputStream fis = new FileInputStream(file); Scanner scanner = new Scanner(fis, useEncoding)) {
            while (scanner.hasNextLine()) {
                text.append(scanner.nextLine()).append(ls);
            }
        }

        String ret = text.toString();
        // Convert to target encoding
        if (StringUtils.isNotEmpty(convertToEncoding) && !convertToEncoding.equals(useEncoding)) {
            ret = StringTools.convertStringEncoding(ret, useEncoding, convertToEncoding);
        }

        return ret.trim();
    }

    /**
     * Uses ICU4J to determine the charset of the given InputStream. Clients are responsible for closing the input stream. Do not re-use this stream
     * for any other operations.
     *
     * @param input a {@link java.io.InputStream} object.
     * @return Detected charset name; null if not detected.
     * @throws java.io.IOException if any.
     * @should detect charset correctly
     * @should not close stream
     */
    public static String getCharset(InputStream input) throws IOException {
        CharsetDetector cd = new CharsetDetector();
        BufferedInputStream bis = new BufferedInputStream(input);
        cd.setText(bis);
        CharsetMatch cm = cd.detect();
        if (cm != null) {
            return cm.getName();
        }

        return null;
    }

    /**
     * Reads a String from a byte array
     *
     * @param bytes an array of {@link byte} objects.
     * @param encoding a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getStringFromByteArray(byte[] bytes, final String encoding) {
        String result = "";
        StringBuilder text = new StringBuilder();
        String nl = System.getProperty("line.separator");
        try (Scanner scanner = new Scanner(new ByteArrayInputStream(bytes), encoding == null ? StringTools.DEFAULT_ENCODING : encoding)) {
            while (scanner.hasNextLine()) {
                text.append(scanner.nextLine()).append(nl);
            }
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
     * @should write file correctly
     * @should append to file correctly
     * @return a {@link java.io.File} object.
     * @throws java.io.IOException if any.
     */
    public static File getFileFromString(String string, String filePath, final String encoding, boolean append) throws IOException {
        if (string == null) {
            throw new IllegalArgumentException("string may not be null");
        }

        File file = new File(filePath);
        String useEncoding = encoding == null ? StringTools.DEFAULT_ENCODING : encoding;
        try (FileWriterWithEncoding writer = FileWriterWithEncoding.builder()
                .setFile(file)
                .setCharset(useEncoding)
                .setCharsetEncoder(Charset.forName(useEncoding).newEncoder())
                .setAppend(append)
                .get()) {
            writer.write(string);
        }

        return file;
    }

    /**
     * <p>
     * decompressGzipFile.
     * </p>
     *
     * @param gzipFile a {@link java.io.File} object.
     * @param newFile a {@link java.io.File} object.
     * @should throw FileNotFoundException if file not found
     * @throws java.io.IOException if any.
     */
    public static void decompressGzipFile(File gzipFile, File newFile) throws IOException {
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
     * <p>
     * compressGzipFile.
     * </p>
     *
     * @param file a {@link java.io.File} object.
     * @param gzipFile a {@link java.io.File} object.
     * @should throw FileNotFoundException if file not found
     * @throws java.io.IOException if any.
     */
    public static void compressGzipFile(File file, File gzipFile) throws IOException {
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
     * <p>
     * compressZipFile.
     * </p>
     *
     * @param files Source files
     * @param zipFile Target file
     * @param level Compression level 0-9
     * @should throw FileNotFoundException if file not found
     * @throws java.io.IOException if any.
     */
    public static void compressZipFile(List<File> files, File zipFile, Integer level) throws IOException {
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
     * <p>
     * compressZipFile.
     * </p>
     *
     * @param zipFile a {@link java.io.File} object.
     * @param level a {@link java.lang.Integer} object.
     * @should throw FileNotFoundException if file not found
     * @param contentMap a {@link java.util.Map} object.
     * @throws java.io.IOException if any.
     */
    public static void compressZipFile(Map<Path, String> contentMap, File zipFile, Integer level) throws IOException {
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
            for (Entry<Path, String> entry : contentMap.entrySet()) {
                try (InputStream in = IOUtils.toInputStream(entry.getValue(), StandardCharsets.UTF_8.name())) {
                    zos.putNextEntry(new ZipEntry(entry.getKey().toString()));
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
     * <p>
     * checkPathExistance.
     * </p>
     *
     * @param path a {@link java.nio.file.Path} object.
     * @param create a boolean.
     * @return a boolean.
     * @throws java.io.IOException if any.
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
            logger.info("Created folder: {}", path.toAbsolutePath());
            return true;
        }
        logger.error("Folder not found: {}", path.toAbsolutePath());
        return false;
    }

    /**
     * <p>
     * copyStream.
     * </p>
     *
     * @param output a {@link java.io.OutputStream} object.
     * @param input a {@link java.io.InputStream} object.
     * @throws java.io.IOException if any.
     */
    public static void copyStream(OutputStream output, InputStream input) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = input.read(buf)) > 0) {
            output.write(buf, 0, len);
        }
    }

    /**
     * <p>
     * isFolderEmpty.
     * </p>
     *
     * @param folder a {@link java.nio.file.Path} object.
     * @return true if folder empty; false otherwise
     * @throws java.io.IOException if any.
     */
    public static boolean isFolderEmpty(final Path folder) throws IOException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
            return !ds.iterator().hasNext();
        }
    }

    /**
     * <p>
     * adaptPathForWindows.
     * </p>
     *
     * @param path Absolute path to adapt
     * @return Windows-compatible path on Windows; unchanged path elsewhere
     */
    public static String adaptPathForWindows(final String path) {
        String os = System.getProperty("os.name").toLowerCase();

        String ret = path;
        if (os.indexOf("win") >= 0 && ret.startsWith("/opt/")) {
            ret = "C:" + ret;
        } else if (os.indexOf("win") >= 0 && ret.startsWith("file:///C:/opt/")) {
            // In case Paths.get() automatically adds "C:" to Unix paths on Windows machines, remove the "C:"
            ret = ret.replace("/C:", "");
        }
        return ret;
    }

    /**
     * Guess the content type (mimeType) of the resource found at the given uri. Content type if primarily guessed from the file extension of the last
     * url path part. If that type is 'text/plain' further analysis is done using the actual content to determine if the actual type is html or xml If
     * the type could not be determined from the file extension, the url response header is probed to return its 'Content-type'
     *
     * @param uri uri of the resource. May be a file uri, a relative uri (then assumed to be a relative file path) or a http(s) uri
     * @return The most likely mimeType of the resource found at the given uri
     * @throws IOException
     */
    public static String probeContentType(URI uri) throws IOException {
        String type = URLConnection.guessContentTypeFromName(uri.toString());
        if (StringConstants.MIMETYPE_TEXT_PLAIN.equals(type)) {
            if (!uri.isAbsolute() || uri.getScheme().equals("file")) {
                Path path = PathConverter.getPath(uri);
                try (InputStream in = Files.newInputStream(path)) {
                    type = URLConnection.guessContentTypeFromStream(in);
                    if (type == null) {
                        String content = new String(in.readAllBytes());
                        type = probeContentType(content);
                    }
                }
            } else if (uri.isAbsolute() && uri.getScheme().matches("https?")) {
                HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
                try {
                    con.connect();
                    try (InputStream in = con.getInputStream()) {
                        type = URLConnection.guessContentTypeFromStream(in);
                        if (type == null) {
                            type = StringConstants.MIMETYPE_TEXT_PLAIN;
                        }
                    }
                } finally {
                    con.disconnect();
                }
            }
        } else if (StringUtils.isBlank(type) && uri.isAbsolute() && uri.getScheme().matches("https?")) {
            HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
            type = con.getContentType();
            if (type != null && type.contains(";")) {
                type = type.substring(0, type.indexOf(";"));
            }
        }
        return type;
    }

    /**
     * Guess the content type of the given text, using {@link URLConnection#guessContentTypeFromName(String)} If no content type could be determined,
     * 'text/plain' is assumed
     *
     * @param content
     * @return Content mime type
     */
    public static String probeContentType(String content) {
        try (InputStream in = IOUtils.toInputStream(content, StringTools.getCharset(content))) {
            String type = URLConnection.guessContentTypeFromStream(in);
            if (type == null) {
                type = StringConstants.MIMETYPE_TEXT_PLAIN;
            }
            return type;
        } catch (IOException e) {
            logger.error("Error reading text to stream", e);
            return null;
        }
    }

    /**
     * @param file
     * @return Charset of the given file
     * @throws IOException
     */
    public static String getCharset(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return getCharset(in);
        }
    }

    /**
     *
     * Parses the given String as {@link java.nio.file.Path Path} and returns the lowest folder name as String. Returns an empty String if the given
     * path is empty or null
     *
     * @param pathString
     * @return The folder name, or an empty String if it could not be determined
     * @should return folder name correctly
     * @should return empty string if no folder in path
     */
    public static String getBottomFolderFromPathString(String pathString) {
        if (StringUtils.isBlank(pathString)) {
            return "";
        }

        Path path = Paths.get(pathString);
        return path.getParent() != null ? path.getParent().getFileName().toString() : "";
    }

    /**
     *
     * Parses the given String as {@link java.nio.file.Path Path} and returns the last path element (the filename) as String. Returns an empty String
     * if the given path is empty or null
     *
     * @param pathString
     * @return The filename, or an empty String if it could not be determined
     * @throws FileNotFoundException
     * @should return file name correctly
     */
    public static String getFilenameFromPathString(String pathString) throws FileNotFoundException {
        if (StringUtils.isBlank(pathString)) {
            return "";
        }

        Path path = getPathFromUrlString(pathString);
        if (path == null) {
            throw new FileNotFoundException(pathString);
        }
        return path.getFileName().toString();
    }

    /**
     * Creates a Path from the given URL in a way that word on Windows machines.
     *
     * @param urlString Relative or absolute path or URL, with or without protocol. If a URL parameter is in itself a complete URL, it must be escaped
     *            first!
     * @return Constructed Path
     */
    public static Path getPathFromUrlString(final String urlString) {
        if (StringUtils.isEmpty(urlString)) {
            return null;
        }

        Path path = null;
        // URL with protocol will cause an exception in Windows if a Path is created directly
        String urlStringLocal = urlString;
        if (urlStringLocal.contains(":")) {
            try {
                // logger.trace("url string: {}", urlString); //NOSONAR Sometimes used for debugging
                URL url = new URL(urlStringLocal);
                URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
                        url.getQuery(), url.getRef());
                if (urlStringLocal.endsWith("/") && Paths.get(uri.getPath()).getFileName().toString().contains(".")) {
                    urlStringLocal = urlStringLocal.substring(0, urlStringLocal.length() - 1);
                }
                path = Paths.get(uri.getPath());
            } catch (URISyntaxException | MalformedURLException e) {
                logger.error(e.getMessage(), e);
            }
        }
        // URL without protocol
        if (path == null) {
            path = Paths.get(urlStringLocal);
        }

        return path;
    }

    public static List<Path> listFiles(Path folder, DirectoryStream.Filter<Path> filter) {
        List<Path> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder, filter)) {
            for (Path path : directoryStream) {
                if (!path.getFileName().toString().startsWith(".")) {
                    fileNames.add(path);
                }
            }
        } catch (IOException ex) {
            //
        }
        Collections.sort(fileNames);
        return fileNames;
    }

    /**
     * Return a path which equals the given path but using the given extension in place of the original one
     * 
     * @param path any file path
     * @param extension the extension, without leading '.'
     * @return Given path with replaced file extension
     */
    public static Path replaceExtension(Path path, String extension) {
        String filename = path.getFileName().toString();
        String basename = FilenameUtils.getBaseName(filename);
        Path relativeFile = Paths.get(basename + "." + extension);
        if (path.getParent() != null) {
            return path.getParent().resolve(relativeFile);
        }
        return relativeFile;
    }
}
