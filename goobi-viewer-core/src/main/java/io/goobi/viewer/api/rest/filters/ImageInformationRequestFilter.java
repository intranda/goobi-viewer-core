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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE_INFO;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * <p>
 * Forwards requests to IIIF image resources referencing a image number (Solr-field "ORDER") to a requests with the appropriate filename.
 * </p>
 */
@Provider
@ContentServerImageInfoBinding
@Priority(FilterTools.PRIORITY_REDIRECT)
public class ImageInformationRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(ImageInformationRequestFilter.class);

    @Context
    private HttpServletRequest servletRequest;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        logger.trace("filter");

        String pi = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_PI);
        String imageName = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_FILENAME);

        imageName = StringTools.decodeUrl(imageName);
        // logger.trace("image: {}", imageName); //NOSONAR Debug
        String redirectURI = forwardToCanonicalUrl(pi, imageName, servletRequest);
        if (redirectURI != null) {
            //if page order is given for image filename, redirect to url with correct filename
            try {
                request.abortWith(Response.status(Response.Status.FOUND).location(new URI(redirectURI)).build());
            } catch (URISyntaxException e) {
                logger.error("Invalid redirect URI '{}': {}", redirectURI, e.getMessage());
            }
            return;
        }
    }

    /**
     * <p>
     * forwardToCanonicalUrl.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param imageName a {@link java.lang.String} object.
     * @param request a {@link jakarta.servlet.http.HttpServletRequest} object.
     * @return the redirect URI if a redirect is needed, or null otherwise.
     * @throws java.io.IOException if any.
     */
    public String forwardToCanonicalUrl(String pi, String imageName, HttpServletRequest request)
            throws IOException {
        if (imageName == null || imageName.contains(".") || !imageName.matches("\\d+")) {
            return null;
        }
        try {
            Optional<String> filename = DataManager.getInstance().getSearchIndex().getFilename(pi, imageName);

            if (filename.isPresent()) {
                String filenameValue = filename.get();
                request.setAttribute(FilterTools.ATTRIBUTE_FILENAME, filenameValue);
                return DataManager.getInstance()
                        .getRestApiManager()
                        .getContentApiManager()
                        .map(urls -> urls
                                .path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_INFO)
                                .params(pi, filenameValue)
                                .build())
                        .orElse(request.getRequestURI().replace("/" + imageName, "/" + filenameValue));
            } else if (imageName.matches("\\d+")) {
                filename = DataManager.getInstance().getSearchIndex().getFilename(pi, Integer.parseInt(imageName));
                if (filename.isPresent()) {
                    String filenameValue = filename.get();
                    request.setAttribute(FilterTools.ATTRIBUTE_FILENAME, filenameValue);
                    return DataManager.getInstance()
                            .getRestApiManager()
                            .getContentApiManager()
                            .map(urls -> urls
                                    .path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_INFO)
                                    .params(pi, filenameValue)
                                    .build())
                            .orElse(request.getRequestURI().replace("/" + imageName, "/" + filenameValue));
                }
            }
        } catch (NumberFormatException | PresentationException | IndexUnreachableException e) {
            logger.error("Unable to resolve image file for image order {} and pi {}", imageName, pi);
        }
        return null;
    }

}
