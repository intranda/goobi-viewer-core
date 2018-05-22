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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import de.intranda.digiverso.presentation.model.iiif.presentation.AbstractPresentationModelElement;

/**
 * @author Florian Alpers
 *
 *Adds the @context property to all IIIF Presentation responses in the topmost json element
 *
 */
@Provider
@IIIFPresentationBinding
public class IIIFPresentationResponseFilter implements ContainerResponseFilter {

    public static final String CONTEXT = "http://iiif.io/api/presentation/2/context.json";
    
    /* (non-Javadoc)
     * @see javax.ws.rs.container.ContainerResponseFilter#filter(javax.ws.rs.container.ContainerRequestContext, javax.ws.rs.container.ContainerResponseContext)
     */
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {

        Object responseObject = response.getEntity();
        if (responseObject != null && responseObject instanceof AbstractPresentationModelElement) {
            AbstractPresentationModelElement element = (AbstractPresentationModelElement) responseObject;
            setResponseCharset(response, "UTF-8");
            element.setContext(CONTEXT);
        }
        
    }

    /**
     * @param response
     */
    public void setResponseCharset(ContainerResponseContext response, String charset) {
        String contentType = response.getHeaderString("Content-Type") + ";charset=" + charset;
        response.getHeaders().remove("Content-Type");
        response.getHeaders().add("Content-Type", contentType);
    }

}
