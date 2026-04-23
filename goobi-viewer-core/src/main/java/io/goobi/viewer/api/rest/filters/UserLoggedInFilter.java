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
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.bindings.UserLoggedInBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SecurityManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.UserToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Only allow requests from a session with a logged in Goobi viewer user, or with a valid Bearer token.
 *
 * @author Florian Alpers
 */
@Provider
@UserLoggedInBinding
public class UserLoggedInFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(UserLoggedInFilter.class);

    @Context
    private HttpServletRequest servletRequest;

    /**
     * @param requestContext the JAX-RS request context
     * @throws IOException
     * @should pass request through when valid bearer token provided
     * @should return 401 with token_expired when expired bearer token provided
     * @should return 401 with invalid_token when unknown bearer token provided
     * @should return 401 when no bearer token and no session
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authHeader = servletRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String plaintext = authHeader.substring(7);
            String hash = SecurityManager.hashToken(plaintext);
            try {
                Optional<UserToken> tokenOpt = DataManager.getInstance().getDao().getUserTokenByTokenHash(hash);
                if (tokenOpt.isEmpty()) {
                    requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                            .type(MediaType.APPLICATION_JSON)
                            .entity("{\"status\":\"error\",\"message\":\"invalid_token\"}")
                            .build());
                    return;
                }
                if (tokenOpt.get().isExpired()) {
                    requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                            .type(MediaType.APPLICATION_JSON)
                            .entity("{\"status\":\"error\",\"message\":\"token_expired\"}")
                            .build());
                    return;
                }
                // Token valid — continue
                return;
            } catch (DAOException e) {
                logger.error("DAO error validating Bearer token", e);
                requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
                return;
            }
        }

        if (!isUserLoggedIn(servletRequest)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You must be logged in to access this resource")
                    .build());
        }
    }

    public static boolean isUserLoggedIn(HttpServletRequest request) {
        return BeanUtils.getUserFromSession(request.getSession()) != null;
    }
}
