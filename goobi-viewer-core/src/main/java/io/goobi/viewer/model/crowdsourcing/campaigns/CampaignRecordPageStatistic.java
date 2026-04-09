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
import java.util.List;

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
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.goobi.viewer.model.security.user.User;

/**
 * Annotation status of a single page in a record in the context of a particular campaign.
 */
@Entity
@Table(name = "cs_campaign_record_page_statistics")
@JsonInclude(Include.NON_EMPTY)
public class CampaignRecordPageStatistic implements Serializable {

    private static final long serialVersionUID = -5449329014162706484L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_record_page_statistic_id")
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
    private CampaignRecordStatistic owner;

    @Column(name = "pi", nullable = false)
    private String pi;

    @Column(name = "page", nullable = false)
    private Integer page;

    /** Key composed of pi and page values. */
    @Column(name = "pi_page_key", nullable = false)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JsonIgnore
    private CrowdsourcingStatus status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cs_campaign_record_page_statistic_annotators", joinColumns = @JoinColumn(name = "campaign_record_page_statistic_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> annotators = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cs_campaign_record_page_statistic_reviewers", joinColumns = @JoinColumn(name = "campaign_record_page_statistic_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> reviewers = new ArrayList<>();

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
        CampaignRecordPageStatistic other = (CampaignRecordPageStatistic) obj;
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
     * @return the database primary key of this page statistic entry
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
     * @return the date and time when this statistic entry was created
     */
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * Setter for the field <code>dateCreated</code>.
     *
     * @param dateCreated the date and time when this statistic entry was created
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * Getter for the field <code>dateUpdated</code>.
     *
     * @return the date and time when this statistic entry was last updated
     */
    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * Setter for the field <code>dateUpdated</code>.
     *
     * @param dateUpdated the date and time when this statistic entry was last updated
     */
    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * Getter for the field <code>owner</code>.
     *
     * @return the campaign record statistic that owns this per-page statistic entry
     */
    public CampaignRecordStatistic getOwner() {
        return owner;
    }

    /**
     * Setter for the field <code>owner</code>.
     *
     * @param owner the campaign record statistic that owns this per-page statistic entry
     */
    public void setOwner(CampaignRecordStatistic owner) {
        this.owner = owner;
    }

    /**
     * Getter for the field <code>pi</code>.
     *
     * @return the persistent identifier of the record this page statistic belongs to
     */
    public String getPi() {
        return pi;
    }

    /**
     * Setter for the field <code>pi</code>.
     *
     * @param pi the persistent identifier of the record this page statistic belongs to
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    
    public Integer getPage() {
        return page;
    }

    
    public void setPage(Integer page) {
        this.page = page;
    }

    
    public String getKey() {
        return key;
    }

    
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Getter for the field <code>status</code>.
     *
     * @return the crowdsourcing processing status for this page within the campaign
     */
    public CrowdsourcingStatus getStatus() {
        return status;
    }

    /**
     * Setter for the field <code>status</code>.
     *
     * @param status the crowdsourcing processing status for this page within the campaign
     */
    public void setStatus(CrowdsourcingStatus status) {
        this.status = status;
    }

    /**
     * Getter for the field <code>annotators</code>.
     *
     * @return the list of users who have contributed annotations to this page
     */
    public List<User> getAnnotators() {
        return annotators;
    }

    /**
     * Setter for the field <code>annotators</code>.
     *
     * @param annotators the list of users who have contributed annotations to this page
     */
    public void setAnnotators(List<User> annotators) {
        this.annotators = annotators;
    }

    /**
     * Getter for the field <code>reviewers</code>.
     *
     * @return the list of users who have reviewed annotations on this page
     */
    public List<User> getReviewers() {
        return reviewers;
    }

    /**
     * Setter for the field <code>reviewers</code>.
     *
     * @param reviewers the list of users who have reviewed annotations on this page
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
}
