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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.AuthenticationException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.authentication.HttpHeaderProvider;
import io.goobi.viewer.model.security.authentication.IAuthenticationProvider;
import io.goobi.viewer.model.security.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * <p>
 * AuthenticationEndpoint class.
 * </p>
 */
@Path(ApiUrls.AUTH)
public class AuthenticationEndpoint {

    private static final Logger logger = LogManager.getLogger(AuthenticationEndpoint.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>
     * authenticateUser.
     * </p>
     *
     * @param email a {@link java.lang.String} object.
     * @param password a {@link java.lang.String} object.
     * @return a {@link javax.ws.rs.core.Response} object.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response authenticateUser(@FormParam("email") String email, @FormParam("password") String password) {
        try {

            // Authenticate the user using the credentials provided
            authenticate(email, password);

            // Issue a token for the user
            String token = issueToken(email);

            // Return the token on the response
            return Response.ok(token).build();

        } catch (AuthenticationException e) {
            logger.debug(e.getMessage());
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (DAOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     *
     * @param email
     * @param password
     * @throws AuthenticationException
     * @throws DAOException
     */
    private static void authenticate(String email, String password) throws AuthenticationException, DAOException {
        User user = new User().auth(email, password);
        if (!user.isSuperuser()) {
            throw new AuthenticationException("Superuser access required");
        }
    }

    /**
     *
     * @param email
     * @return
     */
    private static String issueToken(String email) {
        // Issue a token (can be a random String persisted to a database or a JWT token)
        // The issued token must be associated to a user
        // Return the issued token

        return email;
    }

    @GET
    @Path("/header")
    @Operation(summary = "Header login", description = "Checks a configurable header for a username and logs in the user if it is found in the DB")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "500", description = "Internal error")
    public Response headerParameterLogin(@QueryParam("redirectUrl") String redirectUrl) throws IOException {
        List<HttpHeaderProvider> providers = new ArrayList<>();
        for (IAuthenticationProvider p : DataManager.getInstance().getConfiguration().getAuthenticationProviders()) {
            if (p instanceof HttpHeaderProvider) {
                providers.add((HttpHeaderProvider) p);
                break;
            }
        }
        if (providers.isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN.getStatusCode(), "No authentication providers of type 'httpHeader' configured.").build();
        }

        HttpHeaderProvider useProvider = null;
        String ssoId = null;
        for (HttpHeaderProvider provider : providers) {
            if (HttpHeaderProvider.PARAMETER_TYPE_HEADER.equalsIgnoreCase(provider.getParameterType())) {
                // Header
                final Enumeration<String> headerNames = servletRequest.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    if (headerName.equals(provider.getParameterName())) {
                        useProvider = provider;
                        ssoId = servletRequest.getHeader(headerName);
                        break;
                    }
                }
            } else {
                // Attribute
                final Enumeration<String> names = servletRequest.getAttributeNames();
                while (names.hasMoreElements()) {
                    String name = names.nextElement();
                    if (name.equals(provider.getParameterName())) {
                        useProvider = provider;
                        ssoId = (String) servletRequest.getAttribute(name);
                        break;
                    }
                }
            }
            if (useProvider != null) {
                break;
            }
        }

        if (useProvider == null) {
            logger.warn("No matching authentication provider found.");
            return Response.status(Response.Status.FORBIDDEN.getStatusCode(), "No matching provider found.").build();
        }

        if (ssoId == null) {
            logger.warn("Parameter not provided: {}", useProvider.getParameterName());
            return Response.status(Response.Status.FORBIDDEN.getStatusCode(), "Parameter not provided: " + useProvider.getParameterName()).build();
        }
        UserBean userBean = BeanUtils.getUserBean();
        User user = useProvider.loadUser(ssoId);
        if (user != null) {
            userBean.setUser(user);
        }

        if (StringUtils.isNotEmpty(redirectUrl)) {
            servletResponse.sendRedirect(redirectUrl);
        } else if (BeanUtils.getNavigationHelper() != null) {
            servletResponse.sendRedirect(BeanUtils.getNavigationHelper().getApplicationUrl());
        }

        return Response.ok("").build();
    }
}
