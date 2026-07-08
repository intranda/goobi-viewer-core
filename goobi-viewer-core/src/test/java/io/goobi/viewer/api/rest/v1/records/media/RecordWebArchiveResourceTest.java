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
package io.goobi.viewer.api.rest.v1.records.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.goobi.viewer.api.rest.model.webarchives.WebArchivePage;

class RecordWebArchiveResourceTest {

    @TempDir
    private Path tempDir;

    /** Writes a minimal WACZ (zip) file with the given lines as {@code pages/pages.jsonl} and returns its path. */
    private Path writeWacz(String... lines) throws IOException {
        Path wacz = tempDir.resolve("test.wacz");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(wacz))) {
            zos.putNextEntry(new ZipEntry("pages/pages.jsonl"));
            zos.write(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return wacz;
    }

    /**
     * @verifies return identifier unchanged when it has no source param
     * @see RecordWebArchiveResource#resolveWebArchiveUrl(String)
     */
    @Test
    void resolveWebArchiveUrl_shouldReturnIdentifierWhenNoSourceParam() {
        String identifier = "https://archive.example.org/site1.wacz";
        assertEquals(identifier, RecordWebArchiveResource.resolveWebArchiveUrl(identifier));
    }

    /**
     * @verifies return decoded source param value when present
     * @see RecordWebArchiveResource#resolveWebArchiveUrl(String)
     */
    @Test
    void resolveWebArchiveUrl_shouldReturnDecodedSourceParamValue() {
        String identifier = "https://archive.example.org/replay?source=https%3A%2F%2Fexample.org%2Ffile.json&other=1";
        assertEquals("https://example.org/file.json", RecordWebArchiveResource.resolveWebArchiveUrl(identifier));
    }

    /**
     * @verifies return null for malformed url
     * @see RecordWebArchiveResource#resolveWebArchiveUrl(String)
     */
    @Test
    void resolveWebArchiveUrl_shouldReturnNullForMalformedUrl() {
        assertNull(RecordWebArchiveResource.resolveWebArchiveUrl("http://exa mple.org/broken"));
    }

    /**
     * @verifies return null for blank input
     * @see RecordWebArchiveResource#resolveWebArchiveUrl(String)
     */
    @Test
    void resolveWebArchiveUrl_shouldReturnNullForBlankInput() {
        assertNull(RecordWebArchiveResource.resolveWebArchiveUrl(""));
        assertNull(RecordWebArchiveResource.resolveWebArchiveUrl(null));
    }

    /**
     * @verifies return last path segment
     * @see RecordWebArchiveResource#deriveExternalResourceName(String)
     */
    @Test
    void deriveExternalResourceName_shouldReturnLastPathSegment() {
        assertEquals("site1.wacz", RecordWebArchiveResource.deriveExternalResourceName("https://example.org/archive/site1.wacz"));
    }

    /**
     * @verifies return last path segment for json manifest urls
     * @see RecordWebArchiveResource#deriveExternalResourceName(String)
     */
    @Test
    void deriveExternalResourceName_shouldReturnLastPathSegmentForJsonManifest() {
        assertEquals("index.json", RecordWebArchiveResource.deriveExternalResourceName("https://example.org/manifests/index.json"));
    }

    /**
     * @verifies fall back to the full url when it has no path segment
     * @see RecordWebArchiveResource#deriveExternalResourceName(String)
     */
    @Test
    void deriveExternalResourceName_shouldFallBackToFullUrlWhenNoPath() {
        String url = "https://example.org";
        assertEquals(url, RecordWebArchiveResource.deriveExternalResourceName(url));
    }

    /**
     * @verifies fall back to the full url on malformed input
     * @see RecordWebArchiveResource#deriveExternalResourceName(String)
     */
    @Test
    void deriveExternalResourceName_shouldFallBackToFullUrlOnMalformedUrl() {
        String url = "http://exa mple.org/broken";
        assertEquals(url, RecordWebArchiveResource.deriveExternalResourceName(url));
    }

    /**
     * @verifies skip the header line
     * @see RecordWebArchiveResource#extractSeedPages(Path, String, List)
     */
    @Test
    void extractSeedPages_shouldSkipHeaderLine() throws Exception {
        // First line looks like a seed but is the JSONL header and must be skipped; only the real seed line survives
        Path wacz = writeWacz(
                "{\"url\":\"HEADER\",\"seed\":true}",
                "{\"url\":\"https://example.org/real\",\"seed\":true}");
        List<WebArchivePage> pages = new ArrayList<>();
        RecordWebArchiveResource.extractSeedPages(wacz, "test.wacz", pages);
        assertEquals(1, pages.size());
        assertEquals("https://example.org/real", pages.get(0).getUrl());
    }

    /**
     * @verifies add a seed page
     * @see RecordWebArchiveResource#extractSeedPages(Path, String, List)
     */
    @Test
    void extractSeedPages_shouldAddSeedPage() throws Exception {
        Path wacz = writeWacz(
                "{\"format\":\"json-pages-1.0\"}",
                "{\"url\":\"https://example.org/\",\"seed\":true}");
        List<WebArchivePage> pages = new ArrayList<>();
        RecordWebArchiveResource.extractSeedPages(wacz, "test.wacz", pages);
        assertEquals(1, pages.size());
        assertTrue(pages.get(0).getIsSeed());
        assertEquals("test.wacz", pages.get(0).getFilename());
    }

    /**
     * @verifies add a depth zero page
     * @see RecordWebArchiveResource#extractSeedPages(Path, String, List)
     */
    @Test
    void extractSeedPages_shouldAddDepthZeroPage() throws Exception {
        Path wacz = writeWacz(
                "{\"format\":\"json-pages-1.0\"}",
                "{\"url\":\"https://example.org/p\",\"depth\":0}");
        List<WebArchivePage> pages = new ArrayList<>();
        RecordWebArchiveResource.extractSeedPages(wacz, "test.wacz", pages);
        assertEquals(1, pages.size());
        assertEquals(0, pages.get(0).getDepth());
    }

    /**
     * @verifies skip a page that is neither seed nor depth zero
     * @see RecordWebArchiveResource#extractSeedPages(Path, String, List)
     */
    @Test
    void extractSeedPages_shouldSkipPageThatIsNeitherSeedNorDepthZero() throws Exception {
        Path wacz = writeWacz(
                "{\"format\":\"json-pages-1.0\"}",
                "{\"url\":\"https://example.org/deep\",\"depth\":3}");
        List<WebArchivePage> pages = new ArrayList<>();
        RecordWebArchiveResource.extractSeedPages(wacz, "test.wacz", pages);
        assertTrue(pages.isEmpty());
    }

    /**
     * @verifies extract page fields from the json line
     * @see RecordWebArchiveResource#extractSeedPages(Path, String, List)
     */
    @Test
    void extractSeedPages_shouldExtractPageFieldsFromJsonLine() throws Exception {
        Path wacz = writeWacz(
                "{\"format\":\"json-pages-1.0\"}",
                "{\"id\":\"abc\",\"url\":\"https://example.org/\",\"title\":\"Home\",\"ts\":\"20251016100005\","
                        + "\"loadState\":4,\"status\":200,\"mime\":\"text/html\",\"depth\":0,\"seed\":true}");
        List<WebArchivePage> pages = new ArrayList<>();
        RecordWebArchiveResource.extractSeedPages(wacz, "test.wacz", pages);
        assertEquals(1, pages.size());
        WebArchivePage page = pages.get(0);
        assertEquals("abc", page.getId());
        assertEquals("https://example.org/", page.getUrl());
        assertEquals("Home", page.getTitle());
        assertEquals("20251016100005", page.getTs());
        assertEquals(4, page.getLoadState());
        assertEquals(200, page.getStatus());
        assertEquals("text/html", page.getMime());
        assertEquals(0, page.getDepth());
    }
}
