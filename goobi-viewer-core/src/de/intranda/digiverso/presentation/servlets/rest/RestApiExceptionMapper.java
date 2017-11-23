/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.servlets.rest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.RestApiException;

/**
 * @author Florian Alpers
 *
 */
@Provider
public class RestApiExceptionMapper implements ExceptionMapper<RestApiException>{

        private static final Logger logger = LoggerFactory.getLogger(ExceptionMapper.class);
        
        @Context HttpServletResponse response;
        @Context HttpServletRequest request;
        /* (non-Javadoc)
         * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
         */
        @Override
        public Response toResponse(RestApiException exception) {
            return Response.status(exception.getStatusCode())
                    .entity(new ErrorMessage(exception))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

}
