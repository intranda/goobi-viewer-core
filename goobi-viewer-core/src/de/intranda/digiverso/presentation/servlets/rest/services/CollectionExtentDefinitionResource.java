/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.servlets.rest.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

/**
 * Provides a context for the extent service used in the viewer IIIF Presentation collection responses
 * 
 * @author Florian Alpers
 *
 */
@Path("/api/collections/extent")
@ViewerRestServiceBinding
public class CollectionExtentDefinitionResource {

    /**
     * Returns a service context for the size information service for viewer collections: Number of direct child-collections and of total contained works 
     */
    @GET
    @Path("/context.json")
    @Produces({ MediaType.APPLICATION_JSON })
    public CollectionExtentDefinition getCollectionExtentContext() {

        return new CollectionExtentDefinition();

    }
    
}
