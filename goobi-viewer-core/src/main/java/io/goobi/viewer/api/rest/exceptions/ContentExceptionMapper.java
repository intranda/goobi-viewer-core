/*
 * This file is part of the ContentServer project.
 * Visit the websites for more information. 
 * 		- http://gdz.sub.uni-goettingen.de 
 * 		- http://www.intranda.com 
 * 		- http://www.digiverso.com
 * 
 * Copyright 2009, Center for Retrospective Digitization, Göttingen (GDZ),
 * intranda software
 * 
 * This is the extended version updated by intranda
 * Copyright 2012, intranda GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the “License�?);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS�? BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.goobi.viewer.api.rest.exceptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Copied from ContentServer to catch ContentServer exceptions
 * 
 * @author Florian Alpers
 *
 */
@Provider
public class ContentExceptionMapper implements ExceptionMapper<ContentLibException>{

    private static final Logger logger = LoggerFactory.getLogger(ContentExceptionMapper.class);
    
    @Context HttpServletResponse response;
    @Context HttpServletRequest request;
    
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
        //Get actual exception if e if of the wrapper class ImageManagerException
        if(e.getClass().equals(ImageManagerException.class) && e.getCause() != null && e.getCause() instanceof ContentLibException) {
            e = (ContentLibException) e.getCause();
        }
        if(e instanceof IllegalRequestException) {
            status = Status.BAD_REQUEST;
        } else if(e instanceof ServiceUnavailableException){
            status = Status.SERVICE_UNAVAILABLE;
        } else if(e instanceof ServiceNotImplementedException) {
            status = Status.NOT_IMPLEMENTED;
        } else if(e instanceof ContentNotFoundException) {
            status = Status.NOT_FOUND;
        } else if(e instanceof ServiceNotAllowedException) {
            status = Status.FORBIDDEN;
        } else if(e instanceof TimeoutException) {
        	status = Status.REQUEST_TIMEOUT;
        } else {
            status = Status.INTERNAL_SERVER_ERROR;
            printStackTrace = true;
        }
        if(printStackTrace) {
            logger.error("Error on request {}: {}", request.getRequestURL(), e.toString());
        } else {            
            logger.debug("Faulty request {}: {}", request.getRequestURL(), e.getMessage());
        }
        
        String mediaType = MediaType.APPLICATION_JSON;
        if(request != null && request.getRequestURI().toLowerCase().endsWith(".xml")) {
            mediaType = MediaType.TEXT_XML;
        }
        return  Response.status(status).type(mediaType).entity(new ErrorMessage(status, e, printStackTrace)).build();
    }
    
    @XmlRootElement
    @JsonInclude(Include.NON_NULL)
    public static class ErrorMessage {

        @XmlElement(name = "status")
        @JsonProperty("status")
        private int status;
        @XmlElement(name = "message")
        @JsonProperty("message")
        private String message;
        @XmlElement(name = "errorImage")
        @JsonProperty("errorImage")
        private String errorImage;
        @XmlElement(name = "stacktrace", nillable = false)
        @JsonProperty("stacktrace")
        private String stacktrace;
        
        public ErrorMessage() {}
        
        public ErrorMessage(Status status, Throwable e, boolean printStackTrace) {
            this.status = status.getStatusCode();
            if(e != null) {                
                this.message = e.getMessage();
                if(printStackTrace) {
                    this.stacktrace = ExceptionUtils.getStackTrace(e);
                }
            } else {
                this.message = "unknown error";
            }
        }
        
        public ErrorMessage(Status status, Throwable e, String errorImage, boolean printStackTrace) {
            this.status = status.getStatusCode();
            this.errorImage = errorImage;
            if(e != null) {                
                this.message = e.getMessage();
                if(printStackTrace) {
                    this.stacktrace = ExceptionUtils.getStackTrace(e);
                }
            } else {
                this.message = "unknown error";
            }
        }

        
        
    }

}
