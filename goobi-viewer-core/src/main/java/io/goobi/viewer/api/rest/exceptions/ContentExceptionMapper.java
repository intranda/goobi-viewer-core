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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.intranda.monitoring.timer.TimeoutException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotImplementedException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceUnavailableException;
import io.goobi.viewer.solr.SolrTools;

/**
 * Copied from ContentServer to catch ContentServer exceptions.
 *
 * @author Florian Alpers
 *
 */
@Provider
public class ContentExceptionMapper implements ExceptionMapper<ContentLibException> {

    private static final Logger logger = LogManager.getLogger(ContentExceptionMapper.class);

    @Context
    private HttpServletResponse response;
    @Context
    private HttpServletRequest request;

    public ContentExceptionMapper() {
    }

    public ContentExceptionMapper(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public Response toResponse(ContentLibException e) {
        Response.Status status;
        boolean printStackTrace = false;
        ContentLibException ee = e;
        //Get actual exception if e if of the wrapper class ImageManagerException
        if (ee.getClass().equals(ImageManagerException.class) && ee.getCause() != null && ee.getCause() instanceof ContentLibException cle) {
            ee = cle;
        }
        if (ee instanceof IllegalRequestException) {
            status = Status.BAD_REQUEST;
        } else if (ee instanceof ServiceUnavailableException) {
            status = Status.SERVICE_UNAVAILABLE;
        } else if (ee instanceof ServiceNotImplementedException) {
            status = Status.NOT_IMPLEMENTED;
        } else if (ee instanceof ContentNotFoundException) {
            status = Status.NOT_FOUND;
        } else if (ee instanceof ServiceNotAllowedException) {
            status = Status.FORBIDDEN;
        } else if (ee instanceof TimeoutException) {
            status = Status.REQUEST_TIMEOUT;
        } else {
            status = Status.INTERNAL_SERVER_ERROR;
            printStackTrace = true;
        }
        if (printStackTrace) {
            logger.error("Error on request {}: {}", request.getRequestURI(), ee.toString());
        } else {
            logger.debug("Faulty request {}: {}", request.getRequestURI(), SolrTools.extractExceptionMessageHtmlTitle(ee.getMessage()));
        }

        String mediaType = MediaType.APPLICATION_JSON;

        return Response.status(status).type(mediaType).entity(new ErrorMessage(status, ee, printStackTrace)).build();
    }

    @JsonInclude(Include.NON_NULL)
    public static class ErrorMessage {

        @JsonProperty("status")
        private int status;
        @JsonProperty("message")
        private String message;
        @JsonProperty("errorImage")
        private String errorImage;
        @JsonProperty("stacktrace")
        private String stacktrace;

        public ErrorMessage() {
        }

        /**
         * 
         * @param status
         * @param e
         * @param printStackTrace
         */
        public ErrorMessage(Status status, Throwable e, boolean printStackTrace) {
            this.status = status.getStatusCode();
            if (e != null) {
                this.message = e.getMessage();
                if (printStackTrace) {
                    this.stacktrace = ExceptionUtils.getStackTrace(e);
                }
            } else {
                this.message = "unknown error";
            }
        }

        /**
         * 
         * @param status
         * @param e
         * @param errorImage
         * @param printStackTrace
         */
        public ErrorMessage(Status status, Throwable e, String errorImage, boolean printStackTrace) {
            this.status = status.getStatusCode();
            this.errorImage = errorImage;
            if (e != null) {
                this.message = e.getMessage();
                if (printStackTrace) {
                    this.stacktrace = ExceptionUtils.getStackTrace(e);
                }
            } else {
                this.message = "unknown error";
            }
        }

    }

}
