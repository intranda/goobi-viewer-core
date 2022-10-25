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
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

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
    
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String SCV_VALUE_SEPARATOR = ",";
    private static final Logger logger = LogManager.getLogger(UsageStatisticsResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Context
    private ContainerRequestContext requestContext;
    @Context
    private ContainerResponseContext responseContext;
    
    @GET
    @javax.ws.rs.Path(ApiUrls.STATISTICS_USAGE_DATE)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, "text/csv" })
    @Operation(summary = "Get usage statistics for a single day", tags = { "statistics" })
    public Response getStatisticsForDay(
            @Parameter(description = "date to observe, in format yyyy-mm-dd") @PathParam("date") String date,
            @Parameter(description = "additional SOLR query to filter records which should be counted. "
                    + "Only requests to records matching the query will be counted") @QueryParam("recordFilterQuery") String recordFilterQuery,
            @Parameter(description="the format in which to return the data. May be json, text or csv. Default is json")@QueryParam("format") String format) throws DAOException, IndexUnreachableException, PresentationException {
        
        if(StringUtils.isBlank(format)) {
            format = servletRequest.getHeader("Accept");
        }

        
        StatisticsSummary summary = new StatisticsSummaryBuilder().loadSummary(StatisticsSummaryFilter.of(getLocalDate(date), getLocalDate(date), recordFilterQuery));
        if("text/csv".equals(format) || "csv".equals(format)) {
            return Response.status(Response.Status.OK).entity(summary.getAsCsv(servletRequest.getLocale(), SCV_VALUE_SEPARATOR)).type("text/csv").build();
        } else if("text/plain".equals(format) || "text".equals(format)) {
            return Response.status(Response.Status.OK).entity(summary.getAsCsv(servletRequest.getLocale(), SCV_VALUE_SEPARATOR)).type("text/plain").build();
        } else {
            this.servletResponse.setContentType("application/json");
            return Response.status(Response.Status.OK).entity(summary).type(MediaType.APPLICATION_JSON).build();
        }
    }


    @GET
    @javax.ws.rs.Path(ApiUrls.STATISTICS_USAGE_DATE_RANGE)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, "text/csv" })
    @Operation(summary = "Get combined usage statistics for a range of days", tags = { "statistics" })
    public Response getStatisticsForDays(
            @Parameter(description = "first date to observer, in format yyyy-mm-dd") @PathParam("startDate") String start,
            @Parameter(description = "last date to observer, in format yyyy-mm-dd") @PathParam("endDate") String end,
            @Parameter(description = "additional SOLR query to filter records which should be counted. "
                    + "Only requests to records matching the query will be counted") @QueryParam("recordFilterQuery") String recordFilterQuery,
            @Parameter(description="the format in which to return the data. May be json, text or csv. Default is json")@QueryParam("format") String format) 
                    throws DAOException, IndexUnreachableException, PresentationException, JsonProcessingException {
        
        if(StringUtils.isBlank(format)) {
            format = servletRequest.getHeader("Accept");
        }
        
        StatisticsSummary summary = new StatisticsSummaryBuilder().loadSummary(StatisticsSummaryFilter.of(getLocalDate(start), getLocalDate(end), recordFilterQuery));        
        if("text/csv".equals(format) || "csv".equals(format)) {
            return Response.status(Response.Status.OK).entity(summary.getAsCsv(servletRequest.getLocale(), SCV_VALUE_SEPARATOR)).type("text/csv").build();
        } else if("text/plain".equals(format) || "text".equals(format)) {
            return Response.status(Response.Status.OK).entity(summary.getAsCsv(servletRequest.getLocale(), SCV_VALUE_SEPARATOR)).type("text/plain").build();
        } else {
            this.servletResponse.setContentType("application/json");
            return Response.status(Response.Status.OK).entity(summary).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @javax.ws.rs.Path(ApiUrls.STATISTICS_USAGE_DATE_LIST)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, "text/csv" })
    @Operation(summary = "Get a list of usage statistics for a time frame", tags = { "statistics" })
    public Response getStatisticsListForDates(
            @Parameter(description = "first date to observer, in format yyyy-mm-dd") @PathParam("startDate") String start,
            @Parameter(description = "last date to observer, in format yyyy-mm-dd") @PathParam("endDate") String end,
            @Parameter(description = "additional SOLR query to filter records which should be counted. "
                    + "Only requests to records matching the query will be counted") @QueryParam("recordFilterQuery") String recordFilterQuery,
            @Parameter(description="the format in which to return the data. May be json, text or csv. Default is json")@QueryParam("format") String format,
            @Parameter(description="the number of time units (default: days) each statistics should span")@QueryParam("step") Integer step,
            @Parameter(description="The time unit to use for 'step' paramter. May be years, months, weeks or days")@QueryParam("stepUnit") String stepUnit) 
                    throws DAOException, IndexUnreachableException, PresentationException, JsonProcessingException {
        
        if(StringUtils.isBlank(format)) {
            format = servletRequest.getHeader("Accept");
        }
        
        step = step != null ? step : 1;
        Period stepPeriod = getPeriod(step, stepUnit);
        
        LocalDate startDate = getLocalDate(start);
        LocalDate endDate = getLocalDate(end);
        if(LocalDate.now().isBefore(endDate)) {
            endDate = LocalDate.now();
        }
        if(endDate.isBefore(startDate)) {
            return Response.status(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        } else {
            LocalDate date = startDate;
            List<StatisticsSummary> summaries = new ArrayList<>();
            while(date.isBefore(endDate.plus(stepPeriod))) {
                StatisticsSummary summary = new StatisticsSummaryBuilder().loadSummary(StatisticsSummaryFilter.of(date, date.plus(stepPeriod).minusDays(1), recordFilterQuery));        
                if(!summary.isEmpty()) {                    
                    summaries.add(summary);
                }
                date = date.plus(stepPeriod);
            }
            if("text/csv".equals(format) || "csv".equals(format)) {
                String entity = summaries.stream().map(summary -> summary.getAsCsv(servletRequest.getLocale(), SCV_VALUE_SEPARATOR)).collect(Collectors.joining("\n"));
                return Response.status(Response.Status.OK).entity(entity).type("text/csv").build();
            } else if("text/plain".equals(format) || "text".equals(format)) {
                String entity = summaries.stream().map(summary -> summary.getAsCsv(servletRequest.getLocale(), SCV_VALUE_SEPARATOR)).collect(Collectors.joining("\n"));
                return Response.status(Response.Status.OK).entity(entity).type("text/plain").build();
            } else {
                this.servletResponse.setContentType("application/json");
                return Response.status(Response.Status.OK).entity(summaries).type(MediaType.APPLICATION_JSON).build();
            }
        }

        
    }


    private Period getPeriod(Integer step, String stepUnit) {
        ChronoUnit unit;
        try {            
            unit = ChronoUnit.valueOf(stepUnit.toUpperCase());
        } catch(NullPointerException | IllegalArgumentException e) {
            unit = ChronoUnit.DAYS;
        }
        Period stepPeriod;
        switch(unit) {
            case YEARS: 
                stepPeriod = Period.ofYears(step);
                break;
            case MONTHS:
                stepPeriod = Period.ofMonths(step);
                break;
            case WEEKS:
                stepPeriod = Period.ofWeeks(step);
                break;
            default:
                stepPeriod = Period.ofDays(step);
        }
        return stepPeriod;
    }

    
    LocalDate getLocalDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_FORMAT));
    }
}
