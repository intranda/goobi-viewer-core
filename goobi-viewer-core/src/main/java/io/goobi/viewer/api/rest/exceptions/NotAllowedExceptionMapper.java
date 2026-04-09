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
package io.goobi.viewer.api.rest.exceptions;

import java.util.Set;

import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;

/**
 * Maps {@link NotAllowedException} (HTTP 405) to a JSON response that includes the required
 * {@code Allow} header listing supported HTTP methods for the requested path, as mandated by
 * RFC 9110 §15.5.6. The more general {@link WebApplicationExceptionMapper} would otherwise
 * discard the {@code Allow} header embedded in the exception's response.
 */
@Provider
@ViewerRestServiceBinding
public class NotAllowedExceptionMapper implements ExceptionMapper<NotAllowedException> {

    /** {@inheritDoc} */
    @Override
    public Response toResponse(NotAllowedException exception) {
        // Retrieve the allowed methods from the embedded response that Jersey (or any JAX-RS
        // container) sets when it constructs a NotAllowedException for an unrecognised method.
        Set<String> allowedMethods = exception.getResponse().getAllowedMethods();
        String allowHeader = allowedMethods.isEmpty() ? "" : String.join(", ", allowedMethods);

        Response.ResponseBuilder builder = Response.status(Status.METHOD_NOT_ALLOWED)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage(Status.METHOD_NOT_ALLOWED, exception, false));

        if (!allowHeader.isEmpty()) {
            builder.header(HttpHeaders.ALLOW, allowHeader);
        }

        return builder.build();
    }
}
