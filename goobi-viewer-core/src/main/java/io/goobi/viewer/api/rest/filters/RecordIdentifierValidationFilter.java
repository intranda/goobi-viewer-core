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
import io.goobi.viewer.faces.validators.PIValidator;
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
 * Pre-matching request filter that validates persistent identifiers in {@code /records/{pi}/...}
 * paths before the target resource is instantiated.
 *
 * <p>Without this filter, an invalid PI causes a {@link BadRequestException} thrown in the
 * resource constructor, which HK2 wraps in a {@code MultiException} and logs as an "Unknown
 * HK2 failure" — producing a verbose two-part stack trace for what is merely a client error.
 * By running {@link PreMatching} (before route matching and resource instantiation), this filter
 * intercepts the bad request early and emits a single WARN log line instead.
 */
@Provider
@PreMatching
public class RecordIdentifierValidationFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(RecordIdentifierValidationFilter.class);

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // getPathSegments() returns decoded segments, so %20 becomes a space here.
        List<PathSegment> segments = requestContext.getUriInfo().getPathSegments();
        for (int i = 0; i < segments.size() - 1; i++) {
            if ("records".equals(segments.get(i).getPath())) {
                String pi = segments.get(i + 1).getPath();
                if (!PIValidator.validatePi(pi)) {
                    // Sanitize user-controlled path segment before logging to prevent log injection (Sonar S5145)
                    logger.warn("Rejecting request with invalid record identifier: '{}'", pi.replaceAll("[\r\n]", "_"));
                    requestContext.abortWith(Response.status(Status.BAD_REQUEST)
                            .type(MediaType.APPLICATION_JSON)
                            .entity(new ErrorMessage(Status.BAD_REQUEST,
                                    new BadRequestException("Invalid record identifier: " + pi), false))
                            .build());
                }
                // Only the first /records/{pi} segment matters; stop scanning.
                return;
            }
        }
    }
}
