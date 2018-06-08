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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.sitemap.Sitemap;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * Resource for outputting the current session info.
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
     * @param firstPageOnly
     * @return Short summary of files created
     */
    @GET
    @Path("/update/{firstPageOnly}")
    @Produces({ MediaType.TEXT_PLAIN })
    public String updateSitemap(@PathParam("dataRepository") boolean firstPageOnly) {
        return updateSitemap(null, firstPageOnly);
    }

    /**
     * @param outputPath Output path for sitemap files
     * @param firstPageOnly
     * @return Short summary of files created
     */
    @GET
    @Path("/update/{outputPath}/{firstPageOnly}")
    @Produces({ MediaType.TEXT_PLAIN })
    public String updateSitemap(@PathParam("outputPath") String outputPath, @PathParam("dataRepository") boolean firstPageOnly) {
        if (servletRequest == null) {
            return "Servlet request not found";
        }
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
        }

        StringBuilder sb = new StringBuilder();

        Sitemap sitemap = new Sitemap();
        if (outputPath == null) {
            outputPath = servletRequest.getServletContext().getRealPath("/");
        }
        List<File> sitemapFiles = null;
        try {
            sitemapFiles = sitemap.generate(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest), outputPath, firstPageOnly);

            if (sitemapFiles != null) {
                sb.append("Sitemap files created:\n");
                for (File file : sitemapFiles) {
                    sb.append("- " + file.getName() + "\n");
                }
            } else {
                servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            try {
                servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
            }
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            try {
                servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            try {
                servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
            }
        }

        return sb.toString();
    }
}
