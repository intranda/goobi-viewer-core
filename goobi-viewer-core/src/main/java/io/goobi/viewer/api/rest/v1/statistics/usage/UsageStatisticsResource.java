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
package io.goobi.viewer.api.rest.v1.statistics.usage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.statistics.usage.StatisticsSummary;
import io.goobi.viewer.model.statistics.usage.StatisticsSummaryBuilder;
import io.goobi.viewer.model.statistics.usage.StatisticsSummaryFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * Class for retrieving usage statistics for a day or a range of days
 * @author florian
 *
 */
@javax.ws.rs.Path(ApiUrls.STATISTICS_USAGE)
public class UsageStatisticsResource {

    private static final Logger logger = LogManager.getLogger(UsageStatisticsResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Context
    private ContainerRequestContext requestContext;
    
    @GET
    @javax.ws.rs.Path(ApiUrls.STATISTICS_USAGE_DATE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Checks and reports the availability of relevant data providing services", tags = { "statistics" })
    public StatisticsSummary getStatisticsForDay(
            @Parameter(description = "date to observe, in format yyyy-mm-dd") @PathParam("date") String date,
            @Parameter(description = "additional SOLR query to filter records which should be counted. "
                    + "Only requests to records matching the query will be counted") @QueryParam("recordFilterQuery") String recordFilterQuery) throws DAOException, IndexUnreachableException, PresentationException {
        return new StatisticsSummaryBuilder().loadSummary(StatisticsSummaryFilter.of(getLocalDate(date), getLocalDate(date), recordFilterQuery));
    }


    @GET
    @javax.ws.rs.Path(ApiUrls.STATISTICS_USAGE_DATE_RANGE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Checks and reports the availability of relevant data providing services", tags = { "statistics" })
    public StatisticsSummary getStatisticsForDays(
            @Parameter(description = "first date to observer, in format yyyy-mm-dd") @PathParam("startDate") String start,
            @Parameter(description = "last date to observer, in format yyyy-mm-dd") @PathParam("endDate") String end,
            @Parameter(description = "additional SOLR query to filter records which should be counted. "
                    + "Only requests to records matching the query will be counted") @QueryParam("recordFilterQuery") String recordFilterQuery) throws DAOException, IndexUnreachableException, PresentationException {
        return new StatisticsSummaryBuilder().loadSummary(StatisticsSummaryFilter.of(getLocalDate(start), getLocalDate(end), recordFilterQuery));
    }

    
    LocalDate getLocalDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
