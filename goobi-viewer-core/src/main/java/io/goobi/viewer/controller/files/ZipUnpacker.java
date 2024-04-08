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

import io.goobi.viewer.exceptions.ArchiveSizeExceededException;

public class ZipUnpacker {

    private static final long DEFAULT_MAX_ARCHIVE_SIZE = 10_000_000_000l; //10GB
    private static final long DEFAULT_MAX_ENTRY_SIZE = 10_000_000_000l; //10GB

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
        while ((entry = zis.getNextEntry()) != null) {
            Path entryFile = destination.resolve(entry.getName());
            if (entry.isDirectory()) {
                logger.trace("Creating directory {}", entryFile);
                Files.createDirectory(entryFile);
            } else {
                logger.trace("Writing file {}", entryFile);
                if (!Files.isDirectory(entryFile.getParent())) {
                    Files.createDirectories(entryFile.getParent());
                }
                try {
                    currentArchiveSize = writeFile(entryFile, zis, currentArchiveSize);
                } catch (ArchiveSizeExceededException e) {
                    throw new ArchiveSizeExceededException("Aborted writing archive at entry " + entry.getName() + ": " + e.getMessage());
                }
            }
        }
        return destination;
    }

    private long writeFile(Path entryFile, InputStream zis, long currentArchiveSize) throws IOException, ArchiveSizeExceededException {
        Files.deleteIfExists(entryFile);
        return copy(zis, entryFile, currentArchiveSize, this.maxEntrySize, this.maxArchiveSize);
    }

    private long copy(InputStream in, Path outputPath, long currentArchiveSize, long maxEntrySize, long maxArchiveSize)
            throws IOException, ArchiveSizeExceededException {

        long currentEntrySize = 0;
        try (OutputStream out = Files.newOutputStream(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[4096];
            int nBytes = 0;
            while ((nBytes = in.read(buffer)) > 0) {
                out.write(buffer, 0, nBytes);
                currentEntrySize += nBytes;
                currentArchiveSize += nBytes;
                if (currentEntrySize > maxEntrySize) {
                    throw new ArchiveSizeExceededException("Maximum allowed size {} for zip entry exceeded", maxEntrySize);
                } else if (currentArchiveSize > maxArchiveSize) {
                    throw new ArchiveSizeExceededException("Maximum allowed size {} for extraced zip archive exceeded", maxArchiveSize);
                }
            }
        }
        return currentEntrySize;
    }
}
