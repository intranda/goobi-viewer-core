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
package io.goobi.viewer.api.rest.v1.crowdsourcing;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.Version;
import io.goobi.viewer.api.rest.bindings.CrowdsourcingCampaignBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.CrowdsourcingCampaignFilter;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignLogMessage;
import io.goobi.viewer.model.log.LogMessage;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * REST resource for retrieving activity logs and progress statistics for crowdsourcing campaign items.
 *
 * @author Florian Alpers
 */
@Hidden
@Path("/crowdsourcing/campaigns/{campaignId}")
@ViewerRestServiceBinding
@CrowdsourcingCampaignBinding
public class CampaignItemLogResource {

    private static final Logger logger = LogManager.getLogger(CampaignItemLogResource.class);

    @Context
    private HttpServletResponse servletResponse;

    protected AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getDataApiManager(Version.v1).orElse(null);
    protected AnnotationsResourceBuilder annoBuilder = null;

    private final Long campaignId;

    /**
     * Creates a new CampaignItemResource instance.
     *
     * @param servletRequest HTTP servlet request
     * @param campaignId identifier of the crowdsourcing campaign
     */
    public CampaignItemLogResource(@Context HttpServletRequest servletRequest,
            @Parameter(description = "Crowdsourcing campaign identifier") @PathParam("campaignId") Long campaignId) {
        this.campaignId = campaignId;
        servletRequest.setAttribute(CrowdsourcingCampaignFilter.CAMPAIGN_ID_REQUEST_ATTRIBUTE, campaignId);
    }

    /**
     * 
     * @param servletRequest HTTP servlet request
     * @param urls API URL manager for path construction
     * @param campaignId identifier of the crowdsourcing campaign
     */
    public CampaignItemLogResource(HttpServletRequest servletRequest, AbstractApiUrlManager urls, @PathParam("campaignId") Long campaignId) {
        this.urls = urls;
        this.campaignId = campaignId;
        servletRequest.setAttribute(CrowdsourcingCampaignFilter.CAMPAIGN_ID_REQUEST_ATTRIBUTE, campaignId);
    }

    @GET
    @Path("/{pi}/log")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    @Operation(summary = "Get the log messages for a record within a crowdsourcing campaign", tags = { "crowdsourcing" })
    @ApiResponse(responseCode = "200", description = "List of log messages for the record")
    @ApiResponse(responseCode = "404", description = "Campaign or record not found")
    @ApiResponse(responseCode = "500", description = "Database error")
    public List<LogMessage> getLogForManifest(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Context HttpServletRequest servletRequest)
            throws DAOException, ContentNotFoundException {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
        if (campaign != null) {
            return campaign.getLogMessages()
                    .stream()
                    .filter(m -> m.getPi().equals(pi))
                    .map(clm -> new LogMessage(clm, servletRequest))
                    .sorted()
                    .collect(Collectors.toList());
        }
        throw new ContentNotFoundException("No campaign found with id " + campaignId);
    }

    @POST
    @Path("/{pi}/log")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    @Operation(summary = "Add a log message to a record within a crowdsourcing campaign", tags = { "crowdsourcing" })
    @ApiResponse(responseCode = "200", description = "Log message created and returned")
    @ApiResponse(responseCode = "400", description = "Invalid message body")
    @ApiResponse(responseCode = "404", description = "Campaign or record not found")
    @ApiResponse(responseCode = "500", description = "Database error")
    public LogMessage addMessage(LogMessage message,
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Context HttpServletRequest servletRequest)
            throws DAOException, ContentNotFoundException {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
        if (campaign != null) {
            CampaignLogMessage campaignMessage = campaign.addLogMessage(message, pi);
            DataManager.getInstance().getDao().updateCampaign(campaign);
            return new LogMessage(campaignMessage, servletRequest);
        }
        throw new ContentNotFoundException("No campaign found with id " + campaignId);
    }

    @DELETE
    @Path("/{pi}/log/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    @Operation(summary = "Delete a log message from a crowdsourcing campaign record", tags = { "crowdsourcing" })
    @ApiResponse(responseCode = "200", description = "Message deleted successfully")
    @ApiResponse(responseCode = "404", description = "Message, campaign, or record not found")
    @ApiResponse(responseCode = "500", description = "Database error")
    public void deleteMessage(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Identifier of the log message") @PathParam("id") Long messageId)
            throws DAOException, ContentNotFoundException {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
        if (campaign != null) {
            campaign.deleteLogMessage(messageId);
            DataManager.getInstance().getDao().updateCampaign(campaign);
        } else {
            throw new ContentNotFoundException("No campaign found with id " + campaignId);
        }
    }

    @GET
    @Path("/{pi}/log/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    @Operation(summary = "Get a specific log message by id", tags = { "crowdsourcing" })
    @ApiResponse(responseCode = "200", description = "Log message")
    @ApiResponse(responseCode = "404", description = "Message, campaign, or record not found")
    @ApiResponse(responseCode = "500", description = "Database error")
    public LogMessage getMessage(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Identifier of the log message") @PathParam("id") Long messageId,
            @Context HttpServletRequest servletRequest)
            throws DAOException, ContentNotFoundException {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
        if (campaign != null) {
            Optional<CampaignLogMessage> message =
                    campaign.getLogMessages().stream().filter(m -> pi.equals(m.getPi()) && messageId.equals(m.getId())).findAny();
            return message.map(clm -> new LogMessage(clm, servletRequest))
                    .orElseThrow(() -> new ContentNotFoundException("No message found with id " + messageId));
        }
        throw new ContentNotFoundException("No campaign found with id " + campaignId);
    }

}
