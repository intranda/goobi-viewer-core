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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.authentication.HttpAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.HttpHeaderProvider;
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

    static final String REASON_PHRASE_ILLEGAL_REDIRECT_URL = "Illegal redirect URL or URL cannot be checked.";
    static final String REASON_PHRASE_NO_PROVIDERS_CONFIGURED = "No authentication providers of type 'httpHeader' configured.";
    static final String REASON_PHRASE_NO_PROVIDER_FOUND = "No matching provider found.";

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
     * @return email
     */
    private static String issueToken(String email) {
        // Issue a token (can be a random String persisted to a database or a JWT token)
        // The issued token must be associated to a user
        // Return the issued token

        return email;
    }

    /**
     * 
     * @param redirectUrl
     * @return {@link Response}
     * @should return status 403 if redirectUrl external
     * @should return status 403 if no httpHeader type provider configured
     * @should return status 403 if no matching provider found
     */
    @GET
    @Path(ApiUrls.AUTH_HEADER)
    @Operation(summary = "Header login", description = "Checks a configurable header for a username and logs in the user if it is found in the DB")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "500", description = "Internal error")
    public Response headerParameterLogin(@QueryParam("redirectUrl") String redirectUrl) {
        logger.debug("headerParameterLogin");
        Optional<NavigationHelper> nh =  BeanUtils.getBeanFromRequest(servletRequest, "navigationHelper", NavigationHelper.class);
        if (redirectUrl != null && (!nh.isPresent() || !redirectUrl.startsWith(nh.get().getApplicationUrl()))) {
            return Response.status(Response.Status.FORBIDDEN.getStatusCode(), REASON_PHRASE_ILLEGAL_REDIRECT_URL)
                    .build();
        }

        HttpHeaderProvider useProvider = null;

        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null && userBean.getAuthenticationProvider() instanceof HttpHeaderProvider httpHeaderProvider) {
            useProvider = httpHeaderProvider;
        }

        String ssoId = null;

        // If the login wasn't triggered by the user, find an appropriate provider
        if (useProvider == null) {
            List<HttpHeaderProvider> providers = new ArrayList<>();
            for (HttpAuthenticationProvider provider : DataManager.getInstance().getAuthResponseListener().getProviders()) {
                if (provider instanceof HttpHeaderProvider httpHeaderProvider) {
                    providers.add(httpHeaderProvider);
                    break;
                }
            }
            if (providers.isEmpty()) {
                logger.warn("No providers configured.");
                return Response.status(Response.Status.FORBIDDEN.getStatusCode(), REASON_PHRASE_NO_PROVIDERS_CONFIGURED)
                        .build();
            }

            for (HttpHeaderProvider provider : providers) {
                logger.trace(provider.getName());
                boolean headerType = HttpHeaderProvider.PARAMETER_TYPE_HEADER.equalsIgnoreCase(provider.getParameterType());
                String value = headerType ? servletRequest.getHeader(provider.getParameterName())
                        : (String) servletRequest.getAttribute(provider.getParameterName());
                if (StringUtils.isNotEmpty(value)) {
                    logger.debug("Found request {}: {}:{}", (headerType ? "HEADER" : "ATTRIBUTE"), provider.getParameterName(), value);
                    useProvider = provider;
                    if (userBean != null) {
                        userBean.setAuthenticationProvider(provider);
                    }
                    break;
                }

            }
        }

        if (useProvider == null) {
            logger.warn("No matching authentication provider found.");
            return Response.status(Response.Status.FORBIDDEN.getStatusCode(), REASON_PHRASE_NO_PROVIDER_FOUND).build();
        }
        logger.debug("Provider selected: {}", useProvider.getName());

        // Set ssoId
        boolean headerType = HttpHeaderProvider.PARAMETER_TYPE_HEADER.equalsIgnoreCase(useProvider.getParameterType());
        ssoId = headerType ? servletRequest.getHeader(useProvider.getParameterName())
                : (String) servletRequest.getAttribute(useProvider.getParameterName());

        Future<Boolean> loginSuccess = useProvider.completeLogin(ssoId, servletRequest, servletResponse);
        DataManager.getInstance().getAuthResponseListener().unregister(useProvider);
        try {
            // Before sending response, wait until UserBean.completeLogin() has finished and released the result
            if (Boolean.FALSE.equals(loginSuccess.get())) {
                if (StringUtils.isNotEmpty(redirectUrl)) {
                    logger.debug("Redirecting to redirectUrl");
                    servletResponse.sendRedirect(redirectUrl); //NOSONAR redirectUrl is verified at this point
                } else if (BeanUtils.getNavigationHelper() != null) {
                    logger.debug("No redirect URL found, redirecting to home");
                    servletResponse.sendRedirect(BeanUtils.getNavigationHelper().getApplicationUrl());
                }
            }
        } catch (ExecutionException | IOException e) {
            logger.error(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.trace("endpoint end");
        return Response.ok("").build();
    }
}
