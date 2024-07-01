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
package io.goobi.viewer.api.rest.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import de.intranda.api.iiif.presentation.v2.AbstractPresentationModelElement2;
import de.intranda.api.iiif.presentation.v3.AbstractPresentationModelElement3;
import de.intranda.api.iiif.search.AutoSuggestResult;
import de.intranda.api.iiif.search.SearchResult;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.controller.NetTools;

/**
 * <p>
 * Adds the @context property to all IIIF Presentation responses in the topmost json element.
 * </p>
 *
 * @author Florian Alpers
 *
 */
@Provider
@IIIFPresentationBinding
public class IIIFPresentationResponseFilter implements ContainerResponseFilter {

    public static final String CONTEXT_PRESENTATION_2 = "http://iiif.io/api/presentation/2/context.json";
    public static final String CONTEXT_PRESENTATION_3 = "http://iiif.io/api/presentation/3/context.json";
    public static final String CONTEXT_SEARCH = "http://iiif.io/api/search/1/context.json";
    public static final String CONTENT_TYPE_IIIF3 = "application/ld+json;profile=\"http://iiif.io/api/presentation/3/context.json\"";

    /* (non-Javadoc)
     * @see javax.ws.rs.container.ContainerResponseFilter#filter(javax.ws.rs.container.ContainerRequestContext, ContainerResponseContext)
     */
    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {

        Object responseObject = response.getEntity();
        if (responseObject instanceof AbstractPresentationModelElement2) {
            AbstractPresentationModelElement2 element = (AbstractPresentationModelElement2) responseObject;
            setResponseCharset(response, StandardCharsets.UTF_8.name());
            element.setContext(CONTEXT_PRESENTATION_2);
        } else if (responseObject instanceof AbstractPresentationModelElement3) {
            AbstractPresentationModelElement3 element = (AbstractPresentationModelElement3) responseObject;
            response.getHeaders().remove(NetTools.HTTP_HEADER_CONTENT_TYPE);
            response.getHeaders().add(NetTools.HTTP_HEADER_CONTENT_TYPE, CONTENT_TYPE_IIIF3);
            element.setContext(CONTEXT_PRESENTATION_3);
        } else if (responseObject instanceof SearchResult) {
            SearchResult element = (SearchResult) responseObject;
            setResponseCharset(response, "UTF-8");
            element.addContext(CONTEXT_PRESENTATION_3);
            if (!element.getHits().isEmpty()) {
                element.addContext(CONTEXT_SEARCH);
            }
        } else if (responseObject instanceof AutoSuggestResult) {
            AutoSuggestResult element = (AutoSuggestResult) responseObject;
            setResponseCharset(response, "UTF-8");
            element.addContext(CONTEXT_SEARCH);
        }

    }

    /**
     * <p>
     * setResponseCharset.
     * </p>
     *
     * @param response a {@link javax.ws.rs.container.ContainerResponseContext} object.
     * @param charset a {@link java.lang.String} object.
     */
    public void setResponseCharset(ContainerResponseContext response, String charset) {
        String contentType = response.getHeaderString(NetTools.HTTP_HEADER_CONTENT_TYPE) + ";charset=" + charset;
        response.getHeaders().remove(NetTools.HTTP_HEADER_CONTENT_TYPE);
        response.getHeaders().add(NetTools.HTTP_HEADER_CONTENT_TYPE, contentType);
    }

}
