package io.goobi.viewer.api.rest.v1.statistics.usage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.statistics.usage.StatisticsSummary;
import io.goobi.viewer.model.statistics.usage.StatisticsSummaryBuilder;
import io.goobi.viewer.model.statistics.usage.StatisticsSummaryFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@javax.ws.rs.Path(ApiUrls.STATISTICS_USAGE)
public class UsageStatisticsResource {

    private static final Logger logger = LoggerFactory.getLogger(UsageStatisticsResource.class);
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
    public StatisticsSummary getStatisticsForDay(@Parameter(description = "date to observe, in format yyyy-mm-dd") @PathParam("date") String date) throws DAOException {
        return new StatisticsSummaryBuilder(StatisticsSummaryFilter.ofDate(getLocalDate(date))).loadSummary();
    }


    @GET
    @javax.ws.rs.Path(ApiUrls.STATISTICS_USAGE_DATE_RANGE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Checks and reports the availability of relevant data providing services", tags = { "statistics" })
    public StatisticsSummary getStatisticsForDays(@Parameter(description = "first date to observer, in format yyyy-mm-dd") @PathParam("startDate") String start,
            @Parameter(description = "last date to observer, in format yyyy-mm-dd") @PathParam("endDate") String end) throws DAOException {
        return new StatisticsSummaryBuilder(StatisticsSummaryFilter.ofDateRange(getLocalDate(start), getLocalDate(end))).loadSummary();
    }

    
    LocalDate getLocalDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
