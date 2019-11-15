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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.Collection;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.Layer;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.Range;
import de.intranda.api.iiif.presentation.Sequence;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.search.AutoSuggestResult;
import de.intranda.api.iiif.search.SearchResult;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.iiif.presentation.builder.BuildMode;
import io.goobi.viewer.model.iiif.presentation.builder.LayerBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.SequenceBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.StructureBuilder;
import io.goobi.viewer.model.iiif.search.IIIFSearchBuilder;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;
import io.goobi.viewer.servlets.rest.content.ContentResource;

/**
 * @author Florian Alpers
 *
 */
@Path("/iiif/manifests")
@ViewerRestServiceBinding
@IIIFPresentationBinding
@CORSBinding
public class ManifestResource extends AbstractResource {

    private static Logger logger = LoggerFactory.getLogger(ManifestResource.class);

    private ManifestBuilder manifestBuilder;
    private StructureBuilder structureBuilder;
    private SequenceBuilder sequenceBuilder;
    private LayerBuilder layerBuilder;

    /**
     * Default constructor
     */
    public ManifestResource() {
        super();
    }

    /**
     * Unit test constructor injecting request and response
     * 
     * @param request
     * @param response
     */
    public ManifestResource(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    /**
     * forwards to {@link #getManifest(String)}
     * 
     * @param request
     * @param response
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ConfigurationException
     * @throws DAOException
     */
    @GET
    @Path("/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response geManifestAlt(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("pi") String pi)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException {
        Response resp = Response.seeOther(new URI(request.getRequestURI() + "manifest")).header("Content-Type", response.getContentType()).build();
        return resp;

    }

    /**
     * Returns the entire IIIF manifest for the given pi. If the given pi points to an anchor, a IIIF collection is returned instead
     * 
     * @param pi
     * @return The manifest or collection
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If no object with the given pi was found in the index
     */
    @GET
    @Path("/{pi}/manifest")
    @Produces({ MediaType.APPLICATION_JSON })
    public IPresentationModelElement getManifest(@PathParam("pi") String pi) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException {

        return createManifest(pi, BuildMode.IIIF);
    }

    /**
     * Endpoint for IIIF Search API service in a manifest. Depending on the given motivation parameters, fulltext (motivation=painting), user comments
     * (motivation=commenting) and general (crowdsourcing-) annotations (motivation=describing) may be searched.
     * 
     * 
     * @param pi The pi of the manifest to search
     * @param query The search query; a list of space separated terms. The search is for all complete words which match any of the query terms. Terms
     *            may contain the wildcard charachter '*' to represent an arbitrary number of characters within the word
     * @param motivation a space separated list of motivations of annotations to search for. Search for the following motivations is implemented:
     *            <ul>
     *            <li>painting: fulltext resources</li>
     *            <li>non-painting: all supported resources except fulltext</li>
     *            <li>commenting: user comments</li>
     *            <li>describing: Crowdsourced or other general annotations</li>
     *            </ul>
     * @param date not supported. If this parameter is given, it will be included in the 'ignored' property of the 'within' property of the answer
     * @param user not supported. If this parameter is given, it will be included in the 'ignored' property of the 'within' property of the answer
     * @param page the page number for paged result sets. if this is empty, page=1 is assumed
     * @return a {@link SearchResult} containing all annotations matching the query in the 'resources' property
     * @throws IndexUnreachableException If the index cannot be reached
     * @throws PresentationException if an error occured in the search
     */
    @GET
    @Path("/{pi}/manifest/search")
    @Produces({ MediaType.APPLICATION_JSON })
    public SearchResult searchInManifest(@PathParam("pi") String pi, @QueryParam("q") String query, @QueryParam("motivation") String motivation,
            @QueryParam("date") String date, @QueryParam("user") String user, @QueryParam("page") Integer page)
            throws IndexUnreachableException, PresentationException {
        return new IIIFSearchBuilder(getRequestURI(), query, pi).setMotivation(motivation).setDate(date).setUser(user).setPage(page).build();
    }

    @GET
    @Path("/{pi}/manifest/autocomplete")
    @Produces({ MediaType.APPLICATION_JSON })
    public AutoSuggestResult autoCompleteInManifest(@PathParam("pi") String pi, @QueryParam("q") String query,
            @QueryParam("motivation") String motivation, @QueryParam("date") String date, @QueryParam("user") String user,
            @QueryParam("page") Integer page) throws IndexUnreachableException, PresentationException {
        return new IIIFSearchBuilder(getRequestURI(), query, pi).setMotivation(motivation)
                .setDate(date)
                .setUser(user)
                .setPage(page)
                .buildAutoSuggest();
    }

    /**
     * Returns the entire IIIF manifest for the given pi, excluding all "seeAlso" references and annotation lists other than the images themselves. If
     * the given pi points to an anchor, a IIIF collection is returned instead
     * 
     * @param pi
     * @return The manifest or collection
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If no object with the given pi was found in the index
     */
    @GET
    @Path("/{pi}/manifest/simple")
    @Produces({ MediaType.APPLICATION_JSON })
    public IPresentationModelElement getManifestSimple(@PathParam("pi") String pi) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException {

        return createManifest(pi, BuildMode.IIIF_SIMPLE);
    }

    /**
     * Returns the entire IIIF manifest for the given pi without the sequence and structure lists. If the given pi points to an anchor, a IIIF
     * collection is returned instead
     * 
     * @param pi
     * @return The manifest or collection
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If no object with the given pi was found in the index
     */
    @GET
    @Path("/{pi}/manifest/base")
    @Produces({ MediaType.APPLICATION_JSON })
    public IPresentationModelElement getManifestBase(@PathParam("pi") String pi) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException {

        StructElement doc = getManifestBuilder().getDocument(pi);
        if (doc == null) {
            throw new ContentNotFoundException("No document found for pi " + pi);
        }
        IPresentationModelElement manifest = getManifestBuilder().generateManifest(doc);

        return manifest;
    }

    /**
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ContentNotFoundException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     */
    private IPresentationModelElement createManifest(String pi, BuildMode mode) throws PresentationException, IndexUnreachableException,
            ContentNotFoundException, URISyntaxException, ViewerConfigurationException, DAOException {
        getManifestBuilder().setBuildMode(mode);
        getSequenceBuilder().setBuildMode(mode);
        List<StructElement> docs = getManifestBuilder().getDocumentWithChildren(pi);
        if (docs.isEmpty()) {
            throw new ContentNotFoundException("No document found for pi " + pi);
        }
        StructElement mainDoc = docs.get(0);
        IPresentationModelElement manifest = getManifestBuilder().generateManifest(mainDoc);

        if (manifest instanceof Collection && docs.size() > 1) {
            getManifestBuilder().addVolumes((Collection) manifest, docs.subList(1, docs.size()));
        } else if (manifest instanceof Manifest) {
            getManifestBuilder().addAnchor((Manifest) manifest, mainDoc.getMetadataValue(SolrConstants.PI_ANCHOR));

            getSequenceBuilder().addBaseSequence((Manifest) manifest, mainDoc, manifest.getId().toString());

            String topLogId = mainDoc.getMetadataValue(SolrConstants.LOGID);
            if (StringUtils.isNotBlank(topLogId)) {
                List<Range> ranges = getStructureBuilder().generateStructure(docs, pi, false);
                ranges.forEach(range -> {
                    ((Manifest) manifest).addStructure(range);
                });
            }
        }

        return manifest;
    }

    /**
     * Creates A IIIF sequence containing all pages belonging to the given pi
     * 
     * @param pi
     * @return A IIIF sequence with all pages of the book (if applicable)
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws IllegalRequestException If the document for the given pi can not contain any pages, usually because it is an anchor
     * @throws ContentNotFoundException If no document was found for the given pi
     */
    @GET
    @Path("/{pi}/sequence/basic")
    @Produces({ MediaType.APPLICATION_JSON })
    public Sequence getBasicSequence(@PathParam("pi") String pi) throws PresentationException, IndexUnreachableException, URISyntaxException,
            ViewerConfigurationException, DAOException, IllegalRequestException, ContentNotFoundException {

        StructElement doc = getManifestBuilder().getDocument(pi);

        IPresentationModelElement manifest = getManifestBuilder().generateManifest(doc);

        if (manifest instanceof Collection) {
            throw new IllegalRequestException("Identifier refers to a collection which does not have a sequence");
        } else if (manifest instanceof Manifest) {
            getSequenceBuilder().addBaseSequence((Manifest) manifest, doc, manifest.getId().toString());
            return ((Manifest) manifest).getSequences().get(0);
        }
        throw new ContentNotFoundException("Not manifest with identifier " + pi + " found");

    }

    /**
     * Creates A IIIF sequence containing all pages belonging to the given pi
     * 
     * @param pi
     * @return A IIIF sequence with all pages of the book (if applicable)
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws IllegalRequestException If the document for the given pi can not contain any pages, usually because it is an anchor
     * @throws ContentNotFoundException If no document was found for the given pi
     */
    @GET
    @Path("/{pi}/{preferredView}/thumbnails")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<Canvas> getThumbnailSequence(@PathParam("preferredView") String preferredView, @PathParam("pi") String pi)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, DAOException,
            IllegalRequestException, ContentNotFoundException {

        StructElement doc = getManifestBuilder().getDocument(pi);

        IPresentationModelElement manifest = getManifestBuilder().setBuildMode(BuildMode.THUMBS).generateManifest(doc);

        if (manifest instanceof Collection) {
            throw new IllegalRequestException("Identifier refers to a collection which does not have a sequence");
        } else if (manifest instanceof Manifest) {
            PageType pageType = PageType.getByName(preferredView);
            if (pageType == null) {
                pageType = PageType.viewObject;
            }
            getSequenceBuilder().setPreferredView(pageType)
                    .setBuildMode(BuildMode.THUMBS)
                    .addBaseSequence((Manifest) manifest, doc, manifest.getId().toString());
            return ((Manifest) manifest).getSequences().get(0).getCanvases();
        }
        throw new ContentNotFoundException("Not manifest with identifier " + pi + " found");

    }

    /**
     * Creates a IIIF range for the structural element denoted by the given pi and logid
     * 
     * @param pi The pi of the containing work
     * @param logId The METS logid of the structural element to return
     * @return A IIIF range
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If no structural element was found for the given pi and logid
     */
    @GET
    @Path("/{pi}/range/{logId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Range getRange(@PathParam("pi") String pi, @PathParam("logId") String logId) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException {

        List<StructElement> docs = getStructureBuilder().getDocumentWithChildren(pi);

        if (docs.isEmpty()) {
            throw new ContentNotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found");
        }
        List<Range> ranges = getStructureBuilder().generateStructure(docs, pi, false);
        Optional<Range> range = ranges.stream().filter(r -> r.getId().toString().endsWith(logId + "/")).findFirst();
        return range.orElseThrow(() -> new ContentNotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found"));
    }

    /**
     * Creates a canvas for the page with the given pyhsPageNo (order) within the work with the given pi
     * 
     * @param pi The pi of the containing work
     * @param physPageNo The physical ordering of the page (1-based)
     * @return A IIIF canvas
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If there is no work with the given pi or it doesn't have a page with the given order
     */
    @GET
    @Path("/{pi}/canvas/{physPageNo}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Canvas getCanvas(@PathParam("pi") String pi, @PathParam("physPageNo") int physPageNo) throws PresentationException,
            IndexUnreachableException, URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException {

        StructElement doc = getManifestBuilder().getDocument(pi);
        if (doc != null) {
            PhysicalElement page = getSequenceBuilder().getPage(doc, physPageNo);
            Canvas canvas = getSequenceBuilder().generateCanvas(doc, page);
            if (canvas != null) {
                getSequenceBuilder().addSeeAlsos(canvas, doc, page);
                getSequenceBuilder().addOtherContent(doc, page, canvas, false);
                getSequenceBuilder().addCrowdourcingAnnotations(Collections.singletonList(canvas),
                        getSequenceBuilder().getCrowdsourcingAnnotations(pi, false), null);
                return canvas;
            }
        }
        throw new ContentNotFoundException("No page found with order= " + physPageNo + " and pi = " + pi);
    }

    /**
     * Creates an annotation list for the given page of annotations of the given {@link AnnotationType type}
     * 
     * @param pi The pi of the containing work
     * @param physPageNo The physical ordering of the page (1-based)
     * @param typeName The name of the {@link AnnotationType} for which annotations should be returned
     * @return A IIIF AnnotationList
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If there is no work with the given pi or it doesn't have a page with the given order
     * @throws IllegalRequestException If there is no annotation type of the given name
     */
    @GET
    @Path("/{pi}/list/{physPageNo}/{type}")
    @Produces({ MediaType.APPLICATION_JSON })
    public AnnotationList getOtherContent(@PathParam("pi") String pi, @PathParam("physPageNo") int physPageNo, @PathParam("type") String typeName)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, DAOException,
            ContentNotFoundException, IllegalRequestException {
        AnnotationType type = AnnotationType.valueOf(typeName.toUpperCase());
        if (type != null) {
            StructElement doc = getManifestBuilder().getDocument(pi);
            PhysicalElement page = getSequenceBuilder().getPage(doc, physPageNo);
            Canvas canvas = getSequenceBuilder().generateCanvas(doc, page);
            Map<AnnotationType, AnnotationList> annotations;
            if (AnnotationType.COMMENT.equals(type)) {
                annotations = new HashMap<>();
                List<AnnotationList> comments = getSequenceBuilder().addComments(Collections.singletonMap(physPageNo, canvas), pi, true);
                if (!comments.isEmpty()) {
                    annotations.put(AnnotationType.COMMENT, comments.get(0));
                }
            } else if (AnnotationType.CROWDSOURCING.equals(type)) {
                annotations = new HashMap<>();
                Map<AnnotationType, List<AnnotationList>> annoTempMap = new HashMap<>();
                getSequenceBuilder().addCrowdourcingAnnotations(Collections.singletonList(canvas),
                        getSequenceBuilder().getCrowdsourcingAnnotations(pi, false), annoTempMap);
                AnnotationList annoList = null;
                if (annoTempMap.get(AnnotationType.CROWDSOURCING) != null) {
                    annoList = annoTempMap.get(AnnotationType.CROWDSOURCING).stream().findFirst().orElse(null);
                }
                if (annoList != null) {
                    annotations.put(AnnotationType.CROWDSOURCING, annoList);
                }
            } else {
                annotations = getSequenceBuilder().addOtherContent(doc, page, canvas, true);
            }
            if (annotations.get(type) != null) {
                AnnotationList al = annotations.get(type);
                Layer layer = new Layer(getManifestBuilder().getLayerURI(pi, type));
                layer.setLabel(ViewerResourceBundle.getTranslations(type.name()));
                al.addWithin(layer);
                return al;
            }
            throw new ContentNotFoundException("No otherContent found for " + pi + "/" + physPageNo + "/" + type);
        }
        throw new IllegalRequestException("No valid annotation type: " + typeName);
    }

    /**
     * Creates an annotation list for a annotations of the given {@link AnnotationType type} not bound to a page
     * 
     * @param pi The pi of the containing work
     * @param typeName The name of the {@link AnnotationType} for which annotations should be returned
     * @return A IIIF AnnotationList
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If there is no work with the given pi or it doesn't have a page with the given order
     * @throws IllegalRequestException If there is no annotation type of the given name
     * @throws IOException
     */
    @GET
    @Path("/{pi}/list/{type}")
    @Produces({ MediaType.APPLICATION_JSON })
    public AnnotationList getOtherContent(@PathParam("pi") String pi, @PathParam("type") String typeName)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, DAOException,
            ContentNotFoundException, IllegalRequestException, IOException {
        AnnotationType type = AnnotationType.valueOf(typeName.toUpperCase());
        Layer layer;
        if (AnnotationType.TEI.equals(type)) {
            layer = getLayerBuilder().createAnnotationLayer(pi, type, Motivation.PAINTING, (id, repo) -> ContentResource.getTEIFiles(id, repo),
                    (id, lang) -> ContentResource.getTEIURI(id, lang));
        } else if (AnnotationType.CMDI.equals(type)) {
            layer = getLayerBuilder().createAnnotationLayer(pi, type, Motivation.DESCRIBING, (id, repo) -> ContentResource.getCMDIFiles(id, repo),
                    (id, lang) -> ContentResource.getCMDIURI(id, lang));
        }
        if (AnnotationType.CROWDSOURCING.equals(type)) {
            List<OpenAnnotation> workAnnotations = getSequenceBuilder().getCrowdsourcingAnnotations(pi, false).get(null);
            if (workAnnotations == null) {
                workAnnotations = new ArrayList<>();
            }
            AnnotationList annoList = new AnnotationList(getLayerBuilder().getAnnotationListURI(pi, type));
            workAnnotations.forEach(annoList::addResource);
            layer = new Layer(getManifestBuilder().getLayerURI(pi, type));
            layer.addOtherContent(annoList);
        } else {
            throw new IllegalRequestException("No global annotations for type: " + typeName);
        }
        Optional<AnnotationList> annoList = layer.getOtherContent().stream().findFirst();
        if (annoList.isPresent()) {
            layer.setLabel(ViewerResourceBundle.getTranslations(type.name()));
            annoList.get().addWithin(layer);
            return annoList.get();
        }
        throw new ContentNotFoundException("No annotations found for " + pi + "/" + type);
    }

    /**
     * Creates a layer containing all annnotations of the given {@link AnnotationType type} for the work with the given pi. The annotations are groupd
     * into annotation lists by page, if they belong to a page. Otherwise they are grouped in a single annotation list
     * 
     * @param pi The pi of the containing work
     * @param typeName The name of the {@link AnnotationType} for which annotations should be returned
     * @return A IIIF layer
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If there is no work with the given pi
     * @throws IllegalRequestException If there is no annotation type of the given name
     * @throws IOException
     */
    @GET
    @Path("/{pi}/layer/{type}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Layer getLayer(@PathParam("pi") String pi, @PathParam("type") String typeName) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException, IllegalRequestException, IOException {
        StructElement doc = getStructureBuilder().getDocument(pi);
        AnnotationType type = AnnotationType.valueOf(typeName.toUpperCase());
        if (type == null) {
            throw new IllegalRequestException("No valid annotation type: " + typeName);
        }
        if (doc == null) {
            throw new ContentNotFoundException("Not document with PI = " + pi + " found");
        } else if (AnnotationType.TEI.equals(type)) {
            return getLayerBuilder().createAnnotationLayer(pi, type, Motivation.PAINTING, (id, repo) -> ContentResource.getTEIFiles(id, repo),
                    (id, lang) -> ContentResource.getTEIURI(id, lang));
        } else if (AnnotationType.CMDI.equals(type)) {
            return getLayerBuilder().createAnnotationLayer(pi, type, Motivation.DESCRIBING, (id, repo) -> ContentResource.getCMDIFiles(id, repo),
                    (id, lang) -> ContentResource.getCMDIURI(id, lang));

        } else {
            Map<AnnotationType, List<AnnotationList>> annoLists = getSequenceBuilder().addBaseSequence(null, doc, "");
            Layer layer = getLayerBuilder().generateLayer(pi, annoLists, type);
            return layer;
        }
    }

    private StructureBuilder getStructureBuilder() {
        if (this.structureBuilder == null) {
            this.structureBuilder = new StructureBuilder(this.servletRequest);
        }
        return this.structureBuilder;
    }

    public ManifestBuilder getManifestBuilder() {
        if (this.manifestBuilder == null) {
            this.manifestBuilder = new ManifestBuilder(servletRequest);
        }
        return manifestBuilder;
    }

    public SequenceBuilder getSequenceBuilder() {
        if (this.sequenceBuilder == null) {
            this.sequenceBuilder = new SequenceBuilder(servletRequest);
        }
        return sequenceBuilder;
    }

    public LayerBuilder getLayerBuilder() {
        if (this.layerBuilder == null) {
            this.layerBuilder = new LayerBuilder(servletRequest);
        }
        return layerBuilder;
    }

}
