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
package io.goobi.viewer.api.rest.v1.services;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import de.intranda.api.services.CollectionExtentDefinition;
import de.intranda.api.services.TagListDefinition;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Provides a context for the extent service used in the viewer IIIF Presentation collection responses.
 *
 * @author Florian Alpers
 */
@Hidden
@Path(ApiUrls.CONTEXT)
@ViewerRestServiceBinding
public class JsonLdDefinitionsResource {

    /**
     * Returns a service context for the size information service for viewer collections: Number of direct child-collections and of total contained
     * works.
     *
     * @return the JSON-LD context definition for the collection extent service
     */
    @GET
    @Path(CollectionExtentDefinition.URI_PATH)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Get JSON-LD context definition for the collection extent service", tags = { "services" })
    @ApiResponse(responseCode = "200", description = "JSON-LD context document for collection extent",
            content = @Content(mediaType = "application/ld+json"))
    public CollectionExtentDefinition getCollectionExtentContext() {

        return new CollectionExtentDefinition();

    }

    @GET
    @Path(TagListDefinition.URI_PATH)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Get JSON-LD context definition for the tag list service", tags = { "services" })
    @ApiResponse(responseCode = "200", description = "JSON-LD context document for tag list",
            content = @Content(mediaType = "application/ld+json"))
    public TagListDefinition getTagListContext() {

        return new TagListDefinition();

    }

}
