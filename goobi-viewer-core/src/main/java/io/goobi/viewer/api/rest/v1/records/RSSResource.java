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
package io.goobi.viewer.api.rest.v1.records;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.model.rss.Channel;
import io.goobi.viewer.model.rss.RSSFeed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author florian
 *
 */
@Path(ApiUrls.RECORDS_RSS)
@CORSBinding
@ViewerRestServiceBinding
public class RSSResource {

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @GET
    @Produces({ MediaType.TEXT_XML })
    @Operation(
            tags = { "records", "rss" },
            summary = "Get an rss feed of the most recent records")
    public String getRssFeed(
            @Parameter(description = "Subtheme: Results are filtered to values within the given subtheme (optional)") 
            @QueryParam("subtheme") String subtheme,
            @Parameter(description = "Language of the returned metadata labels and values (optional)") 
            @QueryParam("lang") String language,
            @Parameter(description = "Limit for results to return (optional)") @QueryParam("max") Integer maxHits,
            @Parameter(description = "Search query to filter results (optional)") @QueryParam("query") String query,
            @Parameter(description = "Facet query. Several queries may be entered as ';;' separated list (optional)") 
            @QueryParam("facets") String facets,
            @Parameter(description = "The solr field to sort the results by. Default is 'DATECERATED' (optional)") 
            @QueryParam("sortField") String sortField,
            @Parameter(description = "Set to 'false' to sort entries in ascending order. Default is 'true' (optional)") 
            @QueryParam("sortDescending") Boolean sortDescending)
            throws ContentLibException {

        return RSSFeed.createRssFeedString(language, maxHits, subtheme, query, facets, servletRequest, sortField, sortDescending == null || sortDescending);
    }

    @GET
    @Path(ApiUrls.RECORDS_RSS_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "records", "rss" },
            summary = "Get an a json representation of an RSS feed of the most recent records")
    public Channel getRssJsonFeed(
            @Parameter(description = "Subtheme: Results are filtered to values within the given subtheme (optional)") 
            @QueryParam("subtheme") String subtheme,
            @Parameter(description = "Language of the returned metadata labels and values (optional)") @QueryParam("lang") String language,
            @Parameter(description = "Limit for results to return (optional)") @QueryParam("max") Integer maxHits,
            @Parameter(description = "Search query to filter results (optional)") @QueryParam("query") String query,
            @Parameter(description = "Facet query. Several queries may be entered as ';;' separated list (optional)") 
            @QueryParam("facets") String facets,
            @Parameter(description = "The solr field to sort the results by. Default is 'DATECERATED' (optional)") 
            @QueryParam("sortField") String sortField,
            @Parameter(description = "Set to 'false' to sort entries in ascending order. Default is 'true' (optional)") 
            @QueryParam("sortDescending") Boolean sortDescending)
            throws ContentLibException {

        return RSSFeed.createRssResponse(language, maxHits, subtheme, query, facets, servletRequest, sortField,
                sortDescending == null || sortDescending);
    }

}
