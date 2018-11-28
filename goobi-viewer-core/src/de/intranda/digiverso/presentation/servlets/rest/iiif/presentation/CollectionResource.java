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
package de.intranda.digiverso.presentation.servlets.rest.iiif.presentation;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.iiif.presentation.Collection;
import de.intranda.digiverso.presentation.model.iiif.presentation.builder.CollectionBuilder;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

/**
 * IIIF REST resource providing a collection object as defined in the IIIF presentation api
 * 
 * @author Florian Alpers
 *
 */

@Path("/iiif/collections")
@ViewerRestServiceBinding
@IIIFPresentationBinding
public class CollectionResource extends AbstractResource {

    private static final Logger logger = LoggerFactory.getLogger(CollectionResource.class);

    private CollectionBuilder collectionBuilder;

    /**
     * Returns a iiif collection of all collections from the given solr-field The response includes the metadata and subcollections of the topmost
     * collections. Child collections may be accessed following the links in the @id properties in the member-collections Requires passing a language
     * to set the language for all metadata values
     * 
     * @param language
     * @param collectionField
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    @GET
    @Path("/{collectionField}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Collection getCollections(@PathParam("collectionField") String collectionField)
            throws PresentationException, IndexUnreachableException, URISyntaxException {

        Collection collection = getCollectionBuilder().generateCollection(collectionField, null,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));

        servletResponse.addHeader("Access-Control-Allow-Origin", "*");

        return collection;

    }

    /**
     * Returns a iiif collection of the given topCollection for the give collection field The response includes the metadata and subcollections of the
     * direct child collections. Collections further down the hierarchy may be accessed following the links in the @id properties in the
     * member-collections Requires passing a language to set the language for all metadata values
     * 
     * @throws URISyntaxException
     * @throws PresentationException
     * 
     */
    @GET
    @Path("/{collectionField}/{topElement}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Collection getCollection(@PathParam("collectionField") String collectionField, @PathParam("topElement") final String topElement)
            throws IndexUnreachableException, URISyntaxException, PresentationException {

        Collection collection = getCollectionBuilder().generateCollection(collectionField, topElement,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));

        servletResponse.addHeader("Access-Control-Allow-Origin", "*");

        return collection;

    }

    /**
     * @return the manifestBuilder
     */
    public CollectionBuilder getCollectionBuilder() {
        if (this.collectionBuilder == null) {
            try {
                this.collectionBuilder = new CollectionBuilder(servletRequest);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return collectionBuilder;
    }

}
