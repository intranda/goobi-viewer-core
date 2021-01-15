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
package io.goobi.viewer.servlets.rest.utils;

import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.AuthorizationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;

/**
 * Replaces <code>ToolsServlet</code> (eventually).
 */
@Path("/tools")
@ViewerRestServiceBinding
@AuthorizationBinding
@CORSBinding
public class ToolsResource {

    private static final Logger logger = LoggerFactory.getLogger(ToolsResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>
     * Constructor for RecordsResource.
     * </p>
     */
    public ToolsResource() {
    }

    /**
     * For testing
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    protected ToolsResource(HttpServletRequest request) {
        this.servletRequest = request;
    }

    /**
     * <p>
     * getRecordsForQuery.
     * </p>
     *
     * @return JSON array
     * @param params a {@link io.goobi.viewer.servlets.rest.content.RecordsRequestParameters} object.
     * @throws java.net.MalformedURLException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @POST
    @Path("/updatedatarepository")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateDataRepositoryCache(ToolsRequestParameters params) throws MalformedURLException, ContentNotFoundException,
            ServiceNotAllowedException, IndexUnreachableException, PresentationException, ViewerConfigurationException, DAOException {
        JSONObject ret = new JSONObject();
        if (params == null || params.getPi() == null || params.getDataRepositoryName() == null) {
            ret.put("status", HttpServletResponse.SC_BAD_REQUEST);
            ret.put("message", "Invalid JSON request object");
            return ret.toString();
        }

        DataManager.getInstance().getSearchIndex().updateDataRepositoryNames(params.getPi(), params.getDataRepositoryName());
        logger.debug("Updated data repository for '{}': {}", params.getPi(), params.getDataRepositoryName());

        ret.put("status", HttpServletResponse.SC_OK);
        return ret.toString();
    }
}
