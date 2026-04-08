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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.log.LogMessage;

/**
 * An item containing a campaign and a source to be annotated. Used to set up a frontend annotation view in javascript as well as process status
 * changes created by that view
 *
 * @author Florian Alpers
 */
@JsonInclude(Include.NON_NULL)
public class CampaignItem {

    private URI source;
    private String recordIdentifier;
    private Campaign campaign;
    private CrowdsourcingStatus recordStatus = null;
    private List<LogMessage> log = null;
    private Map<String, List<String>> metadata = new LinkedHashMap<>();
    private boolean pageStatisticMode = false;
    private Map<Integer, String> pageStatusMap = new HashMap<>();
    @JsonProperty("creator")
    private URI creatorURI = null;

    /**
     * URI to a iiif manifest or other collection of iiif canvases. All generated annotations target either the source itself or one of its canvases
     *

     */
    public URI getSource() {
        return source;
    }

    /**
     * Setter for the field <code>source</code>.
     *
     * @param source the URI identifying the resource to be annotated in this campaign item
     */
    public void setSource(URI source) {
        this.source = source;
    }

    /**
     * The {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} to create the annotations.
     *

     */
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * Setter for the field <code>campaign</code>.
     *
     * @param campaign the campaign this item belongs to
     */
    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    /**
     * getQuestions.
     *
     * @return a new list containing all queries
     */
    @JsonIgnore
    public List<Question> getQuestions() {
        return new ArrayList<>(campaign.getQuestions());
    }

    /**
     * The {@link CrowdsourcingStatus status} of the resource within the {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign}.
     *

     */
    public CrowdsourcingStatus getRecordStatus() {
        return recordStatus;
    }

    /**
     * Setter for the field <code>recordStatus</code>.
     *
     * @param recordStatus the crowdsourcing processing status of the record in this campaign
     */
    public void setRecordStatus(CrowdsourcingStatus recordStatus) {
        this.recordStatus = recordStatus;
    }

    /**
     * isFinished.
     *
     * @return true exactly if {@link #getRecordStatus()} is
     *         {@link io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus#FINISHED}
     */
    public boolean isFinished() {
        return CrowdsourcingStatus.FINISHED.equals(getRecordStatus());
    }

    /**
     * isInReview.
     *
     * @return true exactly if {@link #getRecordStatus()} is
     *         {@link io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus#REVIEW}
     */
    public boolean isInReview() {
        return CrowdsourcingStatus.REVIEW.equals(getRecordStatus());
    }

    /**
     * URI for a user who edited the status of this item in the crowdsourcing frontend. The actual {@link io.goobi.viewer.model.security.user.User}-Id
     * may be determined by calling {@link io.goobi.viewer.model.security.user.User#getId(URI)}
     *

     */
    public URI getCreatorURI() {
        return creatorURI;
    }

    /**
     * Setter for the field <code>creatorURI</code>.
     *
     * @param creatorURI the URI identifying the user who last edited the status of this campaign item
     */
    public void setCreatorURI(URI creatorURI) {
        this.creatorURI = creatorURI;
    }

    
    public void setLog(List<LogMessage> log) {
        this.log = log;
    }

    
    public List<LogMessage> getLog() {
        return log;
    }

    
    public Map<String, List<String>> getMetadata() {
        return Collections.unmodifiableMap(this.metadata);
    }

    
    public void setMetadata(Map<String, List<String>> metadata) {
        this.metadata = metadata;
    }

    
    public boolean isPageStatisticMode() {
        return pageStatisticMode;
    }

    
    public void setPageStatisticMode(boolean pageStatisticMode) {
        this.pageStatisticMode = pageStatisticMode;
    }

    
    public Map<Integer, String> getPageStatusMap() {
        return pageStatusMap;
    }

    
    public void setPageStatusMap(Map<Integer, String> pageStatusMap) {
        this.pageStatusMap = pageStatusMap;
    }

    
    public String getRecordIdentifier() {
        return recordIdentifier;
    }

    
    public void setRecordIdentifier(String recordIdentifier) {
        this.recordIdentifier = recordIdentifier;
    }
}
