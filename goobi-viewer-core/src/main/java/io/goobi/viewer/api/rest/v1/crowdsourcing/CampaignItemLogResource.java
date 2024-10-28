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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

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

/**
 * Request filter to ensure only users with sufficient rights may access campaign resources.
 *
 * @author florian
 *
 */
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
     * <p>
     * Constructor for CampaignItemResource.
     * </p>
     * 
     * @param servletRequest
     * @param campaignId
     */
    public CampaignItemLogResource(@Context HttpServletRequest servletRequest, @PathParam("campaignId") Long campaignId) {
        this.campaignId = campaignId;
        servletRequest.setAttribute(CrowdsourcingCampaignFilter.CAMPAIGN_ID_REQUEST_ATTRIBUTE, campaignId);
    }

    /**
     * 
     * @param servletRequest
     * @param urls
     * @param campaignId
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
    public List<LogMessage> getLogForManifest(@PathParam("pi") String pi, @Context HttpServletRequest servletRequest)
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
    public LogMessage addMessage(LogMessage message, @PathParam("pi") String pi, @Context HttpServletRequest servletRequest)
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
    public void deleteMessage(@PathParam("pi") String pi, @PathParam("id") Long messageId) throws DAOException, ContentNotFoundException {
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
    public LogMessage getMessage(@PathParam("pi") String pi, @PathParam("id") Long messageId, @Context HttpServletRequest servletRequest)
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
