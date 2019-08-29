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
package io.goobi.viewer.model.crowdsourcing.campaigns;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import io.goobi.viewer.model.crowdsourcing.queries.CrowdsourcingQuery;

/**
 * @author florian
 *
 */
public class CampaignItem {

    private URI source;
    private Campaign campaign;
    private CampaignItemStatus status = CampaignItemStatus.CREATED;

    /**
     * @return the source
     */
    public URI getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(URI source) {
        this.source = source;
    }

    /**
     * @return the campaign
     */
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * @param campaign the campaign to set
     */
    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    /**
     * @return a new list containing all queries
     */
    public List<CrowdsourcingQuery> getQueries() {
        return new ArrayList<>(campaign.getQueries());
    }

    public void addQuery(CrowdsourcingQuery query) {
        campaign.getQueries().add(query);
    }

    public void removeQuery(CrowdsourcingQuery query) {
        campaign.getQueries().remove(query);
    }

    /**
     * @return the status
     */
    public CampaignItemStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(CampaignItemStatus status) {
        this.status = status;
    }
}
