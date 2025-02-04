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
package io.goobi.viewer.model.transkribus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import io.goobi.viewer.model.job.JobStatus;

/**
 * <p>
 * TranskribusJob class.
 * </p>
 */
@Entity
@Table(name = "transkribus_jobs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public class TranskribusJob implements Serializable {

    private static final long serialVersionUID = 2399740912703228096L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "job_id")
    private Long id;

    /** Unique identifier for the job.. */
    @Column(name = "jobId", nullable = false)
    private String jobId;

    /** Unique identifier for the job.. */
    @Column(name = "docId")
    private String docId;

    @Column(name = "pi", nullable = false)
    private String pi;

    /** Transkribus ID of the job owner. */
    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    /** Transkribus collection (user) */
    @Column(name = "user_collection_id", nullable = false)
    private String userCollectionId;

    /** Transkribus collection (viewer) */
    @Column(name = "viewer_collection_id", nullable = false)
    private String viewerCollectionId;

    @Column(name = "message", nullable = true)
    private String message;

    /**
     * Timestamp of the last request for this download. This can be the time of the initial request, the time of generation completion or any
     * subsequent requests. This + TTL is the time of expiration.
     */
    //    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    /** Description field for stack traces, etc. */
    @Column(name = "description", columnDefinition = "LONGTEXT")
    protected String description;

    /** E-mail recipients that will be notified once the download generation is complete. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "transkribus_job_observers", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "observer")
    protected List<String> observers = new ArrayList<>();

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
     * Getter for the field <code>jobId</code>.
     * </p>
     *
     * @return the jobId
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * <p>
     * Setter for the field <code>jobId</code>.
     * </p>
     *
     * @param jobId the jobId to set
     */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /**
     * <p>
     * Getter for the field <code>docId</code>.
     * </p>
     *
     * @return the docId
     */
    public String getDocId() {
        return docId;
    }

    /**
     * <p>
     * Setter for the field <code>docId</code>.
     * </p>
     *
     * @param docId the docId to set
     */
    public void setDocId(String docId) {
        this.docId = docId;
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
     * <p>
     * Getter for the field <code>ownerId</code>.
     * </p>
     *
     * @return the ownerId
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * <p>
     * Setter for the field <code>ownerId</code>.
     * </p>
     *
     * @param ownerId the ownerId to set
     */
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * <p>
     * Getter for the field <code>userCollectionId</code>.
     * </p>
     *
     * @return the userCollectionId
     */
    public String getUserCollectionId() {
        return userCollectionId;
    }

    /**
     * <p>
     * Setter for the field <code>userCollectionId</code>.
     * </p>
     *
     * @param userCollectionId the userCollectionId to set
     */
    public void setUserCollectionId(String userCollectionId) {
        this.userCollectionId = userCollectionId;
    }

    /**
     * <p>
     * Getter for the field <code>viewerCollectionId</code>.
     * </p>
     *
     * @return the viewerCollectionId
     */
    public String getViewerCollectionId() {
        return viewerCollectionId;
    }

    /**
     * <p>
     * Setter for the field <code>viewerCollectionId</code>.
     * </p>
     *
     * @param viewerCollectionId the viewerCollectionId to set
     */
    public void setViewerCollectionId(String viewerCollectionId) {
        this.viewerCollectionId = viewerCollectionId;
    }

    /**
     * <p>
     * Getter for the field <code>message</code>.
     * </p>
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * <p>
     * Setter for the field <code>message</code>.
     * </p>
     *
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
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
     * Getter for the field <code>status</code>.
     * </p>
     *
     * @return the status
     */
    public JobStatus getStatus() {
        return status;
    }

    /**
     * <p>
     * Setter for the field <code>status</code>.
     * </p>
     *
     * @param status the status to set
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * <p>
     * Getter for the field <code>description</code>.
     * </p>
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * <p>
     * Setter for the field <code>description</code>.
     * </p>
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * <p>
     * Getter for the field <code>observers</code>.
     * </p>
     *
     * @return the observers
     */
    public List<String> getObservers() {
        return observers;
    }

    /**
     * <p>
     * Setter for the field <code>observers</code>.
     * </p>
     *
     * @param observers the observers to set
     */
    public void setObservers(List<String> observers) {
        this.observers = observers;
    }
}
