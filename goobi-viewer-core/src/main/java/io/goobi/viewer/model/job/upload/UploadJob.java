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
package io.goobi.viewer.model.job.upload;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.download.AbstractTaskManagerRequest;

/**
 * <p>
 * Abstract DownloadJob class.
 * </p>
 */
@Entity
@Table(name = "upload_jobs")
@JsonInclude(Include.NON_NULL)
public abstract class UploadJob implements Serializable {

    private static final long serialVersionUID = 2732786560804670250L;

    private static final Logger logger = LoggerFactory.getLogger(UploadJob.class);

    /** Constant <code>DATETIME_FORMAT="yyyy-MM-dd'T'HH:mm:ss'Z'"</code> */
    protected static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    /** Constant <code>TTL_FORMAT="dd'T'HH:mm:ss"</code> */
    protected static final String TTL_FORMAT = "dd'T'HH:mm:ss";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "upload_job_id")
    private Long id;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    protected JobStatus status = JobStatus.UNDEFINED;

    /** Unique identifier for the download (e.g. a combination of PI and LOGID for PDFs). */
    @Column(name = "identifier", nullable = false, unique = true)
    protected String identifier;

    @Column(name = "pi", nullable = false)
    protected String pi;

    /** Title field. */
    @Column(name = "title", columnDefinition = "LONGTEXT")
    protected String title;

    /** Description field. */
    @Column(name = "description", columnDefinition = "LONGTEXT")
    protected String description;

    @Column(name = "message", nullable = true)
    protected String message;
    
    public UploadJob() {
        
    }

    /**
     * <p>
     * generateDownloadIdentifier.
     * </p>
     */
    public abstract void generateDownloadIdentifier();

    /**
     * <p>
     * generateDownloadJobId.
     * </p>
     *
     * @param criteria a {@link java.lang.String} object.
     * @should generate same id from same criteria
     * @return a {@link java.lang.String} object.
     */
    public static String generateDownloadJobId(String... criteria) {
        StringBuilder sbCriteria = new StringBuilder(criteria.length * 10);
        for (String criterion : criteria) {
            if (criterion != null) {
                sbCriteria.append(criterion);
            }
        }

        return StringTools.generateHash(sbCriteria.toString());
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
     * @return the dateCreated
     */
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the creatorId
     */
    public Long getCreatorId() {
        return creatorId;
    }

    /**
     * @param creatorId the creatorId to set
     */
    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
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
     * Getter for the field <code>identifier</code>.
     * </p>
     *
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * <p>
     * Setter for the field <code>identifier</code>.
     * </p>
     *
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * <p>
     * Getter for the field <code>status</code>.
     * </p>
     *
     * @return the status
     */
    public JobStatus getStatus() {
        if (status == null) {
            status = JobStatus.UNDEFINED;
        }
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
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
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

    public static Response postJobRequest(String url, AbstractTaskManagerRequest body) throws IOException {
        try {
            Client client = ClientBuilder.newClient();
            client.property(ClientProperties.CONNECT_TIMEOUT, 12000);
            client.property(ClientProperties.READ_TIMEOUT, 30000);
            return client
                    .target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(javax.ws.rs.client.Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Throwable e) {
            throw new IOException("Error connecting to " + url, e);
        }
    }

    /**
     * <p>
     * getJobStatus.
     * </p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getJobStatus(String identifier) {
        StringBuilder url = new StringBuilder();
        url.append(DataManager.getInstance().getConfiguration().getTaskManagerRestUrl());
        url.append(getRestApiPath()).append("/info/");
        url.append(identifier);
        ResponseHandler<String> handler = new BasicResponseHandler();
        HttpGet httpGet = new HttpGet(url.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            CloseableHttpResponse response = httpclient.execute(httpGet);
            String ret = handler.handleResponse(response);
            logger.trace("TaskManager response: {}", ret);
            return ret;
        } catch (Throwable e) {
            logger.error("Error getting response from TaskManager", e);
            return "";
        }
    }

    /**
     * @return
     */
    protected abstract String getRestApiPath();

    /**
     * <p>
     * updateStatus.
     * </p>
     */
    public void updateStatus() {
        String ret = getJobStatus(identifier);
        try {
            JSONObject object = new JSONObject(ret);
            String statusString = object.getString("status");
            JobStatus status = JobStatus.getByName(statusString);
            setStatus(status);
            if (JobStatus.ERROR.equals(status)) {
                String errorMessage = object.getString("errorMessage");
                setMessage(errorMessage);
            }
        } catch (JSONException e) {
            setStatus(JobStatus.ERROR);
            setMessage("Unable to parse TaskManager response");
        }

    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UploadJob ").append(getIdentifier()).append("; ");
        sb.append("Status ").append(getStatus()).append("; ");
        sb.append("PI ").append(getPi()).append("; ");
        return sb.toString();
    }
}
