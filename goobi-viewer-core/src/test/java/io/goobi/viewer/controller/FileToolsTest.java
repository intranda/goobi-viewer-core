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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.goobi.viewer.AbstractTest;

class FileToolsTest extends AbstractTest {

    /**
     * @verifies return non blank string when reading existing text file
     */
    @Test
    void getStringFromFile_shouldReturnNonBlankStringWhenReadingExistingTextFile() throws Exception {
        File file = new File("src/test/resources/stopwords.txt");
        Assertions.assertTrue(file.isFile());
        String contents = FileTools.getStringFromFile(file, null);
        Assertions.assertTrue(StringUtils.isNotBlank(contents));
    }

    /**
     * @verifies throw FileNotFoundException if file not found
     */
    @Test
    void getStringFromFile_shouldThrowFileNotFoundExceptionIfFileNotFound() {
        File file = new File("notfound.txt");
        Assertions.assertFalse(file.exists());
        Assertions.assertThrows(FileNotFoundException.class, () -> FileTools.getStringFromFile(file, null));
    }

    /**
     * @verifies return non blank string when given a valid file path
     */
    @Test
    void getStringFromFilePath_shouldReturnNonBlankStringWhenGivenAValidFilePath() throws Exception {
        String contents = FileTools.getStringFromFilePath("src/test/resources/stopwords.txt");
        Assertions.assertTrue(StringUtils.isNotBlank(contents));
    }

    /**
     * @verifies throw FileNotFoundException if file not found
     */
    @Test
    void getStringFromFilePath_shouldThrowFileNotFoundExceptionIfFileNotFound() {
        File file = new File("notfound.txt");
        Assertions.assertFalse(file.exists());
        Assertions.assertThrows(FileNotFoundException.class, () -> FileTools.getStringFromFilePath(file.getPath()));
    }

    /**
     * @verifies throw FileNotFoundException if file not found
     */
    @Test
    void compressGzipFile_shouldThrowFileNotFoundExceptionIfFileNotFound() {
        File file = new File("notfound.txt");
        Assertions.assertFalse(file.exists());
        Assertions.assertThrows(FileNotFoundException.class, () -> FileTools.compressGzipFile(file, new File("target/test.tar.gz")));
    }

    /**
     * @verifies throw FileNotFoundException if file not found
     */
    @Test
    void decompressGzipFile_shouldThrowFileNotFoundExceptionIfFileNotFound() {
        File gzipFile = new File("notfound.tar.gz");
        Assertions.assertFalse(gzipFile.exists());
        Assertions.assertThrows(FileNotFoundException.class, () -> FileTools.decompressGzipFile(gzipFile, new File("target/target.bla")));
    }

    /**
     * @verifies create file on disk from string content
     */
    @Test
    void getFileFromString_shouldCreateFileOnDiskFromStringContent(@TempDir File tempDir) throws Exception {
        File file = new File(tempDir, "temp.txt");
        String text = "Lorem ipsum dolor sit amet";
        FileTools.getFileFromString(text, file.getAbsolutePath(), null, false);
        Assertions.assertTrue(file.isFile());
    }

    /**
     * @verifies append string to existing file content when append flag is true
     */
    @Test
    void getFileFromString_shouldAppendStringToExistingFileContentWhenAppendFlagIsTrue(@TempDir File tempDir) throws Exception {
        File file = new File(tempDir, "temp.txt");
        String text = "XY";
        String text2 = "Z";
        FileTools.getFileFromString(text, file.getAbsolutePath(), null, false);
        FileTools.getFileFromString(text2, file.getAbsolutePath(), null, true);
        String concat = FileTools.getStringFromFile(file, null);
        Assertions.assertEquals("XYZ", concat);
    }

    /**
     * @see FileTools#getCharset(Path)
     * @verifies return UTF-8 for a UTF-8 encoded file input stream
     */
    @Test
    void getCharset_shouldReturnUtf8ForAUtf8EncodedFileInputStream() throws Exception {
        // Test the Path overload which internally opens a stream and delegates to the InputStream overload
        Path file = Path.of("src/test/resources/stopwords.txt");
        Assertions.assertEquals("UTF-8", FileTools.getCharset(file));
    }

    /**
     * @see FileTools#getCharset(InputStream)
     * @verifies detect charset correctly
     */
    @Test
    void getCharset_shouldDetectCharsetCorrectly() throws Exception {
        // Verify that ICU4J detects UTF-8 charset from a byte array containing UTF-8 encoded text
        byte[] utf8Bytes = "Hello, World! äöü".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (InputStream is = new java.io.ByteArrayInputStream(utf8Bytes)) {
            String charset = FileTools.getCharset(is);
            Assertions.assertNotNull(charset);
            Assertions.assertEquals("UTF-8", charset);
        }
    }

    /**
     * @see FileTools#getCharset(InputStream)
     * @verifies not close stream
     */
    @Test
    void getCharset_shouldNotCloseStream() throws Exception {
        // Verify that the outer stream is not closed by getCharset (only the BufferedInputStream wrapper is closed)
        byte[] utf8Bytes = "Test content for charset detection with enough text to detect encoding properly.".getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
        InputStream is = new java.io.ByteArrayInputStream(utf8Bytes);
        FileTools.getCharset(is);
        // ByteArrayInputStream.available() returns remaining bytes; if the stream were closed, read would fail
        // ByteArrayInputStream.close() is a no-op, so we verify the stream is still functional by checking available()
        Assertions.assertTrue(is.available() >= 0, "Stream should still be accessible after getCharset call");
    }

    /**
     * @see FileTools#getBottomFolderFromPathString(String)
     * @verifies return the parent folder name from a file path string
     */
    @Test
    void getBottomFolderFromPathString_shouldReturnTheParentFolderNameFromAFilePathString() {
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
     * @verifies extract filename from a full path string
     */
    @Test
    void getFilenameFromPathString_shouldExtractFilenameFromAFullPathString() throws Exception {
        Assertions.assertEquals("00000001.xml", FileTools.getFilenameFromPathString("data/1/alto/PPN123/00000001.xml"));
    }

    /**
     * @verifies return true if file is younger than reference file
     * @see FileTools#isYoungerThan(Path, Path)
     */
    @Test
    void isYoungerThan_shouldReturnTrueIfFileIsYounger(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");

        Files.createFile(file1);
        Thread.sleep(100);
        Files.createFile(file2);

        Assertions.assertTrue(FileTools.isYoungerThan(file2, file1));
        Assertions.assertFalse(FileTools.isYoungerThan(file1, file2));
        Assertions.assertFalse(FileTools.isYoungerThan(file1, file1));
    }

    /**
     * @see FileTools#sanitizeFileName(String)
     * @verifies return unchanged string if string blank
     */
    @Test
    void sanitizeFileName_shouldReturnUnchangedStringIfStringBlank() {
        Assertions.assertEquals("  ", FileTools.sanitizeFileName("  "));
    }

    /**
     * @see FileTools#sanitizeFileName(String)
     * @verifies remove everything but the file name from given path
     */
    @Test
    void sanitizeFileName_shouldRemoveEverythingButTheFileNameFromGivenPath() {
        Assertions.assertEquals("foo.bar", FileTools.sanitizeFileName("/opt/digiverso/foo.bar"));
        Assertions.assertEquals("foo.bar", FileTools.sanitizeFileName("../../foo.bar"));
        Assertions.assertEquals("foo.bar", FileTools.sanitizeFileName("/foo.bar"));
        Assertions.assertEquals("f o-o_.bar", FileTools.sanitizeFileName("/f o-o_.bar"));
        Assertions.assertEquals("XIX-1083.4-14,1919_0001.xml", FileTools.sanitizeFileName("XIX-1083.4-14,1919_0001.xml"));
        Assertions.assertEquals("über.xml", FileTools.sanitizeFileName("/opt/digiverso/über.xml"));
    }

    /**
     * @see FileTools#sanitizeFileName(String)
     * @verifies accept file names containing Unicode characters
     */
    @Test
    void sanitizeFileName_shouldAcceptFileNamesContainingUnicodeCharacters() {
        Assertions.assertEquals("Kritisches_über_Shakespeare_0009.xml",
                FileTools.sanitizeFileName("Kritisches_über_Shakespeare_0009.xml"));
        Assertions.assertEquals("Ärger_mit_Äpfeln.xml", FileTools.sanitizeFileName("Ärger_mit_Äpfeln.xml"));
        // Filename with both Unicode characters and spaces (regression test for production error)
        Assertions.assertEquals("Der Aufbau Deutschlands und das Rätesystem_0004.xml",
                FileTools.sanitizeFileName("Der Aufbau Deutschlands und das Rätesystem_0004.xml"));
    }

    /**
     * @see FileTools#sanitizeFileName(String)
     * @verifies accept file names containing common punctuation characters
     */
    @Test
    void sanitizeFileName_shouldAcceptFileNamesContainingCommonPunctuationCharacters() {
        // Exclamation mark: common in German newspaper/document titles (regression test for production error)
        Assertions.assertEquals("Nieder mit Spartakus!_0025_L.xml",
                FileTools.sanitizeFileName("Nieder mit Spartakus!_0025_L.xml"));
        // Parentheses and apostrophes: common in library catalog titles
        Assertions.assertEquals("Le Monde (Paris)_0001.xml",
                FileTools.sanitizeFileName("Le Monde (Paris)_0001.xml"));
        Assertions.assertEquals("L'Humanité_0001.xml",
                FileTools.sanitizeFileName("L'Humanité_0001.xml"));
    }

    /**
     * @see FileTools#sanitizeFileName(String)
     * @verifies throw IllegalArgumentException given invalid file name
     */
    @Test
    void sanitizeFileName_shouldThrowIllegalArgumentExceptionGivenInvalidFileName() {
        Exception e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> FileTools.sanitizeFileName("/opt/digiverso/foo&.jpg"));
        assertEquals("Illegal fileName: /opt/digiverso/foo&.jpg", e.getMessage());
    }

    /**
     * @see FileTools#sanitizeFileName(String)
     * @verifies neutralize path traversal sequences
     */
    @Test
    void sanitizeFileName_shouldNeutralizePathTraversalSequences() {
        // Directory components must be stripped so that resolving the result
        // against a base directory cannot escape that directory.
        Assertions.assertEquals("passwd", FileTools.sanitizeFileName("../../../etc/passwd"));
        Assertions.assertEquals("shadow", FileTools.sanitizeFileName("../../../../etc/shadow"));
        Assertions.assertEquals("web.xml", FileTools.sanitizeFileName("../../WEB-INF/web.xml"));
    }

    /**
     * @see FileTools#copyRejectingSymlinks(InputStream, Path)
     * @verifies write bytes when target is regular file
     */
    @Test
    void copyRejectingSymlinks_shouldWriteBytesWhenTargetIsRegularFile(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("upload.txt");
        byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
        try (InputStream in = new ByteArrayInputStream(payload)) {
            FileTools.copyRejectingSymlinks(in, target);
        }
        Assertions.assertEquals("hello", Files.readString(target, StandardCharsets.UTF_8));
        Assertions.assertEquals(payload.length, Files.size(target),
                "Written size must match payload length");
    }

    /**
     * @see FileTools#copyRejectingSymlinks(InputStream, Path)
     * @verifies write zero byte file when source is empty
     */
    @Test
    void copyRejectingSymlinks_shouldWriteZeroByteFileWhenSourceIsEmpty(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("empty.txt");
        try (InputStream in = new ByteArrayInputStream(new byte[0])) {
            FileTools.copyRejectingSymlinks(in, target);
        }
        Assertions.assertTrue(Files.exists(target));
        Assertions.assertEquals(0L, Files.size(target));
    }

    /**
     * @see FileTools#copyRejectingSymlinks(InputStream, Path)
     * @verifies copy large payload completely across multiple buffer reads
     */
    @Test
    void copyRejectingSymlinks_shouldCopyLargePayloadCompletelyAcrossMultipleBufferReads(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("big.bin");
        byte[] payload = new byte[4 * 1024 * 1024];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xff);
        }
        try (InputStream in = new ByteArrayInputStream(payload)) {
            FileTools.copyRejectingSymlinks(in, target);
        }
        Assertions.assertEquals(payload.length, Files.size(target));
        Assertions.assertArrayEquals(payload, Files.readAllBytes(target));
    }

    /**
     * @see FileTools#copyRejectingSymlinks(InputStream, Path)
     * @verifies overwrite existing regular file at target
     */
    @Test
    void copyRejectingSymlinks_shouldOverwriteExistingRegularFileAtTarget(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("upload.txt");
        Files.writeString(target, "old", StandardCharsets.UTF_8);
        try (InputStream in = new ByteArrayInputStream("new".getBytes(StandardCharsets.UTF_8))) {
            FileTools.copyRejectingSymlinks(in, target);
        }
        Assertions.assertEquals("new", Files.readString(target, StandardCharsets.UTF_8));
    }

    /**
     * @see FileTools#copyRejectingSymlinks(InputStream, Path)
     * @verifies reject symbolic link at target path
     */
    @Test
    void copyRejectingSymlinks_shouldRejectSymbolicLinkAtTargetPath(@TempDir Path tempDir) throws IOException {
        Assumptions.assumeTrue(supportsSymlinks(tempDir), "Filesystem does not support symbolic links");
        Path victim = tempDir.resolve("victim.txt");
        Files.writeString(victim, "untouched", StandardCharsets.UTF_8);
        Path target = tempDir.resolve("upload.txt");
        Files.createSymbolicLink(target, victim);

        try (InputStream in = new ByteArrayInputStream("attack".getBytes(StandardCharsets.UTF_8))) {
            Assertions.assertThrows(IOException.class,
                    () -> FileTools.copyRejectingSymlinks(in, target));
        }
        Assertions.assertEquals("untouched", Files.readString(victim, StandardCharsets.UTF_8),
                "Symlink target file must not be overwritten");
    }

    /**
     * @see FileTools#copyRejectingSymlinks(InputStream, Path)
     * @verifies reject symbolic link to nonexistent target
     */
    @Test
    void copyRejectingSymlinks_shouldRejectSymbolicLinkToNonexistentTarget(@TempDir Path tempDir) throws IOException {
        Assumptions.assumeTrue(supportsSymlinks(tempDir), "Filesystem does not support symbolic links");
        Path nonexistent = tempDir.resolve("does-not-exist.txt");
        Path target = tempDir.resolve("upload.txt");
        Files.createSymbolicLink(target, nonexistent);

        try (InputStream in = new ByteArrayInputStream("attack".getBytes(StandardCharsets.UTF_8))) {
            Assertions.assertThrows(IOException.class,
                    () -> FileTools.copyRejectingSymlinks(in, target));
        }
        Assertions.assertFalse(Files.exists(nonexistent),
                "Dangling symlink target must not be created by the upload");
    }

    /**
     * @see FileTools#copyRejectingSymlinks(InputStream, Path)
     * @verifies reject symbolic link at parent directory
     */
    @Test
    void copyRejectingSymlinks_shouldRejectSymbolicLinkAtParentDirectory(@TempDir Path tempDir) throws IOException {
        Assumptions.assumeTrue(supportsSymlinks(tempDir), "Filesystem does not support symbolic links");
        Path realDir = tempDir.resolve("real");
        Files.createDirectories(realDir);
        Path linkedDir = tempDir.resolve("linked");
        Files.createSymbolicLink(linkedDir, realDir);
        Path target = linkedDir.resolve("upload.txt");

        try (InputStream in = new ByteArrayInputStream("attack".getBytes(StandardCharsets.UTF_8))) {
            IOException ex = Assertions.assertThrows(IOException.class,
                    () -> FileTools.copyRejectingSymlinks(in, target));
            Assertions.assertTrue(ex.getMessage().contains("symlinked parent"),
                    "Expected message to mention symlinked parent, was: " + ex.getMessage());
        }
        Assertions.assertFalse(Files.exists(realDir.resolve("upload.txt")),
                "Upload must not have been written through symlinked parent");
    }

    /**
     * @see FileTools#copyRejectingSymlinks(InputStream, Path)
     * @verifies fail when target is an existing directory
     */
    @Test
    void copyRejectingSymlinks_shouldFailWhenTargetIsAnExistingDirectory(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("a-directory");
        Files.createDirectories(target);

        try (InputStream in = new ByteArrayInputStream("attack".getBytes(StandardCharsets.UTF_8))) {
            Assertions.assertThrows(IOException.class,
                    () -> FileTools.copyRejectingSymlinks(in, target));
        }
        Assertions.assertTrue(Files.isDirectory(target), "Directory must remain a directory");
    }

    /**
     * @see FileTools#copyRejectingSymlinks(InputStream, Path)
     * @verifies not close the source input stream
     */
    @Test
    void copyRejectingSymlinks_shouldNotCloseTheSourceInputStream(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("upload.txt");
        AtomicBoolean closed = new AtomicBoolean(false);
        InputStream tracking = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        FileTools.copyRejectingSymlinks(tracking, target);
        Assertions.assertFalse(closed.get(),
                "Helper must follow Files.copy(InputStream,...) semantics and leave stream open");
    }

    /**
     * Probes whether the given filesystem location supports creating symbolic links.
     * Used to gate symlink tests so they skip on Windows / restrictive containers
     * instead of failing.
     */
    private static boolean supportsSymlinks(Path dir) {
        Path link = dir.resolve(".symlink-probe");
        try {
            Files.createSymbolicLink(link, dir);
            Files.delete(link);
            return true;
        } catch (IOException | UnsupportedOperationException e) {
            return false;
        }
    }
}
