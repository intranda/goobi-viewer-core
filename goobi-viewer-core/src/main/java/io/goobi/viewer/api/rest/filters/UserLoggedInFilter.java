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

import io.goobi.viewer.api.rest.bindings.UserLoggedInBinding;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Only allow requests from a session with a logged in Goobi viewer user.
 *
 * @author florian
 *
 */
@Provider
@UserLoggedInBinding
public class UserLoggedInFilter implements ContainerRequestFilter {

    @Context
    private HttpServletRequest servletRequest;

    /* (non-Javadoc)
     * @see jakarta.ws.rs.container.ContainerRequestFilter#filter(jakarta.ws.rs.container.ContainerRequestContext)
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!isUserLoggedIn(servletRequest)) {
            Response response = Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You must be logged in to access this resource")
                    .build();
            requestContext.abortWith(response);
        }
    }

    public static boolean isUserLoggedIn(HttpServletRequest request) {
        return BeanUtils.getUserFromSession(request.getSession()) != null;
    }
}
