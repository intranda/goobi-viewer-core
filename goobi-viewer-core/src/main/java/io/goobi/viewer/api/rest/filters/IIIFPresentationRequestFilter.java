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
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;

/**
 * <p>
 * Filter for all IIIF Presentation resources. Checks the {@link io.goobi.viewer.model.security.IPrivilegeHolder#PRIV_GENERATE_IIIF_MANIFEST} privilege for the
 * request.
 * </p>
 */
@Provider
@IIIFPresentationBinding
public class IIIFPresentationRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(IIIFPresentationRequestFilter.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        try {
            String requestPI = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_PI);
            String requestLogId = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_LOGID);
            if (StringUtils.isNotBlank(requestPI)) {
                filterForAccessConditions(requestPI, requestLogId);
            }
        } catch (ServiceNotAllowedException e) {
            String mediaType = MediaType.APPLICATION_JSON;
            //            if (request.getUriInfo() != null && request.getUriInfo().getPath().endsWith("json")) {
            //                mediaType = MediaType.APPLICATION_JSON;
            //            }
            Response response = Response.status(Status.FORBIDDEN).type(mediaType).entity(new ErrorMessage(Status.FORBIDDEN, e, false)).build();
            request.abortWith(response);
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
    public static boolean forwardToCanonicalUrl(String pi, String imageName, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (imageName != null && !imageName.contains(".")) {
            try {
                Optional<String> filename = DataManager.getInstance().getSearchIndex().getFilename(pi, imageName);

                if (filename.isPresent()) {
                    String redirectURI = request.getRequestURI().replace("/" + imageName + "/", "/" + filename.get() + "/");
                    response.sendRedirect(redirectURI);
                    return true;
                } else if (imageName.matches("\\d+")) {
                    filename = DataManager.getInstance().getSearchIndex().getFilename(pi, Integer.parseInt(imageName));
                    if (filename.isPresent()) {
                        String redirectURI = request.getRequestURI().replace("/" + imageName + "/", "/" + filename.get() + "/");
                        response.sendRedirect(redirectURI);
                        return true;
                    }
                }

            } catch (NumberFormatException | PresentationException | IndexUnreachableException e) {
                logger.error("Unable to resolve image file for image order {} and pi {}", imageName, pi);
            }
        }
        return false;

    }

    /**
     * @param pi
     * @param logId
     * @throws ServiceNotAllowedException
     */
    private void filterForAccessConditions(String pi, String logId) throws ServiceNotAllowedException {
        logger.trace("filterForAccessConditions: {}", servletRequest.getSession().getId());

        boolean access = false;
        try {
            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, logId, IPrivilegeHolder.PRIV_GENERATE_IIIF_MANIFEST,
                    servletRequest).isGranted();
        } catch (IndexUnreachableException | DAOException e) {
            throw new ServiceNotAllowedException("Serving this image is currently impossibe due to ");
        } catch (RecordNotFoundException e) {
            throw new ServiceNotAllowedException("Record not found in index: " + pi);
        }

        if (!access) {
            throw new ServiceNotAllowedException("Serving this image is restricted due to access conditions");
        }
    }

}
