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
package io.goobi.viewer.api.rest.v1.cache;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.solr.common.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unigoettingen.sub.commons.cache.CacheManagerInfo.CacheInfo;
import de.unigoettingen.sub.commons.cache.CacheUtils;
import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentServerCacheException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import io.goobi.viewer.api.rest.bindings.AuthorizationBinding;
import io.goobi.viewer.api.rest.model.IResponseMessage;
import io.goobi.viewer.api.rest.model.SuccessMessage;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path(ApiUrls.CACHE)
public class CacheResource {

    private static final Logger logger = LoggerFactory.getLogger(CacheResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Context
    private ContentServerCacheManager cacheManager;
    private ObjectMapper mapper = new ObjectMapper();
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Return information about internal cache status", tags = { "cache" })
    public String getCacheInfo() throws ContentServerCacheException {
//        ContentServerCache content = ContentServerCache.getContentCache();
//        ContentServerCache pdf = ContentServerCache.getPdfCache();
//        ContentServerCache thumbs = ContentServerCache.getThumbnailCache();
        
        JSONObject jCaches = new JSONObject();
        if(ContentServerConfiguration.getInstance().getContentCacheUse()) {     
            try {
                CacheInfo info = new CacheInfo(cacheManager.getContentCache());
                jCaches.append("content", new JSONObject(mapper.writeValueAsString(info)));
            } catch (JsonProcessingException | JSONException e) {
                logger.error("Error creating cache info", e);
            }
        }
        if(ContentServerConfiguration.getInstance().getPdfCacheUse()) {
            try {
                CacheInfo info = new CacheInfo(cacheManager.getPdfCache());
                jCaches.append("content", new JSONObject(mapper.writeValueAsString(info)));
            } catch (JsonProcessingException | JSONException e) {
                logger.error("Error creating cache info", e);
            }
        }
        if(ContentServerConfiguration.getInstance().getThumbnailCacheUse()) {
            try {
                CacheInfo info = new CacheInfo(cacheManager.getThumbnailCache());
                jCaches.append("content", new JSONObject(mapper.writeValueAsString(info)));
            } catch (JsonProcessingException | JSONException e) {
                logger.error("Error creating cache info", e);
            }
        }
        return jCaches.toString();
    }
    
    /**
     * 
     * @param content
     * @param thumbs
     * @param pdf
     * @return
     */
    @DELETE
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthorizationBinding
    @Operation(summary = "Requires an authentication token. Clears cache for main images, thumbnails and PDFs for all records", tags = { "cache" })
    public IResponseMessage clearCache(
            @Parameter(description = "If true, main image content cache will be cleared for all records") @QueryParam("content") boolean content,
            @Parameter(description = "If true, thumbnail cache will be cleared for all records") @QueryParam("thumbs") boolean thumbs,
            @Parameter(description = "If true, PDF cache will be cleared for all records") @QueryParam("pdf") boolean pdf) {
        logger.trace("clearCache: {}/{}/{}", content, thumbs, pdf);

        new CacheUtils(cacheManager).emptyCache(content, thumbs, pdf);

        return new SuccessMessage(true, "Cache emptied successfully");
    }

    /**
     * 
     * @param pi
     * @param content
     * @param thumbs
     * @param pdf
     * @return
     * @throws IOException
     */
    @DELETE
    @Path(ApiUrls.CACHE_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponse(responseCode = "200", description = "Return the number of deleted cache items")
    @AuthorizationBinding
    @Operation(summary = "Requires an authentication token. Clears cache for main images, thumbnails and PDFs for all records", tags = { "cache" })
    public IResponseMessage clearCacheForRecord(@Parameter(description = "Record identifier") @PathParam("pi") String pi,
            @Parameter(description = "If true, main image content cache will be cleared for all records") @QueryParam("content") boolean content,
            @Parameter(description = "If true, thumbnail cache will be cleared for all records") @QueryParam("thumbs") boolean thumbs,
            @Parameter(description = "If true, PDF cache will be cleared for all records") @QueryParam("pdf") boolean pdf) throws IOException {
        logger.trace("clearCacheForRecord: {} {}/{}/{}", pi, content, thumbs, pdf);
        if (StringUtils.isEmpty(pi)) {
            servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "pi is required");
            return null;
        }

        int deleted = new CacheUtils(cacheManager).deleteFromCache(pi, content, thumbs, pdf);

        return new SuccessMessage(true, deleted + " items deleted successfully");
    }
}
