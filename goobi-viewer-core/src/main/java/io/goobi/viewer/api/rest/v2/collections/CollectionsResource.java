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
package io.goobi.viewer.api.rest.v2.collections;

import static io.goobi.viewer.api.rest.v2.ApiUrls.COLLECTIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.COLLECTIONS_COLLECTION;

import de.intranda.api.iiif.presentation.v3.Collection3;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.iiif.presentation.v3.builder.CollectionBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * REST resource providing IIIF Presentation v3 collection endpoints for collection browsing.
 *
 * @author Florian Alpers
 */
@jakarta.ws.rs.Path(COLLECTIONS)
@ViewerRestServiceBinding
public class CollectionsResource {

    @Context
    private HttpServletRequest servletRequest;

    private final String solrField;

    @Inject
    private ApiUrls urls;

    public CollectionsResource(
            @Parameter(description = "Name of the SOLR field the collection is based on. Typically 'DC'",
                    schema = @Schema(pattern = "^[A-Za-z_][A-Za-z0-9_]*$"))
            @PathParam("field") String solrField,
            @Context HttpServletRequest request) {
        // Validate field name against the documented pattern [A-Za-z_][A-Za-z0-9_]* to prevent
        // invalid Solr field names (e.g. "0") from reaching the index and causing HTTP 500 errors.
        if (!solrField.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new BadRequestException("Invalid collection field: " + solrField);
        }
        this.solrField = solrField.toUpperCase();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "iiif" }, summary = "Get all collections as IIIF Presentation 3.0 collection")
    @ApiResponse(responseCode = "200", description = "IIIF Presentation 3.0 collection containing all collections for this field")
    @ApiResponse(responseCode = "400", description = "Invalid collection field parameter")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Collection3 getAllCollections() throws IndexUnreachableException {
        return new CollectionBuilder(urls, this.servletRequest).build(this.solrField);
    }

    @GET
    @jakarta.ws.rs.Path(COLLECTIONS_COLLECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "iiif" }, summary = "Get given collection as a IIIF presentation 3.0 collection")
    @ApiResponse(responseCode = "200", description = "IIIF Presentation 3.0 collection for the given collection name")
    @ApiResponse(responseCode = "400", description = "Invalid collection field parameter")
    @ApiResponse(responseCode = "404", description = "Collection not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Collection3 getCollection(
            @Parameter(
                    description = "Name of the collection. Must be a value of the SOLR field the collection is based on")
            @PathParam("collection") String collectionName)
            throws PresentationException, IndexUnreachableException {
        // Jersey wraps PresentationException (a checked exception) in ContainerException before
        // any ExceptionMapper can handle it in the v2 application, causing Tomcat to serve an
        // HTML 500 error page. Catch it here and convert to a proper 400/404 HTTP response.
        // Also catch unchecked exceptions (IllegalArgumentException, NullPointerException) that
        // can occur for invalid Solr field names or malformed collection names.
        try {
            return new CollectionBuilder(urls, this.servletRequest).build(this.solrField, collectionName);
        } catch (PresentationException e) {
            // Solr rejected the query (e.g. undefined field name) — the collection does not exist
            throw new NotFoundException("Collection not found for field " + this.solrField + " and name " + collectionName);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Invalid collection field or name: " + this.solrField + "/" + collectionName);
        }
    }
}
