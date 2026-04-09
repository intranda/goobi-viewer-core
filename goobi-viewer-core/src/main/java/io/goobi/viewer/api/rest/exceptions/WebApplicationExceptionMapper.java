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

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.NotImplementedException;
import io.goobi.viewer.exceptions.PresentationException;

/**
 * JAX-RS exception mapper that catches general exceptions encountered during REST API calls and creates a JSON-formatted error response.
 *
 * @author Florian Alpers
 */
@Provider
@ViewerRestServiceBinding
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger logger = LogManager.getLogger(WebApplicationExceptionMapper.class);

    @Context
    private HttpServletResponse response;
    @Context
    private HttpServletRequest request;

    /** {@inheritDoc} */
    @Override
    public Response toResponse(WebApplicationException eParent) {
        Response.Status status = null;
        boolean printStackTrace = false;
        Throwable e = eParent.getCause();
        if (e == null) {
            e = eParent;
        }
        if (e instanceof WebApplicationException) {
            status = ((WebApplicationException) e).getResponse().getStatusInfo().toEnum();
        } else if (e instanceof NotFoundException || e instanceof FileNotFoundException) {
            status = Status.NOT_FOUND;
        } else if (e instanceof NotImplementedException) {
            status = Status.NOT_IMPLEMENTED;
        } else if (e instanceof AccessDeniedException) {
            status = Status.UNAUTHORIZED;
        } else if (e instanceof ContentLibException) {
            return new ContentExceptionMapper(request, response).toResponse((ContentLibException) e);
        } else if (e instanceof TimeoutException) {
            status = Status.INTERNAL_SERVER_ERROR;
        } else if (e instanceof NumberFormatException || e.getCause() instanceof NumberFormatException) {
            // Client sent a non-numeric value for an integer path/query parameter → 400, not 500.
            // Jersey may wrap the NumberFormatException in an ExtractorException (RuntimeException)
            // before wrapping it in InternalServerErrorException (WebApplicationException). Check one
            // level of cause to catch the ExtractorException→NumberFormatException wrapping.
            status = Status.BAD_REQUEST;
            e = new IllegalArgumentException("Invalid parameter: not a valid integer");
        } else if (e instanceof RuntimeException) {
            status = Status.INTERNAL_SERVER_ERROR;
            logger.error("Error on request {};\t ERROR MESSAGE: {} (method: {})", request.getRequestURI(), e.getMessage(), request.getMethod());
        } else if (e instanceof PresentationException) {
            status = Status.INTERNAL_SERVER_ERROR;
            logger.error("Error on request {};\t ERROR MESSAGE: {}", request.getRequestURI(), e.getMessage());
        } else if (e instanceof IndexUnreachableException) {
            status = Status.INTERNAL_SERVER_ERROR;
            logger.error("Error on request {};\t SOLR is not responding; ERROR MESSAGE: {}", request.getRequestURI(), e.getMessage());
        } else {
            //unknown error. Probably request error
            status = Status.BAD_REQUEST;
            // Do NOT set printStackTrace=true: stack traces must not be sent to clients.
            logger.error(e.getMessage(), e);
        }

        String mediaType = MediaType.APPLICATION_JSON;
        return Response.status(status).type(mediaType).entity(new ErrorMessage(status, e, printStackTrace)).build();
    }

    /**
     * getRequestHeaders.
     *
     * @return all HTTP request headers as a semicolon-separated key-value string
     */
    public String getRequestHeaders() {
        return Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(headerName -> headerName, headerName -> request.getHeader(headerName)))
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("; "));
    }

}
