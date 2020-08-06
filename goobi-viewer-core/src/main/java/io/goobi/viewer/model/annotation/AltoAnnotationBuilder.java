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
package io.goobi.viewer.model.annotation;

import java.awt.Rectangle;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.oa.SpecificResource;
import de.intranda.api.annotation.oa.SpecificResourceURI;
import de.intranda.api.annotation.oa.TextualResource;
import de.intranda.api.annotation.oa.FragmentSelector;

import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import de.intranda.digiverso.ocr.alto.utils.IDManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.v1.ApiUrls;

/**
 * Creates an {@link de.intranda.api.iiif.presentation.AnnotationList} of {@link TextualAnnotation}s from the content of an ALTO document. Depending
 * on selected granularity, it is either one annotation per page, per TextBlock, per line or per word
 *
 * @author Florian
 */
public class AltoAnnotationBuilder {

    private AbstractApiUrlManager urls;
    private String format;
    
    public AltoAnnotationBuilder(AbstractApiUrlManager urls, String format) {
        this.urls = urls;
        this.format = format;
    }
    
    /**
     * <p>
     * createAnnotations.
     * </p>
     *
     * @param alto a {@link de.intranda.digiverso.ocr.alto.model.structureclasses.Page} object.
     * @param canvas a {@link de.intranda.api.iiif.presentation.Canvas} object.
     * @param granularity a {@link io.goobi.viewer.model.annotation.AltoAnnotationBuilder.Granularity} object.
     * @param baseUrl a {@link java.lang.String} object.
     * @param urlOnlyTarget a boolean.
     * @return a {@link java.util.List} object.
     */
    public List<IAnnotation> createAnnotations(Page alto, String pi, Integer pageNo, Canvas canvas, Granularity granularity, boolean urlOnlyTarget) {
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
        }

        List<IAnnotation> annoList =
                elementsToInclude.stream().map(element -> createAnnotation(element, pi, pageNo, canvas, urlOnlyTarget)).collect(Collectors.toList());
        return annoList;
    }

    /**
     * <p>
     * createAnnotations.
     * </p>
     *
     * @param elements a {@link java.util.List} object.
     * @param canvas a {@link de.intranda.api.iiif.presentation.Canvas} object.
     * @param baseUrl a {@link java.lang.String} object.
     * @param urlOnlyTarget a boolean.
     * @return a {@link java.util.List} object.
     */
    public List<IAnnotation> createAnnotations(List<GeometricData> elements, String pi, Integer pageNo, Canvas canvas, boolean urlOnlyTarget) {
        List<IAnnotation> annoList =
                elements.stream().map(element -> createAnnotation(element, pi, pageNo, canvas, urlOnlyTarget)).collect(Collectors.toList());
        return annoList;
    }

    /**
     * <p>
     * createAnnotation.
     * </p>
     *
     * @param element a {@link de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData} object.
     * @param canvas a {@link de.intranda.api.annotation.IResource} object.
     * @param baseUrl a {@link java.lang.String} object.
     * @param urlOnlyTarget a boolean.
     * @return a {@link de.intranda.api.annotation.IAnnotation} object.
     */
    public IAnnotation createAnnotation(GeometricData element, String pi, Integer pageNo, IResource canvas, boolean urlOnlyTarget) {
        String id = Optional.ofNullable(element.getId()).orElse(buildId(element));
        AbstractAnnotation anno = new OpenAnnotation(createAnnotationId(pi, pageNo, id));
        anno.setMotivation(Motivation.PAINTING);
        anno.setTarget(createSpecificResource(canvas, element.getBounds(), urlOnlyTarget));
        TextualResource body = new TextualResource(element.getContent());
        anno.setBody(body);
        return anno;
    }
    
    /**
     * Method to construct alto element id if no id attribute is available 
     * @param e
     * @return
     */
    private String buildId(GeometricData e) {
        return e.getClass().getSimpleName() + "_" +  e.getBounds().x + "_" + e.getBounds().y + "_" + e.getBounds().width + "_" + e.getBounds().height;
    }

    /**
     * @param canvas
     * @param i
     * @param j
     * @param width
     * @param height
     * @return
     */
    private IResource createSpecificResource(IResource canvas, Rectangle area, boolean urlOnly) {
        SpecificResource part;
        if (urlOnly) {
            part = new SpecificResourceURI(canvas.getId(), new FragmentSelector(area));
        } else {
            part = new SpecificResource(canvas.getId(), new FragmentSelector(area));
        }
        return part;
    }

    /**
     * @param listId
     * @param id
     * @return
     */
    private URI createAnnotationId(String pi, Integer pageNo, String id) {
        ApiPath path = urls.path(ApiUrls.ANNOTATIONS, ApiUrls.ANNOTATIONS_ALTO).params(pi, pageNo, id);
        if(StringUtils.isNotBlank(format)) {
            path = path.query("format", format);
        }
        return URI.create(path.build());
    }

    public static enum Granularity {
        PAGE,
        PAGEAREA,
        BLOCK,
        LINE,
        WORD
    }
}
