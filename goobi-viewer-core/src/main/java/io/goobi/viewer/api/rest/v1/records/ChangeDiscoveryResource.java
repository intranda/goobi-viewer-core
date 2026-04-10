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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CHANGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CHANGES_PAGE;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.iiif.discovery.Activity;
import de.intranda.api.iiif.discovery.OrderedCollection;
import de.intranda.api.iiif.discovery.OrderedCollectionPage;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.iiif.discovery.ActivityCollectionBuilder;
import io.goobi.viewer.solr.SolrTools;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author Florian Alpers
 */
@Path(RECORDS_CHANGES)
@CORSBinding
@ViewerRestServiceBinding
public class ChangeDiscoveryResource {

    private static final Logger logger = LogManager.getLogger(ChangeDiscoveryResource.class);
    private static final String[] CONTEXT = { "http://iiif.io/api/discovery/0/context.json", "https://www.w3.org/ns/activitystreams" };

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private ApiUrls apiUrlManager;

    /**
     * Provides a view of the entire list of all activities by linking to the first and last page of the collection. The pages contain the actual
     * activity entries and are provided by {@link #getPage(int, String startDate, String filterQuery) /iiif/discovery/activities/&lt;pageNo&gt;/}.
     *
     * <p>This resource also contains a count of the total number of activities
     * 
     * @param startDate If not null, must have the form 'yyyy-MM-dd'. Then only activities at or after this date will be listed
     * @param filterQuery If not null or empty, must be a valid SOLR query string which is used to filter the results
     * @return An {@link de.intranda.api.iiif.discovery.OrderedCollection} of {@link Activity Activities}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "records", "iiif" },
            summary = "Get a IIIF change discovery activity stream of all record changes")
    @ApiResponse(responseCode = "200", description = "Return activity stream according to IIIF change discovery specification")
    @ApiResponse(responseCode = "400", description = "Invalid date format for 'start' parameter (expected yyyy-MM-dd)")
    @ApiResponse(responseCode = "500", description = "An internal error occurred, possibly due to an unreachable Solr index")
    public OrderedCollection<Activity> getAllChanges(
            @Parameter(description = "Optional date in the form 'yyyy-MM-dd' of the oldest changes to return",
                    schema = @Schema(pattern = "^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) @QueryParam("start") String startDate,
            @Parameter(description = "Optional Solr query to filter results",
                    schema = @Schema(pattern = "^[ -~]*$")) @QueryParam("filter") String filterQuery)
            throws PresentationException, IndexUnreachableException, IllegalRequestException {
        ActivityCollectionBuilder builder = new ActivityCollectionBuilder(apiUrlManager, DataManager.getInstance().getSearchIndex(),
                DataManager.getInstance().getConfiguration().getIIIFDiscoveryAvtivitiesPerPage());
        if (startDate != null) {
            // Validate the format against the documented pattern before trying to parse.
            // An empty string (start=) or a non-matching string is rejected with 400.
            // A string matching the pattern but calendar-invalid (e.g. 5850-11-31) is silently
            // ignored because it is schema-compliant and rejecting it would violate the contract.
            if (!startDate.matches("[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])")) {
                throw new IllegalRequestException("Invalid date format for 'start': expected yyyy-MM-dd, got: " + startDate);
            }
            try {
                builder.setStartDate(LocalDate.parse(startDate).atStartOfDay());
            } catch (DateTimeParseException e) {
                // Date matches the regex pattern but is calendar-invalid (e.g. month with 30 days
                // but day=31). Ignore the filter rather than returning 400 for a schema-valid value.
                logger.warn("Ignoring calendar-invalid 'start' date '{}': {}", startDate, e.getMessage());
            }
        }
        // Enforce the documented pattern: reject non-ASCII or control characters.
        // The schema defines filter as ^[ -~]*$ (printable ASCII 0x20–0x7E only).
        if (StringUtils.isNotBlank(filterQuery) && !filterQuery.matches("[ -~]*")) {
            throw new IllegalRequestException("Invalid filter: must contain only printable ASCII characters (matching ^[ -~]*$)");
        }
        if (StringUtils.isNotBlank(filterQuery)) {
            builder.setFilterQuery(filterQuery);
        }
        try {
            OrderedCollection<Activity> collection = builder.buildCollection();
            collection.setContext(CONTEXT);
            return collection;
        } catch (IndexUnreachableException e) {
            // If a user-supplied filter query is present, any Solr exception could be caused by
            // that filter (e.g. bare '"', ')', '/' → SyntaxError). Return 400 rather than 500.
            if (StringUtils.isNotBlank(filterQuery) || SolrTools.isQuerySyntaxError(e)) {
                throw new IllegalRequestException("Invalid filter query: " + filterQuery);
            }
            throw e;
        } catch (PresentationException e) {
            if (StringUtils.isNotBlank(filterQuery) || SolrTools.isQuerySyntaxError(e)) {
                throw new IllegalRequestException("Invalid filter query: " + filterQuery);
            }
            throw e;
        }
    }

    /**
     * Provides a partial list of {@link Activity Activities} along with links to the preceding and succeeding page as well as the parent collection
     * as provided by {@link #getAllChanges(String startDate, String filterQuery) /iiif/discovery/activities/}. The number of Activities on the page
     * is determined by {@link io.goobi.viewer.controller.Configuration#getIIIFDiscoveryAvtivitiesPerPage()
     * Configuration#getIIIFDiscoveryAvtivitiesPerPage()}
     *
     * @param pageNo The page number, starting with 0
     * @param startDate If not null, must have the form 'yyyy-MM-dd'. Then only activities at or after this date will be listed
     * @param filterQuery If not null or empty, must be a valid SOLR query string which is used to filter the results
     * @return An {@link de.intranda.api.iiif.discovery.OrderedCollectionPage} of {@link Activity Activities}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    @Hidden
    @GET
    @Path(RECORDS_CHANGES_PAGE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Get a single page of the IIIF change discovery activity stream", tags = { "records", "iiif" })
    @ApiResponse(responseCode = "200", description = "Ordered collection page of change activities")
    @ApiResponse(responseCode = "400", description = "Invalid date format for 'start' parameter (expected yyyy-MM-dd)")
    @ApiResponse(responseCode = "404", description = "No page found for the given page number")
    @ApiResponse(responseCode = "500", description = "An internal error occurred, possibly due to an unreachable Solr index")
    public OrderedCollectionPage<Activity> getPage(
            @Parameter(description = "page order within the collection of activities") @PathParam("pageNo") int pageNo,
            @Parameter(description = "Optional date in the form 'yyyy-MM-dd' of the oldest changes to return",
                    schema = @Schema(pattern = "^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) @QueryParam("start") String startDate,
            @Parameter(description = "Optional Solr query to filter results",
                    schema = @Schema(pattern = "^[ -~]*$")) @QueryParam("filter") String filterQuery)
            throws PresentationException, IndexUnreachableException, IllegalRequestException {
        ActivityCollectionBuilder builder = new ActivityCollectionBuilder(apiUrlManager, DataManager.getInstance().getSearchIndex(),
                DataManager.getInstance().getConfiguration().getIIIFDiscoveryAvtivitiesPerPage());
        if (startDate != null) {
            // Validate the format against the documented pattern before trying to parse.
            // An empty string (start=) or a non-matching string is rejected with 400.
            // A string matching the pattern but calendar-invalid (e.g. 5850-11-31) is silently
            // ignored because it is schema-compliant and rejecting it would violate the contract.
            if (!startDate.matches("[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])")) {
                throw new IllegalRequestException("Invalid date format for 'start': expected yyyy-MM-dd, got: " + startDate);
            }
            try {
                builder.setStartDate(LocalDate.parse(startDate).atStartOfDay());
            } catch (DateTimeParseException e) {
                // Date matches the regex pattern but is calendar-invalid (e.g. month with 30 days
                // but day=31). Ignore the filter rather than returning 400 for a schema-valid value.
                logger.warn("Ignoring calendar-invalid 'start' date '{}': {}", startDate, e.getMessage());
            }
        }
        // Enforce the documented pattern: reject non-ASCII or control characters.
        // The schema defines filter as ^[ -~]*$ (printable ASCII 0x20–0x7E only).
        if (StringUtils.isNotBlank(filterQuery) && !filterQuery.matches("[ -~]*")) {
            throw new IllegalRequestException("Invalid filter: must contain only printable ASCII characters (matching ^[ -~]*$)");
        }
        if (StringUtils.isNotBlank(filterQuery)) {
            builder.setFilterQuery(filterQuery);
        }
        try {
            OrderedCollectionPage<Activity> page = builder.buildPage(pageNo);
            page.setContext(CONTEXT);
            return page;
        } catch (PresentationException e) {
            if (StringUtils.isNotBlank(filterQuery) || SolrTools.isQuerySyntaxError(e)) {
                throw new IllegalRequestException("Invalid filter query: " + filterQuery);
            }
            throw e;
        }
    }

}
