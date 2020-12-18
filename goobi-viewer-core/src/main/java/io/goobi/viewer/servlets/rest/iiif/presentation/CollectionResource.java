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
package io.goobi.viewer.servlets.rest.iiif.presentation;

import static io.goobi.viewer.api.rest.v1.ApiUrls.COLLECTIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.COLLECTIONS_COLLECTION;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.collections.CollectionsResource;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.BrowseDcElement;

/**
 * IIIF REST resource providing a collection object as defined in the IIIF presentation api
 *
 * @deprecated use {@link CollectionsResource} instead
 *
 * @author Florian Alpers
 */

@Path("/iiif/collections")
@ViewerRestServiceBinding
@Deprecated
public class CollectionResource extends AbstractResource {

    private static final Logger logger = LoggerFactory.getLogger(CollectionResource.class);

    
    /**
     * Returns a iiif collection of all collections from the given solr-field The response includes the metadata and subcollections of the topmost
     * collections. Child collections may be accessed following the links in the @id properties in the member-collections Requires passing a language
     * to set the language for all metadata values
     *
     * @param collectionField a {@link java.lang.String} object.
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws IllegalRequestException 
     */
    @GET
    @Path("/{collectionField}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public Response getCollections(@PathParam("collectionField") String collectionField)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, IllegalRequestException {
        URI canonical = URI.create(urls.path(COLLECTIONS).params(collectionField).build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();

    }
    
    /**
     * Returns a iiif collection of all collections from the given solr-field The response includes the metadata and subcollections of the topmost
     * collections. Child collections may be accessed following the links in the @id properties in the member-collections Requires passing a language
     * to set the language for all metadata values
     *
     * @param collectionField a {@link java.lang.String} object.
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws IllegalRequestException 
     */
    @GET
    @Path("/{collectionField}/grouping/{groupingField}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public Response getCollectionsWithGrouping(@PathParam("collectionField") String collectionField, @PathParam("groupingField") final String groupingField)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, IllegalRequestException {
        URI canonical = URI.create(urls.path(COLLECTIONS).params(collectionField).query("grouping", groupingField).build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();

    }

    /**
     * Returns a iiif collection of the given topCollection for the give collection field The response includes the metadata and subcollections of the
     * direct child collections. Collections further down the hierarchy may be accessed following the links in the @id properties in the
     * member-collections Requires passing a language to set the language for all metadata values
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param topElement a {@link java.lang.String} object.
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws IllegalRequestException 
     */
    @GET
    @Path("/{collectionField}/{topElement}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public Response getCollection(@PathParam("collectionField") String collectionField, @PathParam("topElement") final String topElement)
            throws IndexUnreachableException, URISyntaxException, PresentationException, ViewerConfigurationException, IllegalRequestException {
        URI canonical = URI.create(urls.path(COLLECTIONS, COLLECTIONS_COLLECTION).params(collectionField, topElement).build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();

    }
    
    /**
     * Returns a iiif collection of the given topCollection for the give collection field The response includes the metadata and subcollections of the
     * direct child collections. Collections further down the hierarchy may be accessed following the links in the @id properties in the
     * member-collections Requires passing a language to set the language for all metadata values
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param topElement a {@link java.lang.String} object.
     * @param groupingField a solr field by which the collections may be grouped. Included in the response for each {@link BrowseDcElement} to enable grouping by client
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws IllegalRequestException 
     */
    @GET
    @Path("/{collectionField}/{topElement}/grouping/{groupingField}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public Response getCollectionWithGrouping(@PathParam("collectionField") String collectionField, @PathParam("topElement") final String topElement, @PathParam("groupingField") final String facetField)
            throws IndexUnreachableException, URISyntaxException, PresentationException, ViewerConfigurationException, IllegalRequestException {
        URI canonical = URI.create(urls.path(COLLECTIONS, COLLECTIONS_COLLECTION).params(collectionField, topElement).query("grouping", facetField).build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();

    }


}
