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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.api.rest.model.ErrorMessage;
import io.goobi.viewer.exceptions.RestApiException;

/**
 * <p>
 * RestApiExceptionMapper class.
 * </p>
 *
 * @author Florian Alpers
 */
@Provider
public class RestApiExceptionMapper implements ExceptionMapper<RestApiException> {

    private static final Logger logger = LogManager.getLogger(RestApiExceptionMapper.class);

    @Context
    private HttpServletResponse response;
    @Context
    private HttpServletRequest request;

    /* (non-Javadoc)
     * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
     */
    /** {@inheritDoc} */
    @Override
    public Response toResponse(RestApiException exception) {
        return Response.status(exception.getStatusCode()).entity(new ErrorMessage(exception)).type(MediaType.APPLICATION_JSON).build();
    }

}
