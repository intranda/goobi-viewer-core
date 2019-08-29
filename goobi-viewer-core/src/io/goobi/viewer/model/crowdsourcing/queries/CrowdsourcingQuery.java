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
package io.goobi.viewer.model.crowdsourcing.queries;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.api.serializer.MetadataSerializer;
import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;

/**
 * @author florian
 *
 */
@JsonInclude(Include.NON_NULL)
@Table(name = "cs_queries")
public class CrowdsourcingQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "query_id")
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "owner_campaign_id")
    private Campaign ownerCampaign;
    
    // TODO find a way to persist multi-language values in DB
    private IMetadataValue label;
    private IMetadataValue description;
    private IMetadataValue help;
    private QueryType queryType;
    private TargetFrequency targetFrequency;
    private TargetSelector targetSelector;
    
    public CrowdsourcingQuery() {
        
    }
    
    public CrowdsourcingQuery(QueryType queryType, TargetFrequency targetFrequency, TargetSelector targetSelector) {
        this.queryType = queryType;
        this.targetFrequency = targetFrequency;
        this.targetSelector = targetSelector;
    }
    
    /**
     * @return the label
     */
    @JsonSerialize(using = MetadataSerializer.class)
    public IMetadataValue getLabel() {
        return label;
    }
    /**
     * @param label the label to set
     */
    public void setLabel(IMetadataValue label) {
        this.label = label;
    }
    /**
     * @return the description
     */
    @JsonSerialize(using = MetadataSerializer.class)
    public IMetadataValue getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription(IMetadataValue description) {
        this.description = description;
    }
    /**
     * @return the help
     */
    @JsonSerialize(using = MetadataSerializer.class)
    public IMetadataValue getHelp() {
        return help;
    }
    /**
     * @param help the help to set
     */
    public void setHelp(IMetadataValue help) {
        this.help = help;
    }
    /**
     * @return the queryType
     */
    public QueryType getQueryType() {
        return queryType;
    }
    /**
     * @param queryType the queryType to set
     */
    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }
    /**
     * @return the targetType
     */
    public TargetFrequency getTargetFrequency() {
        return targetFrequency;
    }
    /**
     * @param targetType the targetType to set
     */
    public void setTargetFrequency(TargetFrequency targetFrequency) {
        this.targetFrequency = targetFrequency;
    }
    
    /**
     * @return the targetSelector
     */
    public TargetSelector getTargetSelector() {
        return targetSelector;
    }
    
    /**
     * @param targetSelector the targetSelector to set
     */
    public void setTargetSelector(TargetSelector targetSelector) {
        this.targetSelector = targetSelector;
    }
    
    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }
    
    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    
}
