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

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.statistics.MovingWallAnnualStatistics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@javax.ws.rs.Path(ApiUrls.STATISTICS_MOVING_WALL)
public class MovingWallStatisticsResource {

    private static final Logger logger = LogManager.getLogger(MovingWallStatisticsResource.class);

    private static final String VALUE_SEPARATOR = ";";

    @GET
    @javax.ws.rs.Path(ApiUrls.STATISTICS_MOVING_WALL_YEAR)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, "text/csv" })
    @Operation(summary = "Get moving wall unlocked record identfiers for the given year", tags = { "statistics" })
    public Response getStatisticsForYear(@Parameter(description = "statistics year") @PathParam("year") int year)
            throws PresentationException, IndexUnreachableException {
        logger.trace("getStatisticsForYear: {}", year);
        return Response.status(Response.Status.OK)
                .entity(new MovingWallAnnualStatistics(year).exportAsCSV(VALUE_SEPARATOR))
                .type("text/csv")
                .build();
    }
}
