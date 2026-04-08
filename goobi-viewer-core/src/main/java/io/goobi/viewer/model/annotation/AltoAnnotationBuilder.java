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
package io.goobi.viewer.model.annotation;

import java.awt.Rectangle;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.oa.FragmentSelector;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.oa.SpecificResource;
import de.intranda.api.annotation.oa.SpecificResourceURI;
import de.intranda.api.annotation.oa.TextualResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.v1.ApiUrls;

/**
 * Creates an {@link de.intranda.api.iiif.presentation.v2.AnnotationList} of {@link TextualResource}s from the content of an ALTO document.
 *
 * <p>Depending on selected granularity, it is either one annotation per page, per TextBlock, per line or per word
 *
 * @author Florian
 */
public class AltoAnnotationBuilder {

    private AbstractApiUrlManager urls;
    private String format;

    /**
     *
     * @param urls the URL manager for building annotation API paths
     * @param format the annotation format, e.g. "oa" for OpenAnnotation or "wa" for WebAnnotation
     */
    public AltoAnnotationBuilder(AbstractApiUrlManager urls, String format) {
        this.urls = urls;
        this.format = format;
    }

    /**
     * createAnnotations.
     *
     * @param alto parsed ALTO page from which annotations are extracted
     * @param pi persistent identifier of the digitized work
     * @param pageNo physical page number within the work
     * @param target the IIIF canvas resource used as annotation target
     * @param granularity level of text elements to generate annotations for
     * @param urlOnlyTarget if true, use URI-only specific resources as annotation targets
     * @return a {@link java.util.List} object.
     */
    public List<AbstractAnnotation> createAnnotations(Page alto, String pi, Integer pageNo, IResource target, Granularity granularity,
            boolean urlOnlyTarget) {
        List<GeometricData> elementsToInclude = new ArrayList<>();
        switch (granularity) {
            case PAGE:
                elementsToInclude.add(alto);
                break;
            case PAGEAREA:
                elementsToInclude.addAll(alto.getChildren());
                break;
            case BLOCK:
                elementsToInclude.addAll(alto.getAllTextBlocksAsList());
                break;
            case LINE:
                elementsToInclude.addAll(alto.getAllLinesAsList());
                break;
            case WORD:
                elementsToInclude.addAll(alto.getAllWordsAsList());
                break;
            default:
                break;
        }

        return elementsToInclude.stream().map(element -> createAnnotation(element, pi, pageNo, target, urlOnlyTarget)).collect(Collectors.toList());
    }

    /**
     * createAnnotations.
     *
     * @param elements list of ALTO geometric elements to convert to annotations
     * @param pi persistent identifier of the digitized work
     * @param pageNo physical page number within the work
     * @param target the IIIF canvas resource used as annotation target
     * @param urlOnlyTarget if true, use URI-only specific resources as annotation targets
     * @return a {@link java.util.List} object.
     */
    public List<AbstractAnnotation> createAnnotations(List<GeometricData> elements, String pi, Integer pageNo, IResource target,
            boolean urlOnlyTarget) {
        return elements.stream().map(element -> createAnnotation(element, pi, pageNo, target, urlOnlyTarget)).collect(Collectors.toList());
    }

    /**
     * createAnnotation.
     *
     * @param element ALTO geometric element providing bounds and text content
     * @param pi persistent identifier of the digitized work
     * @param pageNo physical page number within the work
     * @param canvas IIIF canvas resource to annotate
     * @param urlOnlyTarget if true, use URI-only specific resources as annotation targets
     * @return a {@link de.intranda.api.annotation.IAnnotation} object.
     */
    public AbstractAnnotation createAnnotation(GeometricData element, String pi, Integer pageNo, IResource canvas, boolean urlOnlyTarget) {
        String id = Optional.ofNullable(element.getId()).orElse(buildId(element));
        AbstractAnnotation anno;
        if ("oa".equalsIgnoreCase(format)) {
            anno = new OpenAnnotation(createAnnotationId(pi, pageNo, id));
            anno.setBody(new TextualResource(element.getContent()));
        } else {
            anno = new WebAnnotation(createAnnotationId(pi, pageNo, id));
            anno.setBody(new de.intranda.api.annotation.wa.TextualResource(element.getContent()));
        }
        anno.setTarget(createSpecificResource(canvas, element.getBounds(), urlOnlyTarget));
        anno.setMotivation(Motivation.PAINTING);
        return anno;
    }

    /**
     * Method to construct alto element id if no id attribute is available.
     *
     * @param e the ALTO geometric element lacking an explicit id attribute
     * @return {@link String}
     */
    private static String buildId(GeometricData e) {
        return e.getClass().getSimpleName() + "_" + e.getBounds().x + "_" + e.getBounds().y + "_" + e.getBounds().width + "_" + e.getBounds().height;
    }

    /**
     * @param canvas the IIIF canvas resource to target
     * @param area the rectangular region on the canvas
     * @param urlOnly if true, return a URI-only specific resource
     * @return {@link IResource}
     */
    private IResource createSpecificResource(IResource canvas, Rectangle area, boolean urlOnly) {
        IResource part;
        if (urlOnly) {
            part = new SpecificResourceURI(canvas.getId(), new FragmentSelector(area));
        } else if ("oa".equalsIgnoreCase(format)) {
            part = new SpecificResource(canvas.getId(), new FragmentSelector(area));
        } else {
            part = new de.intranda.api.annotation.wa.SpecificResource(canvas.getId(), new de.intranda.api.annotation.wa.FragmentSelector(area));

        }
        return part;
    }

    /**
     * @param pi persistent identifier of the digitized work
     * @param pageNo physical page number within the work
     * @param id element-level identifier within the ALTO document
     * @return {@link URI}
     */
    private URI createAnnotationId(String pi, Integer pageNo, String id) {
        ApiPath path = urls.path(ApiUrls.ANNOTATIONS, ApiUrls.ANNOTATIONS_ALTO).params(pi, pageNo, id);
        if (StringUtils.isNotBlank(format)) {
            path = path.query("format", format);
        }
        return URI.create(path.build());
    }

    public enum Granularity {
        PAGE,
        PAGEAREA,
        BLOCK,
        LINE,
        WORD
    }
}
