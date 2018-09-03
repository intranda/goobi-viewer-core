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
package de.intranda.digiverso.presentation.servlets.rest.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.servlets.rest.search.SearchHitsNotificationResource;
import de.intranda.digiverso.presentation.servlets.rest.utils.SitemapResource;

@Provider
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);

    @Context
    private HttpServletRequest req;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String token = requestContext.getHeaderString("token");
        if (StringUtils.isBlank(token)) {
            token = req.getParameter("token");
        }

        String pathInfo = req.getPathInfo();
        String ip = Helper.getIpAddress(req);

        //  check against configured ip range
        if (!checkPermissions(ip, token, pathInfo)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You are not allowed to access the REST API from IP " + ip + " or your password is wrong.")
                    .build());
        }
    }

    /**
     * 
     * @param ip
     * @param token
     * @param pathInfo
     * @return Whether or not access is granted
     */
    private static boolean checkPermissions(String ip, String token, String pathInfo) {
        if (pathInfo == null) {
            return false;
        }
        if (pathInfo.contains(SitemapResource.RESOURCE_PATH)
                || pathInfo.contains(SearchHitsNotificationResource.RESOURCE_PATH + "/sendnotifications")) {
            if (token == null) {
                logger.trace("No token");
                return false;
            } else {
                return token.equals(DataManager.getInstance().getConfiguration().getWebApiToken());
            }
            
        }

        return true;
    }
}
