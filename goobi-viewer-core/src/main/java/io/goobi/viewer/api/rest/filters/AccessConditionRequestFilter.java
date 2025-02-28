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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ImageResource;
import io.goobi.viewer.api.rest.bindings.AccessConditionBinding;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

/**
 * <p>
 * Checks requests for access conditions. Requests must have set the request attribute {@link FilterTools#ATTRIBUTE_PI} and
 * {@link #REQUIRED_PRIVILEGE} to appropriate values for the filter to work properly. Additionally {@link FilterTools#ATTRIBUTE_LOGID} and
 * {@link FilterTools#ATTRIBUTE_FILENAME} may be set in the request to check access to specific files or child documents
 * </p>
 */
@Provider
@AccessConditionBinding
@Priority(Priorities.AUTHORIZATION)
public class AccessConditionRequestFilter implements ContainerRequestFilter {

    /**
     * Privilege name required for accessing a resource.
     */
    public static final String REQUIRED_PRIVILEGE = "requiredPrivilege";

    private static final Logger logger = LogManager.getLogger(AccessConditionRequestFilter.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {

        String responseMediaType = MediaType.APPLICATION_JSON;

        try {
            String pi = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_PI);
            String logid = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_LOGID);
            String filename = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_FILENAME);

            if (StringUtils.isBlank(filename)
                    || (!BeanUtils.getImageDeliveryBean().isExternalUrl(filename)
                            && !BeanUtils.getImageDeliveryBean().isPublicUrl(filename)
                            && !BeanUtils.getImageDeliveryBean().isStaticImageUrl(filename))) {
                filterForAccessConditions(servletRequest, pi, logid, filename);
                if (!FilterTools.checkForConcurrentViewLimit(pi, servletRequest)) {
                    throw new ServiceNotAllowedException("Serving resource not allowed: View limit exceeded for pi " + pi);
                }
            }
        } catch (ServiceNotAllowedException e) {
            servletRequest.setAttribute(ImageResource.REQUEST_ATTRIBUTE_ERROR, e);
        } catch (ViewerConfigurationException e) {
            Response response = Response.status(Status.INTERNAL_SERVER_ERROR)
                    .type(responseMediaType)
                    .entity(new ErrorMessage(Status.INTERNAL_SERVER_ERROR, e, false))
                    .build();
            request.abortWith(response);
        }
    }

    /**
     * @param request
     * @param pi
     * @param logid
     * @param inContentFileName
     * @throws ServiceNotAllowedException
     * @throws IndexUnreachableException
     */
    private static void filterForAccessConditions(HttpServletRequest request, String pi, String logid, final String inContentFileName)
            throws ServiceNotAllowedException {
        // logger.trace("filterForAccessConditions: {}", request.getSession().getId()); //NOSONAR Debug
        String contentFileName = StringTools.decodeUrl(inContentFileName);
        boolean access = false;
        try {
            if (FilterTools.isThumbnail(request)) {
                access = AccessConditionUtils.checkAccessPermissionForThumbnail(request, pi, contentFileName).isGranted();
                // logger.trace("Checked thumbnail access: {}/{}: {}", pi, contentFileName, access); //NOSONAR Debug
            } else {
                String[] privileges = getRequiredPrivileges(request);
                if (privileges.length == 0) {
                    privileges = new String[] { IPrivilegeHolder.PRIV_LIST };
                }
                if (StringUtils.isBlank(pi)) {
                    throw new ServiceNotAllowedException("Serving this resource is currently impossible Because no persistent identifier is given");
                } else if (StringUtils.isNotBlank(contentFileName)) {
                    for (String privilege : privileges) {
                        access = AccessConditionUtils
                                .checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, privilege)
                                .isGranted();
                    }
                } else {
                    for (String privilege : privileges) {
                        access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, logid, privilege, request).isGranted();
                    }
                }
            }
        } catch (IndexUnreachableException | DAOException e) {
            throw new ServiceNotAllowedException("Serving this resource is currently impossible due to " + e.toString());
        } catch (RecordNotFoundException e) {
            throw new ServiceNotAllowedException("Serving this resource is currently impossible because the record could not be found");
        }
        if (!access) {
            throw new ServiceNotAllowedException("Serving this resource is restricted due to access conditions");
        }
    }

    /**
     * Read attribute {@link #REQUIRED_PRIVILEGE} from request and return it as String array. If the attribute doesn't exist, return an empty array
     *
     * @param request
     * @return Required privileges as {@link String}[]
     */
    public static String[] getRequiredPrivileges(HttpServletRequest request) {
        Object privileges = request.getAttribute(REQUIRED_PRIVILEGE);
        if (privileges == null) {
            return new String[0];
        } else if (privileges.getClass().isArray()) {
            return (String[]) privileges;
        } else {
            return new String[] { privileges.toString() };
        }
    }

}
