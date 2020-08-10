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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_COMMENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_COMMENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_NER_TAGS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_SEQUENCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.IAnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.CollectionExtent;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.Sequence;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.ner.DocumentReference;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.IIIFPresentationResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.NERBuilder;
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
@javax.ws.rs.Path(RECORDS_PAGES)
@ViewerRestServiceBinding
@CORSBinding
public class RecordPageResource {

    private static final Logger logger = LoggerFactory.getLogger(RecordResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private AbstractApiUrlManager urls;

    private final String pi;

    public RecordPageResource(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) {
        this.pi = pi;
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_NER_TAGS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records" }, summary = "Get NER tags for a single page")
    public DocumentReference getNERTags(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo,
            @Parameter(description = "Tag type to consider (person, coorporation, event or location)") @QueryParam("type") String type)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        NERBuilder builder = new NERBuilder(urls);
        return builder.getNERTags(pi, type, pageNo, pageNo, 1, servletRequest);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_SEQUENCE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF base sequence")
    @IIIFPresentationBinding
    public IPresentationModelElement getSequence()
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException,
            ViewerConfigurationException, DAOException, IllegalRequestException {
        IIIFPresentationResourceBuilder builder = new IIIFPresentationResourceBuilder(urls);
        Sequence sequence = builder.getBaseSequence(pi);
        return sequence;
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_CANVAS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF canvas for a page")
    @IIIFPresentationBinding
    public IPresentationModelElement getCanvas(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException,
            ViewerConfigurationException, DAOException, IllegalRequestException {
        IIIFPresentationResourceBuilder builder = new IIIFPresentationResourceBuilder(urls);
        return builder.getCanvas(pi, pageNo);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_ANNOTATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations"}, summary = "List annotations for a page")
    public IAnnotationCollection getAnnotationsForRecord(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo,
            @Parameter(description = "annotation format of the response. If it is 'oa' the comments will be delivered as OpenAnnotations, otherwise as W3C-Webannotations") @QueryParam("format") String format)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {

        ApiPath apiPath = urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(pi, pageNo);
        if ("oa".equalsIgnoreCase(format)) {
            URI uri = URI.create(apiPath.query("format", "oa").build());
            return new AnnotationsResourceBuilder(urls).getOAnnotationListForPage(pi, pageNo, uri);
        } else {
            URI uri = URI.create(apiPath.build());
            return new AnnotationsResourceBuilder(urls).getWebAnnotationCollectionForPage(pi, pageNo, uri);
        }

    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_ANNOTATIONS + "/{page}/")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponse(responseCode="400", description="If the page number is out of bounds")
    public AnnotationPage getAnnotationPageForRecord(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo,
            @PathParam("page") Integer page)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException, IllegalRequestException {

        URI uri = URI.create(urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi).build());
        return new AnnotationsResourceBuilder(urls).getWebAnnotationPageForPage(pi, pageNo, uri, page);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_COMMENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations"}, summary = "List comments for a page")
    public IAnnotationCollection getCommentsForPage(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo,
            @Parameter(
                    description = "annotation format of the response. If it is 'oa' the comments will be delivered as OpenAnnotations, otherwise as W3C-Webannotations") @QueryParam("format") String format)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi);
        if ("oa".equalsIgnoreCase(format)) {
            URI uri = URI.create(apiPath.query("format", "oa").build());
            return new AnnotationsResourceBuilder(urls).getOAnnotationListForPageComments(pi, pageNo, uri);
        } else {
            URI uri = URI.create(apiPath.build());
            return new AnnotationsResourceBuilder(urls).getWebAnnotationCollectionForPageComments(pi, pageNo, uri);
        }
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_COMMENTS + "/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponse(responseCode="400", description="If the page number is out of bounds")
    public AnnotationPage getCommentPageForRecord(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo,
            @PathParam("page") Integer page)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException, IllegalRequestException {

        URI uri = URI.create(urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi).build());
        return new AnnotationsResourceBuilder(urls).getWebAnnotationPageForPageComments(pi, pageNo, uri, page);
    }

    
}
