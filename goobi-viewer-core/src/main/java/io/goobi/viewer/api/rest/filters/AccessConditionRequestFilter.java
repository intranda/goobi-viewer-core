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
import java.util.List;
import java.util.Optional;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale.AbsoluteScale;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale.RelativeScale;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageBinding;
import io.goobi.viewer.api.rest.bindings.AccessConditionBinding;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;

/**
 * <p>
 * Checks requests for access conditions. Requests must have set the request attribute {@link FilterTools#ATTRIBUTE_PI}
 * and {@link #REQUIRED_PRIVILEGE} to appropriate values for the filter to work properly.
 * Additionally {@link FilterTools#ATTRIBUTE_LOGID} and {@link FilterTools#ATTRIBUTE_FILENAME} may be set in the request 
 * to check access to specific files or child documents
 * </p>
 */
@Provider
@AccessConditionBinding
@Priority(Priorities.AUTHORIZATION)
public class AccessConditionRequestFilter implements ContainerRequestFilter {

    /**
     * Privilege name required for accessing a resource
     */
    public static final String REQUIRED_PRIVILEGE = "requiredPrivilege";

    private static final Logger logger = LoggerFactory.getLogger(AccessConditionRequestFilter.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        
        String responseMediaType = MediaType.APPLICATION_JSON;
        if (servletRequest != null && servletRequest.getRequestURI().toLowerCase().contains("xml")) {
            responseMediaType = MediaType.TEXT_XML;
        }
        
        try {
                String pi = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_PI);
                String logid = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_LOGID);
                String filename = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_FILENAME);

            if ( StringUtils.isBlank(filename) || 
                      (!BeanUtils.getImageDeliveryBean().isExternalUrl(filename) 
                    && !BeanUtils.getImageDeliveryBean().isPublicUrl(filename)
                    && !BeanUtils.getImageDeliveryBean().isStaticImageUrl(filename))
                ) {
                filterForAccessConditions(servletRequest, pi, logid, filename);
                FilterTools.filterForConcurrentViewLimit(pi, servletRequest);
            }
        } catch (ServiceNotAllowedException e) {
            Response response = Response.status(Status.FORBIDDEN).type(responseMediaType).entity(new ErrorMessage(Status.FORBIDDEN, e, false)).build();
            request.abortWith(response);
        } catch (ViewerConfigurationException e) {
            Response response = Response.status(Status.INTERNAL_SERVER_ERROR)
                    .type(responseMediaType)
                    .entity(new ErrorMessage(Status.INTERNAL_SERVER_ERROR, e, false))
                    .build();
            request.abortWith(response);
        }
    }


    /**
     * @param requestPath
     * @param pathSegments
     * @throws ServiceNotAllowedException
     * @throws IndexUnreachableException
     */
    private void filterForAccessConditions(HttpServletRequest request, String pi, String logid, String contentFileName)
            throws ServiceNotAllowedException {
        // logger.trace("filterForAccessConditions: {}", servletRequest.getSession().getId());
        contentFileName = StringTools.decodeUrl(contentFileName);
        boolean access = false;
        try {
            if (FilterTools.isThumbnail(request)) {
                access = AccessConditionUtils.checkAccessPermissionForThumbnail(request, pi, contentFileName);
                //                                logger.trace("Checked thumbnail access: {}/{}: {}", pi, contentFileName, access);
            } else {
                String privilege = (String) request.getAttribute(REQUIRED_PRIVILEGE);
                if(StringUtils.isBlank(privilege)) {
                    privilege = IPrivilegeHolder.PRIV_LIST;
                }
                if(StringUtils.isNotBlank(contentFileName)) {                    
                    access = AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, privilege);
                } else {
                    access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, logid, privilege, request);

                }
            }
        } catch (IndexUnreachableException e) {
            throw new ServiceNotAllowedException("Serving this resource is currently impossible due to " + e.toString());
        } catch (DAOException e) {
            throw new ServiceNotAllowedException("Serving this resource is currently impossible due to " + e.toString());
        } catch (RecordNotFoundException e) {
            throw new ServiceNotAllowedException("Serving this resource is currently impossible because the record could not be found");
        }
        if (!access) {
            throw new ServiceNotAllowedException("Serving this resource is restricted due to access conditions");
        }
    }

    /**
     * @param width
     * @return
     */
    private static int parse(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
