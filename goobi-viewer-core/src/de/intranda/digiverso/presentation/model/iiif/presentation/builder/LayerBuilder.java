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
package de.intranda.digiverso.presentation.model.iiif.presentation.builder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.iiif.presentation.AnnotationList;
import de.intranda.digiverso.presentation.model.iiif.presentation.Layer;
import de.intranda.digiverso.presentation.model.iiif.presentation.annotation.Annotation;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.LinkingContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.AnnotationType;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.DcType;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Format;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Motivation;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.servlets.rest.content.ContentResource;

/**
 * @author Florian Alpers
 *
 */
public class LayerBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(LayerBuilder.class);
    
    /**
     * @param request
     * @throws URISyntaxException
     */
    public LayerBuilder(HttpServletRequest request) throws URISyntaxException {
        super(request);
    }


    /**
     * @param servletUri
     * @param requestURI
     */
    public LayerBuilder(URI servletUri, URI requestURI) {
        super(servletUri, requestURI);
    }


    /**
     * @param pi
     * @param type
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws IOException
     * @throws URISyntaxException
     */
    public Layer createAnnotationLayer(String pi, AnnotationType type, Motivation motivation, BiFunction<String, String, List<Path>> fileGetter, BiFunction<String, String, URI> linkGetter)
            throws PresentationException, IndexUnreachableException, IOException, URISyntaxException {
//        List<Path> files = ContentResource.getTEIFiles(pi, ContentResource.getDataRepository(pi));
        List<Path> files = fileGetter.apply(pi, ContentResource.getDataRepository(pi));
        List<Annotation> annotations = new ArrayList<>();
        for (Path path : files) {
            Optional<String> language = ContentResource.getLanguage(path.getFileName().toString());
            language.ifPresent(lang -> {      
                try {
//                    URI link = ContentResource.getTEIURI(pi, lang);
                    URI link = linkGetter.apply(pi, lang);
                    URI annotationURI = getAnnotationListURI(pi, type);
                    Annotation anno = createAnnotation(annotationURI, link, type.getFormat(), type.getDcType(), type, motivation);
                    annotations.add(anno);
                } catch (URISyntaxException e) {
                    logger.error("Error creating url for TEI annotation in {}", lang, e);
                }
            });
        }
        URI annoListURI = getAnnotationListURI(pi, type);
        AnnotationList annoList = createAnnotationList(annotations, annoListURI, type);
        Layer layer = generateLayer(pi, Collections.singletonMap(type, Collections.singletonList(annoList)), type);
        return layer;
    }
    
    
    public Annotation createAnnotation(URI annotationId, URI linkURI, Format format, DcType dcType, AnnotationType annoType, Motivation motivation) {
        LinkingContent link = new LinkingContent(linkURI);
        if(format != null) {            
            link.setFormat(format);
        }
        if(dcType != null) {            
            link.setType(dcType);
        }
        if(annoType != null) {            
            link.setLabel(IMetadataValue.getTranslations(annoType.name()));
        }
        Annotation annotation = new Annotation(annotationId);
        if(motivation != null) {
            annotation.setMotivation(motivation);
        } else {
            annotation.setMotivation(Motivation.PAINTING);
        }
        annotation.setResource(link);
        return annotation;
    }
    
    public AnnotationList createAnnotationList(List<Annotation> annotations, URI id, AnnotationType type) {
        AnnotationList annoList = new AnnotationList(id);
        annoList.setLabel(IMetadataValue.getTranslations(type.name()));
        for (Annotation annotation : annotations) {            
            annoList.addResource(annotation);
        }
        return annoList;
    }
    
    

    /**
     * @param pi
     * @param annoLists
     * @param topLogId
     * @param topRange
     * @return
     * @throws URISyntaxException
     */
    public Layer generateContentLayer(String pi, Map<AnnotationType, List<AnnotationList>> annoLists, String logId)
            throws URISyntaxException {
        Layer layer = new Layer(getLayerURI(pi, logId));
        for (AnnotationType annoType : annoLists.keySet()) {
            AnnotationList content = new AnnotationList(getAnnotationListURI(pi, annoType));
            content.setLabel(IMetadataValue.getTranslations(annoType.name()));
            annoLists.get(annoType).stream()
            .filter(al -> al.getResources() != null)
            .flatMap(al -> al.getResources().stream())
            .forEach(annotation -> content.addResource(annotation));
            layer.addOtherContent(content);
        }
        return layer;
    }
    
    public Layer generateLayer(String pi, Map<AnnotationType, List<AnnotationList>> annoLists, AnnotationType annoType)
            throws URISyntaxException {
        Layer layer = new Layer(getLayerURI(pi, annoType));
        if(annoLists.get(annoType) != null) {
            annoLists.get(annoType).stream()
            .forEach(al -> layer.addOtherContent(al));
        }
        return layer;
    }
    
    public Map<AnnotationType, AnnotationList> mergeAnnotationLists(String pi, Map<AnnotationType, List<AnnotationList>> annoLists)
            throws URISyntaxException {
        Map<AnnotationType, AnnotationList> map = new HashMap<>();
        for (AnnotationType annoType : annoLists.keySet()) {
            AnnotationList content = new AnnotationList(getAnnotationListURI(pi, annoType));
            content.setLabel(IMetadataValue.getTranslations(annoType.name()));
            annoLists.get(annoType).stream()
            .filter(al -> al.getResources() != null)
            .flatMap(al -> al.getResources().stream())
            .forEach(annotation -> content.addResource(annotation));
            map.put(annoType, content);
        }
        return map;
    }
}
