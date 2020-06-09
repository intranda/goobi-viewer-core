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
package io.goobi.viewer.servlets.rest.download;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.DownloadBean;
import io.goobi.viewer.model.download.DownloadJob;
import io.goobi.viewer.servlets.rest.security.AuthenticationBinding;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * <p>
 * DownloadResource class.
 * </p>
 */
@ViewerRestServiceBinding
@Path("/download")
public class DownloadResource {

    private static final Logger logger = LoggerFactory.getLogger(DownloadResource.class);

    @Context
    private HttpServletRequest servletRequest;

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
    @Path("/get/{type}/{pi}/{logId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    public DownloadJob getDownloadInfo(@PathParam("type") String type, @PathParam("pi") String pi, @PathParam("logId") String logId)
            throws DAOException, ContentNotFoundException {
        if (StringUtils.isBlank(logId.replaceAll("(?i)[-(null)]", ""))) {
            logId = null;
        }
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByMetadata(type, pi, logId);
        if (downloadJob != null) {
            return downloadJob;
        } else {
            throw new ContentNotFoundException("No download job found for type " + type + ", PI " + pi + " and LogId " + logId);
        }
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
    @Path("/get/{type}/{identifier}")
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    public DownloadJob getDownloadInfo(@PathParam("identifier") String identifier, @PathParam("type") String type)
            throws DAOException, ContentNotFoundException {
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(identifier);
        if (downloadJob != null && downloadJob.getType().equalsIgnoreCase(type)) {
            return downloadJob;
        } else {
            throw new ContentNotFoundException("No " + type + " download job found for identifier " + identifier);
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
    @Path("/get/{type}")
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    public List<DownloadJob> getDownloadJobs(@PathParam("type") String type) throws DAOException, ContentLibException {
        List<DownloadJob> downloadJobs = DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
                .filter(job -> type.equalsIgnoreCase("all") || job.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
        return downloadJobs;
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
    @GET
    @Path("/delete/{type}/{pi}/{logId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    public String deleteDownloadJob(@PathParam("type") String type, @PathParam("pi") String pi, @PathParam("logId") String logId)
            throws DAOException, ContentLibException {
        if (StringUtils.isBlank(logId.replaceAll("(?i)[-(null)]", ""))) {
            logId = null;
        }
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByMetadata(type, pi, logId);
        if (downloadJob != null) {
            if (!DataManager.getInstance().getDao().deleteDownloadJob(downloadJob)) {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: false}";
            } else {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: true}";
            }
        } else {
            throw new ContentNotFoundException("No download job found for type " + type + ", PI " + pi + " and LogId " + logId);
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
    @GET
    @Path("/delete/{type}/{identifier}")
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    public String deleteDownloadJob(@PathParam("type") String type, @PathParam("identifier") String identifier)
            throws DAOException, ContentLibException {
        DownloadJob downloadJob = DataManager.getInstance().getDao().getDownloadJobByIdentifier(identifier);
        if (downloadJob != null && downloadJob.getType().equalsIgnoreCase(type)) {
            if (!DataManager.getInstance().getDao().deleteDownloadJob(downloadJob)) {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: false}";
            } else {
                return "{job: \"" + downloadJob.getIdentifier() + "\", deleted: true}";
            }
        } else {
            throw new ContentNotFoundException("No " + type + " download job found for identifier " + identifier);
        }
    }

    /**
     * Remove all jobs of a type from the database
     *
     * @param type The jobtype, either pdf or epub
     * @return An array of json objects containing the job identifiers and wether the jobs could be deleted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    @GET
    @Path("/delete/{type}")
    @Produces({ MediaType.APPLICATION_JSON })
    @AuthenticationBinding
    public List<String> deleteDownloadJobs(@PathParam("type") String type) throws DAOException, ContentLibException {
        List<DownloadJob> downloadJobs = DataManager.getInstance()
                .getDao()
                .getAllDownloadJobs()
                .stream()
                .filter(job -> type.equalsIgnoreCase("all") || job.getType().equalsIgnoreCase(type))
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
     * <p>
     * redirectToDownloadPage.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param email a {@link java.lang.String} object.
     * @param type a {@link java.lang.String} object.
     * @return a {@link javax.ws.rs.core.Response} object.
     */
    @GET
    @Path("/{type}/{pi}/{logId}/{email}")
    @Produces({ MediaType.TEXT_PLAIN })
    @DownloadBinding
    public Response redirectToDownloadPage(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("pi") String pi,
            @PathParam("logId") String logId, @PathParam("email") String email, @PathParam("type") String type) {
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
                DownloadJob.checkDownload(type, email, pi, logId, id, DownloadBean.getTimeToLive());
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
                return Response.serverError().build();
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                return Response.serverError().build();
            } catch (DAOException e) {
                logger.debug("DAOException thrown here: {}", e.getMessage());
                return Response.serverError().build();
            }
            URI downloadPageUrl = getDownloadPageUrl(id);
            return Response.temporaryRedirect(downloadPageUrl).build();
        } catch (URISyntaxException e) {
            logger.error(e.getMessage(), e);
            return Response.serverError().build();
        }
    }

    /**
     * @return
     * @throws URISyntaxException
     */
    private URI getDownloadPageUrl(String id) throws URISyntaxException {
        return new URI(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest) + "/download/" + id);
    }
}
