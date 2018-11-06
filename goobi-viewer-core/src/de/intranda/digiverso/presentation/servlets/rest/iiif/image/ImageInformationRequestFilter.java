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
package de.intranda.digiverso.presentation.servlets.rest.iiif.image;

import java.io.IOException;
import java.util.List;
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

import org.apache.commons.lang.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.StringTools;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;

@Provider
@ContentServerImageInfoBinding
public class ImageInformationRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ImageInformationRequestFilter.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @SuppressWarnings("unchecked")
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        try {
            String requestPath = servletRequest.getRequestURI();
            requestPath = requestPath.substring(requestPath.indexOf("image/") + 6);
            logger.trace("Filtering request " + requestPath);
            StrTokenizer tokenizer = new StrTokenizer(requestPath, "/");
            List<String> pathSegments = tokenizer.getTokenList();
            String pi = pathSegments.get(0);
            String imageName = pathSegments.get(1);
            imageName = StringTools.decodeUrl(imageName);
            if (forwardToCanonicalUrl(pi, imageName, servletRequest, servletResponse)) {
                //if page order is given for image filename, forward to url with correct filename
                return;
            } else {
                //only for actual image requests, no info requests
                if (!BeanUtils.getImageDeliveryBean().isExternalUrl(imageName) && !BeanUtils.getImageDeliveryBean().isCmsUrl(imageName)
                        && !BeanUtils.getImageDeliveryBean().isStaticImageUrl(imageName)) {
                    filterForAccessConditions(request, pi, imageName);
                }
            }
        } catch (ServiceNotAllowedException e) {
            String mediaType = MediaType.APPLICATION_JSON;
            //            if (request.getUriInfo() != null && request.getUriInfo().getPath().endsWith("json")) {
            //                mediaType = MediaType.APPLICATION_JSON;
            //            }
            Response response = Response.status(Status.FORBIDDEN).type(mediaType).entity(new ErrorMessage(Status.FORBIDDEN, e, false)).build();
            request.abortWith(response);
        } catch (ViewerConfigurationException e) {
            Response response =
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorMessage(Status.INTERNAL_SERVER_ERROR, e, false)).build();
            request.abortWith(response);
        }
    }

    /**
     * @param pi
     * @param imageName
     * @throws IOException
     */
    public static boolean forwardToCanonicalUrl(String pi, String imageName, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (imageName != null && !imageName.contains(".") && imageName.matches("\\d+")) {
            try {
                Optional<String> filename = DataManager.getInstance().getSearchIndex().getFilename(pi, Integer.parseInt(imageName));
                if (filename.isPresent()) {
                    String redirectURI = request.getRequestURI().replace("/" + imageName + "/", "/" + filename.get() + "/");
                    response.sendRedirect(redirectURI);
                    return true;
                }
            } catch (NumberFormatException | PresentationException | IndexUnreachableException e) {
                logger.error("Unable to resolve image file for image order {} and pi {}", imageName, pi);
            }
        }
        return false;

    }

    /**
     * @param requestPath
     * @param pathSegments
     * @throws ServiceNotAllowedException
     * @throws IndexUnreachableException
     */
    private void filterForAccessConditions(ContainerRequestContext request, String pi, String contentFileName)
            throws ServiceNotAllowedException {
        logger.trace("filterForAccessConditions: " + servletRequest.getSession().getId());

        boolean access = false;
        try {
            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_LIST, servletRequest);
        } catch (IndexUnreachableException e) {
            throw new ServiceNotAllowedException("Serving this image is currently impossibe due to ");
        } catch (DAOException e) {
            throw new ServiceNotAllowedException("Serving this image is currently impossibe due to ");
        }

        if (!access) {
            throw new ServiceNotAllowedException("Serving this image is restricted due to access conditions");
        }
    }


}
