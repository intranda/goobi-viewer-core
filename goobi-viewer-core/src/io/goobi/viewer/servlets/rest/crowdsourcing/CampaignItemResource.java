/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.servlets.rest.crowdsourcing;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignItem;
import io.goobi.viewer.model.crowdsourcing.queries.CrowdsourcingQuery;
import io.goobi.viewer.model.crowdsourcing.queries.QueryType;
import io.goobi.viewer.model.crowdsourcing.queries.TargetFrequency;
import io.goobi.viewer.model.crowdsourcing.queries.TargetSelector;
import io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * @author florian
 *
 */
@Path("/crowdsourcing/campaign")
@ViewerRestServiceBinding
public class CampaignItemResource {
    
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    private final URI requestURI;
    
    public CampaignItemResource() {        
        try {
            this.requestURI = URI.create(DataManager.getInstance().getConfiguration().getRestApiUrl());
        } catch (ViewerConfigurationException e) {
            throw new WebApplicationException(e);
        }
    }


    @GET
    @Path("/{campaignId}/annotate/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public CampaignItem getItemForManifest(@PathParam("campaignId") Long campaignId, @PathParam("pi") String pi) throws URISyntaxException {
        URI servletURI = URI.create(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest));
        URI manifestURI = new ManifestBuilder(servletURI, requestURI).getManifestURI(pi);
        
        //TODO: Create item from campaign
        CampaignItem item = new CampaignItem();
        item.setSource(manifestURI);
        
        CrowdsourcingQuery query = new CrowdsourcingQuery(QueryType.PLAINTEXT, TargetFrequency.MULTIPLE_PER_CANVAS, TargetSelector.RECTANGLE);
        query.setLabel(new MultiLanguageMetadataValue(new String[]{"de", "Bild auswählen"}, new String[]{"en", "Select image"}));
        query.setDescription(new MultiLanguageMetadataValue(new String[]{"de", "Wählen Sie einen Bildbereich aus und geben Sie eine kurze Beschreibung dazu ein."}, new String[]{"en", "Select an area in the image and enter a short description about it."}));
        item.addQuery(query);
        
        CrowdsourcingQuery comment = new CrowdsourcingQuery(QueryType.PLAINTEXT, TargetFrequency.ONE_PER_CANVAS, TargetSelector.WHOLE_PAGE);
        comment.setLabel(new MultiLanguageMetadataValue(new String[]{"de", "Anmerkungen"}, new String[]{"en", "Notes"}));
        comment.setDescription(new MultiLanguageMetadataValue(new String[]{"de", "Hier ist Platz für Anmerkungen zu den Annotationen dieser Seite"}, new String[]{"en", "This is a space for notes about the annotations on this page"}));

        item.addQuery(comment);
        
        return item;
    }

}
