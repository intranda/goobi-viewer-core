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
package io.goobi.viewer.api.rest.v1.statistics;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.bindings.AuthorizationBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.statistics.MovingWallAnnualStatistics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * REST resource providing statistics about moving wall access restrictions and record availability by publication year.
 */
@jakarta.ws.rs.Path(ApiUrls.STATISTICS_MOVING_WALL)
public class MovingWallStatisticsResource {

    private static final Logger logger = LogManager.getLogger(MovingWallStatisticsResource.class);

    private static final String VALUE_SEPARATOR = ";";

    /**
     * Returns the identifiers of records unlocked by the moving wall in the given year, as CSV.
     *
     * <p>A record is included when it is a work (ISWORK:true) whose DATE_PUBLICRELEASEDATE Solr field falls within the given calendar
     * year; each line contains the release date and the record's persistent identifier, sorted by release date.
     *
     * @param year the year for which to retrieve moving wall statistics
     * @return CSV listing of unlocked record identifiers with their release dates
     */
    @GET
    @jakarta.ws.rs.Path(ApiUrls.STATISTICS_MOVING_WALL_YEAR)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, "text/csv" })
    @AuthorizationBinding
    @Operation(summary = "Requires an authentication token. Get moving wall unlocked record identifiers for the given year", tags = { "statistics" },
            description = "A record is included when it is a work whose release date (the DATE_PUBLICRELEASEDATE Solr field) falls"
                    + " within the given calendar year; entries are sorted by that date. Although json and plain text are also declared"
                    + " as possible response media types, the response body is always CSV.")
    @ApiResponse(responseCode = "200", description = "CSV list of record identifiers unlocked by the moving wall for the given year",
            content = { @Content(mediaType = "text/csv", schema = @Schema(type = "string")),
                    @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(type = "string")),
                    @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "string")) })
    @ApiResponse(responseCode = "400", description = "Invalid year value (not an integer)")
    @ApiResponse(responseCode = "401", description = "No authorization token provided or token is invalid")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable or internal error")
    public Response getStatisticsForYear(
            @Parameter(description = "The year for which to retrieve moving wall statistics") @PathParam("year") int year)
            throws PresentationException, IndexUnreachableException {
        logger.trace("getStatisticsForYear: {}", year);
        return Response.status(Response.Status.OK)
                .entity(new MovingWallAnnualStatistics(year).exportAsCSV(VALUE_SEPARATOR))
                .type("text/csv")
                .build();
    }
}
