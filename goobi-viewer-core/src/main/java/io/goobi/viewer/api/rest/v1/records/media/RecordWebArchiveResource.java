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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.model.webarchives.ReplayJson;
import io.goobi.viewer.api.rest.model.webarchives.WebArchivePage;
import io.goobi.viewer.api.rest.model.webarchives.WebArchiveResource;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.media.webarchives.WebArchiveReader;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path(ApiUrls.RECORDS_RECORD)
public class RecordWebArchiveResource {

    private static final Logger logger = LogManager.getLogger(RecordWebArchiveResource.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Cache: absolute path → (lastModifiedMillis, sha256hex) to avoid rehashing large files on every request. */
    private static final ConcurrentHashMap<String, long[]> HASH_TIMESTAMP_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> HASH_VALUE_CACHE = new ConcurrentHashMap<>();

    private final String pi;

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    private final AbstractApiUrlManager urls;

    public RecordWebArchiveResource(@Context HttpServletRequest servletRequest,
            @Parameter(description = "Persistent identifier of the record",
                    schema = @Schema(pattern = "^[A-Za-z0-9][A-Za-z0-9_.-]*$")) @PathParam("pi") String pi) {
        this.pi = pi;
        this.urls = DataManager.getInstance().getRestApiManager().getContentApiManager().orElse(null);
        servletRequest.setAttribute("pi", pi);
    }

    /**
     * Returns the replay JSON for this record's web archives, preferring locally indexed WACZ/WARC files and falling back to externally referenced
     * archives via {@code MD_WEBARCHIVE_IDENTIFIER}.
     *
     * @return replay JSON, a redirect to a single external JSON manifest, or {@code 404} if no archive is found
     * @throws IndexUnreachableException if the Solr index cannot be reached
     * @throws PresentationException if the Solr query fails
     * @should return local replay json when local archive docs exist
     * @should return 404 when neither local nor external archive docs are found
     * @should return replay json with one external resource per identifier when multiple identifiers are found
     * @should return replay json with a single external resource when exactly one identifier is found and it is not a json url
     * @should redirect to the resolved url when exactly one external identifier is found and it resolves to a json url
     * @should redirect to the decoded source query parameter value when the identifier url has one and it resolves to a json url
     * @should return 404 when a matching document is found but every identifier fails to parse
     */
    @GET
    @Path("/webarchives.json")
    @Produces("application/json")
    @Operation(tags = { "records" }, summary = "Get json containing all webarchive resources")
    public Response getWebarchiveJson() throws IndexUnreachableException, PresentationException {

        SolrSearchIndex search = DataManager.getInstance().getSearchIndex();
        String query = "+PI_TOPSTRUCT:%s +DOCTYPE:PAGE +MIMETYPE:application/warc".formatted(this.pi);
        String filteredQuery = query + SearchHelper.getAllSuffixes(servletRequest, true, true);

        SolrDocumentList docs = search.getDocs(filteredQuery, Collections.emptyList());

        if (docs != null && !docs.isEmpty()) {
            return buildLocalWebarchiveJson(search, docs);
        }

        List<String> externalUrls = findExternalWebArchiveUrls(search);
        if (externalUrls.isEmpty()) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (externalUrls.size() == 1 && externalUrls.get(0).toLowerCase().endsWith(".json")) {
            return Response.status(Status.FOUND).location(URI.create(externalUrls.get(0))).build();
        }

        return buildExternalWebarchiveJson(search, externalUrls);
    }

    private Response buildLocalWebarchiveJson(SolrSearchIndex search, SolrDocumentList docs)
            throws IndexUnreachableException, PresentationException {
        StructElement topStruct = new StructElement(search.getDocumentByPI(this.pi));

        List<WebArchiveResource> resources = new ArrayList<>();
        List<WebArchivePage> initialPages = new ArrayList<>();

        for (SolrDocument doc : docs) {
            String filename = doc.getFieldValue(SolrConstants.FILENAME).toString();
            String url = getWebArchiveUrl(this.pi, filename);
            String hash = null;
            Long size = null;
            java.nio.file.Path filePath = null;
            try {
                filePath = DataFileTools.getDataFilePath(this.pi,
                        DataManager.getInstance().getConfiguration().getMediaFolder(), null, filename);
                if (Files.isRegularFile(filePath)) {
                    size = Files.size(filePath);
                    hash = getCachedSha256(filePath);
                }
                // Catch only the concrete checked exceptions the path/size/hash pipeline declares; a failure here is
                // non-fatal (the resource is still listed, just without hash/size).
            } catch (IOException | NoSuchAlgorithmException | PresentationException | IndexUnreachableException e) {
                logger.warn("Could not compute hash/size for web archive {}: {}", filename, e.getMessage());
            }
            resources.add(new WebArchiveResource(filename, url, hash, size));
            if (filePath != null && Files.isRegularFile(filePath)) {
                extractSeedPages(filePath, filename, initialPages);
            }
        }

        return Response.ok(buildReplayJson(topStruct, resources, initialPages)).build();
    }

    /**
     * Finds all external web archive URLs referenced via {@code MD_WEBARCHIVE_IDENTIFIER} on the document matching
     * {@code PI:<pi>}, resolving each raw identifier via {@link #resolveWebArchiveUrl(String)}.
     *
     * @param search Solr search index to query
     * @return resolved URLs; empty if no matching document is found or none of its identifiers could be resolved
     */
    private List<String> findExternalWebArchiveUrls(SolrSearchIndex search) throws IndexUnreachableException, PresentationException {
        String query = "+PI:%s +MD_WEBARCHIVE_IDENTIFIER:*".formatted(this.pi);
        String filteredQuery = query + SearchHelper.getAllSuffixes(servletRequest, true, true);
        SolrDocumentList docs = search.getDocs(filteredQuery, Collections.emptyList());
        if (docs == null || docs.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> resolvedUrls = new ArrayList<>();
        for (SolrDocument doc : docs) {
            Collection<Object> rawIdentifiers = doc.getFieldValues(SolrConstants.MD_WEBARCHIVE_IDENTIFIER);
            if (rawIdentifiers == null) {
                continue;
            }
            for (Object rawIdentifier : rawIdentifiers) {
                String resolvedUrl = resolveWebArchiveUrl(rawIdentifier.toString());
                if (resolvedUrl != null) {
                    resolvedUrls.add(resolvedUrl);
                }
            }
        }
        return resolvedUrls;
    }

    private Response buildExternalWebarchiveJson(SolrSearchIndex search, List<String> externalUrls)
            throws IndexUnreachableException, PresentationException {
        StructElement topStruct = new StructElement(search.getDocumentByPI(this.pi));

        List<WebArchiveResource> resources = new ArrayList<>();
        for (String url : externalUrls) {
            resources.add(new WebArchiveResource(deriveExternalResourceName(url), url, null, null));
        }

        return Response.ok(buildReplayJson(topStruct, resources, Collections.emptyList())).build();
    }

    private ReplayJson buildReplayJson(StructElement topStruct, List<WebArchiveResource> resources, List<WebArchivePage> initialPages) {
        ReplayJson json = new ReplayJson(this.pi, topStruct.getLabel(), resources, initialPages);
        json.setCaption("My Caption");
        json.setDescription("My Description");
        json.setHomUrl("https://replayweb.page");
        return json;
    }

    /**
     * Reads the seed pages from the {@code pages/pages.jsonl} entry of a WACZ file and appends them to {@code pages}. The first line (the JSONL
     * header) is skipped; only entries that are marked {@code seed:true} or have {@code depth:0} are included.
     *
     * <p>
     * Package-private so it can be unit-tested directly against a WACZ file.
     *
     * @param waczPath path to the WACZ file
     * @param waczFilename filename stored on each produced page
     * @param pages list the extracted pages are appended to
     * @should skip the header line
     * @should add a seed page
     * @should add a depth zero page
     * @should skip a page that is neither seed nor depth zero
     * @should extract page fields from the json line
     */
    static void extractSeedPages(java.nio.file.Path waczPath, String waczFilename, List<WebArchivePage> pages) {
        try (ZipFile zip = new ZipFile(waczPath.toFile())) {
            ZipEntry entry = zip.getEntry("pages/pages.jsonl");
            if (entry == null) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    if (firstLine) {
                        firstLine = false;
                        // skip header line (contains "format" field)
                        continue;
                    }
                    try {
                        JsonNode node = MAPPER.readTree(line);
                        boolean seed = node.path("seed").asBoolean(false);
                        int depth = node.path("depth").asInt(-1);
                        if (!seed && depth != 0) {
                            continue;
                        }
                        WebArchivePage page = new WebArchivePage();
                        if (node.hasNonNull("id")) {
                            page.setId(node.get("id").asText());
                        }
                        if (node.hasNonNull("url")) {
                            page.setUrl(node.get("url").asText());
                        }
                        if (node.hasNonNull("title")) {
                            page.setTitle(node.get("title").asText());
                        }
                        if (node.hasNonNull("ts")) {
                            page.setTs(node.get("ts").asText());
                        }
                        if (node.hasNonNull("loadState")) {
                            page.setLoadState(node.get("loadState").asInt());
                        }
                        if (node.hasNonNull("status")) {
                            page.setStatus(node.get("status").asInt());
                        }
                        if (node.hasNonNull("mime")) {
                            page.setMime(node.get("mime").asText());
                        }
                        if (depth >= 0) {
                            page.setDepth(depth);
                        }
                        page.setIsSeed(true);
                        page.setFilename(waczFilename);
                        pages.add(page);
                        // A single malformed JSON line must not abort parsing of the remaining lines
                    } catch (JsonProcessingException e) {
                        logger.warn("Could not parse page entry in {}: {}", waczFilename, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Could not read seed pages from {}: {}", waczFilename, e.getMessage());
        }
    }

    private String getWebArchiveUrl(String identifier, String filename) {
        return urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_MEDIA).params(identifier, filename).build();
    }

    /**
     * Resolves the actual archive URL from a raw {@code MD_WEBARCHIVE_IDENTIFIER} value: if the identifier has a
     * {@code source} query parameter, that parameter's value is the actual URL; otherwise the identifier itself is used.
     *
     * @param rawIdentifier value of the {@code MD_WEBARCHIVE_IDENTIFIER} field
     * @return the resolved URL, or {@code null} if {@code rawIdentifier} is blank or not a valid URI
     * @should return identifier unchanged when it has no source param
     * @should return decoded source param value when present
     * @should return null for malformed url
     * @should return null for blank input
     */
    static String resolveWebArchiveUrl(String rawIdentifier) {
        if (StringUtils.isBlank(rawIdentifier)) {
            return null;
        }
        try {
            URI uri = new URI(rawIdentifier);
            String source = WebArchiveReader.extractParamValue(uri.getRawQuery(), "source");
            return source != null ? source : rawIdentifier;
        } catch (URISyntaxException e) {
            logger.warn("Could not parse web archive identifier URL '{}': {}", rawIdentifier, e.getMessage());
            return null;
        }
    }

    /**
     * Derives a display name for an external web archive resource from its URL: the last path segment, or the full URL
     * if it has no path segment or cannot be parsed.
     *
     * @param url resolved external web archive URL
     * @return a display name for the resource
     * @should return last path segment
     * @should return last path segment for json manifest urls
     * @should fall back to the full url when it has no path segment
     * @should fall back to the full url on malformed input
     */
    static String deriveExternalResourceName(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        try {
            String path = new URI(url).getPath();
            if (StringUtils.isNotBlank(path)) {
                int lastSlash = path.lastIndexOf('/');
                String segment = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
                if (StringUtils.isNotBlank(segment)) {
                    return segment;
                }
            }
        } catch (URISyntaxException e) {
            logger.warn("Could not parse web archive URL '{}' for name derivation: {}", url, e.getMessage());
        }
        return url;
    }

    private static String getCachedSha256(java.nio.file.Path filePath) throws IOException, NoSuchAlgorithmException {
        String key = filePath.toAbsolutePath().toString();
        long lastModified = Files.getLastModifiedTime(filePath).toMillis();
        long[] cached = HASH_TIMESTAMP_CACHE.get(key);
        if (cached != null && cached[0] == lastModified) {
            return HASH_VALUE_CACHE.get(key);
        }
        String hash = computeSha256(filePath);
        HASH_TIMESTAMP_CACHE.put(key, new long[] { lastModified });
        HASH_VALUE_CACHE.put(key, hash);
        return hash;
    }

    private static String computeSha256(java.nio.file.Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[65536];
            int n;
            while ((n = is.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

}
