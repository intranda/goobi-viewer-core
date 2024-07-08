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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.exceptions.ArchiveSizeExceededException;

public class ZipUnpacker {

    private static final long DEFAULT_MAX_ARCHIVE_SIZE = 10_000_000_000L; //10GB
    private static final long DEFAULT_MAX_ENTRY_SIZE = 10_000_000_000L; //10GB
    private static final double COMPRESSION_RATION_THRESHOLD = 100;

    private static final Logger logger = LogManager.getLogger(ZipUnpacker.class);

    private final long maxArchiveSize;
    private final long maxEntrySize;

    public ZipUnpacker(long maxArchiveSize, long maxEntrySize) {
        this.maxArchiveSize = maxArchiveSize;
        this.maxEntrySize = maxEntrySize;
    }

    public ZipUnpacker() {
        this(DEFAULT_MAX_ARCHIVE_SIZE, DEFAULT_MAX_ENTRY_SIZE);
    }

    public Path extractZip(Path destination, ZipInputStream zis) throws IOException, ArchiveSizeExceededException {
        ZipEntry entry = null;
        long currentArchiveSize = 0;
        while ((entry = zis.getNextEntry()) != null) { //NOSONAR   entry is tested for size while extracting it
            Path entryFile = destination.resolve(entry.getName());
            if (!FileTools.isWithin(entryFile, destination)) {
                throw new IOException("Attemping to write illegal entry " + entryFile + ". Not within archive destination " + destination);
            }
            if (entry.isDirectory()) {
                logger.trace("Creating directory {}", entryFile);
                Files.createDirectory(entryFile);
            } else {
                logger.trace("Writing file {}", entryFile);
                if (!Files.isDirectory(entryFile.getParent())) {
                    Files.createDirectories(entryFile.getParent());
                }
                try {
                    currentArchiveSize = writeFile(entryFile, zis, currentArchiveSize, entry.getCompressedSize());
                } catch (ArchiveSizeExceededException e) {
                    throw new ArchiveSizeExceededException("Aborted writing archive at entry " + entry.getName() + ": " + e.getMessage());
                }
            }
        }
        return destination;
    }

    private long writeFile(Path entryFile, InputStream zis, long currentArchiveSize, long entryCompressedSize)
            throws IOException, ArchiveSizeExceededException {
        Files.deleteIfExists(entryFile);
        return copy(zis, entryFile, currentArchiveSize, entryCompressedSize, this.maxEntrySize, this.maxArchiveSize);
    }

    private long copy(InputStream in, Path outputPath, long currentArchiveSize, long entryCompressedSize, long maxEntrySize, long maxArchiveSize)
            throws IOException, ArchiveSizeExceededException {

        long currentEntrySize = 0;
        try (OutputStream out = Files.newOutputStream(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[4096];
            int nBytes = 0;
            while ((nBytes = in.read(buffer)) > 0) {
                out.write(buffer, 0, nBytes);
                currentEntrySize += nBytes;
                double compressionRatio = currentEntrySize / (double) (entryCompressedSize == 0 ? 1 : entryCompressedSize);
                if (currentEntrySize > maxEntrySize) {
                    throw new ArchiveSizeExceededException("Maximum allowed size {} for zip entry exceeded", maxEntrySize);
                } else if ((currentArchiveSize + currentEntrySize) > maxArchiveSize) {
                    throw new ArchiveSizeExceededException("Maximum allowed size {} for extraced zip archive exceeded", maxArchiveSize);
                } else if (compressionRatio > COMPRESSION_RATION_THRESHOLD) {
                    throw new ArchiveSizeExceededException("Maximum allowed compression ratio {} for zip entry exceeded", maxEntrySize);

                }
            }
        }
        return currentEntrySize;
    }
}
