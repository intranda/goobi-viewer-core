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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author Florian Alpers
 */
@Path(ApiUrls.RECORDS_RSS)
@CORSBinding
@ViewerRestServiceBinding
public class RSSResource {

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * Returns an RSS feed of the most recent records as an XML string.
     *
     * <p>The query, subtheme and facets parameters are combined into a single Solr query aggregated to the top-level structure of each
     * matching record; facets is parsed as an active facet string and applied as separate filter queries. Parameters left out fall back to
     * configuration or request defaults: max to the configured feed item count, sortField to DATECREATED, sortDescending to true, and lang
     * to the request's locale. A Solr query rejected for invalid syntax is reported as a client error instead of an internal one.
     *
     * @param subtheme Subtheme: results are filtered to values within the given subtheme (optional)
     * @param language language of the returned metadata labels and values (optional)
     * @param maxHits limit for results to return (optional)
     * @param query search query to filter results (optional)
     * @param facets facet query. Several queries may be entered as ';;' separated list (optional)
     * @param sortField the Solr field to sort the results by. Default is 'DATECREATED' (optional)
     * @param sortDescending set to 'false' to sort entries in ascending order. Default is 'true' (optional)
     * @return the RSS feed as an XML string
     * @throws ContentLibException if the Solr query is malformed or the index cannot be reached
     */
    @GET
    @Produces({ MediaType.TEXT_XML })
    @Operation(
            tags = { "records", "rss" },
            summary = "Get an rss feed of the most recent records",
            description = "The query, subtheme and facets parameters are combined into a single Solr query aggregated to the top-level"
                    + " structure of each matching record; facets is parsed as an active facet string and applied as separate filter"
                    + " queries. Parameters left out fall back to configuration or request defaults: max to the configured feed item count,"
                    + " sortField to DATECREATED, sortDescending to true, and lang to the request's locale. A Solr query rejected for"
                    + " invalid syntax is reported as a client error instead of an internal one.")
    @ApiResponse(responseCode = "200", description = "RSS feed in XML format",
            content = @Content(mediaType = MediaType.TEXT_XML, schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400", description = "The provided query parameter contains invalid Solr query syntax")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable or internal error")
    public String getRssFeed(
            @Parameter(description = "Subtheme: Results are filtered to values within the given subtheme (optional)") 
            @QueryParam("subtheme") String subtheme,
            @Parameter(description = "Language of the returned metadata labels and values (optional)") 
            @QueryParam("lang") String language,
            @Parameter(description = "Limit for results to return (optional)",
                    schema = @Schema(minimum = "0", maximum = "2147483647")) @QueryParam("max") Integer maxHits,
            @Parameter(description = "Search query to filter results (optional)") @QueryParam("query") String query,
            @Parameter(description = "Facet query. Several queries may be entered as ';;' separated list (optional)") 
            @QueryParam("facets") String facets,
            @Parameter(description = "The solr field to sort the results by. Default is 'DATECERATED' (optional)",
                    schema = @Schema(pattern = "^[A-Za-z_][A-Za-z0-9_]*$"))
            @QueryParam("sortField") String sortField,
            @Parameter(description = "Set to 'false' to sort entries in ascending order. Default is 'true' (optional)") 
            @QueryParam("sortDescending") Boolean sortDescending)
            throws ContentLibException {

        return RSSFeed.createRssFeedString(language, maxHits, subtheme, query, facets, servletRequest, sortField, sortDescending == null
                || sortDescending);
    }

    /**
     * Returns an RSS feed of the most recent records as a JSON object.
     *
     * <p>The query, subtheme and facets parameters are combined into a single Solr query searched without aggregating hits to a record's
     * top-level structure; facets is parsed as an active facet string and applied as separate filter queries. Parameters left out fall back
     * to configuration or request defaults: max to the configured feed item count, sortField to DATECREATED, sortDescending to true, and
     * lang to the request's locale. A Solr query rejected for invalid syntax is reported as a client error instead of an internal one.
     *
     * @param subtheme Subtheme: results are filtered to values within the given subtheme (optional)
     * @param language language of the returned metadata labels and values (optional)
     * @param maxHits limit for results to return (optional)
     * @param query search query to filter results (optional)
     * @param facets facet query. Several queries may be entered as ';;' separated list (optional)
     * @param sortField the Solr field to sort the results by. Default is 'DATECREATED' (optional)
     * @param sortDescending set to 'false' to sort entries in ascending order. Default is 'true' (optional)
     * @return the RSS feed as a {@link Channel}
     * @throws ContentLibException if the Solr query is malformed or the index cannot be reached
     */
    @GET
    @Path(ApiUrls.RECORDS_RSS_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "records", "rss" },
            summary = "Get a JSON representation of an RSS feed of the most recent records",
            description = "The query, subtheme and facets parameters are combined into a single Solr query searched without aggregating"
                    + " hits to a record's top-level structure; facets is parsed as an active facet string and applied as separate filter"
                    + " queries. Parameters left out fall back to configuration or request defaults: max to the configured feed item count,"
                    + " sortField to DATECREATED, sortDescending to true, and lang to the request's locale. A Solr query rejected for"
                    + " invalid syntax is reported as a client error instead of an internal one.")
    @ApiResponse(responseCode = "200", description = "RSS feed as JSON object", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "The provided query parameter contains invalid Solr query syntax")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable or internal error")
    public Channel getRssJsonFeed(
            @Parameter(description = "Subtheme: Results are filtered to values within the given subtheme (optional)") 
            @QueryParam("subtheme") String subtheme,
            @Parameter(description = "Language of the returned metadata labels and values (optional)") @QueryParam("lang") String language,
            @Parameter(description = "Limit for results to return (optional)",
                    schema = @Schema(minimum = "0", maximum = "2147483647")) @QueryParam("max") Integer maxHits,
            @Parameter(description = "Search query to filter results (optional)") @QueryParam("query") String query,
            @Parameter(description = "Facet query. Several queries may be entered as ';;' separated list (optional)") 
            @QueryParam("facets") String facets,
            @Parameter(description = "The solr field to sort the results by. Default is 'DATECERATED' (optional)",
                    schema = @Schema(pattern = "^[A-Za-z_][A-Za-z0-9_]*$"))
            @QueryParam("sortField") String sortField,
            @Parameter(description = "Set to 'false' to sort entries in ascending order. Default is 'true' (optional)") 
            @QueryParam("sortDescending") Boolean sortDescending)
            throws ContentLibException {

        return RSSFeed.createRssResponse(language, maxHits, subtheme, query, facets, servletRequest, sortField,
                sortDescending == null || sortDescending);
    }

}
