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

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.iiif.presentation.v2.Collection2;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(COLLECTIONS)
@ViewerRestServiceBinding
public class CollectionsResource {

    private final String solrField;
    private final HttpServletRequest request;

    @Inject
    private ApiUrls urls;

    public CollectionsResource(
            @Parameter(description = "Name of the SOLR field the collection is based on. Typically 'DC'") @PathParam("field") String solrField,
            @Context HttpServletRequest request) {
        this.solrField = solrField.toUpperCase();
        this.request = request;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "iiif" }, summary = "Get all collections as IIIF Presentation 2.1.1 collection")
    @ApiResponse(responseCode = "400", description = "No collections available for field")
    public Collection2 getAllCollections(
            @Parameter(description = "Add values of this field to response to allow grouping of results") @QueryParam("grouping") String grouping,
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
            //can't be a collection
            throw new IllegalRequestException("No collections found for field " + solrField);
        }
        return collection;
    }

    @GET
    @javax.ws.rs.Path(COLLECTIONS_COLLECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "iiif" }, summary = "Get given collection as a IIIF Presentation 2.1.1 collection")
    @ApiResponse(responseCode = "400", description = "Invalid collection name or field")
    public Collection2 getCollection(
            @Parameter(description = "Name of the collection. Must be a value of the SOLR field the collection is based on") 
            @PathParam("collection") final String inCollectionName,
            @Parameter(description = "Add values of this field to response to allow grouping of results") 
            @QueryParam("grouping") String grouping,
            @Parameter(description = "comma separated list of subcollections to ignore in response") 
            @QueryParam("ignore") String ignoreString)
            throws PresentationException, IndexUnreachableException, ContentLibException, URISyntaxException, ViewerConfigurationException {
        IIIFPresentation2ResourceBuilder builder = new IIIFPresentation2ResourceBuilder(urls, request);
        String collectionName = StringTools.decodeUrl(inCollectionName);
        Collection2 collection;
        List<String> ignore = StringUtils.isNotBlank(ignoreString) ? Arrays.asList(ignoreString.split(",")) : Collections.emptyList();
        if (StringUtils.isBlank(grouping)) {
            collection = builder.getCollection(solrField, collectionName, ignore);
        } else {
            collection = builder.getCollectionWithGrouping(solrField, collectionName, grouping, ignore);
        }
        if (collection.getMembers() == null || collection.getMembers().isEmpty()) {
            //can't be a collection
            throw new IllegalRequestException("No valid collection: " + solrField + ":" + collectionName);
        }
        return collection;
    }

    @GET
    @javax.ws.rs.Path(COLLECTIONS_CONTENTASSIST)
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponse(responseCode = "400", description = "No collections available for field")
    //    @Operation(tags = { "collections"}, summary = "Return a list of collections starting with the given input")
    public List<String> contentAssist(
            @Parameter(description = "User input for which content assist is requested") @QueryParam("query") String input)
            throws IndexUnreachableException, IllegalRequestException {
        ContentAssistResourceBuilder builder = new ContentAssistResourceBuilder();
        return builder.getCollections(solrField, input);
    }

}
