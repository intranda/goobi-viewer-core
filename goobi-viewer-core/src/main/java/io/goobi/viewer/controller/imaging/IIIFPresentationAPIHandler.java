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
package io.goobi.viewer.controller.imaging;

import java.net.URISyntaxException;

import de.intranda.api.iiif.presentation.enums.AnnotationType;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.model.iiif.presentation.v2.builder.AbstractBuilder;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Creates urls to IIIF Presentation api calls to get manifests, canvases, annotationLists or layers
 *
 * @author Florian Alpers
 */
public class IIIFPresentationAPIHandler {

    private final AbstractBuilder builder;

    /**
     * <p>
     * Constructor for IIIFPresentationAPIHandler.
     * </p>
     *
     * @param urls
     * @param configuration a {@link io.goobi.viewer.controller.Configuration} object.
     * @throws java.net.URISyntaxException if any.
     */
    public IIIFPresentationAPIHandler(AbstractApiUrlManager urls, Configuration configuration) throws URISyntaxException {
        this.builder = new AbstractBuilder(urls) {
            //
        };
    }

    /**
     * Returns the url to the manifest for the given pi
     *
     * @param pi a {@link java.lang.String} object.
     * @param pageNo
     * @return The IIIF manifest
     * @throws java.net.URISyntaxException if any.
     */
    public String getPageManifestUrl(String pi, int pageNo) throws URISyntaxException {
        return builder.getPageManifestURI(pi, pageNo).toString();
    }
    
    /**
     * Returns the url to the manifest for the given pi
     *
     * @param pi a {@link java.lang.String} object.
     * @return The IIIF manifest
     * @throws java.net.URISyntaxException if any.
     */
    public String getManifestUrl(String pi) throws URISyntaxException {
        return builder.getManifestURI(pi).toString();
    }

    /**
     * Returns the url to a IIIF collection resource containing all top level collections for the field DC
     *
     * @return The IIIF collection url
     * @throws java.net.URISyntaxException if any.
     */
    public String getCollectionUrl() throws URISyntaxException {
        return getCollectionUrl(SolrConstants.DC);
    }

    /**
     * Returns the url to a IIIF collection resource containing all top level collections for the given field
     *
     * @return The IIIF collection url
     * @param field a {@link java.lang.String} object.
     * @throws java.net.URISyntaxException if any.
     */
    public String getCollectionUrl(String field) throws URISyntaxException {
        return getCollectionUrl(field, null);
    }

    /**
     * Returns the url to a IIIF collection resource for the given collection name for the given field
     *
     * @return The IIIF collection url
     * @param field a {@link java.lang.String} object.
     * @param collection a {@link java.lang.String} object.
     * @throws java.net.URISyntaxException if any.
     */
    public String getCollectionUrl(String field, String collection) throws URISyntaxException {
        return builder.getCollectionURI(field, collection).toString();
    }

    /**
     *
     * Returns a IIIF layer with all annotations of the given {@link AnnotationType type} within the work of the given pi
     *
     * @param pi a {@link java.lang.String} object.
     * @param annotationType a {@link java.lang.String} object.
     * @return The IIIF layer url
     * @throws java.net.URISyntaxException if any.
     */
    public String getLayerUrl(String pi, String annotationType) throws URISyntaxException {
        AnnotationType type = AnnotationType.valueOf(annotationType.toUpperCase());
        if (type == null) {
            throw new IllegalArgumentException(annotationType + " is not valid annotation type");
        }
        return builder.getLayerURI(pi, type).toString();
    }

    /**
     * Returns a IIIF annotation list containing all annotations of the given type for the given page
     *
     * @param pi a {@link java.lang.String} object.
     * @param pageOrder a int.
     * @param annotationType a {@link java.lang.String} object.
     * @return The IIIF annotation list
     * @throws java.net.URISyntaxException if any.
     */
    public String getAnnotationsUrl(String pi, int pageOrder, String annotationType) throws URISyntaxException {
        AnnotationType type = AnnotationType.valueOf(annotationType.toUpperCase());
        if (type == null) {
            throw new IllegalArgumentException(annotationType + " is not valid annotation type");
        }
        return builder.getAnnotationListURI(pi, pageOrder, type, false).toString();
    }

    /**
     * Returns the IIIF canvas for the given page
     *
     * @param pi a {@link java.lang.String} object.
     * @param pageOrder a int.
     * @return The IIIF canvas url
     * @throws java.net.URISyntaxException if any.
     */
    public String getCanvasUrl(String pi, int pageOrder) throws URISyntaxException {
        return builder.getCanvasURI(pi, pageOrder).toString();
    }

    /**
     * Returns a IIIF range representing the structural element for the given PI and logid. If the logid is the logid of the work itself, The
     * "CONTENT" range is returned, containing all topmost ranges but no canvases and no metadata
     *
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @return The IIIF range url
     * @throws java.net.URISyntaxException if any.
     */
    public String getRangeUrl(String pi, String logId) throws URISyntaxException {
        return builder.getRangeURI(pi, logId).toString();
    }
}
