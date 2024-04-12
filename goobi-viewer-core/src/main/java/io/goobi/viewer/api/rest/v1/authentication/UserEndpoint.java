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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.NetTools;
import io.swagger.v3.oas.annotations.Operation;

/**
 * <p>
 * UserEndpoint class.
 * </p>
 */
@Path(ApiUrls.USERS)
public class UserEndpoint {

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>
     * authenticateUser.
     * </p>
     *
     * @return a {@link javax.ws.rs.core.Response} object.
     */
    @Path(ApiUrls.USERS_CURRENT)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(summary = "Returns the IP address of the servlet request.", tags = { "users" })
    public Response getUserInfo() {
        if (servletRequest == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String ipAddress = NetTools.getIpAddress(servletRequest);

        JSONObject json = new JSONObject();
        json.put("ip", ipAddress);

        return Response.ok(json.toString()).build();
    }
}
