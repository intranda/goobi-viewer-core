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
import io.goobi.viewer.api.rest.bindings.AccessRightsBinding;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.iiif.presentation.v3.builder.DataRetriever;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

/**
 * Checks requests for access conditions. Requests must have set the request attribute {@link FilterTools#ATTRIBUTE_PI} to appropriate values for the
 * filter to work properly. Additionally {@link FilterTools#ATTRIBUTE_LOGID} and {@link FilterTools#ATTRIBUTE_FILENAME} may be set in the request to
 * check access to specific files or child documents
 */
@Provider
@AccessRightsBinding({})
@Priority(Priorities.AUTHORIZATION)
public class AccessRightsRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(AccessRightsRequestFilter.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Context
    private ResourceInfo resourceInfo;
    @Context
    private UriInfo uriInfo;

    private final DataRetriever dataRetriever = new DataRetriever();

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {

        try {
            String pi = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_PI);
            String logId = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_LOGID);
            String filename = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_FILENAME);
            Integer pageNo = (Integer) servletRequest.getAttribute(FilterTools.ATTRIBUTE_PAGENO);

            if (StringUtils.isBlank(pi)) {
                pi = getPathParameter("pi", "identifier");
            }
            if (StringUtils.isBlank(logId)) {
                logId = getPathParameter("logid", "logId", "divId", "divid");
            }
            if (StringUtils.isBlank(filename)) {
                filename = getPathParameter("filename", "fileName", "imageUrl", "imageName");
            }

            if (pageNo == null) {
                pageNo = getInteger(getPathParameter("pageNo", "pageno", "order"));
            }

            filterForAccessConditions(servletRequest, pi, logId, filename, pageNo);

        } catch (ServiceNotAllowedException e) {
            String mediaType = MediaType.APPLICATION_JSON;
            Response response = Response.status(Status.FORBIDDEN).type(mediaType).entity(new ErrorMessage(Status.FORBIDDEN, e, false)).build();
            request.abortWith(response);
        }
    }

    private Integer getInteger(String param) {
        try {
            return Integer.parseInt(param);
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
    }

    private String getPathParameter(String... paramNames) {
        if (uriInfo != null) {
            MultivaluedMap<String, String> pathParams = uriInfo.getPathParameters();
            for (String param : paramNames) {
                String value = pathParams.getFirst(param);
                if (StringUtils.isNotBlank(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    /**
     * @param request HTTP servlet request
     * @param pi persistent identifier of the record
     * @param logid logical structure ID of the element
     * @param inContentFileName content file name to check access for
     * @param pageNo 1-based physical page order, or null if not page-specific
     * @throws ServiceNotAllowedException
     * @throws IndexUnreachableException
     */
    private void filterForAccessConditions(HttpServletRequest request, String pi, String logid, final String inContentFileName, final Integer pageNo)
            throws ServiceNotAllowedException {
        String contentFileName = StringTools.decodeUrl(inContentFileName);
        boolean access = false;
        try {
            if (FilterTools.isThumbnail(request)) {
                access = AccessConditionUtils
                        .checkAccessPermissionForThumbnail(request.getSession(), pi, contentFileName, NetTools.getIpAddress(request))
                        .isGranted();
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
                                .checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request.getSession(), pi, contentFileName, privilege,
                                        NetTools.getIpAddress(
                                                request))
                                .isGranted();
                    }
                } else if (pageNo != null) {
                    for (String privilege : privileges) {
                        access = AccessConditionUtils
                                .checkAccessPermissionByIdentifierAndPageOrder(pi, pageNo, privilege, request)
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
     * Reads required privilege from request and return it as String array. If the attribute doesn't exist, return an empty array
     *
     * @param request HTTP servlet request to read required privilege from
     * @return Required privileges as {@link String}[]
     */
    public String[] getRequiredPrivileges(HttpServletRequest request) {
        AccessRightsBinding accessBinding =
                resourceInfo.getResourceMethod()
                        .getAnnotation(AccessRightsBinding.class);

        if (accessBinding == null) {
            accessBinding = resourceInfo.getResourceClass()
                    .getAnnotation(AccessRightsBinding.class);
        }

        return accessBinding.value();
    }

}
