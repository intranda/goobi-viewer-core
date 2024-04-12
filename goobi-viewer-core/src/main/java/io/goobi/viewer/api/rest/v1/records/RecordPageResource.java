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

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.annotation.IAnnotationCollection;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Layer;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
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

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(RECORDS_PAGES)
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
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) {
        this.pi = pi;
        request.setAttribute("pi", pi);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_NER_TAGS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records" }, summary = "Get NER tags for a single page")
    public DocumentReference getNERTags(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo,
            @Parameter(description = "Tag type to consider (person, coorporation, event or location)") @QueryParam("type") String type)
            throws PresentationException, IndexUnreachableException {
        NERBuilder builder = new NERBuilder();
        return builder.getNERTags(pi, type, pageNo, pageNo, 1, servletRequest);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_SEQUENCE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 2.1.1 base sequence")
    @IIIFPresentationBinding
    public IPresentationModelElement getSequence(@Parameter(
            description = "Build mode for manifest to select type of resources to include. Default is 'iiif' which returns the full IIIF"
                    + " manifest with all resources. 'thumbs' Does not read width and height of canvas resources and 'iiif_simple' ignores"
                    + " all resources from files") @QueryParam("mode") String mode,
            @Parameter(
                    description = "Set prefered goobi-viewer view for rendering attribute of canvases. Only valid values is 'fullscreen',"
                            + " any other value results in default object/image view being referenced.") @QueryParam("preferedView") String preferedView)

            throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException,
            ViewerConfigurationException, DAOException, IllegalRequestException {
        IIIFPresentation2ResourceBuilder builder = new IIIFPresentation2ResourceBuilder(urls, servletRequest);
        BuildMode buildMode = RecordResource.getBuildeMode(mode);
        return builder.getBaseSequence(pi, buildMode, preferedView);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_MANIFEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 2.1.1 manifest for record")
    @IIIFPresentationBinding
    public IPresentationModelElement getManifest(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo,
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

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_CANVAS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 2.1.1 canvas for a page")
    @IIIFPresentationBinding
    public IPresentationModelElement getCanvas(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException,
            ViewerConfigurationException, DAOException {
        IIIFPresentation2ResourceBuilder builder = new IIIFPresentation2ResourceBuilder(urls, servletRequest);
        return builder.getCanvas(pi, pageNo);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_ANNOTATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List annotations for a page")
    public IAnnotationCollection getAnnotationsForRecord(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo)
            throws DAOException {

        ApiPath apiPath = urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(pi, pageNo);
        URI uri = URI.create(apiPath.query("format", "oa").build());
        return new OpenAnnotationBuilder(urls).getCrowdsourcingAnnotationCollection(uri, pi, pageNo, false, servletRequest);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_COMMENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List comments for a page")
    public IAnnotationCollection getCommentsForPage(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo)
            throws DAOException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi);
        URI uri = URI.create(apiPath.query("format", "oa").build());
        return new AnnotationsResourceBuilder(urls, servletRequest).getOAnnotationListForPageComments(pi, pageNo, uri);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_PAGES_TEXT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records" }, summary = "List annotations for a page")
    public IAnnotationCollection getTextForPage(
            @Parameter(description = "Page numer (1-based") @PathParam("pageNo") Integer pageNo,
            @Parameter(
                    description = "annotation format of the response. If it is 'oa' the comments will be delivered as OpenAnnotations,"
                            + " otherwise as W3C-Webannotations") @QueryParam("format") String format)
            throws URISyntaxException, DAOException, PresentationException, IndexUnreachableException, ViewerConfigurationException {
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
            annotations = builder.addOtherContent(doc, page, canvas, true);
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
}
