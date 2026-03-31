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
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_ALTO;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.JDOMException;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.IncomingAnnotation;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.NotImplementedException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.annotation.AltoAnnotationBuilder;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationDeleter;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@jakarta.ws.rs.Path(ANNOTATIONS)
@ViewerRestServiceBinding
public class AnnotationResource {

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    private final AbstractApiUrlManager urls;

    public AnnotationResource() {
        this.urls = DataManager.getInstance().getRestApiManager().getContentApiManager().orElse(null);
    }

    /**
     *
     * @return AnnotationCollection
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get an annotation collection over all annotations")
    @ApiResponse(responseCode = "200", description = "Annotation collection containing all annotations")
    public AnnotationCollection getAnnotationCollection() throws PresentationException, IndexUnreachableException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getWebAnnotationCollection();
    }

    /**
     *
     * @param page
     * @return &lt;a&gt;
     * @throws DAOException
     * @throws ContentLibException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @GET
    @jakarta.ws.rs.Path("/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get a page within the annotation collection over all annotations")
    @ApiResponse(responseCode = "200", description = "A page of annotations from the annotation collection")
    @ApiResponse(responseCode = "400", description = "If the page number is out of bounds")
    public AnnotationPage getAnnotationCollectionPage(
            // Page numbers are 1-based; document minimum in schema so clients and schemathesis know 0 is invalid
            @Parameter(description = "Page number (1-based)", schema = @Schema(minimum = "1")) @PathParam("page") Integer page)
            throws ContentLibException, DAOException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getWebAnnotationPage(page);
    }

    /**
     * Resolves an ALTO text element annotation by its composite identifier (pi, pageNo, elementId).
     * This endpoint handles URLs generated by {@link AltoAnnotationBuilder} for ALTO TextLines, Words, and Blocks.
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
    @Operation(tags = { "annotations" }, summary = "Get an ALTO text annotation by its composite identifier")
    @ApiResponse(responseCode = "200", description = "Returns the annotation for the given ALTO element")
    // 400 is returned when the persistent identifier fails validation or the page number is invalid
    @ApiResponse(responseCode = "400", description = "Invalid persistent identifier or page number")
    @ApiResponse(responseCode = "404", description = "No page, ALTO file, or element found for the given identifier")
    public IAnnotation getAltoAnnotation(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Page order number") @PathParam("pageNo") Integer pageNo,
            @Parameter(description = "ID of the ALTO element") @PathParam("elementId") String elementId,
            @Parameter(description = "Annotation format: 'oa' for OpenAnnotation, default is WebAnnotation")
            @QueryParam("format") String format)
            throws ContentLibException, PresentationException, IndexUnreachableException {
        // Reject invalid PIs before they reach the Solr query to prevent syntax errors (HTTP 500).
        if (!PIValidator.validatePi(pi)) {
            throw new IllegalRequestException("Invalid persistent identifier: " + pi);
        }
        // Look up the ALTO filename for this pi/pageNo via Solr
        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " +" + SolrConstants.ORDER + ":" + pageNo;
        SolrDocumentList docs = DataManager.getInstance().getSearchIndex()
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
     *
     * @param id
     * @return {@link IAnnotation}
     * @throws DAOException
     * @throws ContentLibException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @GET
    @jakarta.ws.rs.Path(ANNOTATIONS_ANNOTATION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get an annotation by its identifier")
    @ApiResponse(responseCode = "200", description = "Return the annotation with the given id")
    @ApiResponse(responseCode = "400", description = "Invalid annotation ID")
    @ApiResponse(responseCode = "404", description = "No annotation found for the given id")
    public IAnnotation getAnnotation(@Parameter(description = "Identifier of the annotation") @PathParam("id") Long id)
            throws DAOException, ContentLibException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getWebAnnotation(id).orElseThrow(() -> new ContentNotFoundException("Not annotation with id = " + id + "found"));
    }

    /**
     *
     * @param id
     * @return {@link IAnnotation}
     * @throws DAOException
     * @throws ContentLibException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @GET
    @jakarta.ws.rs.Path(ANNOTATIONS_COMMENT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get a comment annotation by its identifier")
    @ApiResponse(responseCode = "200", description = "Return the comment annotation with the given id")
    @ApiResponse(responseCode = "400", description = "Invalid annotation ID")
    @ApiResponse(responseCode = "404", description = "No comment annotation found for the given id")
    public IAnnotation getComment(@Parameter(description = "Identifier of the annotation") @PathParam("id") Long id)
            throws DAOException, ContentLibException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getCommentWebAnnotation(id).orElseThrow(() -> new ContentNotFoundException("Not annotation with id = " + id + "found"));
    }

    /**
     *
     * @param anno
     * @return {@link IAnnotation}
     * @throws DAOException
     * @throws NotImplementedException
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Create a new annotation")
    @ApiResponse(responseCode = "201", description = "The created annotation")
    @ApiResponse(responseCode = "400", description = "Missing or invalid request body")
    @ApiResponse(responseCode = "404",
            description = "Annotation target not found or annotation type not supported. Only W3C Web Annotations targeting a manifest,"
                    + " canvas or part of a canvas may be persisted")
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
     *
     * @param id
     * @return {@link IAnnotation}
     * @throws DAOException
     * @throws ContentLibException
     * @throws ViewerConfigurationException
     */
    @DELETE
    @Path(ANNOTATIONS_ANNOTATION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Delete an existing annotation")
    @ApiResponse(responseCode = "200", description = "Return the deleted annotation")
    @ApiResponse(responseCode = "400", description = "Invalid annotation ID")
    @ApiResponse(responseCode = "404", description = "Annotation not found by the given id")
    @ApiResponse(responseCode = "405", description = "May not delete the annotation because it was created by another user")
    public IAnnotation deleteAnnotation(@Parameter(description = "Identifier of the annotation") @PathParam("id") Long id)
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
     *
     * @param anno
     * @return {@link CrowdsourcingAnnotation}
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
     *
     * @return User from session
     */
    public User getUser() {
        UserBean userBean = BeanUtils.getUserBeanFromSession(servletRequest.getSession());
        if (userBean != null) {
            return userBean.getUser();
        }
        return null;
    }

}
