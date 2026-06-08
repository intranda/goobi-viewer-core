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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.model.webarchives.ReplayJson;
import io.goobi.viewer.api.rest.model.webarchives.WebArchiveResource;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
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

    /** Cache: absolute path → (lastModifiedMillis, sha256hex) to avoid rehashing large files on every request. */
    private static final ConcurrentHashMap<String, long[]> hashTimestampCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> hashValueCache = new ConcurrentHashMap<>();

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

    @GET
    @Path("/webarchives.json")
    @Produces("application/json")
    @Operation(tags = { "records" }, summary = "Get json containing all webarchive resources")
    public Response getWebarchiveJson() throws IndexUnreachableException, PresentationException {

        SolrSearchIndex search = DataManager.getInstance().getSearchIndex();
        String query = "+PI_TOPSTRUCT:%s +DOCTYPE:PAGE +MIMETYPE:application/warc".formatted(this.pi);
        String filteredQuery = query + SearchHelper.getAllSuffixes(servletRequest, true, true);

        SolrDocumentList docs = search.getDocs(filteredQuery, Collections.emptyList());

        SolrDocument topDoc = search.getDocumentByPI(pi);
        StructElement topStruct = new StructElement(topDoc);

        if (!docs.isEmpty()) {

            List<WebArchiveResource> resources = docs.stream().map(doc -> {
                String filename = doc.getFieldValue(SolrConstants.FILENAME).toString();
                String url = getWebArchiveUrl(this.pi, filename);
                String hash = null;
                Long size = null;
                try {
                    java.nio.file.Path filePath = DataFileTools.getDataFilePath(this.pi,
                            DataManager.getInstance().getConfiguration().getMediaFolder(), null, filename);
                    if (Files.isRegularFile(filePath)) {
                        size = Files.size(filePath);
                        hash = getCachedSha256(filePath);
                    }
                } catch (Exception e) {
                    logger.warn("Could not compute hash/size for web archive {}: {}", filename, e.getMessage());
                }
                return new WebArchiveResource(filename, url, hash, size);
            }).toList();

            ReplayJson json = new ReplayJson(this.pi, topStruct.getLabel(), resources);

            return Response.ok(json).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }

    }

    private String getWebArchiveUrl(String identifier, String filename) {
        return urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_MEDIA).params(identifier, filename).build();
    }

    private static String getCachedSha256(java.nio.file.Path filePath) throws IOException, NoSuchAlgorithmException {
        String key = filePath.toAbsolutePath().toString();
        long lastModified = Files.getLastModifiedTime(filePath).toMillis();
        long[] cached = hashTimestampCache.get(key);
        if (cached != null && cached[0] == lastModified) {
            return hashValueCache.get(key);
        }
        String hash = computeSha256(filePath);
        hashTimestampCache.put(key, new long[] { lastModified });
        hashValueCache.put(key, hash);
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
