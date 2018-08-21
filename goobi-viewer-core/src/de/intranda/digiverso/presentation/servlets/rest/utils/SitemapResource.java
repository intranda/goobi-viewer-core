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
package de.intranda.digiverso.presentation.servlets.rest.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.sitemap.Sitemap;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * Resource for sitemap generation.
 */
@Path(SitemapResource.RESOURCE_PATH)
@ViewerRestServiceBinding
public class SitemapResource {

    private static final Logger logger = LoggerFactory.getLogger(SitemapResource.class);

    public static final String RESOURCE_PATH = "/sitemap";

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public SitemapResource() {
    }

    /**
     * For testing
     * 
     * @param request
     */
    protected SitemapResource(HttpServletRequest request) {
        this.servletRequest = request;
    }

    /**
     * @param outputPath Output path for sitemap files
     * @param firstPageOnly
     * @param params
     * @return Short summary of files created
     */
    @SuppressWarnings("unchecked")
    @POST
    @Path("/update")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    public String updateSitemap(SitemapRequestParameters params) {
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
        }

        JSONObject ret = new JSONObject();

        if (params == null) {
            ret.put("status", HttpServletResponse.SC_BAD_REQUEST);
            ret.put("message", "Invalid JSON request object");
            return ret.toJSONString();
        }

        Sitemap sitemap = new Sitemap();
        String outputPath = params.getOutputPath();
        if (outputPath == null) {
            outputPath = servletRequest.getServletContext().getRealPath("/");
        }
        List<File> sitemapFiles = null;
        try {
            sitemapFiles =
                    sitemap.generate(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest), outputPath, params.isFirstPageOnly());
            if (sitemapFiles != null) {
                ret.put("status", HttpServletResponse.SC_OK);
                ret.put("message", sitemapFiles.size() + " sitemap files created");
                JSONArray fileArray = new JSONArray();
                for (File file : sitemapFiles) {
                    JSONObject fileObj = new JSONObject();
                    fileObj.put("filename", file.getName());
                    fileArray.add(fileObj);
                }
                ret.put("files", fileArray);
            } else {
                servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            ret.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ret.put("message", e.getMessage());
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            ret.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ret.put("message", e.getMessage());
        } catch (DAOException e) {
            logger.debug("DAOException thrown here: {}", e.getMessage());
            ret.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ret.put("message", e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            ret.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ret.put("message", e.getMessage());
        }

        return ret.toJSONString();
    }
}
