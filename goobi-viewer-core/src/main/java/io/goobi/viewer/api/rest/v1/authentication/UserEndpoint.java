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
package io.goobi.viewer.api.rest.v1.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.goobi.viewer.api.rest.model.CurrentUserResponse;
import io.goobi.viewer.api.rest.model.UserJsonFacade;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * UserEndpoint class.
 */
@Path(ApiUrls.USERS)
public class UserEndpoint {

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * Returns the client IP address and, if a user is logged in, the current user's information.
     *
     * <p>When no user is authenticated, the {@code user} field is omitted from the response.
     *
     * @return a {@link jakarta.ws.rs.core.Response} object containing a {@link CurrentUserResponse}
     */
    @Path(ApiUrls.USERS_CURRENT)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(summary = "Get the IP address of the current request and, if logged in, information about the current user",
            tags = { "users" })
    @ApiResponse(responseCode = "200", description = "JSON object containing the client IP address and optional user info")
    @ApiResponse(responseCode = "400", description = "No servlet request available")
    public Response getUserInfo() {
        if (servletRequest == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String ipAddress = NetTools.getIpAddress(servletRequest);

        // Retrieve the currently logged-in user from the session, if any
        UserJsonFacade userFacade = null;
        UserBean userBean = BeanUtils.getUserBeanFromSession(servletRequest.getSession(false));
        if (userBean != null) {
            User user = userBean.getUser();
            if (user != null) {
                userFacade = new UserJsonFacade(user, servletRequest);
            }
        }

        return Response.ok(new CurrentUserResponse(ipAddress, userFacade)).build();
    }
}
