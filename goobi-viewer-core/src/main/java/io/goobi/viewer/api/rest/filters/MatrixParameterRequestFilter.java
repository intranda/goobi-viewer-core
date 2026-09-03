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
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import io.goobi.viewer.controller.StringTools;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

/**
 * Pre-matching request filter that rejects requests whose path contains matrix parameters (a {@code ;name=value} suffix on a path segment, e.g.
 * {@code /records/ID/files/images/FILE.tif/;LbNtg/512,512/0/default.jpg}).
 *
 * <p>
 * This API never uses {@code @MatrixParam}, so a matrix parameter is always a malformed request rather than legitimate input. But leaving it
 * unhandled is worse than merely ignoring an unused parameter: JAX-RS strips the matrix-parameter suffix from the path segment before route matching,
 * so {@link PathSegment#getPath()} for that segment becomes an empty string. Since the default JAX-RS URI template regex requires at least one
 * character per path variable, that empty segment never matches any resource method's {@code @Path}, so the request fails at the routing stage with a
 * generic 404 instead of reaching a resource method, where it could otherwise be rejected as a 400 for an invalid parameter value.
 *
 * <p>
 * Running as {@link PreMatching} (before route matching), this filter catches this case for every path segment - not just a specific endpoint's
 * trailing parameters - and rejects it with an explicit 400 instead of letting it fall through to an opaque 404.
 */
@Provider
@PreMatching
public class MatrixParameterRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(MatrixParameterRequestFilter.class);

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        List<PathSegment> segments = requestContext.getUriInfo().getPathSegments();
        for (PathSegment segment : segments) {
            if (!segment.getMatrixParameters().isEmpty()) {
                String requestPath = StringTools.cleanUserGeneratedData(requestContext.getUriInfo().getPath());
                logger.warn("Rejecting request containing a matrix parameter (';') in its path: {}", requestPath);
                requestContext.abortWith(Response.status(Status.BAD_REQUEST)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(new ErrorMessage(Status.BAD_REQUEST,
                                new BadRequestException(
                                        "Invalid request path: matrix parameters are not supported. This means that the path must not contain a ';' character."),
                                false))
                        .build());
                return;
            }
        }
    }

}
