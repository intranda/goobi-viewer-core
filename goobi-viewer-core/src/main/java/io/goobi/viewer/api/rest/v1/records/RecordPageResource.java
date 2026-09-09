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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_COMMENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_COMMENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_MANIFEST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_NER_TAGS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_SEQUENCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_TEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.annotation.IAnnotationCollection;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Collection2;
import de.intranda.api.iiif.presentation.v2.Layer;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.intranda.api.iiif.presentation.v2.Sequence;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.ner.DocumentReference;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.IIIFPresentation2ResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.NERBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.AltoAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.BuildMode;
import io.goobi.viewer.model.iiif.presentation.v2.builder.ManifestBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.OpenAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.SequenceBuilder;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author Florian Alpers
 */
@jakarta.ws.rs.Path(RECORDS_PAGES)
@ViewerRestServiceBinding
@CORSBinding
public class RecordPageResource {

    private static final Logger logger = LogManager.getLogger(RecordPageResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private ApiUrls urls;

    private final String pi;

    public RecordPageResource(@Context HttpServletRequest request,
            @Parameter(description = "Persistent identifier of the record",
                    schema = @Schema(pattern = "^[A-Za-z0-9][A-Za-z0-9_.-]*$")) @PathParam("pi") String pi) {
        // Reject PIs containing characters illegal in URI paths / Solr queries before any
        // Solr or file-system access occurs.  BadRequestException (HTTP 400) is an unchecked
        // WebApplicationException that Jersey maps to 400 before invoking the endpoint.
        if (!PIValidator.validatePi(pi)) {
            throw new BadRequestException("Invalid record identifier: " + pi);
        }
        this.pi = pi;
        request.setAttribute("pi", pi);
    }

    /**
     * Returns the named entities (persons, corporations, events, locations or miscellaneous) recognized on a single page.
     *
     * <p>Tags are extracted from the page's indexed ALTO file; pages for which no ALTO file is indexed, or for which the fulltext view
     * permission is not granted, are silently omitted from the result rather than causing an error. The type filter is matched against the
     * type recorded in the ALTO file; tags whose recorded type is not one of the known types are reported as "miscellaneous" and therefore
     * appear only when no type is given.
     *
     * @param pageNo page number (1-based)
     * @param type tag type to consider (person, corporation, event or location)
     * @return the named entities found on the page, grouped by tag
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_NER_TAGS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records" }, summary = "Get NER tags for a single page",
            description = "Tags are extracted from the page's indexed ALTO file; a page for which no ALTO file is indexed, or for which the"
                    + " fulltext view permission is not granted, is silently omitted from the result rather than causing an error. The type"
                    + " filter is matched against the type recorded in the ALTO file; tags whose recorded type is not one of the known types"
                    + " are reported as 'miscellaneous' and therefore appear only when no type is given.")
    @ApiResponse(responseCode = "200", description = "NER tags for the requested page", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or page number")
    @ApiResponse(responseCode = "404", description = "No record found for the given identifier")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable")
    public DocumentReference getNERTags(
            @Parameter(description = "Page number (1-based)",
                    schema = @Schema(minimum = "1", maximum = "2147483647")) @PathParam("pageNo") Integer pageNo,
            @Parameter(description = "Tag type to consider (person, corporation, event or location)") @QueryParam("type") String type)
            throws PresentationException, IndexUnreachableException {
        requireValidPageNo(pageNo);
        NERBuilder builder = new NERBuilder();
        return builder.getNERTags(pi, type, pageNo, pageNo, 1, servletRequest);
    }

    /**
     * Returns the IIIF 2.1.1 base sequence of all canvases for the record.
     *
     * <p>Unlike the manifest and canvas endpoints, this returns only the sequence element, not the surrounding manifest. Access requires
     * the record's basic list permission.
     *
     * @param mode build mode for manifest to select type of resources to include. Default is 'iiif' which returns the full IIIF manifest
     *             with all resources. 'thumbs' Does not read width and height of canvas resources and 'iiif_simple' ignores all resources
     *             from files
     * @param preferedView set prefered goobi-viewer view for rendering attribute of canvases. Only valid values is 'fullscreen', any other
     *             value results in default object/image view being referenced.
     * @return the {@link Sequence} of all canvases of the record
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException if the identifier refers to a collection, which
     *             has no sequence
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_SEQUENCE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 2.1.1 base sequence",
            description = "Unlike the manifest and canvas endpoints, this returns only the sequence element, not the surrounding manifest."
                    + " Access requires the record's basic list permission.")
    @ApiResponse(responseCode = "200", description = "IIIF 2.1.1 base sequence for the record",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Sequence.class)))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier")
    @ApiResponse(responseCode = "403", description = "Access denied or record not accessible (e.g. record not found in index)")
    @ApiResponse(responseCode = "404", description = "No record found for the given identifier")
    @IIIFPresentationBinding
    public IPresentationModelElement getSequence(@Parameter(
            description = "Build mode for manifest to select type of resources to include. Default is 'iiif' which returns the full IIIF"
                    + " manifest with all resources. 'thumbs' Does not read width and height of canvas resources and 'iiif_simple' ignores"
                    + " all resources from files") @QueryParam("mode") String mode,
            @Parameter(
                    description = "Set prefered goobi-viewer view for rendering attribute of canvases. Only valid values is 'fullscreen',"
                            + " any other value results in default object/image"
                            + " view being referenced.") @QueryParam("preferedView") String preferedView)

            throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException,
            ViewerConfigurationException, DAOException, IllegalRequestException {
        IIIFPresentation2ResourceBuilder builder = new IIIFPresentation2ResourceBuilder(urls, servletRequest);
        BuildMode buildMode = RecordResource.getBuildeMode(mode);
        return builder.getBaseSequence(pi, buildMode, preferedView);
    }

    /**
     * Returns the full IIIF 2.1.1 manifest for the record, with the base sequence restricted to the given page.
     *
     * <p>The manifest still describes the whole record (and, for multi-volume works, its child volumes); the canvas sequence embedded in it
     * is limited to the requested page instead of listing every page of the record, and the manifest carries no structural ranges. Access
     * requires the record's basic list permission.
     *
     * @param pageNo page number (1-based)
     * @param mode build mode for manifest to select type of resources to include. Default is 'iiif' which returns the full IIIF manifest
     *             with all resources. 'thumbs' Does not read width and height of canvas resources and 'iiif_simple' ignores all resources
     *             from files
     * @return the {@link Manifest2} or {@link Collection2} for the record
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_MANIFEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 2.1.1 manifest for record",
            description = "The manifest still describes the whole record (and, for multi-volume works, its child volumes); the canvas"
                    + " sequence embedded in it is limited to the requested page instead of listing every page of the record, and the"
                    + " manifest carries no structural ranges. Access requires the record's basic list permission.")
    @ApiResponse(responseCode = "200", description = "IIIF 2.1.1 manifest for the given page",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(oneOf = { Manifest2.class, Collection2.class })))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or page number")
    // 403 is returned by AccessConditionRequestFilter when the record is not found in the Solr index
    @ApiResponse(responseCode = "403", description = "Access denied or record not accessible (e.g. record not found in index)")
    @ApiResponse(responseCode = "404", description = "No record or page found for the given identifiers")
    @IIIFPresentationBinding
    public IPresentationModelElement getManifest(
            @Parameter(description = "Page number (1-based)",
                    schema = @Schema(minimum = "1", maximum = "2147483647")) @PathParam("pageNo") Integer pageNo,
            @Parameter(
                    description = "Build mode for manifest to select type of resources to include. Default is 'iiif' which returns"
                            + " the full IIIF manifest with all resources. 'thumbs' Does not read width and height of canvas resources"
                            + " and 'iiif_simple' ignores all resources from files") @QueryParam("mode") String mode)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException,
            DAOException {
        IIIFPresentation2ResourceBuilder b = new IIIFPresentation2ResourceBuilder(urls, servletRequest);
        BuildMode buildMode = RecordResource.getBuildeMode(mode);
        return b.getManifest(pi, List.of(pageNo), buildMode);
    }

    /**
     * Returns the IIIF 2.1.1 canvas for a single page.
     *
     * <p>The canvas includes links to other content representations of the page (e.g. fulltext/ALTO) and, if any exist, the page's
     * crowdsourcing annotations. Access requires the record's basic list permission.
     *
     * @param pageNo page number (1-based)
     * @return the {@link Canvas2} for the requested page
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_CANVAS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 2.1.1 canvas for a page",
            description = "The canvas includes links to other content representations of the page (e.g. fulltext/ALTO) and, if any exist,"
                    + " the page's crowdsourcing annotations. Access requires the record's basic list permission.")
    @ApiResponse(responseCode = "200", description = "IIIF 2.1.1 canvas for the given page",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Canvas2.class)))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or page number")
    @ApiResponse(responseCode = "403", description = "Access to this record is restricted")
    @ApiResponse(responseCode = "404", description = "No record or page found for the given identifiers")
    @IIIFPresentationBinding
    public IPresentationModelElement getCanvas(
            @Parameter(description = "Page number (1-based)",
                    schema = @Schema(minimum = "1", maximum = "2147483647")) @PathParam("pageNo") Integer pageNo)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException,
            ViewerConfigurationException, DAOException {
        IIIFPresentation2ResourceBuilder builder = new IIIFPresentation2ResourceBuilder(urls, servletRequest);
        return builder.getCanvas(pi, pageNo);
    }

    /**
     * Returns the crowdsourcing annotations created for a single page.
     *
     * <p>Annotations are read from the database, not the Solr index, and are delivered as an Open Annotation collection. This includes
     * every crowdsourcing motivation (e.g. describing, commenting, tagging), which distinguishes it from the dedicated comments endpoint.
     *
     * @param pageNo page number (1-based)
     * @return the annotation collection for the requested page
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_ANNOTATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List annotations for a page",
            description = "Annotations are read from the database, not the Solr index, and are delivered as an Open Annotation collection."
                    + " This includes every crowdsourcing motivation (e.g. describing, commenting, tagging), which distinguishes it from the"
                    + " dedicated comments endpoint.")
    @ApiResponse(responseCode = "200", description = "Annotation collection for the given page",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AnnotationList.class)))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or page number")
    @ApiResponse(responseCode = "404", description = "No record found for the given identifier")
    public IAnnotationCollection getAnnotationsForRecord(
            @Parameter(description = "Page number (1-based)",
                    schema = @Schema(minimum = "1", maximum = "2147483647")) @PathParam("pageNo") Integer pageNo)
            throws DAOException {
        requireValidPageNo(pageNo);
        ApiPath apiPath = urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(pi, pageNo);
        URI uri = URI.create(apiPath.query("format", "oa").build());
        return new OpenAnnotationBuilder(urls).getCrowdsourcingAnnotationCollection(uri, pi, pageNo, false, servletRequest);
    }

    /**
     * Returns the comments left on a single page.
     *
     * <p>Comments are read from the database and delivered as an Open Annotation collection, one annotation per comment. Unlike the
     * annotations endpoint, this only returns simple page comments, not other crowdsourcing motivations.
     *
     * @param pageNo page number (1-based)
     * @return the annotation collection of comments for the requested page
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_COMMENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List comments for a page",
            description = "Comments are read from the database and delivered as an Open Annotation collection, one annotation per comment."
                    + " Unlike the annotations endpoint, this only returns simple page comments, not other crowdsourcing motivations.")
    @ApiResponse(responseCode = "200", description = "Annotation collection of comments for the given page",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AnnotationList.class)))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or page number")
    @ApiResponse(responseCode = "404", description = "No record found for the given identifier")
    public IAnnotationCollection getCommentsForPage(
            @Parameter(description = "Page number (1-based)",
                    schema = @Schema(minimum = "1", maximum = "2147483647")) @PathParam("pageNo") Integer pageNo)
            throws DAOException {
        requireValidPageNo(pageNo);
        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi);
        URI uri = URI.create(apiPath.query("format", "oa").build());
        return new AnnotationsResourceBuilder(urls, servletRequest).getOAnnotationListForPageComments(pi, pageNo, uri);
    }

    /**
     * Returns the text content of a page as an IIIF 2 annotation list, at block, line or word granularity.
     *
     * @param pageNo page number (1-based)
     * @param format annotation format of the response ('oa' or W3C web annotations)
     * @param granularity OCR annotation granularity, 'word', 'block' or 'line'; unknown values are treated as 'line'
     * @return annotation list containing the page text
     * @throws URISyntaxException
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_PAGES_TEXT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Get the text content of a single page as annotations", tags = { "records" },
            description = "If the record's fulltext view permission is not granted, an empty annotation collection is returned instead of an"
                    + " error response. Where an ALTO file is available its content is preferred over plain fulltext.")
    @ApiResponse(responseCode = "200", description = "Annotation collection containing page text",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AnnotationList.class)))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or page number")
    @ApiResponse(responseCode = "404", description = "No record found for the given identifier")
    public IAnnotationCollection getTextForPage(
            @Parameter(description = "Page number (1-based)",
                    schema = @Schema(minimum = "1", maximum = "2147483647")) @PathParam("pageNo") Integer pageNo,
            @Parameter(
                    description = "annotation format of the response. If it is 'oa' the annotations will be delivered as OpenAnnotations,"
                            + " otherwise as W3C-Webannotations") @QueryParam("format") String format,
            @Parameter(description = "OCR annotation granularity: 'word' for word-level annotations, 'block' for text-block-level,"
                    + " 'line' (default) for line-level",
                    schema = @Schema(allowableValues = { "line", "word", "block" }, defaultValue = "line"))
            @QueryParam("granularity") @DefaultValue("line") String granularity)
            throws URISyntaxException, DAOException, PresentationException, IndexUnreachableException, ViewerConfigurationException {
        requireValidPageNo(pageNo);
        // logger.trace("getTextForPage"); //NOSONAR Debug
        //        ApiPath apiPath = urls.path(RECORDS_PAGES, RECORDS_PAGES_TEXT).params(pi, pageNo);
        boolean access;
        try {
            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT, servletRequest)
                    .isGranted();
        } catch (RecordNotFoundException e) {
            access = false;
        }

        Map<AnnotationType, AnnotationList> annotations;
        if (access) {
            SequenceBuilder builder = new SequenceBuilder(urls);
            StructElement doc = new ManifestBuilder(urls).getDocument(pi);
            PhysicalElement page = builder.getPage(doc, pageNo);
            Canvas2 canvas = builder.generateCanvas(doc.getPi(), page);
            AltoAnnotationBuilder.Granularity altoGranularity;
            if (AltoAnnotationBuilder.Granularity.WORD.name().equalsIgnoreCase(granularity)) {
                altoGranularity = AltoAnnotationBuilder.Granularity.WORD;
            } else if (AltoAnnotationBuilder.Granularity.BLOCK.name().equalsIgnoreCase(granularity)) {
                altoGranularity = AltoAnnotationBuilder.Granularity.BLOCK;
            } else {
                altoGranularity = AltoAnnotationBuilder.Granularity.LINE;
            }
            annotations = builder.addOtherContent(doc, page, canvas, true, altoGranularity);
        } else {
            annotations = new HashMap<>();
        }

        if (annotations.containsKey(AnnotationType.ALTO)) {
            AnnotationList al = annotations.get(AnnotationType.ALTO);
            Layer layer = new Layer(new ManifestBuilder(urls).getLayerURI(pi, AnnotationType.ALTO));
            layer.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.ALTO.name()));
            al.addWithin(layer);
            return al;
        } else if (annotations.containsKey(AnnotationType.FULLTEXT)) {
            AnnotationList al = annotations.get(AnnotationType.FULLTEXT);
            Layer layer = new Layer(new ManifestBuilder(urls).getLayerURI(pi, AnnotationType.FULLTEXT));
            layer.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.FULLTEXT.name()));
            al.addWithin(layer);
            return al;
        } else {
            return new AnnotationList(new SequenceBuilder(urls).getAnnotationListURI(pi, pageNo, AnnotationType.FULLTEXT, true));
        }
    }

    /**
     * Validates that the given page number is at least 1.
     *
     * <p>The schema documents minimum=1, but JAX-RS does not enforce schema constraints server-side.
     * Without this check, pageNo=0 returns an empty annotation collection instead of a 400.
     *
     * @param pageNo the page number path parameter value
     * @throws BadRequestException if pageNo is less than 1
     */
    private void requireValidPageNo(Integer pageNo) {
        if (pageNo != null && pageNo < 1) {
            throw new BadRequestException("Page number must be at least 1, got: " + pageNo);
        }
    }
}
