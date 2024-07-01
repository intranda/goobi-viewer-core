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
package io.goobi.viewer.controller.files;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.goobi.viewer.exceptions.ArchiveSizeExceededException;

class ZipUnpackerTest {

    private static final Path ARCHIVE_PATH = Path.of("src/test/resources/data/viewer/external-files/sampleArchive.zip").toAbsolutePath();

    @Test
    void test_downloadArchive(@TempDir Path tempDir) throws IOException, ArchiveSizeExceededException {

        try (InputStream input = Files.newInputStream(ARCHIVE_PATH);
                ZipInputStream zis = new ZipInputStream(input)) {
            Path extractedArchive = new ZipUnpacker().extractZip(tempDir, zis);
            assertTrue(Files.isDirectory(extractedArchive));
            assertTrue(Files.isDirectory(extractedArchive.resolve("texts")));
            assertTrue(Files.isRegularFile(extractedArchive.resolve("owl.png")));
            assertTrue(Files.isRegularFile(extractedArchive.resolve("texts").resolve("text1.txt")));
            assertTrue(Files.isDirectory(extractedArchive.resolve("moretexts").resolve("samples2")));
            assertTrue(Files.isRegularFile(extractedArchive.resolve("moretexts").resolve("samples2").resolve("sample4.txt")));
        }
    }

    @Test
    void test_archiveSizeExceeded(@TempDir Path tempDir) throws IOException {
        ArchiveSizeExceededException thrown = assertThrows(ArchiveSizeExceededException.class,
                () -> {
                    try (InputStream input = Files.newInputStream(ARCHIVE_PATH);
                            ZipInputStream zis = new ZipInputStream(input)) {
                        new ZipUnpacker(100_000, 100_000_000).extractZip(tempDir, zis);
                    }

                });
        String expectedMessage = "Maximum allowed size 100000 for extraced zip archive exceeded";
        assertTrue(thrown.getMessage().contains(expectedMessage),
                String.format("Expected exception message \"%s\", but was \"%s\"", expectedMessage, thrown.getMessage()));
    }

    @Test
    void test_antrySizeExceeded(@TempDir Path tempDir) throws IOException {
        ArchiveSizeExceededException thrown = assertThrows(ArchiveSizeExceededException.class,
                () -> {
                    try (InputStream input = Files.newInputStream(ARCHIVE_PATH);
                            ZipInputStream zis = new ZipInputStream(input)) {
                        new ZipUnpacker(100_000_000, 100_000).extractZip(tempDir, zis);
                    }

                });
        String expectedMessage = "Maximum allowed size 100000 for zip entry exceeded";
        assertTrue(thrown.getMessage().contains(expectedMessage),
                String.format("Expected exception message \"%s\", but was \"%s\"", expectedMessage, thrown.getMessage()));
    }

}
