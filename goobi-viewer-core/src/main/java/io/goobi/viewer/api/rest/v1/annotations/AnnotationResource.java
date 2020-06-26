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
package io.goobi.viewer.api.rest.v1.annotations;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;

import java.net.URISyntaxException;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.Collection;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.ContentAssistResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.IIIFPresentationResourceBuilder;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(ANNOTATIONS)
@ViewerRestServiceBinding
public class AnnotationResource {
        
    
    @Inject
    private AbstractApiUrlManager urls;

    public AnnotationResource() {
    }
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations"}, summary = "Get an annotation collection over all annotations")
    public AnnotationCollection getAnnotationCollection()
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException, URISyntaxException, ViewerConfigurationException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls);
        AnnotationCollection collection = builder.getWebnnotationCollection();
        return collection;
    }
    
    @GET
    @javax.ws.rs.Path("/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations"}, summary = "Get a page within the annotation collection over all annotations")
    @ApiResponse(responseCode="404", description="If the page number is out of bounds")
    public AnnotationPage getAnnotationCollectionPage(
            @PathParam("page") Integer page)
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException, URISyntaxException, ViewerConfigurationException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls);
        AnnotationPage annoPage = builder.getWebAnnotationPage(page);
        return annoPage;
    }
    
    @GET
    @javax.ws.rs.Path(ANNOTATIONS_ANNOTATION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations"}, summary = "Get an annotation by its identifier")
    @ApiResponse(responseCode="404", description="If the page number is out of bounds")
    public IAnnotation getAnnotation(
            @Parameter(description="Identifier of the annotation")@PathParam("id") Long id)
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException, URISyntaxException, ViewerConfigurationException {
        PersistentAnnotation anno = DataManager.getInstance().getDao().getAnnotation(id);
        if(anno != null) {            
            AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls);
            WebAnnotation wa = builder.getAsWebAnnotation(anno);
            return wa;
        } else {
            throw new ContentNotFoundException("Not annotation with id = " + id + "found");
        }
    }

    
    
}
