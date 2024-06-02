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
 * @author florian
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
     * @return the source
     */
    public URI getSource() {
        return source;
    }

    /**
     * <p>
     * Setter for the field <code>source</code>.
     * </p>
     *
     * @param source the source to set
     */
    public void setSource(URI source) {
        this.source = source;
    }

    /**
     * The {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} to create the annotations
     *
     * @return the campaign
     */
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * <p>
     * Setter for the field <code>campaign</code>.
     * </p>
     *
     * @param campaign the campaign to set
     */
    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    /**
     * <p>
     * getQuestions.
     * </p>
     *
     * @return a new list containing all queries
     */
    @JsonIgnore
    public List<Question> getQuestions() {
        return new ArrayList<>(campaign.getQuestions());
    }

    /**
     * The {@link CrowdsourcingStatus status} of the resource within the {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign}
     *
     * @return the recordStatus
     */
    public CrowdsourcingStatus getRecordStatus() {
        return recordStatus;
    }

    /**
     * <p>
     * Setter for the field <code>recordStatus</code>.
     * </p>
     *
     * @param recordStatus the recordStatus to set
     */
    public void setRecordStatus(CrowdsourcingStatus recordStatus) {
        this.recordStatus = recordStatus;
    }

    /**
     * <p>
     * isFinished.
     * </p>
     *
     * @return true exactly if {@link #getRecordStatus()} is
     *         {@link io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus#FINISHED}
     */
    public boolean isFinished() {
        return CrowdsourcingStatus.FINISHED.equals(getRecordStatus());
    }

    /**
     * <p>
     * isInReview.
     * </p>
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
     * @return the creatorURI
     */
    public URI getCreatorURI() {
        return creatorURI;
    }

    /**
     * <p>
     * Setter for the field <code>creatorURI</code>.
     * </p>
     *
     * @param creatorURI the creatorURI to set
     */
    public void setCreatorURI(URI creatorURI) {
        this.creatorURI = creatorURI;
    }

    /**
     * @param log the log to set
     */
    public void setLog(List<LogMessage> log) {
        this.log = log;
    }

    /**
     * @return the log
     */
    public List<LogMessage> getLog() {
        return log;
    }

    /**
     * @return the metadata
     */
    public Map<String, List<String>> getMetadata() {
        return Collections.unmodifiableMap(this.metadata);
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(Map<String, List<String>> metadata) {
        this.metadata = metadata;
    }

    /**
     * @return the pageStatisticMode
     */
    public boolean isPageStatisticMode() {
        return pageStatisticMode;
    }

    /**
     * @param pageStatisticMode the pageStatisticMode to set
     */
    public void setPageStatisticMode(boolean pageStatisticMode) {
        this.pageStatisticMode = pageStatisticMode;
    }

    /**
     * @return the pageStatusMap
     */
    public Map<Integer, String> getPageStatusMap() {
        return pageStatusMap;
    }

    /**
     * @param pageStatusMap the pageStatusMap to set
     */
    public void setPageStatusMap(Map<Integer, String> pageStatusMap) {
        this.pageStatusMap = pageStatusMap;
    }

    /**
     * @return the recordIdentifier
     */
    public String getRecordIdentifier() {
        return recordIdentifier;
    }

    /**
     * @param recordIdentifier the recordIdentifier to set
     */
    public void setRecordIdentifier(String recordIdentifier) {
        this.recordIdentifier = recordIdentifier;
    }
}
