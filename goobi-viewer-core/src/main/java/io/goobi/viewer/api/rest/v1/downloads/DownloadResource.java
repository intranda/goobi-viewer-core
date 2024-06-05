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
package io.goobi.viewer.api.rest.v1.downloads;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.bindings.AuthorizationBinding;
import io.goobi.viewer.api.rest.bindings.DownloadBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.DownloadBean;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.download.EPUBDownloadJob;
import io.goobi.viewer.model.job.download.PDFDownloadJob;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * <p>
 * DownloadResource class.
 * </p>
 */
@ViewerRestServiceBinding
@Path(ApiUrls.DOWNLOADS)
public class DownloadResource {

    private static final Logger logger = LogManager.getLogger(DownloadResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private MessageQueueManager messageBroker;

    public DownloadResource() {
    }

    /**
     * Get information about a specific downloadJob
     *
     * @param pi The PI of the underlying record
     * @param logId The logId of the underyling docStruct. Is ignored if it matches the regex [-(null)]/i
     * @return A json representation of the {@link io.goobi.viewer.model.job.download.DownloadJob}
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_PDF_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthorizationBinding
    @Operation(tags = { "downloads" }, summary = "Return information about the PDF download job for the given PI and divId")
    public DownloadJob getPDFDownloadInfo(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Identifier of the METS div for a logical section") @PathParam("divId") final String logId)
            throws DAOException, ContentNotFoundException {
        String useLogId = logId;
        if (StringUtils.isBlank(useLogId.replaceAll("(?i)[-(null)]", ""))) {
            useLogId = null;
        }
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByMetadata(PDFDownloadJob.LOCAL_TYPE, pi, useLogId);
        if (downloadJob != null) {
            return downloadJob;
        }
        throw new ContentNotFoundException("No PDF download job found for PI " + pi + " and LogId " + useLogId);
    }

    /**
     * Remove a download job from the database
     *
     * @param pi The PI of the underlying record
     * @param logId The logId of the underyling docStruct. Is ignored if it matches the regex [-(null)]/i
     * @return A json object containing the job identifier and wether the job could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_PDF_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "downloads" }, summary = "Delete a PDF download job from the database")
    @AuthorizationBinding
    public String deletePDFDownloadJob(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Identifier of the METS div for a logical section") @PathParam("divId") final String logId)
            throws DAOException, ContentLibException {
        String useLogId = logId;
        if (StringUtils.isBlank(useLogId.replaceAll("(?i)[-(null)]", ""))) {
            useLogId = null;
        }
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByMetadata(PDFDownloadJob.LOCAL_TYPE, pi, useLogId);
        if (downloadJob != null) {
            if (!DataManager.getInstance().getDao().deleteDownloadJob(downloadJob)) {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: false}";
            }
            return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: true}";
        }
        throw new ContentNotFoundException("No PDF download job found for PI " + pi + " and LogId " + useLogId);
    }

    @PUT
    @Path(ApiUrls.DOWNLOADS_PDF_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "downloads" }, summary = "Get the PDF download job for the given PI and divId, creating it if neccessary",
            description = "Returns a json object with properties 'url', containing the URL to the download page,"
                    + " and 'job' containing job information")
    @DownloadBinding
    public String putPDFDownloadJob(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Identifier of the METS div for a logical section") @PathParam("divId") String logId,
            @Parameter(
                    description = "whether to use prerendered single page pdf files for pdf creation") @QueryParam("usePdfSource") String usePdfSource,
            @Parameter(description = "email to notify on job completion") @QueryParam("email") String email)
            throws DAOException, URISyntaxException, JsonProcessingException {

        ViewerMessage message = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        // create new downloadjob

        DownloadJob job = new PDFDownloadJob(pi, logId, LocalDateTime.now(), DownloadBean.getTimeToLive());
        if (StringUtils.isNotBlank(email)) {
            job.getObservers().add(email.toLowerCase());
            message.getProperties().put("email", email.toLowerCase());
        }
        message.getProperties().put("pi", pi);
        if (StringUtils.isNotBlank(logId)) {
            message.getProperties().put("logId", logId);
        } else {
            message.getProperties().put("logId", "-");
        }
        if (StringUtils.isNotBlank(usePdfSource)) {
            message.getProperties().put("usePdfSource", usePdfSource);
        }

        job.setStatus(JobStatus.WAITING);
        DataManager.getInstance().getDao().addDownloadJob(job);

        // create new activemq message
        String messageId = message.getMessageId();
        try {
            messageId = this.messageBroker.addToQueue(message);
            messageId = URLEncoder.encode(messageId, Charset.defaultCharset());
        } catch (MessageQueueException e) {
            throw new WebApplicationException(e);
        }

        // forward to download page
        String id = DownloadJob.generateDownloadJobId(PDFDownloadJob.LOCAL_TYPE, pi, logId);
        URI downloadPageUrl = getDownloadPageUrl(messageId);
        return getForwardToDownloadPageResponse(downloadPageUrl, job);
    }

    /**
     * Get information about a specific downloadJob
     *
     * @param pi
     * @return A json representation of the {@link io.goobi.viewer.model.job.download.DownloadJob}
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_PDF_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthorizationBinding
    @Operation(tags = { "downloads" }, summary = "Return information about the PDF download job for the given PI")
    public DownloadJob getPDFDownloadInfo(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi)
            throws DAOException, ContentNotFoundException {
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(pi);
        if (downloadJob != null && PDFDownloadJob.LOCAL_TYPE.equalsIgnoreCase(downloadJob.getType())) {
            return downloadJob;
        }
        throw new ContentNotFoundException("No PDF download job found for identifier " + pi);
    }

    /**
     * Remove a download job from the database
     *
     * @param pi
     * @return A json object containing the job identifier and wether the job could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_PDF_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "downloads" }, summary = "Delete a PDF download job from the database")
    @AuthorizationBinding
    public String deletePDFDownloadJob(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi)
            throws DAOException, ContentLibException {
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(pi);
        if (downloadJob != null && PDFDownloadJob.LOCAL_TYPE.equalsIgnoreCase(downloadJob.getType())) {
            if (!DataManager.getInstance().getDao().deleteDownloadJob(downloadJob)) {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: false}";
            }
            return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: true}";
        }
        throw new ContentNotFoundException("No PDF download job found for identifier " + pi);
    }

    @PUT
    @Path(ApiUrls.DOWNLOADS_PDF_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "downloads" }, summary = "Get the PDF download job for the given PI, creating it if neccessary",
            description = "Returns a json object with properties 'url', containing the URL to the download page,"
                    + " and 'job' containing job information")
    @DownloadBinding
    public String putPDFDownloadJob(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(
                    description = "whether to use prerendered single page pdf files for pdf creation") @QueryParam("usePdfSource") String usePdfSource,
            @Parameter(description = "email to notify on job completion") @QueryParam("email") String email)
            throws ContentLibException, JsonProcessingException, DAOException, URISyntaxException {
        return putPDFDownloadJob(pi, null, usePdfSource, email);

    }

    /**
     * Get information about a specific downloadJob
     *
     * @param pi The PI of the underlying record
     * @param logId The logId of the underyling docStruct. Is ignored if it matches the regex [-(null)]/i
     * @return A json representation of the {@link io.goobi.viewer.model.job.download.DownloadJob}
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_EPUB_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthorizationBinding
    @Operation(tags = { "downloads" }, summary = "Return information about the EPUB download job for the given PI and divId")
    public DownloadJob getEPUBDownloadInfo(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Identifier of the METS div for a logical section") @PathParam("divId") final String logId)
            throws DAOException, ContentNotFoundException {
        String useLogId = logId;
        if (StringUtils.isBlank(useLogId.replaceAll("(?i)[-(null)]", ""))) {
            useLogId = null;
        }
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByMetadata(EPUBDownloadJob.LOCAL_TYPE, pi, useLogId);
        if (downloadJob != null) {
            return downloadJob;
        }
        throw new ContentNotFoundException("No EPUB download job found for PI " + pi + " and LogId " + useLogId);
    }

    /**
     * Remove a download job from the database
     *
     * @param pi The PI of the underlying record
     * @param logId The logId of the underyling docStruct. Is ignored if it matches the regex [-(null)]/i
     * @return A json object containing the job identifier and wether the job could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_EPUB_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "downloads" }, summary = "Delete an EPUB download job from the database")
    @AuthorizationBinding
    public String deleteEPUBDownloadJob(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Identifier of the METS div for a logical section") @PathParam("divId") final String logId)
            throws DAOException, ContentLibException {
        String useLogId = logId;
        if (StringUtils.isBlank(useLogId.replaceAll("(?i)[-(null)]", ""))) {
            useLogId = null;
        }
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByMetadata(EPUBDownloadJob.LOCAL_TYPE, pi, useLogId);
        if (downloadJob != null) {
            if (!DataManager.getInstance().getDao().deleteDownloadJob(downloadJob)) {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: false}";
            }
            return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: true}";
        }
        throw new ContentNotFoundException("No EPUB download job found for PI " + pi + " and LogId " + useLogId);
    }

    @PUT
    @Path(ApiUrls.DOWNLOADS_EPUB_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "downloads" }, summary = "Get the EPUB download job for the given PI and divId, creating it if neccessary",
            description = "Returns a json object with properties 'url', containing the URL to the download page,"
                    + " and 'job' containing job information")
    @DownloadBinding
    public String putEPUBDownloadJob(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Identifier of the METS div for a logical section") @PathParam("divId") String logId,
            @Parameter(description = "email to notify on job completion") @QueryParam("email") String email) throws ContentLibException {
        return getOrCreateDownloadJob(pi, logId, email, EPUBDownloadJob.LOCAL_TYPE);

    }

    /**
     * Get information about a specific downloadJob
     *
     * @param pi
     * @return A json representation of the {@link io.goobi.viewer.model.job.download.DownloadJob}
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_EPUB_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthorizationBinding
    @Operation(tags = { "downloads" }, summary = "Return information about the EPUB download job for the given PI")
    public DownloadJob getEPUBDownloadInfo(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi)
            throws DAOException, ContentNotFoundException {
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(pi);
        if (downloadJob != null && EPUBDownloadJob.LOCAL_TYPE.equalsIgnoreCase(downloadJob.getType())) {
            return downloadJob;
        }
        throw new ContentNotFoundException("No EPUB download job found for identifier " + pi);
    }

    /**
     * Remove a download job from the database
     *
     * @param pi
     * @return A json object containing the job identifier and wether the job could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_EPUB_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "downloads" }, summary = "Delete an EPUB download job from the database")
    @AuthorizationBinding
    public String deleteEPUBDownloadJob(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi)
            throws DAOException, ContentLibException {
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(pi);
        if (downloadJob != null && EPUBDownloadJob.LOCAL_TYPE.equalsIgnoreCase(downloadJob.getType())) {
            if (!DataManager.getInstance().getDao().deleteDownloadJob(downloadJob)) {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: false}";
            }
            return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: true}";
        }
        throw new ContentNotFoundException("No EPUB download job found for identifier " + pi);
    }

    @PUT
    @Path(ApiUrls.DOWNLOADS_EPUB_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "downloads" }, summary = "Get the EPUB download job for the given PI, creating it if neccessary",
            description = "Returns a json object with properties 'url', containing the URL to the download page,"
                    + " and 'job' containing job information")
    @DownloadBinding
    public String putEPUBDownloadJob(@Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "email to notify on job completion") @QueryParam("email") String email) throws ContentLibException {
        return getOrCreateDownloadJob(pi, "", email, EPUBDownloadJob.LOCAL_TYPE);

    }

    /**
     * Get information about all download jobs of a type
     *
     * @return An array of json representations of all {@link io.goobi.viewer.model.job.download.DownloadJob}s of the given type
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthorizationBinding
    @Operation(tags = { "downloads" }, summary = "Return information all download jobs")
    public List<DownloadJob> getAllDownloadJobs() throws DAOException, ContentLibException {
        return DataManager.getInstance().getDao().getAllDownloadJobs().stream().collect(Collectors.toList());
    }

    /**
     * Remove all jobs from the database
     *
     * @return An array of json objects containing the job identifiers and wether the jobs could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "downloads" }, summary = "Delete all download jobs")
    @AuthorizationBinding
    public List<String> deleteAllDownloadJobs() throws DAOException, ContentLibException {
        List<DownloadJob> downloadJobs = DataManager.getInstance().getDao().getAllDownloadJobs().stream().collect(Collectors.toList());
        if (!downloadJobs.isEmpty()) {
            List<String> results = new ArrayList<>();
            for (DownloadJob job : downloadJobs) {
                if (DataManager.getInstance().getDao().deleteDownloadJob(job)) {
                    results.add("{job: \"" + job.getIdentifier() + "\", deleted: true}");
                } else {
                    results.add("{job: \"" + job.getIdentifier() + "\", deleted: false}");
                }
            }
            return results;
        }

        return Collections.emptyList();
    }

    /**
     * Get information about all download jobs of a type
     *
     * @return An array of json representations of all {@link io.goobi.viewer.model.job.download.DownloadJob}s of the given type
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_PDF)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthorizationBinding
    @Operation(tags = { "downloads" }, summary = "Return information all PDF download jobs")
    public List<DownloadJob> getPDFDownloadJobs() throws DAOException, ContentLibException {
        return DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
                .filter(job -> PDFDownloadJob.LOCAL_TYPE.equalsIgnoreCase(job.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Remove all jobs of a type from the database
     *
     * @return An array of json objects containing the job identifiers and wether the jobs could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_PDF)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "downloads" }, summary = "Delete all PDF download jobs")

    @AuthorizationBinding
    public List<String> deletePDFDownloadJobs() throws DAOException, ContentLibException {
        List<DownloadJob> downloadJobs = DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
                .filter(job -> PDFDownloadJob.LOCAL_TYPE.equalsIgnoreCase(job.getType()))
                .collect(Collectors.toList());
        if (!downloadJobs.isEmpty()) {
            List<String> results = new ArrayList<>();
            for (DownloadJob job : downloadJobs) {
                if (DataManager.getInstance().getDao().deleteDownloadJob(job)) {
                    results.add("{job: \"" + job.getIdentifier() + "\", deleted: true}");
                } else {
                    results.add("{job: \"" + job.getIdentifier() + "\", deleted: false}");
                }
            }
            return results;
        }

        return Collections.emptyList();
    }

    /**
     * Get information about all download jobs of a type
     *
     * @return An array of json representations of all {@link io.goobi.viewer.model.job.download.DownloadJob}s of the given type
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_EPUB)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthorizationBinding
    @Operation(tags = { "downloads" }, summary = "Return information all EPUB download jobs")
    public List<DownloadJob> getDownloadJobs() throws DAOException, ContentLibException {
        return DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
                .filter(job -> EPUBDownloadJob.LOCAL_TYPE.equalsIgnoreCase(job.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Remove all jobs of a type from the database
     *
     * @return An array of json objects containing the job identifiers and wether the jobs could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_EPUB)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "downloads" }, summary = "Delete all EPUB download jobs")
    @AuthorizationBinding
    public List<String> deleteEPUBDownloadJobs() throws DAOException, ContentLibException {
        List<DownloadJob> downloadJobs = DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
                .filter(job -> EPUBDownloadJob.LOCAL_TYPE.equalsIgnoreCase(job.getType()))
                .collect(Collectors.toList());
        if (!downloadJobs.isEmpty()) {
            List<String> results = new ArrayList<>();
            for (DownloadJob job : downloadJobs) {
                if (DataManager.getInstance().getDao().deleteDownloadJob(job)) {
                    results.add("{job: \"" + job.getIdentifier() + "\", deleted: true}");
                } else {
                    results.add("{job: \"" + job.getIdentifier() + "\", deleted: false}");
                }
            }
            return results;
        }

        return Collections.emptyList();
    }

    /**
     * @param pi
     * @param inLogId
     * @param inEmail
     * @param type
     * @return Response as JSON
     * @throws ContentLibException
     */
    public String getOrCreateDownloadJob(String pi, final String inLogId, final String inEmail, String type) throws ContentLibException {
        String email = inEmail;
        if (email == null || "-".equals(email)) {
            email = "";
        }
        String logId = inLogId;
        if (logId == null || "-".equals(logId)) {
            logId = "";
        }
        try {
            email = StringEscapeUtils.unescapeHtml4(email);
            String id = DownloadJob.generateDownloadJobId(type, pi, logId);
            try {
                DownloadJob job = DownloadJob.checkDownload(type, email, pi, logId, id, DownloadBean.getTimeToLive());
                URI downloadPageUrl = getDownloadPageUrl(id);
                return getForwardToDownloadPageResponse(downloadPageUrl, job);
            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
                throw new ContentLibException("Error creating download job", e);
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                throw new ContentLibException("Failed to communicate with database", e);
            } catch (DAOException e) {
                logger.debug("DAOException thrown here: {}", e.getMessage());
                throw new ContentLibException("Error communicating with database", e);
            }
            //            return Response.temporaryRedirect(downloadPageUrl).build();
        } catch (URISyntaxException | JsonProcessingException e) {
            throw new ContentLibException("Failed to create url to download page ", e);
        }
    }

    /**
     * @param downloadPageUrl
     * @param job
     * @return Response as JSON
     * @throws JsonProcessingException
     */
    private static String getForwardToDownloadPageResponse(URI downloadPageUrl, DownloadJob job) throws JsonProcessingException {
        String jobString = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(job);
        JSONObject jobJson = new JSONObject(jobString);
        JSONObject object = new JSONObject();
        object.put("url", downloadPageUrl);
        object.put("job", jobJson);
        return object.toString();
    }

    /**
     * 
     * @param id
     * @return {@link URI}
     * @throws URISyntaxException
     */
    private URI getDownloadPageUrl(String id) throws URISyntaxException {
        return new URI(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest) + "/download/" + id + "/");
    }
}
