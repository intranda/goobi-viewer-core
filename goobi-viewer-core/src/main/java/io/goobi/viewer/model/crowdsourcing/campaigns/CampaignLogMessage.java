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
package io.goobi.viewer.model.crowdsourcing.campaigns;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.goobi.viewer.model.log.LogMessage;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "cs_campaign_log")
public class CampaignLogMessage extends LogMessage {

    private static final long serialVersionUID = 2810349140888620668L;

    @ManyToOne
    @JoinColumn(name = "campaign_id", nullable = false)
    @JsonIgnore
    private Campaign campaign;

    @Column(name = "pi", nullable = false)
    private String pi;

    public CampaignLogMessage() {
        super();
    }

    /**
     * @param message
     * @param creatorId
     * @param dateCreated
     */
    public CampaignLogMessage(String message, Long creatorId, LocalDateTime dateCreated, Campaign campaign, String pi) {
        super(message, creatorId, dateCreated, null);
        this.campaign = campaign;
        this.pi = pi;
    }

    /**
     * @param message
     * @param creatorId
     */
    public CampaignLogMessage(String message, Long creatorId, Campaign campaign, String pi) {
        super(message, creatorId, null);
        this.campaign = campaign;
        this.pi = pi;
    }

    public CampaignLogMessage(LogMessage source, Campaign campaign, String pi) {
        super(source);
        this.campaign = campaign;
        this.pi = pi;
    }

    /**
     * @return the campaign
     */
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }
}
