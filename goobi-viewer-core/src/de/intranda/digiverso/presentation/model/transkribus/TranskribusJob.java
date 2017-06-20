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
package de.intranda.digiverso.presentation.model.transkribus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "transkribus_jobs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public class TranskribusJob implements Serializable {

    private static final long serialVersionUID = 2399740912703228096L;

    public enum JobStatus {
        WAITING,
        READY,
        ERROR;

        public static JobStatus getByName(String name) {
            if (name != null) {
                switch (name) {
                    case "WAITING":
                        return WAITING;
                    case "READY":
                        return READY;
                    case "ERROR":
                        return ERROR;
                }
            }

            return null;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = false)
    private Date dateCreated;

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

    /**
     * @return the jobId
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @param jobId the jobId to set
     */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /**
     * @return the docId
     */
    public String getDocId() {
        return docId;
    }

    /**
     * @param docId the docId to set
     */
    public void setDocId(String docId) {
        this.docId = docId;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @return the ownerId
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * @param ownerId the ownerId to set
     */
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * @return the userCollectionId
     */
    public String getUserCollectionId() {
        return userCollectionId;
    }

    /**
     * @param userCollectionId the userCollectionId to set
     */
    public void setUserCollectionId(String userCollectionId) {
        this.userCollectionId = userCollectionId;
    }

    /**
     * @return the viewerCollectionId
     */
    public String getViewerCollectionId() {
        return viewerCollectionId;
    }

    /**
     * @param viewerCollectionId the viewerCollectionId to set
     */
    public void setViewerCollectionId(String viewerCollectionId) {
        this.viewerCollectionId = viewerCollectionId;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the status
     */
    public JobStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the observers
     */
    public List<String> getObservers() {
        return observers;
    }

    /**
     * @param observers the observers to set
     */
    public void setObservers(List<String> observers) {
        this.observers = observers;
    }
}
