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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.apache.solr.common.SolrDocument;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Annotation status of a record in the context of a particular campaign.
 */
@Entity
@Table(name = "cs_campaign_record_statistics")
@JsonInclude(Include.NON_EMPTY)
public class CampaignRecordStatistic implements Serializable {

    private static final Logger logger = LogManager.getLogger(CampaignRecordStatistic.class);

    private static final long serialVersionUID = 8902904205183851565L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_record_statistic_id")
    private Long id;

    @Column(name = "date_created", nullable = false)
    @JsonIgnore
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    @JsonIgnore
    private LocalDateTime dateUpdated;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private Campaign owner;

    @Column(name = "pi", nullable = false)
    private String pi;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JsonIgnore
    private CrowdsourcingStatus status;

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    @MapKeyColumn(name = "pi_page_key", insertable = false, updatable = false)
    @JsonIgnore
    private Map<String, CampaignRecordPageStatistic> pageStatistics = new HashMap<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cs_campaign_record_statistic_annotators", joinColumns = @JoinColumn(name = "campaign_record_statistic_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> annotators = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cs_campaign_record_statistic_reviewers", joinColumns = @JoinColumn(name = "campaign_record_statistic_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> reviewers = new ArrayList<>();

    @Column(name = "total_pages", nullable = true)
    private Integer totalPages = null;

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((pi == null) ? 0 : pi.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CampaignRecordStatistic other = (CampaignRecordStatistic) obj;
        if (owner == null) {
            if (other.owner != null) {
                return false;
            }
        } else if (!owner.equals(other.owner)) {
            return false;
        }
        if (pi == null) {
            if (other.pi != null) {
                return false;
            }
        } else if (!pi.equals(other.pi)) {
            return false;
        }
        return true;
    }

    /**
     * Getter for the field <code>id</code>.
     *

     */
    public Long getId() {
        return id;
    }

    /**
     * Setter for the field <code>id</code>.
     *
     * @param id the database primary key to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Getter for the field <code>dateCreated</code>.
     *

     */
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * Setter for the field <code>dateCreated</code>.
     *
     * @param dateCreated the date and time when this record statistic entry was created
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * Getter for the field <code>dateUpdated</code>.
     *

     */
    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * Setter for the field <code>dateUpdated</code>.
     *
     * @param dateUpdated the date and time when this record statistic entry was last updated
     */
    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * Getter for the field <code>owner</code>.
     *

     */
    public Campaign getOwner() {
        return owner;
    }

    /**
     * Setter for the field <code>owner</code>.
     *
     * @param owner the campaign this record statistic belongs to
     */
    public void setOwner(Campaign owner) {
        this.owner = owner;
    }

    /**
     * Getter for the field <code>pi</code>.
     *

     */
    public String getPi() {
        return pi;
    }

    /**
     * Setter for the field <code>pi</code>.
     *
     * @param pi the persistent identifier of the record this statistic tracks
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    
    public Map<String, CampaignRecordPageStatistic> getPageStatistics() {
        return pageStatistics;
    }

    
    public void setPageStatistics(Map<String, CampaignRecordPageStatistic> pageStatistics) {
        this.pageStatistics = pageStatistics;
    }

    /**
     * Getter for the field <code>status</code>.
     *

     */
    public CrowdsourcingStatus getStatus() {
        return status;
    }

    /**
     * Setter for the field <code>status</code>.
     *
     * @param status the crowdsourcing processing status for this record within the campaign
     */
    public void setStatus(CrowdsourcingStatus status) {
        this.status = status;
    }

    /**
     * Getter for the field <code>annotators</code>.
     *

     */
    public List<User> getAnnotators() {
        return annotators;
    }

    /**
     * Setter for the field <code>annotators</code>.
     *
     * @param annotators the list of users who have contributed annotations for this record
     */
    public void setAnnotators(List<User> annotators) {
        this.annotators = annotators;
    }

    /**
     * Getter for the field <code>reviewers</code>.
     *

     */
    public List<User> getReviewers() {
        return reviewers;
    }

    /**
     * Setter for the field <code>reviewers</code>.
     *
     * @param reviewers the list of users who have reviewed annotations for this record
     */
    public void setReviewers(List<User> reviewers) {
        this.reviewers = reviewers;
    }

    /**
     * addAnnotater.
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     */
    public void addAnnotater(User user) {
        if (user != null && !getAnnotators().contains(user)) {
            getAnnotators().add(user);
        }
    }

    /**
     * addReviewer.
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     */
    public void addReviewer(User user) {
        if (user != null && !getReviewers().contains(user)) {
            getReviewers().add(user);
        }
    }

    /**
     * Checks both record status and all page status to check if any matches the given status.
     *
     * @param status crowdsourcing status to check for
     * @return false if status is null, otherwise true exactly if {@link #getStatus()} equals status or if any
     *         {@link CampaignRecordPageStatistic#getStatus()} of {@link #pageStatistics} returns true
     */
    public boolean containsPageStatus(CrowdsourcingStatus status) {
        if (status == null) {
            return false;
        } else if (CrowdsourcingStatus.ANNOTATE.equals(status) && this.pageStatistics.size() < this.getTotalPages()) {
            //if not all pages have a pageStatstic, assume the others are in annotation status, so return true
            return true;
        } else {
            return this.pageStatistics.values().stream().anyMatch(pageStatistic -> status.equals(pageStatistic.getStatus()));
        }
    }

    
    public Integer getTotalPages() {
        if (totalPages == null) {
            this.totalPages = calculateTotalPages();
        }
        return totalPages;
    }

    
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * Retrieves the total number of pages for this record from the Solr index.
     *
     * @return the number of pages, 0 if no document was found, or null if an index error occurred
     */
    private Integer calculateTotalPages() {
        String query = String.format("PI:\"%s\"", pi);
        try {
            SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, Collections.singletonList(SolrConstants.NUMPAGES));
            if (doc != null && doc.containsKey(SolrConstants.NUMPAGES)) {
                return (Integer) doc.getFieldValue(SolrConstants.NUMPAGES);
            }
            return 0;
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error("Error retrieving page cound for query: {}", query);
            return null;
        }

    }

}
