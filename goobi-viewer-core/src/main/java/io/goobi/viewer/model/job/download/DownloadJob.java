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
package io.goobi.viewer.model.job.download;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.DownloadException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.job.JobStatus;
import jakarta.mail.MessagingException;
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

/**
 * <p>
 * Abstract DownloadJob class.
 * </p>
 */
@Entity
@Table(name = "download_jobs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@JsonInclude(Include.NON_NULL)
public abstract class DownloadJob implements Serializable {

    /** Constant <code>DATETIME_FORMAT="yyyy-MM-dd'T'HH:mm:ss'Z'"</code> */
    protected static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    /** Constant <code>TTL_FORMAT="dd'T'HH:mm:ss"</code> */
    protected static final String TTL_FORMAT = "dd'T'HH:mm:ss";

    private static final Logger logger = LogManager.getLogger(DownloadJob.class);

    private static final long serialVersionUID = -491389510147134159L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "download_job_id")
    protected Long id;

    @Column(name = "type", nullable = false)
    protected String type;

    /** Unique identifier for the download (e.g. a combination of PI and LOGID for PDFs). */
    @Column(name = "identifier", nullable = false, unique = true)
    protected String identifier;

    @Column(name = "pi", nullable = false)
    protected String pi;

    @Column(name = "logid")
    protected String logId;

    @Column(name = "message", nullable = true)
    protected String message;

    /**
     * Timestamp of the last request for this download. This can be the time of the initial request, the time of generation completion or any
     * subsequent requests. This + TTL is the time of expiration.
     */
    @Column(name = "last_requested", nullable = false)
    @JsonIgnore
    protected LocalDateTime lastRequested;

    @Column(name = "ttl", nullable = false)
    protected long ttl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    protected JobStatus status = JobStatus.UNDEFINED;

    /** Description field for stack traces, etc. */
    @Column(name = "description", columnDefinition = "LONGTEXT")
    protected String description;

    /** E-mail recipients that will be notified once the download generation is complete. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "download_job_observers", joinColumns = @JoinColumn(name = "download_job_id"))
    @Column(name = "observer")
    @JsonIgnore
    private List<String> observers = new ArrayList<>();

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
     * checkDownload.
     * </p>
     *
     * @param type For now just 'pdf'.
     * @param email Optional e-mail address to be notified.
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param downloadIdentifier Identifier has (Construct via DownloadJob.generateDownloadJobId()).
     * @param ttl Number of ms before the job expires.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static synchronized DownloadJob checkDownload(String type, final String email, String pi, String logId, String downloadIdentifier,
            long ttl)
            throws DAOException, PresentationException, IndexUnreachableException {
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }
        if (downloadIdentifier == null) {
            throw new IllegalArgumentException("downloadIdentifier may not be null");
        }
        String controlIdentifier = DownloadJob.generateDownloadJobId(type, pi, logId);
        if (!controlIdentifier.equals(downloadIdentifier)) {
            throw new IllegalArgumentException("wrong downloadIdentifier");
        }

        logger.debug("Checking download of job {}", controlIdentifier);

        try {
            /*Get or create job*/
            boolean newJob = false;
            DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(downloadIdentifier);
            if (downloadJob == null) {
                logger.debug("Create new download job");
                newJob = true;
                switch (type) {
                    case PDFDownloadJob.LOCAL_TYPE:
                        downloadJob = new PDFDownloadJob(pi, logId, LocalDateTime.now(), ttl);
                        break;
                    case EPUBDownloadJob.LOCAL_TYPE:
                        downloadJob = new EPUBDownloadJob(pi, logId, LocalDateTime.now(), ttl);
                        break;
                    default:
                        throw new IllegalArgumentException("Uknown type: " + type);
                }
            } else {
                // Update latest request timestamp of an existing job
                logger.debug("Retrieve existing job");
                downloadJob.setLastRequested(LocalDateTime.now());
                downloadJob.updateStatus();
            }

            /*set observer email*/
            String useEmail = null;
            if (StringUtils.isNotBlank(email)) {
                useEmail = email.trim().toLowerCase();
            }
            if (StringUtils.isNotBlank(useEmail)) {
                downloadJob.getObservers().add(useEmail);
            }
            if (downloadJob.status.equals(JobStatus.WAITING)) {
                //keep waiting
            } else if (downloadJob.getFile() != null && downloadJob.getFile().toFile().exists()) {
                //not waiting and file exists -> file has been created
                downloadJob.setStatus(JobStatus.READY);
            } else {
                //not waiting but file doesn't exist -> trigger creation
                logger.debug("Triggering {} creation", downloadJob.getType());
                try {
                    downloadJob.triggerCreation();
                    downloadJob.setStatus(JobStatus.WAITING);
                } catch (DownloadException e) {
                    downloadJob.setStatus(JobStatus.ERROR);
                    downloadJob.setMessage(e.getMessage());
                }
            }

            /*Add or update job in database*/
            boolean updated = false;
            if (newJob) {
                DataManager.getInstance().getDao().addDownloadJob(downloadJob);
            }
            updated = DataManager.getInstance().getDao().updateDownloadJob(downloadJob);
            if (updated) {
                return downloadJob;
            }
            return null;
        } finally {
            // Clean up expired jobs AFTER updating the one in use
            DownloadJobTools.cleanupExpiredDownloads();
        }
    }

    /**
     * <p>
     * triggerCreation.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    protected abstract void triggerCreation() throws PresentationException, IndexUnreachableException;

    /**
     * <p>
     * ocrFolderExists.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static boolean ocrFolderExists(String pi) throws PresentationException, IndexUnreachableException {
        Path abbyyFolder = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getAbbyyFolder());
        Path altoFolder = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getAltoFolder());
        return Files.isDirectory(abbyyFolder) || Files.isDirectory(altoFolder);
    }

    /**
     * <p>
     * isExpired.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isExpired() {
        if (lastRequested == null) {
            return false;
        }

        return System.currentTimeMillis() > DateTools.getMillisFromLocalDateTime(lastRequested, false) + ttl;
    }

    /**
     * Deletes the file associated with this job.
     * 
     * @return true if file successfully deleted; false otherwise
     */
    public boolean deleteFile() {
        Path path = DownloadJobTools.getDownloadFileStatic(getIdentifier(), getType(), getFileExtension()).toPath();
        if (Files.isRegularFile(path)) {
            try {
                Files.delete(path);
                return true;
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        return false;
    }

    /**
     * <p>
     * getMimeType.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    @JsonIgnore
    public abstract String getMimeType();

    /**
     * <p>
     * getFileExtension.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    @JsonIgnore
    public abstract String getFileExtension();

    /**
     * <p>
     * getDisplayName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    @JsonIgnore
    public abstract String getDisplayName();

    /**
     * <p>
     * getQueuePosition.
     * </p>
     *
     * @return a int.
     */
    public abstract int getQueuePosition();

    /**
     * <p>
     * getFile.
     * </p>
     *
     * @return a {@link java.nio.file.Path} object.
     */
    @JsonIgnore
    public Path getFile() {
        Path path = DownloadJobTools.getDownloadFileStatic(identifier, type, getFileExtension()).toPath();
        logger.trace(path.toString());
        if (Files.isRegularFile(path)) {
            return path;
        }

        return null;
    }

    /**
     * <p>
     * notifyObservers.
     * </p>
     *
     * @param status a {@link io.goobi.viewer.model.job.download.DownloadJob.JobStatus} object.
     * @param message a {@link java.lang.String} object.
     * @return a boolean.
     * @throws java.io.UnsupportedEncodingException if any.
     * @throws javax.mail.MessagingException if any.
     */
    public boolean notifyObservers(JobStatus status, String message) throws UnsupportedEncodingException, MessagingException {
        if (observers == null || observers.isEmpty()) {
            return false;
        }
        String subject = "Unknown status";
        String body = "";
        switch (status) {
            case READY:
                subject = ViewerResourceBundle.getTranslation("downloadReadySubject", null);
                body = ViewerResourceBundle.getTranslation("downloadReadyBody", null);
                if (body != null) {
                    body = body.replace("{0}", pi);
                    body = body.replace("{1}", DataManager.getInstance().getConfiguration().getDownloadUrl() + identifier + "/");
                    body = body.replace("{4}", getType().toUpperCase());
                    LocalDateTime exirationDate = lastRequested;
                    exirationDate = exirationDate.plus(ttl, ChronoUnit.MILLIS);
                    body = body.replace("{2}", DateTools.format(exirationDate, DateTools.FORMATTERISO8601DATE, false));
                    body = body.replace("{3}", DateTools.format(exirationDate, DateTools.FORMATTERISO8601DATE, false));
                }
                break;
            case ERROR:
                subject = ViewerResourceBundle.getTranslation("downloadErrorSubject", null);
                body = ViewerResourceBundle.getTranslation("downloadErrorBody", null);
                if (body != null) {
                    body = body.replace("{0}", pi);
                    body = body.replace("{1}", DataManager.getInstance().getConfiguration().getDefaultFeedbackEmailAddress());
                    body = body.replace("{2}", getType().toUpperCase());
                }
                break;
            default:
                break;
        }
        if (subject != null) {
            subject = subject.replace("{0}", pi);
        }

        return NetTools.postMail(observers, null, null, subject, body);
    }

    /**
     * <p>
     * getDownloadFile.
     * </p>
     *
     * @param pi The pi of the work to download.
     * @param logId the logId of the structure element to download. Is ignored if it is null, empty, blank or equals "-"
     * @param type either "pdf" or "epub"
     * @return The Download location file, ending with ".pdf" or ".epub" depending on type
     * @throws java.lang.IllegalArgumentException If the pi is null, empty or blank, or if the type is not "epub" or "pdf"
     */
    public File getDownloadFile(String pi, final String logId, String type) {
        if (StringUtils.isBlank(pi)) {
            throw new IllegalArgumentException("Cannot determine download path for empty pi");
        }
        String useLogId = logId;
        if (StringUtils.isBlank(useLogId) || "-".equals(useLogId)) {
            useLogId = "";
        }
        String hash = DownloadJob.generateDownloadJobId(type, pi, useLogId);
        return DownloadJobTools.getDownloadFileStatic(hash, type, getFileExtension());
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
     * Getter for the field <code>type</code>.
     * </p>
     *
     * @return the type
     */
    public String getType() {
        return type;
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
     * Getter for the field <code>logId</code>.
     * </p>
     *
     * @return the logId
     */
    public String getLogId() {
        return logId;
    }

    /**
     * <p>
     * Setter for the field <code>logId</code>.
     * </p>
     *
     * @param logId the logId to set
     */
    public void setLogId(String logId) {
        this.logId = logId;
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
     * Getter for the field <code>lastRequested</code>.
     * </p>
     *
     * @return the lastRequested
     */
    @JsonFormat(pattern = DATETIME_FORMAT)
    public LocalDateTime getLastRequested() {
        return lastRequested;
    }

    /**
     * <p>
     * Setter for the field <code>lastRequested</code>.
     * </p>
     *
     * @param lastRequested the lastRequested to set
     */
    public void setLastRequested(LocalDateTime lastRequested) {
        this.lastRequested = lastRequested;
    }

    /**
     * <p>
     * Getter for the field <code>ttl</code>.
     * </p>
     *
     * @return the ttl
     */
    @JsonIgnore
    public long getTtl() {
        return ttl;
    }

    /**
     * <p>
     * getTimeToLive.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTimeToLive() {
        Duration d = Duration.ofMillis(ttl);
        return String.format("%dd %d:%02d:%02d", d.toDays(), d.toHours() % 24, d.toMinutes() % 60, d.getSeconds() % 60);
    }

    /**
     * <p>
     * Setter for the field <code>ttl</code>.
     * </p>
     *
     * @param ttl the ttl to set
     */
    public void setTtl(long ttl) {
        this.ttl = ttl;
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
    @JsonIgnore
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

    /**
     * Empties the complete observer list. Should be used after observers have been notified to avoid repeat notifications
     */
    public void resetObservers() {
        this.observers = new ArrayList<>();
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            logger.error("Error getting response from TaskManager", e);
            return "";
        }
    }

    /**
     * @return {@link String}
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
            JobStatus s = JobStatus.getByName(statusString);
            setStatus(s);
            if (JobStatus.ERROR.equals(s)) {
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
        sb.append("DownloadJob ").append(getIdentifier()).append("; ");
        sb.append("Type ").append(getType()).append("; ");
        sb.append("Status ").append(getStatus()).append("; ");
        sb.append("Expired: ").append(isExpired()).append("; ");
        sb.append("PI ").append(getPi()).append("; ");
        sb.append("LOGID ").append(getLogId()).append("; ");
        return sb.toString();
    }
}
