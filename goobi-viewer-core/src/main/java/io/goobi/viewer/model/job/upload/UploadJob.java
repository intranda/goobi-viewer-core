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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.jersey.client.ClientProperties;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.UploadException;
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
public class UploadJob implements Serializable {

    private static final long serialVersionUID = 2732786560804670250L;

    private static final Logger logger = LoggerFactory.getLogger(UploadJob.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "upload_job_id")
    private Long id;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    protected JobStatus status = JobStatus.UNDEFINED;

    /** Assigned Goobi workflow process ID. */
    @Column(name = "process_id")
    protected Integer processId;

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
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
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
     * @return the processId
     */
    public Integer getProcessId() {
        return processId;
    }

    /**
     * @param processId the processId to set
     */
    public void setProcessId(Integer processId) {
        this.processId = processId;
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
     * 
     * @return {@link Document}
     * @should create xml document correctly
     */
    Document buildXmlBody() {
        Document doc = new Document();
        Element root = new Element("record")
                .addContent(new Element("identifier").setText(getPi()))
                .addContent(new Element("processtitle").setText("TODO"))
                .addContent(new Element("docstruct").setText("monograph")); // TODO
        doc.setRootElement(root);

        Element eleMetadataList = new Element("metadataList")
                .addContent(new Element("metadata").setAttribute("name", "TitleDocMain").setText(getTitle()))
                .addContent(new Element("metadata").setAttribute("name", "Description").setText(getDescription()));
        root.addContent(eleMetadataList);

        Element propertyList = new Element("propertyList").addContent(new Element("property").setAttribute("name", "email").setText(email));
        root.addContent(propertyList);

        return doc;
    }

    /**
     * @throws UploadException
     */
    public void createProcess() throws UploadException {
        String url = DataManager.getInstance().getConfiguration().getWorkflowRestUrl() + "processes";
        String body = XmlTools.getStringFromElement(buildXmlBody(), StringTools.DEFAULT_ENCODING);
        try {
            // TODO auth via header param "password"
            String response = NetTools.getWebContentPOST(url, null, null, body, description);
            if (StringUtils.isEmpty(response)) {
                logger.error("No XML response received.");
                throw new UploadException("No XML response received.");
            }
            Document doc = XmlTools.getDocumentFromString(response, StringTools.DEFAULT_ENCODING);
            if (doc == null || doc.getRootElement() == null) {
                logger.error("Could not parse XML.");
                throw new UploadException("Could not parse XML.");
            }

            if (!"success".equals(doc.getRootElement().getChildText("result"))) {
                String errorText = doc.getRootElement().getChildText("errorText");
                throw new UploadException(errorText);
            }

            String processId = doc.getRootElement().getChildText("processId");
            try {
                setProcessId(Integer.valueOf(processId));
                if (DataManager.getInstance().getDao().addUploadJob(this)) {
                    return;
                }
            } catch (NumberFormatException e) {
                logger.error("Cannot parse process ID: {}", processId);
                throw new UploadException("Cannot parse process ID: " + processId);
            } catch (DAOException e) {
                logger.error(e.getMessage());
                throw new UploadException(e.getMessage());
            }
        } catch (ClientProtocolException e) {
            logger.error(e.getMessage());
            throw new UploadException(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new UploadException(e.getMessage());
        } catch (HTTPException e) {
            logger.error(e.getMessage());
            throw new UploadException(e.getMessage());
        } catch (JDOMException e) {
            logger.error(e.getMessage());
            throw new UploadException(e.getMessage());
        }
    }

    /**
     * <p>
     * getJobStatus.
     * </p>
     *
     * @param processId a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getJobStatus(Integer processId) {
        StringBuilder url = new StringBuilder()
                .append(DataManager.getInstance().getConfiguration().getWorkflowRestUrl())
                .append("process/details/id/")
                .append(processId)
                .append('/');
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
     * <p>
     * updateStatus.
     * </p>
     */
    public void updateStatus() {
        String ret = getJobStatus(processId);
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
        sb.append("UploadJob process ID ").append(getProcessId()).append("; ");
        sb.append("Status ").append(getStatus()).append("; ");
        sb.append("PI ").append(getPi()).append("; ");
        return sb.toString();
    }
}
