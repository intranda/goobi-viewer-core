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

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((pi == null) ? 0 : pi.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>dateCreated</code>.
     * </p>
     *
     * @return the dateCreated
     */
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * <p>
     * Setter for the field <code>dateCreated</code>.
     * </p>
     *
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * <p>
     * Getter for the field <code>dateUpdated</code>.
     * </p>
     *
     * @return the dateUpdated
     */
    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * <p>
     * Setter for the field <code>dateUpdated</code>.
     * </p>
     *
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * <p>
     * Getter for the field <code>owner</code>.
     * </p>
     *
     * @return the owner
     */
    public CampaignRecordStatistic getOwner() {
        return owner;
    }

    /**
     * <p>
     * Setter for the field <code>owner</code>.
     * </p>
     *
     * @param owner the owner to set
     */
    public void setOwner(CampaignRecordStatistic owner) {
        this.owner = owner;
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * <p>
     * Setter for the field <code>pi</code>.
     * </p>
     *
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @return the page
     */
    public Integer getPage() {
        return page;
    }

    /**
     * @param page the page to set
     */
    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * <p>
     * Getter for the field <code>status</code>.
     * </p>
     *
     * @return the status
     */
    public CrowdsourcingStatus getStatus() {
        return status;
    }

    /**
     * <p>
     * Setter for the field <code>status</code>.
     * </p>
     *
     * @param status the status to set
     */
    public void setStatus(CrowdsourcingStatus status) {
        this.status = status;
    }

    /**
     * <p>
     * Getter for the field <code>annotators</code>.
     * </p>
     *
     * @return the annotators
     */
    public List<User> getAnnotators() {
        return annotators;
    }

    /**
     * <p>
     * Setter for the field <code>annotators</code>.
     * </p>
     *
     * @param annotators the annotators to set
     */
    public void setAnnotators(List<User> annotators) {
        this.annotators = annotators;
    }

    /**
     * <p>
     * Getter for the field <code>reviewers</code>.
     * </p>
     *
     * @return the reviewers
     */
    public List<User> getReviewers() {
        return reviewers;
    }

    /**
     * <p>
     * Setter for the field <code>reviewers</code>.
     * </p>
     *
     * @param reviewers the reviewers to set
     */
    public void setReviewers(List<User> reviewers) {
        this.reviewers = reviewers;
    }

    /**
     * <p>
     * addAnnotater.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     */
    public void addAnnotater(User user) {
        if (user != null && !getAnnotators().contains(user)) {
            getAnnotators().add(user);
        }
    }

    /**
     * <p>
     * addReviewer.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     */
    public void addReviewer(User user) {
        if (user != null && !getReviewers().contains(user)) {
            getReviewers().add(user);
        }
    }
}
