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
package io.goobi.viewer.api.rest.v1.collections;

import static io.goobi.viewer.api.rest.v1.ApiUrls.COLLECTIONS_ARCHIVE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.COLLECTIONS_ARCHIVE_DOWNLOAD;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.bindings.UserLoggedInBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.MediaResourceHelper;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.model.export.bagit.CollectionArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * REST resource exposing the pre-generated per-collection BagIt archive for download. Both endpoints are restricted to logged-in users (or
 * a valid bearer token) via {@link UserLoggedInBinding}. The bag's payload is filtered at generation time against <b>anonymous</b> access
 * rights — per file for images/ALTO/plaintext and per whole record for the METS/LIDO source and TEI — so restricted pages and works are
 * excluded from the archive (see {@code CollectionArchiveService}). Access is not, and cannot be, re-checked per downloading user at this
 * endpoint, which is why only anonymously-accessible content is ever packed.
 */
@jakarta.ws.rs.Path(COLLECTIONS_ARCHIVE)
@ViewerRestServiceBinding
@UserLoggedInBinding
public class CollectionArchiveResource {

    private static final Logger logger = LogManager.getLogger(CollectionArchiveResource.class);

    private final String field;
    private final String collection;
    private final CollectionArchiveService service;

    @Context
    private HttpServletResponse servletResponse;
    @Context
    private Configuration config;

    public CollectionArchiveResource(
            @Parameter(description = "Name of the Solr field the collection is based on. Typically 'DC'") @PathParam("field") String field,
            @Parameter(description = "Collection name") @PathParam("collection") String collection) {
        if (field == null || !field.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new BadRequestException("Invalid collection field: " + field);
        }
        this.field = field.toUpperCase();
        this.collection = collection;
        this.service = new CollectionArchiveService();
    }

    /**
     * @return metadata about the collection's archive (whether one exists, its size, record count and download URL)
     * @throws IOException on filesystem error
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "collections" }, summary = "Information about the downloadable BagIt archive for a collection")
    @ApiResponse(responseCode = "200", description = "Archive availability and metadata")
    @ApiResponse(responseCode = "401", description = "User is not logged in")
    public Map<String, Object> getArchiveInfo() throws IOException {
        assertFieldConfigured();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("field", field);
        result.put("collection", collection);
        Optional<CollectionArchiveService.ArchiveInfo> info = service.findArchive(field, collection);
        result.put("available", info.isPresent());
        info.ifPresent(a -> {
            result.put("recordCount", a.recordCount());
            result.put("sizeBytes", a.sizeBytes());
            result.put("generatedMillis", a.maxIndexedMillis());
        });
        return result;
    }

    /**
     * Streams the collection's pre-generated BagIt archive as a ZIP download.
     *
     * @return a streaming ZIP response
     * @throws IOException on filesystem error
     * @throws ContentNotFoundException if no archive exists for the collection
     */
    @GET
    @jakarta.ws.rs.Path(COLLECTIONS_ARCHIVE_DOWNLOAD)
    @Operation(tags = { "collections" }, summary = "Download the BagIt archive for a collection")
    @ApiResponse(responseCode = "200", description = "The zipped BagIt archive")
    @ApiResponse(responseCode = "401", description = "User is not logged in")
    @ApiResponse(responseCode = "404", description = "No archive available for the collection")
    public Response downloadArchive() throws IOException, ContentNotFoundException {
        assertFieldConfigured();
        Optional<CollectionArchiveService.ArchiveInfo> info = service.findArchive(field, collection);
        if (info.isEmpty() || !Files.isRegularFile(info.get().path())) {
            throw new ContentNotFoundException("No archive available for collection " + collection);
        }
        Path archive = info.get().path();
        // Defense in depth: the resolved file must stay inside the configured storage base.
        Path base = service.getStorageBaseFolder().normalize();
        if (!archive.normalize().startsWith(base)) {
            throw new ContentNotFoundException("No archive available for collection " + collection);
        }

        String downloadName = CollectionArchiveService.slugify(collection) + "_bag.zip";
        String mimeType = "application/zip";
        try {
            mimeType = new MediaResourceHelper(config).setContentHeaders(servletResponse, downloadName, archive);
        } catch (IOException e) {
            logger.error("Failed to probe archive content type for {}", archive);
        }
        StreamingOutput so = out -> {
            try (InputStream in = FileTools.openRejectingSymlinks(archive)) {
                IOUtils.copy(in, out);
            }
        };
        return Response.ok(so, mimeType).build();
    }

    /**
     * Rejects requests for fields that are not configured for archiving, so callers cannot probe arbitrary folders under the storage base.
     */
    private void assertFieldConfigured() {
        if (!config.getConfiguredArchiveCollectionFields().contains(field)) {
            throw new BadRequestException("Collection field is not configured for archiving: " + field);
        }
    }
}
