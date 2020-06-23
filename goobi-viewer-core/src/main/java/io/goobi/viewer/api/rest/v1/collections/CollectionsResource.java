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
package io.goobi.viewer.api.rest.v1.collections;

import static io.goobi.viewer.api.rest.v1.ApiUrls.COLLECTIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.COLLECTIONS_COLLECTION;

import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.iiif.presentation.Collection;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.resourcebuilders.IIIFPresentationResourceBuilder;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(COLLECTIONS)
@ViewerRestServiceBinding
public class CollectionsResource {
    
    private String solrField;
    
    @Inject
    AbstractApiUrlManager urls;

    public CollectionsResource(
            @Parameter(description="Name of the SOLR field the collection is based on. Typically 'DC'")@PathParam("field")String solrField
            ) {
        this.solrField = solrField;
    }
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get all collections as IIIF presentation collection")
    public Collection getAllCollections(
            @Parameter(description ="Add values of this field to response to allow grouping of results")@QueryParam("grouping")String grouping
                    )
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException, URISyntaxException, ViewerConfigurationException {
        IIIFPresentationResourceBuilder builder = new IIIFPresentationResourceBuilder(urls);
        if(StringUtils.isBlank(grouping)) {            
            return builder.getCollections(solrField);
        } else {
            return builder.getCollectionsWithGrouping(solrField, grouping);
        }
    }
    
    @GET
    @javax.ws.rs.Path(COLLECTIONS_COLLECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get given collection as a IIIF presentation collection")
    public Collection getCollection(
            @Parameter(description="Name of the collection. Must be a value of the SOLR field the collection is based on")@PathParam("collection")String collection,
            @Parameter(description ="Add values of this field to response to allow grouping of results")@QueryParam("grouping")String grouping
            )
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException, URISyntaxException, ViewerConfigurationException {
        IIIFPresentationResourceBuilder builder = new IIIFPresentationResourceBuilder(urls);
        if(StringUtils.isBlank(grouping)) {                   
            return builder.getCollection(solrField, collection);
        } else {
          return builder.getCollectionWithGrouping(solrField, collection, grouping);
        }
    }
    
}
