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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

/**
 * Resource for sitemap generation.
 */
@Path(IndexingResource.RESOURCE_PATH)
@ViewerRestServiceBinding
public class IndexingResource {

    private static final Logger logger = LoggerFactory.getLogger(IndexingResource.class);

    public static final String RESOURCE_PATH = "/index";

    private static Thread workerThread = null;

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public IndexingResource() {
    }

    /**
     * For testing
     * 
     * @param request
     */
    protected IndexingResource(HttpServletRequest request) {
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
    @Path("/delete")
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


        if (workerThread == null || !workerThread.isAlive()) {
            workerThread = new Thread(new Runnable() {

                @Override
                public void run() {

                }
            });

            workerThread.start();
            try {
                workerThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            ret.put("status", HttpServletResponse.SC_FORBIDDEN);
            ret.put("message", "Sitemap generation currently in progress");
        }

        return ret.toJSONString();
    }
}
