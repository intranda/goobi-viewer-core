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
package io.goobi.viewer.model.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BagItWriterTest {

    /**
     * @see BagItWriter#writeZippedBag(Path, Path, Map, Integer)
     * @verifies write a valid bag with manifest and payload oxum
     */
    @Test
    void writeZippedBag_shouldWriteValidBag(@TempDir Path tmp) throws Exception {
        Path bagRoot = tmp.resolve("PPN_collection_bag");
        Path metaDir = bagRoot.resolve("data/metadata");
        Path altoDir = bagRoot.resolve("data/alto/PPN123");
        Files.createDirectories(metaDir);
        Files.createDirectories(altoDir);

        byte[] metsBytes = "<mets>test</mets>".getBytes(StandardCharsets.UTF_8);
        byte[] altoBytes = "<alto/>".getBytes(StandardCharsets.UTF_8);
        Files.write(metaDir.resolve("PPN123.xml"), metsBytes);
        Files.write(altoDir.resolve("00000001.xml"), altoBytes);

        Map<String, String> info = new LinkedHashMap<>();
        info.put("Source-Organization", "intranda");
        info.put("External-Description", "Collection varia");

        Path zip = tmp.resolve("out.zip");
        BagItWriter.writeZippedBag(bagRoot, zip, info, 9);

        assertTrue(Files.isRegularFile(zip));

        Map<String, byte[]> entries = unzip(zip);

        // Top-level folder is the bag name, and required tag files exist
        assertTrue(entries.containsKey("PPN_collection_bag/bagit.txt"));
        assertTrue(entries.containsKey("PPN_collection_bag/bag-info.txt"));
        assertTrue(entries.containsKey("PPN_collection_bag/manifest-sha512.txt"));
        assertTrue(entries.containsKey("PPN_collection_bag/tagmanifest-sha512.txt"));

        // Payload directory structure preserved
        assertTrue(entries.containsKey("PPN_collection_bag/data/metadata/PPN123.xml"));
        assertTrue(entries.containsKey("PPN_collection_bag/data/alto/PPN123/00000001.xml"));
        assertEquals(new String(metsBytes, StandardCharsets.UTF_8),
                new String(entries.get("PPN_collection_bag/data/metadata/PPN123.xml"), StandardCharsets.UTF_8));

        // bagit.txt content
        String bagit = new String(entries.get("PPN_collection_bag/bagit.txt"), StandardCharsets.UTF_8);
        assertTrue(bagit.contains("BagIt-Version: " + BagItWriter.BAGIT_VERSION));
        assertTrue(bagit.contains("Tag-File-Character-Encoding: UTF-8"));

        // manifest lists both payload files with correct SHA-512 digests, relative to the bag root
        String manifest = new String(entries.get("PPN_collection_bag/manifest-sha512.txt"), StandardCharsets.UTF_8);
        assertTrue(manifest.contains(sha512Hex(metsBytes) + "  data/metadata/PPN123.xml"));
        assertTrue(manifest.contains(sha512Hex(altoBytes) + "  data/alto/PPN123/00000001.xml"));

        // bag-info.txt carries supplied metadata and a computed Payload-Oxum of "<bytes>.<count>"
        String bagInfo = new String(entries.get("PPN_collection_bag/bag-info.txt"), StandardCharsets.UTF_8);
        assertTrue(bagInfo.contains("Source-Organization: intranda"));
        assertTrue(bagInfo.contains("External-Description: Collection varia"));
        assertTrue(bagInfo.contains("Payload-Oxum: " + (metsBytes.length + altoBytes.length) + ".2"));
    }

    private static Map<String, byte[]> unzip(Path zip) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), readAll(zis));
                }
            }
        }
        assertNotNull(entries);
        return entries;
    }

    private static byte[] readAll(InputStream in) throws Exception {
        return in.readAllBytes();
    }

    private static String sha512Hex(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(data));
    }
}
