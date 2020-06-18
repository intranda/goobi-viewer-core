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

import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.iiif.presentation.Collection;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.iiif.presentation.builder.CollectionBuilder;
import io.goobi.viewer.model.viewer.BrowseDcElement;

/**
 * IIIF REST resource providing a collection object as defined in the IIIF presentation api
 *
 * @author Florian Alpers
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
     * @param collectionField a {@link java.lang.String} object.
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    @GET
    @Path("/{collectionField}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public Collection getCollections(@PathParam("collectionField") String collectionField)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException {

        Collection collection = getCollectionBuilder().generateCollection(collectionField, null, null,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));

        return collection;

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
     */
    @GET
    @Path("/{collectionField}/grouping/{groupingField}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public Collection getCollectionsWithGrouping(@PathParam("collectionField") String collectionField, @PathParam("groupingField") final String groupingField)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException {

        Collection collection = getCollectionBuilder().generateCollection(collectionField, null, groupingField,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));
        
        getCollectionBuilder().addTagListService(collection, collectionField, groupingField, "grouping");
        
        return collection;

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
     */
    @GET
    @Path("/{collectionField}/{topElement}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public Collection getCollection(@PathParam("collectionField") String collectionField, @PathParam("topElement") final String topElement)
            throws IndexUnreachableException, URISyntaxException, PresentationException, ViewerConfigurationException {

        Collection collection = getCollectionBuilder().generateCollection(collectionField, topElement, null,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));

        return collection;

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
     */
    @GET
    @Path("/{collectionField}/{topElement}/grouping/{groupingField}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public Collection getCollectionWithGrouping(@PathParam("collectionField") String collectionField, @PathParam("topElement") final String topElement, @PathParam("groupingField") final String facetField)
            throws IndexUnreachableException, URISyntaxException, PresentationException, ViewerConfigurationException {

        Collection collection = getCollectionBuilder().generateCollection(collectionField, topElement, facetField,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));

        getCollectionBuilder().addTagListService(collection, collectionField, facetField, "grouping");
        
        return collection;

    }

    /**
     * <p>
     * Getter for the field <code>collectionBuilder</code>.
     * </p>
     *
     * @return the manifestBuilder
     */
    public CollectionBuilder getCollectionBuilder() {
        if (this.collectionBuilder == null) {
            try {
                this.collectionBuilder = new CollectionBuilder(new ApiUrls(DataManager.getInstance().getConfiguration().getRestApiUrl()));
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return collectionBuilder;
    }

}
