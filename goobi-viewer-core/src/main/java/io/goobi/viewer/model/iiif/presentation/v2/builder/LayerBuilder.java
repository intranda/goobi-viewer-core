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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.presentation.enums.DcType;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.intranda.api.iiif.presentation.v2.Layer;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

/**
 * <p>
 * LayerBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class LayerBuilder extends AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(LayerBuilder.class);

    /**
     * <p>
     * Constructor for LayerBuilder.
     * </p>
     *
     * @param apiUrlManager
     */
    public LayerBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
    }

    /**
     * <p>
     * createAnnotationLayer.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param type a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @param motivation a {@link java.lang.String} object.
     * @param fileGetter a {@link java.util.function.BiFunction} object.
     * @param linkGetter a {@link java.util.function.BiFunction} object.
     * @return a {@link de.intranda.api.iiif.presentation.v2.Layer} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.io.IOException if any.
     * @throws java.net.URISyntaxException if any.
     */
    public Layer createAnnotationLayer(String pi, AnnotationType type, String motivation, BiFunction<String, String, List<Path>> fileGetter,
            BiFunction<String, String, URI> linkGetter) throws PresentationException, IndexUnreachableException, IOException, URISyntaxException {
        List<Path> files = new TextResourceBuilder().getTEIFiles(pi);
        //        List<Path> files = fileGetter.apply(pi, ContentResource.getDataRepository(pi));
        List<IAnnotation> annotations = new ArrayList<>();
        for (Path path : files) {
            Optional<String> language = getLanguage(path.getFileName().toString());
            language.ifPresent(lang -> {
                URI link = linkGetter.apply(pi, lang);
                URI annotationURI = getAnnotationListURI(pi, type);
                OpenAnnotation anno = createAnnotation(annotationURI, link, type.getFormat(), type.getDcType(), type, motivation);
                annotations.add(anno);
            });
        }
        URI annoListURI = getAnnotationListURI(pi, type);
        AnnotationList annoList = createAnnotationList(annotations, annoListURI, type);
        return generateLayer(pi, Collections.singletonMap(type, Collections.singletonList(annoList)), type);
    }

    /**
     * <p>
     * createAnnotation.
     * </p>
     *
     * @param annotationId a {@link java.net.URI} object.
     * @param linkURI a {@link java.net.URI} object.
     * @param format a {@link de.intranda.api.iiif.presentation.enums.Format} object.
     * @param dcType a {@link de.intranda.api.iiif.presentation.enums.DcType} object.
     * @param annoType a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @param motivation a {@link java.lang.String} object.
     * @return a {@link de.intranda.api.annotation.oa.OpenAnnotation} object.
     */
    public OpenAnnotation createAnnotation(URI annotationId, URI linkURI, Format format, DcType dcType, AnnotationType annoType, String motivation) {
        LinkingContent link = new LinkingContent(linkURI);
        if (format != null) {
            link.setFormat(format);
        }
        if (dcType != null) {
            link.setType(dcType.getLabel());
        }
        if (annoType != null) {
            link.setLabel(getLabel(annoType.name()));
        }
        OpenAnnotation annotation = new OpenAnnotation(annotationId);
        if (motivation != null) {
            annotation.setMotivation(motivation);
        } else {
            annotation.setMotivation(Motivation.PAINTING);
        }
        annotation.setBody(link);
        return annotation;
    }

    /**
     * <p>
     * createAnnotationList.
     * </p>
     *
     * @param annotations a {@link java.util.List} object.
     * @param id a {@link java.net.URI} object.
     * @param type a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @return a {@link de.intranda.api.iiif.presentation.v2.AnnotationList} object.
     */
    public AnnotationList createAnnotationList(List<IAnnotation> annotations, URI id, AnnotationType type) {
        AnnotationList annoList = new AnnotationList(id);
        annoList.setLabel(getLabel(type.name()));
        for (IAnnotation annotation : annotations) {
            annoList.addResource(annotation);
        }
        return annoList;
    }

    /**
     * <p>
     * generateLayer.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param annoLists a {@link java.util.Map} object.
     * @param annoType a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @return a {@link de.intranda.api.iiif.presentation.v2.Layer} object.
     * @throws java.net.URISyntaxException if any.
     */
    public Layer generateLayer(String pi, Map<AnnotationType, List<AnnotationList>> annoLists, AnnotationType annoType) throws URISyntaxException {
        Layer layer = new Layer(getLayerURI(pi, annoType));
        if (annoLists.get(annoType) != null) {
            annoLists.get(annoType).stream().forEach(layer::addOtherContent);
        }
        return layer;
    }

    /**
     * <p>
     * mergeAnnotationLists.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param annoLists a {@link java.util.Map} object.
     * @return a {@link java.util.Map} object.
     * @throws java.net.URISyntaxException if any.
     */
    public Map<AnnotationType, AnnotationList> mergeAnnotationLists(String pi, Map<AnnotationType, List<AnnotationList>> annoLists)
            throws URISyntaxException {
        Map<AnnotationType, AnnotationList> map = new EnumMap<>(AnnotationType.class);
        for (Entry<AnnotationType, List<AnnotationList>> entry : annoLists.entrySet()) {
            AnnotationList content = new AnnotationList(getAnnotationListURI(pi, entry.getKey()));
            content.setLabel(getLabel(entry.getKey().name()));
            entry.getValue()
                    .stream()
                    .filter(al -> al.getResources() != null)
                    .flatMap(al -> al.getResources().stream())
                    .forEach(content::addResource);
            map.put(entry.getKey(), content);
        }
        return map;
    }

    /**
     * <p>
     * getLanguage.
     * </p>
     *
     * @param filename a {@link java.lang.String} object.
     * @return a {@link java.util.Optional} object.
     */
    private static Optional<String> getLanguage(String filename) {
        String regex = "([a-z]{1,3})\\.[a-z]+";
        Matcher matcher = Pattern.compile(regex).matcher(filename);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }
}
