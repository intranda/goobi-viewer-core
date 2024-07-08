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
import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;

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
    @Context
    private HttpServletResponse servletResponse;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        logger.trace("filter");

        String pi = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_PI);
        String imageName = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_FILENAME);

        imageName = StringTools.decodeUrl(imageName);
        // logger.trace("image: {}", imageName); //NOSONAR Logging sometimes needed for debugging
        if (forwardToCanonicalUrl(pi, imageName, servletRequest, servletResponse)) {
            //if page order is given for image filename, forward to url with correct filename
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
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @return a boolean.
     * @throws java.io.IOException if any.
     */
    public boolean forwardToCanonicalUrl(String pi, String imageName, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (imageName == null || imageName.contains(".") || !imageName.matches("\\d+")) {
            return false;
        }
        //        if (imageName != null && !imageName.contains(".") && imageName.matches("\\d+")) {
        try {
            Optional<String> filename = DataManager.getInstance().getSearchIndex().getFilename(pi, imageName);

            if (filename.isPresent()) {
                String filenameValue = filename.get();
                request.setAttribute(FilterTools.ATTRIBUTE_FILENAME, filenameValue);
                String redirectURI = DataManager.getInstance()
                        .getRestApiManager()
                        .getContentApiManager()
                        .map(urls -> urls
                                .path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_INFO)
                                .params(pi, filenameValue)
                                .build())
                        .orElse(request.getRequestURI().replace("/" + imageName, "/" + filenameValue));
                response.sendRedirect(redirectURI);
                return true;
            } else if (imageName.matches("\\d+")) {
                filename = DataManager.getInstance().getSearchIndex().getFilename(pi, Integer.parseInt(imageName));
                if (filename.isPresent()) {
                    String filenameValue = filename.get();
                    request.setAttribute(FilterTools.ATTRIBUTE_FILENAME, filenameValue);
                    String redirectURI = DataManager.getInstance()
                            .getRestApiManager()
                            .getContentApiManager()
                            .map(urls -> urls
                                    .path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_INFO)
                                    .params(pi, filenameValue)
                                    .build())
                            .orElse(request.getRequestURI().replace("/" + imageName, "/" + filenameValue));
                    response.sendRedirect(redirectURI);
                    return true;
                }
            }
        } catch (NumberFormatException | PresentationException | IndexUnreachableException e) {
            logger.error("Unable to resolve image file for image order {} and pi {}", imageName, pi);
        }
        //        }
        return false;
    }

}
