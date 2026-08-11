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
package io.goobi.viewer.model.export.bagit;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Minimal, dependency-free writer for <a href="https://datatracker.ietf.org/doc/html/rfc8493">BagIt</a> bags, serialized as a ZIP.
 *
 * <p>
 * The caller assembles the payload into {@code <bagRoot>/data/…} (arbitrary sub-directory structure) and then calls
 * {@link #writeZippedBag(Path, Path, Map, Integer)}. This class then:
 * <ol>
 * <li>writes {@code bagit.txt} (version + encoding declaration),</li>
 * <li>computes a SHA-512 digest for every payload file and writes {@code manifest-sha512.txt},</li>
 * <li>writes {@code bag-info.txt} (the supplied metadata plus a computed {@code Payload-Oxum} and {@code Bag-Size}),</li>
 * <li>writes {@code tagmanifest-sha512.txt} over the tag files,</li>
 * <li>serializes the whole bag into a ZIP whose single top-level directory is the bag name.</li>
 * </ol>
 *
 * <p>
 * All file I/O is streamed, so bags containing large binary media are handled without loading files into memory. Payload checksums are
 * computed in a single pass while reading each file.
 */
public final class BagItWriter {

    private static final Logger logger = LogManager.getLogger(BagItWriter.class);

    /** BagIt version declared in {@code bagit.txt}. */
    public static final String BAGIT_VERSION = "1.0";
    /** Checksum algorithm used for both the payload manifest and the tag manifest. */
    public static final String ALGORITHM = "sha512";

    private static final String DATA_DIR = "data";
    private static final String BAGIT_TXT = "bagit.txt";
    private static final String BAG_INFO_TXT = "bag-info.txt";
    private static final String MANIFEST_TXT = "manifest-" + ALGORITHM + ".txt";
    private static final String TAGMANIFEST_TXT = "tagmanifest-" + ALGORITHM + ".txt";

    private BagItWriter() {
    }

    /**
     * Finalizes the bag rooted at {@code bagRoot} (which must already contain a populated {@code data/} directory) by writing its tag files
     * and manifests, then serializes it into {@code targetZip}.
     *
     * @param bagRoot the bag root directory containing a {@code data/} payload sub-directory
     * @param targetZip the ZIP file to create (parent directories must exist)
     * @param bagInfo ordered metadata entries for {@code bag-info.txt} (e.g. Source-Organization, External-Description); may be null
     * @param compressionLevel ZIP compression level 0-9, or null for the default; use a low value for already-compressed media payloads
     * @throws IOException on any I/O error
     */
    public static void writeZippedBag(Path bagRoot, Path targetZip, Map<String, String> bagInfo, Integer compressionLevel)
            throws IOException {
        Path dataDir = bagRoot.resolve(DATA_DIR);
        if (!Files.isDirectory(dataDir)) {
            throw new IOException("Bag payload directory does not exist: " + dataDir);
        }

        // 1. bagit.txt
        writeString(bagRoot.resolve(BAGIT_TXT),
                "BagIt-Version: " + BAGIT_VERSION + "\nTag-File-Character-Encoding: UTF-8\n");

        // 2. manifest-sha512.txt over all payload files (+ collect oxum)
        long payloadBytes = 0;
        long payloadCount = 0;
        List<String> manifestLines = new ArrayList<>();
        List<Path> payloadFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dataDir)) {
            walk.filter(Files::isRegularFile).sorted().forEach(payloadFiles::add);
        }
        for (Path file : payloadFiles) {
            String digest = sha512(file);
            // Manifest paths are relative to the bag root and use forward slashes; payload paths always start with "data/".
            String relPath = toPosix(bagRoot.relativize(file));
            manifestLines.add(digest + "  " + relPath);
            payloadBytes += Files.size(file);
            payloadCount++;
        }
        writeString(bagRoot.resolve(MANIFEST_TXT), String.join("\n", manifestLines) + (manifestLines.isEmpty() ? "" : "\n"));

        // 3. bag-info.txt (supplied metadata + computed Payload-Oxum / Bag-Size)
        StringBuilder info = new StringBuilder();
        if (bagInfo != null) {
            for (Map.Entry<String, String> entry : bagInfo.entrySet()) {
                appendInfoLine(info, entry.getKey(), entry.getValue());
            }
        }
        appendInfoLine(info, "Payload-Oxum", payloadBytes + "." + payloadCount);
        appendInfoLine(info, "Bag-Size", humanReadableSize(payloadBytes));
        writeString(bagRoot.resolve(BAG_INFO_TXT), info.toString());

        // 4. tagmanifest-sha512.txt over the tag files
        List<String> tagLines = new ArrayList<>();
        for (String tagFile : new String[] { BAGIT_TXT, BAG_INFO_TXT, MANIFEST_TXT }) {
            Path path = bagRoot.resolve(tagFile);
            if (Files.isRegularFile(path)) {
                tagLines.add(sha512(path) + "  " + tagFile);
            }
        }
        writeString(bagRoot.resolve(TAGMANIFEST_TXT), String.join("\n", tagLines) + (tagLines.isEmpty() ? "" : "\n"));

        // 5. serialize to ZIP
        zipDirectory(bagRoot, targetZip, compressionLevel);
        logger.trace("Wrote BagIt archive with {} payload file(s), {} bytes to {}", payloadCount, payloadBytes, targetZip);
    }

    /**
     * Streams the SHA-512 hex digest of a file without loading it into memory.
     *
     * @param file the file to hash
     * @return the lower-case hex SHA-512 digest
     * @throws IOException on read error
     */
    static String sha512(Path file) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            // SHA-512 is a mandatory JDK algorithm, so this cannot happen in practice.
            throw new IOException("SHA-512 algorithm unavailable", e);
        }
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(md.digest());
    }

    /**
     * Serializes a directory tree into a ZIP file. Entry names are relative to the parent of {@code sourceDir}, so the archive contains a
     * single top-level directory named after {@code sourceDir}. Entries are written in sorted order for reproducibility, and file contents
     * are streamed.
     *
     * @param sourceDir the directory to compress (its own name becomes the top-level ZIP folder)
     * @param targetZip the ZIP file to create
     * @param compressionLevel ZIP compression level 0-9, or null for the default
     * @throws IOException on any I/O error
     */
    static void zipDirectory(Path sourceDir, Path targetZip, Integer compressionLevel) throws IOException {
        Path base = sourceDir.getParent();
        List<Path> entries = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(sourceDir)) {
            walk.filter(Files::isRegularFile).sorted().forEach(entries::add);
        }
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(targetZip))) {
            if (compressionLevel != null) {
                // Level 0 keeps the DEFLATED method but performs no compression, which is the cheap option for already-compressed media.
                zos.setLevel(compressionLevel);
            }
            for (Path file : entries) {
                String name = toPosix(base.relativize(file));
                zos.putNextEntry(new ZipEntry(name));
                try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
                    in.transferTo(zos);
                }
                zos.closeEntry();
            }
        }
    }

    private static void appendInfoLine(StringBuilder sb, String key, String value) {
        if (key == null || value == null) {
            return;
        }
        // Keep values single-line: bag-info.txt uses newline-delimited "Label: value" records, so strip embedded line breaks.
        String sanitized = value.replaceAll("[\\r\\n]+", " ").trim();
        sb.append(key).append(": ").append(sanitized).append('\n');
    }

    private static void writeString(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static String toPosix(Path path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.getNameCount(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(path.getName(i));
        }
        return sb.toString();
    }

    private static String humanReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = { "kB", "MB", "GB", "TB" };
        double value = bytes;
        int unit = -1;
        do {
            value /= 1024.0;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);
        return String.format("%.1f %s", value, units[unit]);
    }
}
