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
package de.intranda.digiverso.presentation.servlets.rest.media;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.AccessDeniedException;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import net.balusc.webapp.ContentDeliveryServlet;

/**
 * A rest resource for delivering video and audio files
 * 
 * @author Florian Alpers
 *
 */
@Path("/media")
public class MediaResource {
    
    private static final Logger logger = LoggerFactory.getLogger(MediaResource.class);

    @Context 
    private HttpServletRequest request;
    @Context 
    private HttpServletResponse response;
    
    @GET
    @Path("/{type}/{format}/{identifier}/{filename}")
    public String serveMediaContent(@PathParam("type") String type, @PathParam("format") String format, @PathParam("identifier") String identifier, @PathParam("filename") String filename) throws PresentationException, IndexUnreachableException, IOException, AccessDeniedException {
       
        String mimeType = type + "/" + format;
        String mediaFilePath = identifier + "/" +  filename;
        String dataRepository = getDataRepository(identifier);

        checkAccess(type, identifier, filename);

        File file;
        if (StringUtils.isNotEmpty(dataRepository)) {
            file = new File(DataManager.getInstance().getConfiguration().getDataRepositoriesHome() + mediaFilePath);
        } else {
            // Backwards compatibility with old indexes
            file = new File(DataManager.getInstance().getConfiguration().getViewerHome() + DataManager.getInstance()
                    .getConfiguration().getMediaFolder() + '/' + mediaFilePath + '/');
        }
        if (file.isFile()) {
            logger.debug("AV file: {} ({} bytes)", file.getAbsolutePath(), file.length());
            
            try {
                new ContentDeliveryServlet().processRequest(request, response, true, file.getAbsolutePath(), mimeType);
            } catch (IOException e) {
                throw new PresentationException("Error accessing media resource", e);
            }

        } else {
            logger.error("File '{}' not found.", file.getAbsolutePath());
        }
        return "";
    }

    /**
     * @param mediaFilePath
     * @throws AccessDeniedException    if access is not granted
     */
    public void checkAccess(String action, String pi, String contentFilename) throws AccessDeniedException {
        boolean access = false;
        try {
            access = AccessConditionUtils.checkAccess(request, action, pi, contentFilename, false);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
        } catch (DAOException e) {
            logger.debug("DAOException thrown here: {}", e.getMessage());
        }
        if(!access) {
            throw new AccessDeniedException("Access denied for " + pi + "/" + contentFilename);
        }
    }

    /**
     * @return
     * @throws IndexUnreachableException 
     * @throws PresentationException 
     */
    private String getDataRepository(String pi) throws PresentationException, IndexUnreachableException {
            return DataManager.getInstance().getSearchIndex().findDataRepository(pi);
    }
}
