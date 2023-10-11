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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
 * Catches general exceptions encountered during rest-api calls and creates an error response
 *
 * @author Florian Alpers
 */
@Provider
@ViewerRestServiceBinding
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger logger = LogManager.getLogger(WebApplicationExceptionMapper.class);

    @Context
    HttpServletResponse response;
    @Context
    HttpServletRequest request;

    /** {@inheritDoc} */
    @Override
    public Response toResponse(WebApplicationException eParent) {
        Response.Status status = Status.INTERNAL_SERVER_ERROR;
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
            printStackTrace = true;
            logger.error(e.getMessage(), e);
        }

        String mediaType = MediaType.APPLICATION_JSON;
        return Response.status(status).type(mediaType).entity(new ErrorMessage(status, e, printStackTrace)).build();
    }

    /**
     * <p>
     * getRequestHeaders.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRequestHeaders() {
        String headers = Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(headerName -> headerName, headerName -> request.getHeader(headerName)))
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("; "));
        return headers;
    }

}
