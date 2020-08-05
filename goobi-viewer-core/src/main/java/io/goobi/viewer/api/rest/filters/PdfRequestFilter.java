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
package io.goobi.viewer.api.rest.filters;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfBinding;

/**
 * <p>
 * PdfRequestFilter class.
 * </p>
 */
@Provider
@ContentServerPdfBinding
public class PdfRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(PdfRequestFilter.class);

    @Context
    private HttpServletRequest servletRequest;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        try {

            if (DataManager.getInstance().getConfiguration().isPdfApiDisabled()) {
                throw new ServiceNotAllowedException("PDF API is disabled");
            }

            Path requestPath = Paths.get(request.getUriInfo().getPath());
            //            String requestPath = request.getUriInfo().getPath();

            String type = requestPath.getName(0).toString();
            String pi = null;
            String divId = null;
            String imageName = null;
            String privName = IPrivilegeHolder.PRIV_DOWNLOAD_PDF;
            if (servletRequest.getAttribute("pi") != null) {
                pi = (String) servletRequest.getAttribute("pi");
                divId = (String) servletRequest.getAttribute("divId");
                if(servletRequest.getAttribute("filename") != null) {
                    privName = IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF;
                    imageName = (String) servletRequest.getAttribute("filename");
                }
            } else {
                if ("pdf".equalsIgnoreCase(type)) {
                    //multipage mets pdf
                    pi = requestPath.getName(2).toString().replace(".xml", "").replaceAll(".XML", "");
                    if (requestPath.getNameCount() == 5) {
                        divId = requestPath.getName(3).toString();
                    }
                } else if ("image".equalsIgnoreCase(type)) {
                    //single page pdf
                    pi = requestPath.getName(1).toString();
                    imageName = requestPath.getName(2).toString();
                    privName = IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF;
                }
            }
            filterForAccessConditions(pi, divId, imageName, privName);
        } catch (ServiceNotAllowedException e) {
            String mediaType = MediaType.TEXT_XML;
            if (request.getUriInfo() != null && request.getUriInfo().getPath().endsWith("json")) {
                mediaType = MediaType.APPLICATION_JSON;
            }
            Response response = Response.status(Status.FORBIDDEN).type(mediaType).entity(new ErrorMessage(Status.FORBIDDEN, e, false)).build();
            request.abortWith(response);
        }
    }

    /**
     * @param pi
     * @param divId
     * @param contentFileName
     * @param privName
     * @throws ServiceNotAllowedException
     * @throws IndexUnreachableException
     */
    private void filterForAccessConditions(String pi, String divId, String contentFileName, String privName) throws ServiceNotAllowedException {
        logger.trace("filterForAccessConditions: " + servletRequest.getSession().getId() + " " + contentFileName);
        contentFileName = StringTools.decodeUrl(contentFileName);
        boolean access = false;
        try {

            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, divId, privName, servletRequest);

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
