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
package de.intranda.digiverso.presentation.servlets.download;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.DownloadBean;
import de.intranda.digiverso.presentation.model.download.DownloadJob;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

@DownloadBinding
@Path("/download")
public class DownloadResource {

    private static final Logger logger = LoggerFactory.getLogger(DownloadResource.class);

    @Context
    private HttpServletRequest servletRequest;

    @GET
    @Path("/{type}/{pi}/{logId}/{email}")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response redirectToDownloadPage(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("pi") String pi,
            @PathParam("logId") String logId, @PathParam("email") String email, @PathParam("type") String type) {
        if (email == null || email.equals("-")) {
            email = "";
        }
        if (logId == null || logId.equals("-")) {
            logId = "";
        }
        try {
            email = StringEscapeUtils.unescapeHtml(email);
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
