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
package io.goobi.viewer.api.rest.v2.records;

import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_COMMENTS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_MEDIA;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_TEXT;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.iiif.presentation.v2.builder.WebAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.v3.builder.CanvasBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(RECORDS_PAGES)
@ViewerRestServiceBinding
@CORSBinding
public class RecordPagesResource {

    private static final Logger logger = LogManager.getLogger(RecordPagesResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private ApiUrls urls;

    private final String pi;
    private final Integer pageNo;

    public RecordPagesResource(@Context HttpServletRequest request,
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Order of the page") @PathParam("pageNo") Integer pageNo) {
        this.pi = pi;
        this.pageNo = pageNo;
        request.setAttribute(FilterTools.ATTRIBUTE_PI, pi);
        request.setAttribute(FilterTools.ATTRIBUTE_PAGENO, pageNo);

    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_CANVAS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 3.0 canvas for page")
    @IIIFPresentationBinding
    public IPresentationModelElement getCanvas()
            throws PresentationException, IndexUnreachableException, URISyntaxException, ContentLibException {
        return new CanvasBuilder(urls).build(pi, pageNo);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_MEDIA)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get media resources for page")
    @IIIFPresentationBinding
    public AnnotationPage getMedia()
            throws PresentationException, IndexUnreachableException, URISyntaxException, ContentLibException {
        URI itemId = urls.path(RECORDS_PAGES, RECORDS_PAGES_MEDIA).params(pi, pageNo).buildURI();
        return new CanvasBuilder(urls).build(pi, pageNo)
                .getItems()
                .stream()
                .filter(p -> p.getId().equals(itemId))
                .findAny()
                .orElseThrow(() -> new ContentNotFoundException(String.format("No media annotations found for page %d in %s", pageNo, pi)));
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_MEDIA + "/{itemid}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get media resources for page")
    @IIIFPresentationBinding
    public IAnnotation getMediaItem(
            @Parameter(description = "Identifier string of the annotation") @PathParam("itemid") String itemId)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ContentLibException {
        URI itemUrl = urls.path(RECORDS_PAGES, RECORDS_PAGES_MEDIA, "/" + itemId).params(pi, pageNo).buildURI();
        return new CanvasBuilder(urls).build(pi, pageNo)
                .getItems()
                .stream()
                .flatMap(p -> p.getItems().stream())
                .filter(p -> p.getId().equals(itemUrl))
                .findAny()
                .orElseThrow(() -> new ContentNotFoundException(String.format("No media annotation found for page %d in %s", pageNo, pi)));
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_TEXT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get fulltext annotations for page")
    @IIIFPresentationBinding
    public AnnotationPage getFulltext()
            throws PresentationException, IndexUnreachableException, URISyntaxException, ContentLibException {
        return new CanvasBuilder(urls).buildFulltextAnnotations(pi, pageNo);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_ANNOTATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List annotations for a page")
    public AnnotationPage getAnnotationsForRecord() throws DAOException {

        ApiPath apiPath = urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(pi, pageNo);

        URI uri = URI.create(apiPath.build());
        AnnotationPage annoPage = new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationCollectionForPage(pi, pageNo, uri).getFirst();
        if (annoPage != null) {
            return annoPage;
        } else {
            return new AnnotationPage(uri);
        }
        //        return new WebAnnotationBuilder(urls).getCrowdsourcingAnnotationCollection(uri, pi, pageNo, false);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_COMMENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List comments for a page")
    public AnnotationPage getCommentsForPage() throws DAOException {
        ApiPath apiPath = urls.path(RECORDS_PAGES, RECORDS_PAGES_COMMENTS).params(pi, pageNo);
        URI uri = URI.create(apiPath.build());
        return new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationPageForPageComments(pi, pageNo, uri);
    }

}
