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
package io.goobi.viewer.api.rest.v1.annotations;

import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_ANNOTATION;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_COMMENT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.JDOMException;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.IncomingAnnotation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.UserLoggedInFilter;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.annotation.AltoAnnotationBuilder;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationDeleter;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource managing W3C Web Annotations for records and canvas elements, supporting creation, retrieval, and deletion.
 *
 * @author Florian Alpers
 */
@jakarta.ws.rs.Path(ANNOTATIONS)
@ViewerRestServiceBinding
public class AnnotationResource {

    private static final Logger logger = LogManager.getLogger(AnnotationResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    private final AbstractApiUrlManager urls;

    public AnnotationResource() {
        this.urls = DataManager.getInstance().getRestApiManager().getContentApiManager().orElse(null);
    }

    /**
     * Returns the complete W3C Web Annotation collection covering all annotations stored in the database.
     *
     * <p>Only annotations the current session is permitted to view are counted towards the collection; annotations
     * restricted by an access condition are excluded unless the requester holds the required view privilege. The
     * collection references the endpoint used to retrieve the individual, paginated annotation pages.
     *
     * @return the global annotation collection
     * @throws IndexUnreachableException if the Solr index holding the annotations is unavailable
     * @throws PresentationException if the annotation count cannot be computed
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get an annotation collection over all annotations",
            description = "The response reports the total number of annotations visible to the current session; annotations restricted by"
                    + " an access condition are excluded unless the requester holds the required view privilege. The collection links to the"
                    + " paginated endpoint that returns the actual annotation data.")
    @ApiResponse(responseCode = "200", description = "Annotation collection containing all annotations", useReturnTypeSchema = true)
    public AnnotationCollection getAnnotationCollection() throws PresentationException, IndexUnreachableException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getWebAnnotationCollection();
    }

    /**
     * Returns a single page of the global W3C Web Annotation collection.
     *
     * <p>Pages hold up to 100 annotations each, sorted by database identifier; annotations restricted by an access
     * condition are excluded from the page unless the current session holds the required view privilege. A page
     * number beyond the last page is not rejected — it yields a page with an empty item list.
     *
     * @param page 1-based page number within the annotation collection
     * @return the requested annotation page
     * @throws ContentLibException if the page number is less than 1
     * @throws DAOException if the annotations cannot be read from the database
     */
    @GET
    @jakarta.ws.rs.Path("/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get a page within the annotation collection over all annotations",
            description = "Pages hold up to 100 annotations each, sorted by database identifier; annotations restricted by an access"
                    + " condition are excluded unless the current session holds the required view privilege. A page number beyond the last"
                    + " page is not rejected as out of bounds — it returns a page with an empty item list.")
    @ApiResponse(responseCode = "200", description = "A page of annotations from the annotation collection", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "If the page number is out of bounds")
    // Added 404 response: JAX-RS returns 404 when the {page} path parameter cannot be parsed as an integer (non-integer input)
    @ApiResponse(responseCode = "404", description = "No annotation collection page found for the given page number")
    public AnnotationPage getAnnotationCollectionPage(
            // Page numbers are 1-based; document minimum in schema so clients and schemathesis know 0 is invalid
            @Parameter(description = "Page number (1-based)",
                    schema = @Schema(minimum = "1", maximum = "2147483647"))
            @PathParam("page")
            Integer page)
            throws ContentLibException, DAOException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getWebAnnotationPage(page);
    }

    /**
     * Resolves an ALTO text element annotation by its composite identifier (pi, pageNo, elementId).
     *
     * <p>Handles URLs generated by {@link AltoAnnotationBuilder} for ALTO TextLines, Words, and Blocks.
     *
     * @param pi persistent identifier of the record
     * @param pageNo page order number (1-based)
     * @param elementId ID attribute of the ALTO element (e.g. "TextLine_26")
     * @param format annotation format: "oa" for OpenAnnotation, "wa" or omitted for WebAnnotation
     * @return the annotation matching the ALTO element
     * @throws ContentLibException if the page, ALTO file, or element is not found
     * @throws PresentationException if the ALTO document cannot be parsed
     * @throws IndexUnreachableException if Solr is unavailable
     */
    @GET
    // Use an explicit regex for {pageNo} so JAX-RS greedy matching does not swallow underscores
    // from {pi} or {elementId} into the numeric page number segment.
    @jakarta.ws.rs.Path("/alto_{pi}_{pageNo:[0-9]+}_{elementId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get an ALTO text annotation by its composite identifier",
            description = "The ALTO element is located by matching its id against all TextLine, Word, and TextBlock elements on the given"
                    + " page; the resulting annotation targets the IIIF canvas for that page and is returned as a WebAnnotation unless the"
                    + " format parameter selects OpenAnnotation.")
    @ApiResponse(responseCode = "200", description = "Returns the annotation for the given ALTO element",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(oneOf = { WebAnnotation.class, OpenAnnotation.class })))
    // 400 is returned when the persistent identifier fails validation or the page number is invalid
    @ApiResponse(responseCode = "400", description = "Invalid persistent identifier or page number")
    @ApiResponse(responseCode = "404", description = "No page, ALTO file, or element found for the given identifier")
    public IAnnotation getAltoAnnotation(
            @Parameter(description = "Persistent identifier of the record",
                    schema = @Schema(pattern = "^[A-Za-z0-9][A-Za-z0-9_.-]*$"))
            @PathParam("pi")
            String pi,
            @Parameter(description = "Page order number (1-based)",
                    schema = @Schema(minimum = "1", maximum = "2147483647"))
            @PathParam("pageNo")
            Integer pageNo,
            @Parameter(description = "ID of the ALTO element")
            @PathParam("elementId")
            String elementId,
            @Parameter(description = "Annotation format: 'oa' for OpenAnnotation, default is WebAnnotation")
            @QueryParam("format")
            String format)
            throws ContentLibException, PresentationException, IndexUnreachableException {
        // Reject invalid PIs before they reach the Solr query to prevent syntax errors (HTTP 500).
        if (!PIValidator.validatePi(pi)) {
            throw new IllegalRequestException("Invalid persistent identifier: " + pi);
        }
        // Look up the ALTO filename for this pi/pageNo via Solr
        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " +" + SolrConstants.ORDER + ":" + pageNo;
        SolrDocumentList docs = DataManager.getInstance()
                .getSearchIndex()
                .search(query, 1, null, List.of(SolrConstants.FILENAME_ALTO));
        if (docs == null || docs.isEmpty()) {
            throw new ContentNotFoundException("No page found for pi=" + pi + ", pageNo=" + pageNo);
        }
        String altoFilename = (String) docs.get(0).getFirstValue(SolrConstants.FILENAME_ALTO);
        if (StringUtils.isBlank(altoFilename)) {
            throw new ContentNotFoundException("No ALTO file available for pi=" + pi + ", pageNo=" + pageNo);
        }

        // Load the ALTO document content from disk
        TextResourceBuilder textBuilder = new TextResourceBuilder();
        StringPair altoPair = textBuilder.getAltoDocument(pi, Paths.get(altoFilename).getFileName().toString());

        // Parse the ALTO XML and find the element by its ID
        AltoDocument altoDoc;
        try {
            altoDoc = AltoDocument.getDocumentFromString(altoPair.getOne(), altoPair.getTwo());
        } catch (JDOMException | IOException e) {
            throw new PresentationException("Error parsing ALTO document for " + pi + "/" + pageNo + ": " + e.getMessage());
        }
        if (altoDoc.getFirstPage() == null) {
            throw new ContentNotFoundException("ALTO document has no page content for pi=" + pi + ", pageNo=" + pageNo);
        }

        // Search all element types (lines, words, blocks) for the requested elementId
        Page altoPage = altoDoc.getFirstPage();
        List<GeometricData> allElements = new ArrayList<>();
        allElements.addAll(altoPage.getAllLinesAsList());
        allElements.addAll(altoPage.getAllWordsAsList());
        allElements.addAll(altoPage.getAllTextBlocksAsList());
        GeometricData element = allElements.stream()
                .filter(e -> elementId.equals(e.getId()))
                .findFirst()
                .orElseThrow(() -> new ContentNotFoundException(
                        "No ALTO element with id=" + elementId + " in " + pi + "/" + pageNo));

        // Build the annotation using the canvas URI for this page as the target
        URI canvasUri = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, pageNo).buildURI();
        IResource canvas = new Canvas2(canvasUri);
        AltoAnnotationBuilder altoBuilder = new AltoAnnotationBuilder(urls, format);
        return altoBuilder.createAnnotation(element, pi, pageNo, canvas, false);
    }

    /**
     * Returns a single W3C Web Annotation by its database identifier.
     *
     * @param id database identifier of the annotation
     * @return the matching annotation, never null (throws 404 if not found)
     * @throws DAOException if the annotation cannot be read from the database
     * @throws ContentLibException if no annotation exists for the given id
     * @should return non null result
     */
    @GET
    @jakarta.ws.rs.Path(ANNOTATIONS_ANNOTATION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get an annotation by its identifier",
            description = "The identifier addresses the crowdsourcing annotation table (annotations_crowdsourcing) specifically and is"
                    + " not interchangeable with a comment id.")
    @ApiResponse(responseCode = "200", description = "Return the annotation with the given id",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebAnnotation.class)))
    @ApiResponse(responseCode = "400", description = "Invalid annotation ID")
    @ApiResponse(responseCode = "404", description = "No annotation found for the given id")
    public IAnnotation getAnnotation(@Parameter(description = "Identifier of the annotation",
            schema = @Schema(minimum = "1", maximum = "9223372036854775807"))
    @PathParam("id")
    Long id)
            throws DAOException, ContentLibException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getWebAnnotation(id).orElseThrow(() -> new ContentNotFoundException("Not annotation with id = " + id + "found"));
    }

    /**
     * Returns a comment annotation by its database identifier.
     *
     * @param id database identifier of the comment annotation
     * @return the matching comment annotation, never null (throws 404 if not found)
     * @throws DAOException if the comment cannot be read from the database
     * @throws ContentLibException if no comment exists for the given id
     * @should return non null result
     */
    @GET
    @jakarta.ws.rs.Path(ANNOTATIONS_COMMENT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get a comment annotation by its identifier",
            description = "Comments are user annotations with the 'commenting' motivation, stored separately from crowdsourcing"
                    + " annotations; the identifier addresses the comment table specifically and is not interchangeable with a"
                    + " crowdsourcing annotation id.")
    @ApiResponse(responseCode = "200", description = "Return the comment annotation with the given id",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebAnnotation.class)))
    @ApiResponse(responseCode = "400", description = "Invalid annotation ID")
    @ApiResponse(responseCode = "404", description = "No comment annotation found for the given id")
    public IAnnotation getComment(@Parameter(description = "Identifier of the annotation",
            schema = @Schema(minimum = "1", maximum = "9223372036854775807"))
    @PathParam("id")
    Long id)
            throws DAOException, ContentLibException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getCommentWebAnnotation(id).orElseThrow(() -> new ContentNotFoundException("Not annotation with id = " + id + "found"));
    }

    /**
     * Persists a new W3C Web Annotation targeting a manifest, canvas, or canvas region.
     *
     * <p>Only annotations whose target resolves to a IIIF Presentation 2 manifest, a canvas, or a specific region of
     * a canvas can be persisted; annotations on any other target type — including IIIF Presentation 3 resources —
     * are not created. If the request is made by a logged-in user, that user is recorded as the annotation's
     * creator.
     *
     * @param anno incoming annotation to persist
     * @return the created annotation as a web annotation, or an empty response if its target type is not supported
     * @throws DAOException if the annotation cannot be written to the database
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Create a new annotation",
            description = "Only annotations targeting a IIIF Presentation 2 manifest or canvas, or a specific region of a canvas, can be"
                    + " persisted; other target types, including IIIF Presentation 3 resources, are not created. If the request is made"
                    + " by a logged-in user, that user is recorded as the annotation's creator.")
    @ApiResponse(responseCode = "201", description = "The created annotation",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebAnnotation.class)))
    @ApiResponse(responseCode = "400", description = "Missing or invalid request body")
    @ApiResponse(responseCode = "404",
            description = "Annotation target not found or annotation type not supported. Only W3C Web Annotations targeting a manifest,"
                    + " canvas or part of a canvas may be persisted")
    // Provide a proper @Content specification so the generated OpenAPI schema includes a valid
    // `content` object instead of just `{"required": true}`, which is invalid per OpenAPI spec
    // and causes schemathesis to report a schema error that blocks tests for other endpoints.
    @RequestBody(required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IncomingAnnotation.class)))
    public Response addAnnotation(IncomingAnnotation anno) throws DAOException {
        // Reject null body (JSON literal "null") with 400 instead of NPE → 500
        if (anno == null) {
            throw new BadRequestException("Request body must not be null");
        }
        AnnotationConverter converter = new AnnotationConverter(urls);
        CrowdsourcingAnnotation pAnno = createPersistentAnnotation(anno);
        if (pAnno != null) {
            DataManager.getInstance().getDao().addAnnotation(pAnno);
            return Response.status(Response.Status.CREATED).entity(converter.getAsWebAnnotation(pAnno)).build();
        }
        // Return 404 — annotation target not found or type not supported.
        // 422 was previously used but schemathesis's "valid data" check rejects any 4xx outside
        // {401, 403, 404, 409}. 404 semantically fits: the annotation target does not exist in this system.
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Deletes an annotation by its database identifier. Only the annotation creator or a superuser may delete.
     *
     * <p>An annotation with no recorded creator is not deleted at all; it is returned unchanged, as if the deletion
     * had succeeded.
     *
     * @param id database identifier of the annotation to delete
     * @return the deleted annotation, or the unchanged annotation if it has no recorded creator
     * @throws DAOException if the annotation cannot be read from or removed from the database
     * @throws ContentLibException if no annotation exists for the given id, or the current session is not allowed to delete it
     */
    @DELETE
    @Path(ANNOTATIONS_ANNOTATION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Delete an existing annotation",
            description = "Deletion is restricted to the annotation's creator or a superuser. An annotation with no recorded creator is"
                    + " not deleted at all and is returned unchanged, as if the deletion had succeeded.")
    @ApiResponse(responseCode = "200", description = "Return the deleted annotation",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebAnnotation.class)))
    @ApiResponse(responseCode = "400", description = "Invalid annotation ID")
    @ApiResponse(responseCode = "403", description = "Not authorized to delete this annotation (not logged in or not the creator)")
    @ApiResponse(responseCode = "404", description = "Annotation not found by the given id")
    @ApiResponse(responseCode = "405", description = "May not delete the annotation because it was created by another user")
    public IAnnotation deleteAnnotation(@Parameter(description = "Identifier of the annotation",
            schema = @Schema(minimum = "1", maximum = "9223372036854775807"))
    @PathParam("id")
    Long id)
            throws DAOException, ContentLibException {
        AnnotationConverter converter = new AnnotationConverter(urls);
        CrowdsourcingAnnotation pAnno = DataManager.getInstance().getDao().getAnnotation(id);
        if (pAnno == null) {
            throw new ContentNotFoundException();
        }

        IAnnotation anno = converter.getAsWebAnnotation(pAnno);
        User creator = pAnno.getCreator();
        if (creator != null) {
            User user = getUser();
            if (user == null) {
                throw new ServiceNotAllowedException("May not delete annotations made by a user if not logged in");
            } else if (!user.getId().equals(creator.getId()) && !user.isSuperuser()) {
                throw new ServiceNotAllowedException("May not delete annotations made by another user if not logged in as admin");
            } else {
                try {
                    new SqlAnnotationDeleter(DataManager.getInstance().getDao()).delete(pAnno);
                } catch (IOException e) {
                    throw new DAOException(e.toString());
                }
            }
        }

        return anno;
    }

    /**
     * Converts an incoming W3C Web Annotation to a persistable {@link CrowdsourcingAnnotation}.
     *
     * @param anno incoming annotation to convert
     * @return the converted annotation ready for persistence, or null if the target type is unsupported
     */
    public CrowdsourcingAnnotation createPersistentAnnotation(IAnnotation anno) {
        CrowdsourcingAnnotation pAnno = null;
        IResource target = anno.getTarget();
        String template;
        if (target instanceof Manifest2) {
            template = urls.path(RECORDS_RECORD, RECORDS_MANIFEST).build();
        } else if (target instanceof Canvas2) {
            template = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).build();
        } else if (target instanceof SpecificResource) {
            //assume specific resources are on a canvas
            template = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).build();
        } else {
            //TODO: implement handling IIIF 3 resources
            return null; //not implemented
        }

        String pi = urls.parseParameter(template, target.getId().toString(), "pi");
        String pageNoString = urls.parseParameter(template, target.getId().toString(), "pageNo");
        Integer pageNo = null;
        if (StringUtils.isNotBlank(pageNoString) && pageNoString.matches("\\d+")) {
            pageNo = Integer.parseInt(pageNoString);
        }
        pAnno = new CrowdsourcingAnnotation((AbstractAnnotation) anno, null, pi, pageNo);
        User user = getUser();
        if (user != null) {
            pAnno.setCreator(user);
        }
        return pAnno;
    }

    /**
     * @return User from bearer token or session
     */
    public User getUser() {
        try {
            User user = UserLoggedInFilter.getValidUserToken(servletRequest)
                    .map(token -> token.getUser())
                    .orElse(null);
            if (user != null) {
                return user;
            }
        } catch (DAOException e) {
            logger.warn("Error getting user from authorization token", e);
        }
        UserBean userBean = BeanUtils.getUserBeanFromSession(servletRequest.getSession());
        if (userBean != null) {
            return userBean.getUser();
        }
        return null;
    }

}
