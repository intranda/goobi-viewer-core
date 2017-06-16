/**
 * This file is part of the Goobi Viewer - a content presentation and management application for digitized objects.
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
package de.intranda.digiverso.presentation.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.faces.validators.PIValidator;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.user.IPrivilegeHolder;

/**
 * Servlet implementation class MetsResolver
 */
public class MetsResolver extends HttpServlet {

    private static final long serialVersionUID = 1663103361341321142L;

    private static final Logger logger = LoggerFactory.getLogger(MetsResolver.class);

    private static final String ERRTXT_DOC_NOT_FOUND = "No matching document could be found. ";
    private static final String ERRTXT_ILLEGAL_IDENTIFIER = "Illegal identifier";

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     * @should return METS file correctly
     * @should return LIDO file correctly
     * @should return 404 if access denied
     * @should return 404 if file not found
     * @should return 500 if record identifier bad
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getParameter("id") != null) {
            String id = request.getParameter("id");
            if (!PIValidator.validatePi(id)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERRTXT_ILLEGAL_IDENTIFIER + ": " + id);
                return;
            }
            StringBuilder sbPath = new StringBuilder();
            try {
                SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":" + id);
                if (hits != null && !hits.isEmpty()) {
                    // If the user has no listing privilege for this record, act as if it does not exist
                    boolean access = SearchHelper.checkAccessPermissionByIdentifierAndLogId(id, null, IPrivilegeHolder.PRIV_LIST, request);
                    if (!access) {
                        logger.debug("User may not list {}", id);
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_DOC_NOT_FOUND);
                        return;
                    }

                    String format = (String) hits.get(0).getFieldValue(SolrConstants.SOURCEDOCFORMAT);
                    String dataRepository = (String) hits.get(0).getFieldValue(SolrConstants.DATAREPOSITORY);
                    if (StringUtils.isNotEmpty(dataRepository)) {
                        if (format != null) {
                            switch (format.toUpperCase()) {
                                case SolrConstants._METS:
                                    sbPath.append(DataManager.getInstance().getConfiguration().getDataRepositoriesHome()).append('/').append(
                                            dataRepository).append('/').append(DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
                                    break;
                                case SolrConstants._LIDO:
                                    sbPath.append(DataManager.getInstance().getConfiguration().getDataRepositoriesHome()).append('/').append(
                                            dataRepository).append('/').append(DataManager.getInstance().getConfiguration().getIndexedLidoFolder());
                                    break;
                            }
                        } else {
                            sbPath.append(DataManager.getInstance().getConfiguration().getDataRepositoriesHome()).append('/').append(dataRepository)
                                    .append('/').append(DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
                        }
                    } else {
                        // Backwards compatibility for old indexes
                        if (format != null) {
                            switch (format.toUpperCase()) {
                                case SolrConstants._METS:
                                    sbPath.append(DataManager.getInstance().getConfiguration().getViewerHome()).append(DataManager.getInstance()
                                            .getConfiguration().getIndexedMetsFolder());
                                    break;
                                case SolrConstants._LIDO:
                                    sbPath.append(DataManager.getInstance().getConfiguration().getViewerHome()).append(DataManager.getInstance()
                                            .getConfiguration().getIndexedLidoFolder());
                                    break;
                                default: // nothing
                            }
                        } else {
                            sbPath.append(DataManager.getInstance().getConfiguration().getViewerHome()).append(DataManager.getInstance()
                                    .getConfiguration().getIndexedMetsFolder());
                        }
                    }
                    if (sbPath.charAt(sbPath.length() - 1) != '/') {
                        sbPath.append('/');
                    }
                }
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            } catch (DAOException e) {
                logger.debug("DAOException thrown here: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }

            sbPath.append(id).append(".xml");
            response.setContentType("text/xml");
            File file = new File(sbPath.toString());
            response.setHeader("Content-Disposition", "filename=\"" + file.getName() + "\"");
            try (FileInputStream fis = new FileInputStream(file); ServletOutputStream out = response.getOutputStream()) {
                int bytesRead = 0;
                byte[] byteArray = new byte[300];
                while ((bytesRead = fis.read(byteArray)) != -1) {
                    out.write(byteArray, 0, bytesRead);
                }
                out.flush();
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
                response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_DOC_NOT_FOUND);
            }
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
