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
package de.intranda.digiverso.presentation.servlets.rest.content;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.FileTools;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;

/**
 * Resource for delivering content documents such as ALTO and plain full-text.
 */

@Path("/content")
@ViewerRestServiceBinding
public class ContentResource {

    private static final Logger logger = LoggerFactory.getLogger(ContentResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * @param pi
     * @param fileName
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException * @throws DAOException
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     */
    @GET
    @Path("/alto/{pi}/{fileName}")
    @Produces({ MediaType.APPLICATION_XML })
    public String getAltoDocument(@PathParam("pi") String pi, @PathParam("fileName") String fileName) throws PresentationException,
            IndexUnreachableException, DAOException, MalformedURLException, ContentNotFoundException {
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        String filePath = DataManager.getInstance().getConfiguration().getAltoFolder() + '/' + pi + '/' + fileName;

        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(servletRequest, pi, fileName,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        if (!access) {
            throw new ContentNotFoundException("No permission found");
        }

        servletResponse.addHeader("Access-Control-Allow-Origin", "*");
        java.nio.file.Path file = Paths.get(Helper.getRepositoryPath(dataRepository), filePath);
        if (file != null && Files.isRegularFile(file)) {
            Document doc;
            try {
                doc = FileTools.readXmlFile(file);
                return new XMLOutputter().outputString(doc);
            } catch (FileNotFoundException e) {
                logger.debug(e.getMessage());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } catch (JDOMException e) {
                logger.error(e.getMessage(), e);
            }
        }

        throw new ContentNotFoundException("Resource not found");

    }

    /**
     * @param pi Record identifier
     * @param fileName
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     */
    @GET
    @Path("/fulltext/{pi}/{fileName}")
    @Produces({ MediaType.TEXT_HTML })
    public String getFulltextDocument(@PathParam("pi") String pi, @PathParam("fileName") String fileName) throws PresentationException,
            IndexUnreachableException, DAOException, MalformedURLException, ContentNotFoundException {
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        String filePath = DataManager.getInstance().getConfiguration().getFulltextFolder() + '/' + pi + '/' + fileName;

        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(servletRequest, pi, fileName,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        if (!access) {
            throw new ContentNotFoundException("No permission found");
        }

        servletResponse.addHeader("Access-Control-Allow-Origin", "*");
        java.nio.file.Path file = Paths.get(Helper.getRepositoryPath(dataRepository), filePath);
        ;
        if (file != null && Files.isRegularFile(file)) {
            try {
                return FileTools.getStringFromFile(file.toFile(), null);
            } catch (FileNotFoundException e) {
                logger.debug(e.getMessage());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        throw new ContentNotFoundException("Resource not found");
    }

    /**
     * API method for retrieving any type of content by its relative path within its data repository.
     * 
     * @param pi Record identifier
     * @param dataRepository Absolute path of the data repository
     * @param filePath File path relative to the data repository
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     */
    @GET
    @Path("/document/{pi}/{filePath}")
    @Produces({ MediaType.TEXT_HTML })
    public String getContentDocument(@PathParam("pi") String pi, @PathParam("dataRepository") String dataRepository,
            @PathParam("filePath") String filePath) throws PresentationException, IndexUnreachableException, DAOException, MalformedURLException,
            ContentNotFoundException {
        servletResponse.addHeader("Access-Control-Allow-Origin", "*");
        java.nio.file.Path file = Paths.get(Helper.getRepositoryPath(dataRepository), filePath);
        String fileName = FilenameUtils.getName(filePath);
        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(servletRequest, pi, fileName,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        if (!access) {
            throw new ContentNotFoundException("No permission found");
        }

        if (file != null && Files.isRegularFile(file)) {
            try {
                return FileTools.getStringFromFile(file.toFile(), null);
            } catch (FileNotFoundException e) {
                logger.debug(e.getMessage());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        throw new ContentNotFoundException("Resource not found");
    }
}
