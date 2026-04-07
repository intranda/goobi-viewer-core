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

import static io.goobi.viewer.api.rest.v1.ApiUrls.COLLECTIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.COLLECTIONS_COLLECTION;
import static io.goobi.viewer.api.rest.v1.ApiUrls.COLLECTIONS_CONTENTASSIST;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.iiif.presentation.v2.Collection2;
import jakarta.ws.rs.BadRequestException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.resourcebuilders.ContentAssistResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.IIIFPresentation2ResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 */
@jakarta.ws.rs.Path(COLLECTIONS)
@ViewerRestServiceBinding
public class CollectionsResource {

    private final String solrField;
    private final HttpServletRequest request;

    @Inject
    private ApiUrls urls;

    public CollectionsResource(
            @Parameter(description = "Name of the Solr field the collection is based on. Typically 'DC'",
                    schema = @Schema(pattern = "^[A-Za-z_][A-Za-z0-9_]*$")) @PathParam("field") String solrField,
            @Context HttpServletRequest request) {
        // Validate field name against the documented pattern [A-Za-z_][A-Za-z0-9_]* to prevent
        // invalid Solr field names (e.g. "-") from reaching the index and causing 500 errors.
        if (!solrField.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new BadRequestException("Invalid collection field: " + solrField);
        }
        this.solrField = solrField.toUpperCase();
        this.request = request;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "iiif" }, summary = "Get all collections as IIIF Presentation 2.1.1 collection")
    @ApiResponse(responseCode = "200", description = "IIIF Presentation 2.1.1 collection containing all collections for this field")
    @ApiResponse(responseCode = "400", description = "Invalid or missing collection field parameter")
    @ApiResponse(responseCode = "404", description = "No collections available for field")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Collection2 getAllCollections(
            @Parameter(description = "Add values of this field to response to allow grouping of results",
                    schema = @Schema(pattern = "^[A-Za-z_][A-Za-z0-9_]*$")) @QueryParam("grouping") String grouping,
            @Parameter(description = "comma separated list of collections to ignore in response") @QueryParam("ignore") String ignoreString)
            throws PresentationException, IndexUnreachableException, ContentLibException, URISyntaxException, ViewerConfigurationException {
        IIIFPresentation2ResourceBuilder builder = new IIIFPresentation2ResourceBuilder(urls, request);
        Collection2 collection;
        List<String> ignore = StringUtils.isNotBlank(ignoreString) ? Arrays.asList(ignoreString.split(",")) : Collections.emptyList();
        if (StringUtils.isBlank(grouping)) {
            collection = builder.getCollections(solrField, ignore);
        } else {
            collection = builder.getCollectionsWithGrouping(solrField, ignore, grouping);
        }
        if (collection.getMembers() == null || collection.getMembers().isEmpty()) {
            // No collections exist for this field - return 404 rather than 400
            throw new ContentNotFoundException("No collections found for field " + solrField);
        }
        return collection;
    }

    @GET
    @jakarta.ws.rs.Path(COLLECTIONS_COLLECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "iiif" }, summary = "Get given collection as a IIIF Presentation 2.1.1 collection")
    @ApiResponse(responseCode = "200", description = "IIIF Presentation 2.1.1 collection for the given collection name")
    @ApiResponse(responseCode = "400", description = "Invalid or missing collection field or name parameter")
    @ApiResponse(responseCode = "404", description = "Collection not found for given field and name")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Collection2 getCollection(
            @Parameter(description = "Name of the collection. Must be a value of the Solr field the collection is based on") 
            @PathParam("collection") final String inCollectionName,
            @Parameter(description = "Add values of this field to response to allow grouping of results",
                    schema = @Schema(pattern = "^[A-Za-z_][A-Za-z0-9_]*$"))
            @QueryParam("grouping") String grouping,
            @Parameter(description = "comma separated list of subcollections to ignore in response")
            @QueryParam("ignore") String ignoreString)
            throws PresentationException, IndexUnreachableException, ContentLibException, URISyntaxException, ViewerConfigurationException {
        IIIFPresentation2ResourceBuilder builder = new IIIFPresentation2ResourceBuilder(urls, request);
        // StringTools.decodeUrl uses URLDecoder which throws IllegalArgumentException for malformed
        // percent-encoded sequences (e.g. '%ó' — a bare '%' not followed by two hex digits).
        // Catch this and return 400 instead of letting the unchecked exception produce HTTP 500.
        String collectionName;
        try {
            collectionName = StringTools.decodeUrl(inCollectionName);
        } catch (IllegalArgumentException e) {
            throw new jakarta.ws.rs.BadRequestException("Invalid collection name: " + inCollectionName);
        }
        Collection2 collection;
        List<String> ignore = StringUtils.isNotBlank(ignoreString) ? Arrays.asList(ignoreString.split(",")) : Collections.emptyList();
        if (StringUtils.isBlank(grouping)) {
            collection = builder.getCollection(solrField, collectionName, ignore);
        } else {
            collection = builder.getCollectionWithGrouping(solrField, collectionName, grouping, ignore);
        }
        if (collection.getMembers() == null || collection.getMembers().isEmpty()) {
            // Collection does not exist - return 404 rather than 400
            throw new ContentNotFoundException("No valid collection: " + solrField + ":" + collectionName);
        }
        return collection;
    }

    @GET
    @jakarta.ws.rs.Path(COLLECTIONS_CONTENTASSIST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "iiif" }, summary = "Return a list of collection names starting with the given input for content assist")
    @ApiResponse(responseCode = "200", description = "List of matching collection names")
    @ApiResponse(responseCode = "400", description = "Invalid collection field name")
    @ApiResponse(responseCode = "404", description = "Solr field not found in index")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public List<String> contentAssist(
            @Parameter(description = "User input for which content assist is requested") @QueryParam("query") String input)
            throws IndexUnreachableException, IllegalRequestException, ContentNotFoundException {
        ContentAssistResourceBuilder builder = new ContentAssistResourceBuilder();
        return builder.getCollections(solrField, input);
    }

}
