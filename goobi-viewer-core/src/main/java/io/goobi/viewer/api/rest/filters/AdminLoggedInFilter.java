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
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.bindings.AdminLoggedInBinding;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;

/**
 * Only allows requests by sessisions belonging to a logged in goobi-viewer admin account.
 *
 * @author florian
 *
 */
@Provider
@AdminLoggedInBinding
public class AdminLoggedInFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(AdminLoggedInFilter.class);

    @Context
    private HttpServletRequest req;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!isAdminLoggedIn(req)) {
            Response response = Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You must be logged in as administrator to access this resource")
                    .build();
            requestContext.abortWith(response);
        }
    }

    public static boolean isAdminLoggedIn(HttpServletRequest request) {
        User user = BeanUtils.getUserFromRequest(request);
        return user != null && user.isSuperuser();
    }

}
