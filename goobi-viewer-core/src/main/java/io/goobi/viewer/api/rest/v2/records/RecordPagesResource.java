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
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_MANIFEST;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_MEDIA;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_TEXT;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.v3.Canvas3;
import de.intranda.api.iiif.presentation.v3.Collection3;
import de.intranda.api.iiif.presentation.v3.Manifest3;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.bindings.AccessRightsBinding;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.iiif.presentation.v3.builder.CanvasBuilder;
import io.goobi.viewer.model.iiif.presentation.v3.builder.ManifestBuilder;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Florian Alpers
 */
@jakarta.ws.rs.Path(RECORDS_PAGES)
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

    /**
     * Returns the IIIF 3.0 canvas for a single page.
     *
     * <p>The canvas links to the page's comment and crowdsourcing annotation pages, adds the fulltext annotation page only if the page has
     * fulltext, and, if enabled in the configuration, links to the record's page view in this viewer instance. Access requires the record's
     * basic list permission.
     *
     * @return the {@link Canvas3} for the requested page
     * @throws ContentNotFoundException if no page exists at the given order for the record
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_CANVAS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 3.0 canvas for page",
            description = "The canvas links to the page's comment and crowdsourcing annotation pages, adds the fulltext annotation page"
                    + " only if the page has fulltext, and, if enabled in the configuration, links to the record's page view in this viewer"
                    + " instance. Access requires the record's basic list permission.")
    @ApiResponse(responseCode = "200", description = "IIIF 3.0 canvas for the given page",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Canvas3.class)))
    @ApiResponse(responseCode = "400", description = "Invalid page number — must be a valid integer")
    @ApiResponse(responseCode = "403", description = "Record found but access is restricted")
    @ApiResponse(responseCode = "404", description = "Record or page not found")
    @IIIFPresentationBinding
    public IPresentationModelElement getCanvas()
            throws PresentationException, IndexUnreachableException, URISyntaxException, ContentLibException, DAOException {
        return new CanvasBuilder(urls, this.servletRequest).build(pi, pageNo);
    }

    /**
     * Returns the annotation page containing the page's media resources.
     *
     * <p>The media annotation page of the page's canvas is returned. The response is 404 if the IIIF content API is configured with a
     * different base URL than this REST API. Access requires the record's basic list permission.
     *
     * @return the {@link AnnotationPage} containing the page's media resources
     * @throws ContentNotFoundException if the page has no matching media annotation page (e.g. the page is not an image)
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_MEDIA)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get media resources for page",
            description = "The media annotation page of the page's canvas is returned. The response is 404 if the IIIF content API is"
                    + " configured with a different base URL than this REST API. Access requires the record's basic list permission.")
    @ApiResponse(responseCode = "200", description = "Annotation page containing media resources for the given page", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid page number — must be a valid integer")
    @ApiResponse(responseCode = "403", description = "Record found but access is restricted")
    @ApiResponse(responseCode = "404", description = "Record, page, or media annotations not found")
    @IIIFPresentationBinding
    public AnnotationPage getMedia()
            throws PresentationException, IndexUnreachableException, URISyntaxException, ContentLibException, DAOException {
        URI itemId = urls.path(RECORDS_PAGES, RECORDS_PAGES_MEDIA).params(pi, pageNo).buildURI();
        return new CanvasBuilder(urls, this.servletRequest).build(pi, pageNo)
                .getItems()
                .stream()
                .filter(p -> p.getId().equals(itemId))
                .findAny()
                .orElseThrow(() -> new ContentNotFoundException(String.format("No media annotations found for page %d in %s", pageNo, pi)));
    }

    /**
     * Returns a single media annotation for a page by its identifier.
     *
     * <p>The page's canvas is built and searched, across all of its annotation pages, for a single annotation whose identifier matches
     * this endpoint's own URL. Access requires the record's basic list permission.
     *
     * @param itemId identifier string of the annotation
     * @return the {@link IAnnotation} matching the given identifier
     * @throws ContentNotFoundException if no matching annotation exists
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_MEDIA + "/{itemid}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get a single media annotation for a page by its identifier",
            description = "The page's canvas is built and searched, across all of its annotation pages, for a single annotation whose"
                    + " identifier matches this endpoint's own URL. Access requires the record's basic list permission.")
    @ApiResponse(responseCode = "200", description = "The media annotation for the given identifier",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebAnnotation.class)))
    @ApiResponse(responseCode = "400", description = "Invalid page number — must be a valid integer")
    @ApiResponse(responseCode = "403", description = "Record found but access is restricted")
    @ApiResponse(responseCode = "404", description = "Record, page, or media annotation not found")
    @IIIFPresentationBinding
    public IAnnotation getMediaItem(
            @Parameter(description = "Identifier string of the annotation") @PathParam("itemid") String itemId)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ContentLibException, DAOException {
        URI itemUrl = urls.path(RECORDS_PAGES, RECORDS_PAGES_MEDIA, "/" + itemId).params(pi, pageNo).buildURI();
        return new CanvasBuilder(urls, this.servletRequest).build(pi, pageNo)
                .getItems()
                .stream()
                .flatMap(p -> p.getItems().stream())
                .filter(p -> p.getId().equals(itemUrl))
                .findAny()
                .orElseThrow(() -> new ContentNotFoundException(String.format("No media annotation found for page %d in %s", pageNo, pi)));
    }

    /**
     * Returns the fulltext of a page as IIIF 3.0 annotations.
     *
     * <p>If the page has an ALTO file, one annotation is created per text line; otherwise the page's plain fulltext, if any, is returned
     * as a single annotation. If the page has neither, an empty annotation page is returned instead of an error. Access requires the
     * page's fulltext view permission.
     *
     * @return the {@link AnnotationPage} of fulltext annotations for the page
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_TEXT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get fulltext annotations for page",
            description = "If the page has an ALTO file, one annotation is created per text line; otherwise the page's plain fulltext, if"
                    + " any, is returned as a single annotation. If the page has neither, an empty annotation page is returned instead of"
                    + " an error. Access requires the page's fulltext view permission.")
    @ApiResponse(responseCode = "200", description = "Annotation page containing fulltext annotations for the given page",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid page number — must be a valid integer")
    @ApiResponse(responseCode = "403", description = "Access to fulltext for this record is restricted")
    @ApiResponse(responseCode = "404", description = "Record or page not found")
    @AccessRightsBinding({ IPrivilegeHolder.PRIV_VIEW_FULLTEXT })
    public AnnotationPage getFulltext()
            throws PresentationException, IndexUnreachableException, URISyntaxException, ContentLibException, DAOException {
        return new CanvasBuilder(urls, this.servletRequest).buildFulltextAnnotations(pi, pageNo);
    }

    /**
     * Returns the crowdsourcing annotations created for a single page.
     *
     * <p>Annotations are read from the database, not the Solr index, and delivered as a W3C web annotation page. This includes every
     * crowdsourcing motivation (e.g. describing, commenting, tagging), which distinguishes it from the dedicated comments endpoint. Access
     * requires the page's view-user-generated-content permission.
     *
     * @return the {@link AnnotationPage} for the requested page
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_ANNOTATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List annotations for a page",
            description = "Annotations are read from the database, not the Solr index, and delivered as a W3C web annotation page. This"
                    + " includes every crowdsourcing motivation (e.g. describing, commenting, tagging), which distinguishes it from the"
                    + " dedicated comments endpoint. Access requires the page's view-user-generated-content permission.")
    @ApiResponse(responseCode = "200", description = "Annotation page containing annotations for the given page", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid page number — must be a valid integer")
    @ApiResponse(responseCode = "403", description = "Access to user-generated content for this record is restricted")
    @AccessRightsBinding({ IPrivilegeHolder.PRIV_VIEW_UGC })
    public AnnotationPage getAnnotationsForRecord() throws DAOException {

        ApiPath apiPath = urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(pi, pageNo);

        URI uri = URI.create(apiPath.build());
        AnnotationPage annoPage = new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationCollectionForPage(pi, pageNo, uri).getFirst();
        if (annoPage != null) {
            return annoPage;
        }
        return new AnnotationPage(uri);
    }

    /**
     * Returns the comments left on a single page.
     *
     * <p>Comments are read from the database and delivered as a W3C web annotation page, one annotation per comment. Unlike the
     * annotations endpoint, this only returns simple page comments, not other crowdsourcing motivations. Access requires the page's
     * view-user-generated-content permission.
     *
     * @return the {@link AnnotationPage} of comments for the requested page
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_COMMENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    @AccessRightsBinding({ IPrivilegeHolder.PRIV_VIEW_UGC })
    @Operation(tags = { "records", "annotations" }, summary = "List comments for a page",
            description = "Comments are read from the database and delivered as a W3C web annotation page, one annotation per comment."
                    + " Unlike the annotations endpoint, this only returns simple page comments, not other crowdsourcing motivations."
                    + " Access requires the page's view-user-generated-content permission.")
    @ApiResponse(responseCode = "200", description = "Annotation page containing comments for the given page", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid page number — must be a valid integer")
    @ApiResponse(responseCode = "403", description = "Access to user-generated content for this record is restricted")
    public AnnotationPage getCommentsForPage() throws DAOException {
        ApiPath apiPath = urls.path(RECORDS_PAGES, RECORDS_PAGES_COMMENTS).params(pi, pageNo);
        URI uri = URI.create(apiPath.build());
        return new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationPageForPageComments(pi, pageNo, uri);
    }

    /**
     * Returns the IIIF 3.0 manifest for the record, limited to a single page.
     *
     * <p>Only the requested page is added to the manifest as a canvas, instead of every page of the record, and the manifest's thumbnail
     * is taken from that page; the manifest's own metadata still describes the whole record. Requesting a page for an anchor record,
     * which has no pages of its own, is rejected instead of falling back to the anchor's collection. Access requires the record's basic
     * list permission. The {@code mode} query parameter has no effect on this endpoint.
     *
     * @param mode accepted but not evaluated by this endpoint; the manifest is always built with its default resource set
     * @return the {@link IPresentationModelElement} for the record
     * @throws ContentLibException if the record is an anchor record without pages of its own
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_MANIFEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 3.0 manifest for record starting at the given page",
            description = "Only the requested page is added to the manifest as a canvas, instead of every page of the record, and the"
                    + " manifest's thumbnail is taken from that page; the manifest's own metadata still describes the whole record."
                    + " Requesting a page for an anchor record, which has no pages of its own, is rejected instead of falling back to the"
                    + " anchor's collection. Access requires the record's basic list permission. The 'mode' query parameter has no effect"
                    + " on this endpoint.")
    @ApiResponse(responseCode = "200", description = "IIIF 3.0 manifest for the given record",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(oneOf = { Manifest3.class, Collection3.class })))
    @ApiResponse(responseCode = "400", description = "Invalid page number — must be a valid integer")
    @ApiResponse(responseCode = "403", description = "Record found but access is restricted")
    @ApiResponse(responseCode = "404", description = "Record or page not found")
    @IIIFPresentationBinding
    public IPresentationModelElement getManifest(
            @Parameter(
                    description = "Build mode for manifest to select type of resources to include."
                            + " Default is 'iiif' which returns the full IIIF manifest with all resources."
                            + " 'thumbs' Does not read width and height of canvas resources and 'iiif_simple'"
                            + " ignores all resources from files") @QueryParam("mode") String mode)
            throws PresentationException, IndexUnreachableException, URISyntaxException, DAOException, ContentLibException {
        return new ManifestBuilder(urls, servletRequest).build(pi, pageNo);
    }

}
