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

import io.goobi.viewer.api.rest.bindings.AdminLoggedInBinding;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS request filter that only allows requests from sessions belonging to a logged-in Goobi viewer administrator account, or with a valid Bearer
 * token belonging to an admin user.
 *
 * @author Florian Alpers
 */
@Provider
@AdminLoggedInBinding
public class AdminLoggedInFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(AdminLoggedInFilter.class);

    @Context
    private HttpServletRequest req;

    /**
     * @param requestContext the JAX-RS request context
     * @throws IOException
     * @should pass request through when valid admin bearer token provided
     * @should return 401 when bearer token belongs to non-admin user
     * @should return 401 with token_expired when expired admin bearer token provided
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            Optional<UserToken> tokenOpt = UserLoggedInFilter.getUserToken(req);

            tokenOpt.ifPresentOrElse(token -> {
                if (token.isExpired()) {
                    //abort: token expired
                    requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                            .type(MediaType.APPLICATION_JSON)
                            .entity("{\"status\":\"error\",\"message\":\"token_expired\"}")
                            .build());
                } else {
                    User user = tokenOpt.get().getUser();
                    if (user == null || !user.isSuperuser()) {
                        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                                .entity("You must be logged in as administrator to access this resource")
                                .build());
                        return;
                    }
                }
                //token valid: continue
            }, () -> {
                //no token
                if (!isAdminLoggedIn(req)) {
                    //abort: no user
                    requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                            .entity("You must be logged in as administrator to access this resource")
                            .build());
                }
                //user logged in: continue
            });

        } catch (DAOException e) {
            //error reading db: abort
            logger.error("DAO error validating Bearer token", e);
            requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
            return;
        }
    }

    public static boolean isAdminLoggedIn(HttpServletRequest request) {
        User user = BeanUtils.getUserFromSession(request.getSession());
        return user != null && user.isSuperuser();
    }

}
