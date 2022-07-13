package io.goobi.viewer.api.rest.v1.statistics.usage;

import java.time.LocalDate;

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

import io.goobi.viewer.api.rest.model.MonitoringStatus;
import io.goobi.viewer.api.rest.v1.ApiUrls;
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
    @Operation(summary = "Checks and reports the availability of relevant data providing services", tags = { "monitoring" })
    public String getStatisticsForDay(@Parameter(description = "date to observe, in format yyyy-mm-dd") @PathParam("date") LocalDate date) {
        
    }
}
