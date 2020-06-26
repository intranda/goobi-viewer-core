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
package io.goobi.viewer.api.rest.v1.downloads;

import java.net.URI;
import java.net.URISyntaxException;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.DownloadBinding;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.api.rest.v1.records.RecordResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.DownloadBean;
import io.goobi.viewer.model.download.DownloadJob;
import io.goobi.viewer.model.download.EPUBDownloadJob;
import io.goobi.viewer.model.download.PDFDownloadJob;
import io.goobi.viewer.servlets.rest.security.AuthenticationBinding;
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

    private static final Logger logger = LoggerFactory.getLogger(DownloadResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private AbstractApiUrlManager urls;

    public DownloadResource() {
    }

    /**
     * Get information about a specific downloadJob
     *
     * @param type The jobtype, either pdf or epub
     * @param pi The PI of the underlying record
     * @param logId The logId of the underyling docStruct. Is ignored if it matches the regex [-(null)]/i
     * @return A json representation of the {@link io.goobi.viewer.model.download.DownloadJob}
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_PDF_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    @Operation(tags= {"downloads"}, summary="Return information about the PDF download job for the given PI and divId")
    public DownloadJob getPDFDownloadInfo(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description="Identifier of the METS div for a logical section")@PathParam("divId") String logId)
            throws DAOException, ContentNotFoundException {
        if (StringUtils.isBlank(logId.replaceAll("(?i)[-(null)]", ""))) {
            logId = null;
        }
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByMetadata(PDFDownloadJob.TYPE, pi, logId);
        if (downloadJob != null) {
            return downloadJob;
        } else {
            throw new ContentNotFoundException("No PDF download job found for PI " + pi + " and LogId " + logId);
        }
    }
    
    /**
     * Remove a download job from the database
     *
     * @param type The jobtype, either pdf or epub
     * @param pi The PI of the underlying record
     * @param logId The logId of the underyling docStruct. Is ignored if it matches the regex [-(null)]/i
     * @return A json object containing the job identifier and wether the job could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_PDF_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags= {"downloads"}, summary="Delete a PDF download job from the database")
    @AuthenticationBinding
    public String deletePDFDownloadJob(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description="Identifier of the METS div for a logical section")@PathParam("divId") String logId)
            throws DAOException, ContentLibException {
        if (StringUtils.isBlank(logId.replaceAll("(?i)[-(null)]", ""))) {
            logId = null;
        }
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByMetadata(PDFDownloadJob.TYPE, pi, logId);
        if (downloadJob != null) {
            if (!DataManager.getInstance().getDao().deleteDownloadJob(downloadJob)) {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: false}";
            } else {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: true}";
            }
        } else {
            throw new ContentNotFoundException("No PDF download job found for PI " + pi + " and LogId " + logId);
        }
    }
    
    @PUT
    @Path(ApiUrls.DOWNLOADS_PDF_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags= {"downloads"}, summary="Get the PDF download job for the given PI and divId, creating it if neccessary",
    description="Returns a json object with properties 'url', containing the URL to the download page, and 'job' containing job information")
    @DownloadBinding
    public String putPDFDownloadJob(            
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description="Identifier of the METS div for a logical section")@PathParam("divId") String logId,
            @Parameter(description = "email to notify on job completion") @QueryParam("email") String email) throws ContentLibException {
        return getOrCreateDownloadJob(pi, logId, email, PDFDownloadJob.TYPE);
        
    }

    /**
     * Get information about a specific downloadJob
     *
     * @param type The jobtype, either pdf or epub
     * @param identifier The job idenfier
     * @return A json representation of the {@link io.goobi.viewer.model.download.DownloadJob}
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_PDF_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    @Operation(tags= {"downloads"}, summary="Return information about the PDF download job for the given PI")
    public DownloadJob getPDFDownloadInfo(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi)
            throws DAOException, ContentNotFoundException {
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(pi);
        if (downloadJob != null && downloadJob.getType().equalsIgnoreCase(PDFDownloadJob.TYPE)) {
            return downloadJob;
        } else {
            throw new ContentNotFoundException("No PDF download job found for identifier " + pi);
        }
    }
    
    /**
     * Remove a download job from the database
     *
     * @param type The jobtype, either pdf or epub
     * @param identifier The job idenfier
     * @return A json object containing the job identifier and wether the job could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_PDF_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags= {"downloads"}, summary="Delete a PDF download job from the database")
    @AuthenticationBinding
    public String deletePDFDownloadJob(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi)
            throws DAOException, ContentLibException {
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(pi);
        if (downloadJob != null && downloadJob.getType().equalsIgnoreCase(PDFDownloadJob.TYPE)) {
            if (!DataManager.getInstance().getDao().deleteDownloadJob(downloadJob)) {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: false}";
            } else {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: true}";
            }
        } else {
            throw new ContentNotFoundException("No PDF download job found for identifier " + pi);
        }
    }
    
    @PUT
    @Path(ApiUrls.DOWNLOADS_PDF_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags= {"downloads"}, summary="Get the PDF download job for the given PI, creating it if neccessary",
    description="Returns a json object with properties 'url', containing the URL to the download page, and 'job' containing job information")

    @DownloadBinding
    public String putPDFDownloadJob(            
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "email to notify on job completion") @QueryParam("email") String email) throws ContentLibException {
        return getOrCreateDownloadJob(pi, "", email, PDFDownloadJob.TYPE);
        
    }
    
    /**
     * Get information about a specific downloadJob
     *
     * @param type The jobtype, either pdf or epub
     * @param pi The PI of the underlying record
     * @param logId The logId of the underyling docStruct. Is ignored if it matches the regex [-(null)]/i
     * @return A json representation of the {@link io.goobi.viewer.model.download.DownloadJob}
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_EPUB_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    @Operation(tags= {"downloads"}, summary="Return information about the EPUB download job for the given PI and divId")
    public DownloadJob getEPUBDownloadInfo(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description="Identifier of the METS div for a logical section")@PathParam("divId") String logId)
            throws DAOException, ContentNotFoundException {
        if (StringUtils.isBlank(logId.replaceAll("(?i)[-(null)]", ""))) {
            logId = null;
        }
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByMetadata(EPUBDownloadJob.TYPE, pi, logId);
        if (downloadJob != null) {
            return downloadJob;
        } else {
            throw new ContentNotFoundException("No EPUB download job found for PI " + pi + " and LogId " + logId);
        }
    }
    
    /**
     * Remove a download job from the database
     *
     * @param type The jobtype, either pdf or epub
     * @param pi The PI of the underlying record
     * @param logId The logId of the underyling docStruct. Is ignored if it matches the regex [-(null)]/i
     * @return A json object containing the job identifier and wether the job could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_EPUB_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags= {"downloads"}, summary="Delete an EPUB download job from the database")
    @AuthenticationBinding
    public String deleteEPUBDownloadJob(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description="Identifier of the METS div for a logical section")@PathParam("divId") String logId)
            throws DAOException, ContentLibException {
        if (StringUtils.isBlank(logId.replaceAll("(?i)[-(null)]", ""))) {
            logId = null;
        }
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByMetadata(EPUBDownloadJob.TYPE, pi, logId);
        if (downloadJob != null) {
            if (!DataManager.getInstance().getDao().deleteDownloadJob(downloadJob)) {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: false}";
            } else {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: true}";
            }
        } else {
            throw new ContentNotFoundException("No EPUB download job found for PI " + pi + " and LogId " + logId);
        }
    }
    
    @PUT
    @Path(ApiUrls.DOWNLOADS_EPUB_SECTION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags= {"downloads"}, summary="Get the EPUB download job for the given PI and divId, creating it if neccessary",
    description="Returns a json object with properties 'url', containing the URL to the download page, and 'job' containing job information")

    @DownloadBinding
    public String putEPUBDownloadJob(            
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description="Identifier of the METS div for a logical section")@PathParam("divId") String logId,
            @Parameter(description = "email to notify on job completion") @QueryParam("email") String email) throws ContentLibException {
        return getOrCreateDownloadJob(pi, logId, email, EPUBDownloadJob.TYPE);
        
    }

    /**
     * Get information about a specific downloadJob
     *
     * @param type The jobtype, either pdf or epub
     * @param identifier The job idenfier
     * @return A json representation of the {@link io.goobi.viewer.model.download.DownloadJob}
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_EPUB_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    @Operation(tags= {"downloads"}, summary="Return information about the EPUB download job for the given PI")
    public DownloadJob getEPUBDownloadInfo(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi)
            throws DAOException, ContentNotFoundException {
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(pi);
        if (downloadJob != null && downloadJob.getType().equalsIgnoreCase(EPUBDownloadJob.TYPE)) {
            return downloadJob;
        } else {
            throw new ContentNotFoundException("No EPUB download job found for identifier " + pi);
        }
    }
    
    /**
     * Remove a download job from the database
     *
     * @param type The jobtype, either pdf or epub
     * @param identifier The job idenfier
     * @return A json object containing the job identifier and wether the job could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_EPUB_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags= {"downloads"}, summary="Delete an EPUB download job from the database")
    @AuthenticationBinding
    public String deleteEPUBDownloadJob(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi)
            throws DAOException, ContentLibException {
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(pi);
        if (downloadJob != null && downloadJob.getType().equalsIgnoreCase(EPUBDownloadJob.TYPE)) {
            if (!DataManager.getInstance().getDao().deleteDownloadJob(downloadJob)) {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: false}";
            } else {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: true}";
            }
        } else {
            throw new ContentNotFoundException("No EPUB download job found for identifier " + pi);
        }
    }

    @PUT
    @Path(ApiUrls.DOWNLOADS_EPUB_RECORD)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags= {"downloads"}, summary="Get the EPUB download job for the given PI, creating it if neccessary",
    description="Returns a json object with properties 'url', containing the URL to the download page, and 'job' containing job information")
    @DownloadBinding
    public String putEPUBDownloadJob(            
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "email to notify on job completion") @QueryParam("email") String email) throws ContentLibException {
        return getOrCreateDownloadJob(pi, "", email, EPUBDownloadJob.TYPE);
        
    }
    
    /**
     * Get information about all download jobs of a type
     *
     * @param type The jobtype, either pdf or epub
     * @return An array of json representations of all {@link io.goobi.viewer.model.download.DownloadJob}s of the given type
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    @Operation(tags= {"downloads"}, summary="Return information all download jobs")
    public List<DownloadJob> getAllDownloadJobs() throws DAOException, ContentLibException {
        List<DownloadJob> downloadJobs = DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
                .collect(Collectors.toList());
        return downloadJobs;
    }
    
    /**
     * Remove all jobs of from the database
     *
     * @param type The jobtype, either pdf or epub
     * @return An array of json objects containing the job identifiers and wether the jobs could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags= {"downloads"}, summary="Delete all download jobs")
    @AuthenticationBinding
    public List<String> deleteAllDownloadJobs() throws DAOException, ContentLibException {
        List<DownloadJob> downloadJobs = DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
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
        } else {
            return Collections.emptyList();
        }
    }
    
    /**
     * Get information about all download jobs of a type
     *
     * @param type The jobtype, either pdf or epub
     * @return An array of json representations of all {@link io.goobi.viewer.model.download.DownloadJob}s of the given type
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_PDF)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    @Operation(tags= {"downloads"}, summary="Return information all PDF download jobs")
    public List<DownloadJob> getṔDFDownloadJobs() throws DAOException, ContentLibException {
        List<DownloadJob> downloadJobs = DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
                .filter(job -> job.getType().equalsIgnoreCase(PDFDownloadJob.TYPE))
                .collect(Collectors.toList());
        return downloadJobs;
    }
    
    /**
     * Remove all jobs of a type from the database
     *
     * @param type The jobtype, either pdf or epub
     * @return An array of json objects containing the job identifiers and wether the jobs could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_PDF)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags= {"downloads"}, summary="Delete all PDF download jobs")

    @AuthenticationBinding
    public List<String> deletePDFDownloadJobs() throws DAOException, ContentLibException {
        List<DownloadJob> downloadJobs = DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
                .filter(job -> job.getType().equalsIgnoreCase(PDFDownloadJob.TYPE))
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
        } else {
            return Collections.emptyList();
        }
    }
    
    /**
     * Get information about all download jobs of a type
     *
     * @param type The jobtype, either pdf or epub
     * @return An array of json representations of all {@link io.goobi.viewer.model.download.DownloadJob}s of the given type
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @GET
    @Path(ApiUrls.DOWNLOADS_EPUB)
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    @Operation(tags= {"downloads"}, summary="Return information all EPUB download jobs")
    public List<DownloadJob> getDownloadJobs() throws DAOException, ContentLibException {
        List<DownloadJob> downloadJobs = DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
                .filter(job -> job.getType().equalsIgnoreCase(EPUBDownloadJob.TYPE))
                .collect(Collectors.toList());
        return downloadJobs;
    }
    
    /**
     * Remove all jobs of a type from the database
     *
     * @param type The jobtype, either pdf or epub
     * @return An array of json objects containing the job identifiers and wether the jobs could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @DELETE
    @Path(ApiUrls.DOWNLOADS_EPUB)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags= {"downloads"}, summary="Delete all EPUB download jobs")
    @AuthenticationBinding
    public List<String> deleteEPUBDownloadJobs() throws DAOException, ContentLibException {
        List<DownloadJob> downloadJobs = DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
                .filter(job -> job.getType().equalsIgnoreCase(EPUBDownloadJob.TYPE))
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
        } else {
            return Collections.emptyList();
        }
    }



    /**
     * @param pi
     * @param logId
     * @param email
     * @param type
     * @return
     * @throws ContentLibException 
     */
    public String getOrCreateDownloadJob(String pi, String logId, String email, String type) throws ContentLibException {
        if (email == null || email.equals("-")) {
            email = "";
        }
        if (logId == null || logId.equals("-")) {
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
                logger.debug("PresentationException thrown here: {}", e.getMessage());
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
     * @return
     * @throws JsonProcessingException 
     */
    private String getForwardToDownloadPageResponse(URI downloadPageUrl, DownloadJob job) throws JsonProcessingException {
        String jobString = new ObjectMapper().writeValueAsString(job);
        JSONObject jobJson = new JSONObject(jobString);
        JSONObject object = new JSONObject();
        object.put("url", downloadPageUrl);
        object.put("job", jobJson);
        return object.toString();
    }

    /**
     * @return
     * @throws URISyntaxException
     */
    private URI getDownloadPageUrl(String id) throws URISyntaxException {
        return new URI(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest) + "/download/" + id);
    }
}
