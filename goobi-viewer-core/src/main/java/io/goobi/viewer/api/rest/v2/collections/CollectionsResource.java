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

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import de.intranda.api.iiif.presentation.v3.Collection3;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.iiif.presentation.v3.builder.CollectionBuilder;
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

    @Inject
    private ApiUrls urls;

    public CollectionsResource(
            @Parameter(description = "Name of the SOLR field the collection is based on. Typically 'DC'") @PathParam("field") String solrField,
            @Context HttpServletRequest request) {
        this.solrField = solrField.toUpperCase();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "iiif" }, summary = "Get all collections as IIIF Presentation 3.0 collection")
    @ApiResponse(responseCode = "400", description = "No collections available for field")
    public Collection3 getAllCollections() throws IndexUnreachableException {
        return new CollectionBuilder(urls).build(this.solrField);
    }

    @GET
    @javax.ws.rs.Path(COLLECTIONS_COLLECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "iiif" }, summary = "Get given collection as a IIIF presentation 3.0 collection")
    @ApiResponse(responseCode = "400", description = "Invalid collection name or field")
    public Collection3 getCollection(
            @Parameter(
                    description = "Name of the collection. Must be a value of the SOLR field the collection is based on")
            @PathParam("collection") String collectionName)
            throws PresentationException, IndexUnreachableException {
        return new CollectionBuilder(urls).build(this.solrField, collectionName);
    }
}
