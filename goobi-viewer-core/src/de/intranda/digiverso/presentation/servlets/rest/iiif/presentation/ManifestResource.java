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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
import de.intranda.digiverso.presentation.model.iiif.presentation.builder.ManifestBuilder;
import de.intranda.digiverso.presentation.model.iiif.presentation.builder.SequenceBuilder;
import de.intranda.digiverso.presentation.model.iiif.presentation.builder.StructureBuilder;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.AnnotationType;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.ViewingHint;
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

    @GET
    @Path("/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public IPresentationModelElement geManifest(@PathParam("pi") String pi)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException {

        StructElement doc = getManifestBuilder().getDocument(pi);
        servletResponse.addHeader("Access-Control-Allow-Origin", "*");

        IPresentationModelElement manifest = getManifestBuilder().generateManifest(doc);

        if (manifest instanceof Collection) {
            getManifestBuilder().addVolumes((Collection) manifest, doc.getLuceneId());
        } else if (manifest instanceof Manifest) {
            getManifestBuilder().addAnchor((Manifest) manifest, doc);
            Map<AnnotationType, List<AnnotationList>> annoLists =
            getSequenceBuilder().addBaseSequence((Manifest) manifest, doc, manifest.getId().toString());

            String topLogId = doc.getMetadataValue(SolrConstants.LOGID);
            if (StringUtils.isNotBlank(topLogId)) {
                Range topRange = getStructureBuilder().generateStructure(doc, getStructureBuilder().getRangeURI(pi, topLogId), true);
                
                if(topRange.getMembers() != null) {
                    for (IPresentationModelElement ele : topRange.getMembers()) {
                        if(ele instanceof Range) {
                            ((Range) ele).setViewingHint(ViewingHint.top);
                            ((Manifest)manifest).addStructure((Range)ele);
                        }
                    }
                }
//                ((Manifest) manifest).setStructure(topRange);

//                Layer layer = getManifestBuilder().generateContentLayer(pi, annoLists, null);
//                topRange.setContentLayer(layer);
            }
        }

        return manifest;

    }


    @GET
    @Path("/{pi}/sequence/basic")
    @Produces({ MediaType.APPLICATION_JSON })
    public Sequence getBasicSequence(@PathParam("pi") String pi) throws PresentationException, IndexUnreachableException, URISyntaxException,
            ConfigurationException, DAOException, IllegalRequestException {

        StructElement doc = getManifestBuilder().getDocument(pi);
        servletResponse.addHeader("Access-Control-Allow-Origin", "*");

        IPresentationModelElement manifest = getManifestBuilder().generateManifest(doc);

        if (manifest instanceof Collection) {
            //            addVolumes((Collection) manifest, doc.getLuceneId(), getBaseUrl());
            throw new IllegalRequestException("Identifier refers to a collection which does not have a sequence");
        } else if (manifest instanceof Manifest) {
            //            addAnchor((Manifest) manifest, doc, getBaseUrl());
            getSequenceBuilder().addBaseSequence((Manifest) manifest, doc, manifest.getId().toString());
            return ((Manifest) manifest).getSequences().get(0);
        }
        throw new IllegalRequestException("Not manifest with identifier " + pi + " found");

    }

    @GET
    @Path("/{pi}/range/{logId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Range getRange(@PathParam("pi") String pi, @PathParam("logId") String logId)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException {

        StructElement doc = getStructureBuilder().getDocument(pi, logId);

        if (doc == null) {
            throw new NotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found");
        } else {
            Range topRange = getStructureBuilder().generateStructure(doc, getStructureBuilder().getRangeURI(pi, logId), false);
            getStructureBuilder().populatePages(doc, topRange);
            return topRange;
        }
    }

    @GET
    @Path("/{pi}/canvas/{physPageNo}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Canvas getCanvas(@PathParam("pi") String pi, @PathParam("physPageNo") int physPageNo)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException {
        StructElement doc = getManifestBuilder().getDocument(pi);
        PhysicalElement page = getSequenceBuilder().getPage(doc, physPageNo);
        Canvas canvas = getSequenceBuilder().generateCanvas(doc, page);
        getSequenceBuilder().addOtherContent(page, canvas, ContentResource.getDataRepository(pi));
        return canvas;
    }
    
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
                Map<AnnotationType, AnnotationList> annotations = getSequenceBuilder().addOtherContent(page, canvas, ContentResource.getDataRepository(pi));
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

    @GET
    @Path("/{pi}/list/{type}")
    @Produces({ MediaType.APPLICATION_JSON })
    public AnnotationList getOtherContent(@PathParam("pi") String pi, @PathParam("type") String typeName) throws PresentationException,
            IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException, ContentNotFoundException, IllegalRequestException {
            AnnotationType type = AnnotationType.valueOf(typeName.toUpperCase());
            if(type != null) {                
                StructElement doc = getStructureBuilder().getDocument(pi);

                if (doc == null) {
                    throw new NotFoundException("Not document with PI = " + pi + " found");
//                    throw new NotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found");
                } else {
                    Map<AnnotationType, List<AnnotationList>> annoLists = getSequenceBuilder().addBaseSequence(null, doc, "");
                    Map<AnnotationType, AnnotationList> annoMap = getManifestBuilder().mergeAnnotationLists(pi, annoLists);
                    if(annoMap.get(type) != null) {                        
                        AnnotationList al = annoMap.get(type);
                        Layer layer = new Layer(getManifestBuilder().getLayerURI(pi, type));
                        layer.setLabel(IMetadataValue.getTranslations(type.name()));
                        al.addWithin(layer);
                        return al;
                    } else {
                        throw new ContentNotFoundException("Not annotations found of type " + type);
                    }
                }
            } else {
                throw new IllegalRequestException("No valid annotation type: " + typeName);
            }
    }
    
    @GET
    @Path("/{pi}/layer/{type}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Layer getLayer(@PathParam("pi") String pi, @PathParam("type") String typeName) throws PresentationException,
            IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException, ContentNotFoundException, IllegalRequestException {
                StructElement doc = getStructureBuilder().getDocument(pi);
                AnnotationType type = AnnotationType.valueOf(typeName.toUpperCase());
                
                if (doc == null) {
                    throw new NotFoundException("Not document with PI = " + pi + " found");
//                    throw new NotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found");
                } else {
                    Map<AnnotationType, List<AnnotationList>> annoLists = getSequenceBuilder().addBaseSequence(null, doc, "");
                    Layer layer = getManifestBuilder().generateLayer(pi, annoLists, type);
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

    /**
     * @param baseUrl
     * @param pi
     * @return
     * @throws URISyntaxException
     */
    public static URI getManifestUrl(String baseUrl, String pi) throws URISyntaxException {
        return new URI(baseUrl + "/" + pi);
    }

    protected String getPath() {
        return "/manifests";
    }

}
