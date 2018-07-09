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
package de.intranda.digiverso.presentation.servlets.rest.content;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

public abstract class AbstractAnnotation {
    

    public static final String PATH = "/comments";
    public static final String SERVICE = "webannotation";


    protected static final String CONTEXT_URI = "http://www.w3.org/ns/anno.jsonld";
    private static final String GENERATOR_URI = "https://www.intranda.com/en/digiverso/goobi-viewer/goobi-viewer-overview/";
    protected static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

//    protected HttpServletRequest servletRequest;
    protected final String servicePath;
    protected final String applicationPath;
    protected boolean addContext = true;
    
    @JsonSerialize()
    @JsonProperty("@context")
    @JsonInclude(Include.NON_NULL)
    public String getContext() {
        if (addContext) {
            return CONTEXT_URI;
        }

        return null;
    }
    
    /**
     * 
     */
    public AbstractAnnotation(HttpServletRequest servletRequest) {
        this.servicePath = new StringBuilder(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest))
                .append(servletRequest.getRequestURI().substring(servletRequest.getContextPath().length(),
                        servletRequest.getRequestURI().indexOf(PATH) + PATH.length())).toString();
        this.applicationPath = ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest);
    }
    
    public AbstractAnnotation(String applicationPath) throws ViewerConfigurationException {
        this.servicePath = DataManager.getInstance().getConfiguration().getRestApiUrl() + SERVICE + PATH;
        this.applicationPath = applicationPath;
    }

    @JsonSerialize()
    public URI getId() throws URISyntaxException {
        String idString = new StringBuilder(servicePath)
                .toString();
        return new URI(idString);
    }

    @JsonSerialize()
    public String getGenerator() {
        return GENERATOR_URI;
    }
}
