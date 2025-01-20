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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.api.rest.bindings.AuthorizationBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;

/**
 * <p>
 * Allows requests authorized by an authrization token.
 * </p>
 */
@Provider
@AuthorizationBinding
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(AuthorizationFilter.class);

    @Context
    private HttpServletRequest req;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        //  check against configured ip range
        if (!isAuthorized(req)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You are not allowed to access the REST API from IP " + NetTools.getIpAddress(req) + " or your password is wrong.")
                    .build());
        }
    }

    public static boolean isAuthorized(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }
        String pathInfo = request.getPathInfo();
        String ip = NetTools.getIpAddress(request);
        return checkPermissions(ip, token, pathInfo);
    }

    /**
     *
     * @param ip
     * @param token
     * @param pathInfo
     * @return Whether or not access is granted
     */
    private static boolean checkPermissions(String ip, String token, String pathInfo) {

        if (token == null) {
            logger.trace("No token");
            return false;
        }
        return token.equals(DataManager.getInstance().getConfiguration().getWebApiToken());
    }
}
