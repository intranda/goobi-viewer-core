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
package de.intranda.digiverso.presentation.servlets.rest.iiif.presentation;

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
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.iiif.presentation.AnnotationList;
import de.intranda.digiverso.presentation.model.iiif.presentation.Canvas;
import de.intranda.digiverso.presentation.model.iiif.presentation.Collection;
import de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Layer;
import de.intranda.digiverso.presentation.model.iiif.presentation.Manifest;
import de.intranda.digiverso.presentation.model.iiif.presentation.Range;
import de.intranda.digiverso.presentation.model.iiif.presentation.Sequence;
import de.intranda.digiverso.presentation.model.iiif.presentation.annotation.Annotation;
import de.intranda.digiverso.presentation.model.iiif.presentation.builder.LayerBuilder;
import de.intranda.digiverso.presentation.model.iiif.presentation.builder.ManifestBuilder;
import de.intranda.digiverso.presentation.model.iiif.presentation.builder.SequenceBuilder;
import de.intranda.digiverso.presentation.model.iiif.presentation.builder.StructureBuilder;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.AnnotationType;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.DcType;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Format;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Motivation;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.intranda.digiverso.presentation.servlets.rest.content.ContentResource;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;

/**
 * @author Florian Alpers
 *
 */
@Path("/iiif/manifests")
@ViewerRestServiceBinding
@IIIFPresentationBinding
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
        Response resp = Response.seeOther(new URI(request.getRequestURI() + "manifest")).header("Content-Type", response.getContentType())
                .build();
        return resp;
        
    }
    
    /**
     * Returns the entire IIIF manifest for the given pi. If the given pi points to an anchor, a IIIF collection is returned instead 
     * 
     * @param pi
     * @return  The manifest or collection
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException     If no object with the given pi was found in the index
     */
    @GET
    @Path("/{pi}/manifest")
    @Produces({ MediaType.APPLICATION_JSON })
    public IPresentationModelElement getManifest(@PathParam("pi") String pi)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException, ContentNotFoundException {

        servletResponse.addHeader("Access-Control-Allow-Origin", "*");

        List<StructElement> docs = getManifestBuilder().getDocumentWithChildren(pi);
        if(docs.isEmpty()) {
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
                List<Range> ranges = getStructureBuilder().generateStructure(docs, false);
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
     * @return  A IIIF sequence with all pages of the book (if applicable)
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ConfigurationException
     * @throws DAOException
     * @throws IllegalRequestException If the document for the given pi can not contain any pages, usually because it is an anchor
     * @throws ContentNotFoundException  If no document was found for the given pi
     */
    @GET
    @Path("/{pi}/sequence/basic")
    @Produces({ MediaType.APPLICATION_JSON })
    public Sequence getBasicSequence(@PathParam("pi") String pi) throws PresentationException, IndexUnreachableException, URISyntaxException,
            ConfigurationException, DAOException, IllegalRequestException, ContentNotFoundException {

        StructElement doc = getManifestBuilder().getDocument(pi);
        servletResponse.addHeader("Access-Control-Allow-Origin", "*");

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
     * Creates a IIIF range for the structural element denoted by the given pi and logid
     * 
     * @param pi        The pi of the containing work
     * @param logId     The METS logid of the structural element to return
     * @return  A IIIF range
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If no structural element was found for the given pi and logid
     */
    @GET
    @Path("/{pi}/range/{logId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Range getRange(@PathParam("pi") String pi, @PathParam("logId") String logId)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException, ContentNotFoundException {

        List<StructElement> docs = getStructureBuilder().getDocumentWithChildren(pi);

        if (docs.isEmpty()) {
            throw new ContentNotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found");
        } else {
            List<Range> ranges = getStructureBuilder().generateStructure(docs, false);
            Optional<Range> range = ranges.stream().filter(r -> r.getId().toString().endsWith(logId)).findFirst();
            return range.orElseThrow(() -> new ContentNotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found"));
        }
    }

    /**
     * Creates a canvas for the page with the given pyhsPageNo (order) within the work with the given pi
     * 
     * @param pi            The pi of the containing work
     * @param physPageNo    The physical ordering of the page (1-based)
     * @return  A IIIF canvas
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If there is no work with the given pi or it doesn't have a page with the given order
     */
    @GET
    @Path("/{pi}/canvas/{physPageNo}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Canvas getCanvas(@PathParam("pi") String pi, @PathParam("physPageNo") int physPageNo)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException, ContentNotFoundException {
        StructElement doc = getManifestBuilder().getDocument(pi);
        if(doc != null) {            
            PhysicalElement page = getSequenceBuilder().getPage(doc, physPageNo);
            Canvas canvas = getSequenceBuilder().generateCanvas(doc, page);
            if(canvas != null) {            
                getSequenceBuilder().addOtherContent(doc, page, canvas, ContentResource.getDataRepository(pi), false);
                return canvas;
            }
        }
        throw new ContentNotFoundException("No page found with order= " + physPageNo + " and pi = " + pi);
    }
    
    /**
     * Creates an annotation list for the given page of annotations of the given {@link AnnotationType type}
     * 
     * @param pi            The pi of the containing work
     * @param physPageNo    The physical ordering of the page (1-based)
     * @param typeName      The name of the {@link AnnotationType} for which annotations should be returned
     * @return  A IIIF AnnotationList
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If there is no work with the given pi or it doesn't have a page with the given order
     * @throws IllegalRequestException  If there is no annotation type of the given name
     */
    @GET
    @Path("/{pi}/list/{physPageNo}/{type}")
    @Produces({ MediaType.APPLICATION_JSON })
    public AnnotationList getOtherContent(@PathParam("pi") String pi, @PathParam("physPageNo") int physPageNo, @PathParam("type") String typeName) throws PresentationException,
            IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException, ContentNotFoundException, IllegalRequestException {
            AnnotationType type = AnnotationType.valueOf(typeName.toUpperCase());
            if(type != null) {         
                StructElement doc = getManifestBuilder().getDocument(pi);
                PhysicalElement page = getSequenceBuilder().getPage(doc, physPageNo);
                Canvas canvas = getSequenceBuilder().generateCanvas(doc, page);
                Map<AnnotationType, AnnotationList> annotations;
                if(AnnotationType.COMMENT.equals(type)) {
                    annotations = new HashMap<>();
                    List<AnnotationList> comments = getSequenceBuilder().addComments(Collections.singletonMap(physPageNo, canvas), pi, true);
                    if(!comments.isEmpty()) {
                        annotations.put(AnnotationType.COMMENT, comments.get(0));
                    }
                } else {                    
                    annotations = getSequenceBuilder().addOtherContent(doc, page, canvas, ContentResource.getDataRepository(pi), true);
                }
                if (annotations.get(type) != null) {
                    AnnotationList al = annotations.get(type);
                    Layer layer = new Layer(getManifestBuilder().getLayerURI(pi, type));
                    layer.setLabel(IMetadataValue.getTranslations(type.name()));
                    al.addWithin(layer);
                    return al;
                } else {
                    throw new ContentNotFoundException("No otherContent found for " + pi + "/" + physPageNo + "/" + type);
                }
            } else {
                throw new IllegalRequestException("No valid annotation type: " + typeName);
            }
    }
    
    /**
     * Creates a layer containing all annnotations of the given {@link AnnotationType type} for the work with the given pi. 
     * The annotations are groupd into annotation lists by page, if they belong to a page. Otherwise they are grouped in a single annotation list
     * 
     * @param pi            The pi of the containing work
     * @param typeName      The name of the {@link AnnotationType} for which annotations should be returned
     * @return  A IIIF layer
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException If there is no work with the given pi
     * @throws IllegalRequestException  If there is no annotation type of the given name
     * @throws IOException
     */
    @GET
    @Path("/{pi}/layer/{type}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Layer getLayer(@PathParam("pi") String pi, @PathParam("type") String typeName) throws PresentationException,
            IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException, ContentNotFoundException, IllegalRequestException, IOException {
                StructElement doc = getStructureBuilder().getDocument(pi);
                AnnotationType type = AnnotationType.valueOf(typeName.toUpperCase());
                if(type == null) {
                    throw new IllegalRequestException("No valid annotation type: " + typeName);
                }
                if (doc == null) {
                    throw new ContentNotFoundException("Not document with PI = " + pi + " found");
                } else if(AnnotationType.TEI.equals(type)) {
                    return getLayerBuilder().createAnnotationLayer(pi, type, Motivation.PAINTING, (id, repo) -> ContentResource.getTEIFiles(id, repo), (id, lang) -> ContentResource.getTEIURI(id, lang));
                } else if(AnnotationType.CMDI.equals(type)) {
                    return getLayerBuilder().createAnnotationLayer(pi, type, Motivation.DESCRIBING, (id, repo) -> ContentResource.getCMDIFiles(id, repo), (id, lang) -> ContentResource.getCMDIURI(id, lang));
    
                } else {
                    Map<AnnotationType, List<AnnotationList>> annoLists = getSequenceBuilder().addBaseSequence(null, doc, "");
                    Layer layer = getLayerBuilder().generateLayer(pi, annoLists, type);
                    return layer;
                }
    }



    private StructureBuilder getStructureBuilder() {
        if (this.structureBuilder == null) {
            try {
                this.structureBuilder = new StructureBuilder(this.servletRequest);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return this.structureBuilder;
    }

    public ManifestBuilder getManifestBuilder() {
        if (this.manifestBuilder == null) {
            try {
                this.manifestBuilder = new ManifestBuilder(servletRequest);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return manifestBuilder;
    }

    public SequenceBuilder getSequenceBuilder() {
        if (this.sequenceBuilder == null) {
            try {
                this.sequenceBuilder = new SequenceBuilder(servletRequest);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return sequenceBuilder;
    }
    
    public LayerBuilder getLayerBuilder() {
        if (this.layerBuilder == null) {
            try {
                this.layerBuilder = new LayerBuilder(servletRequest);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return layerBuilder;
    }



}
