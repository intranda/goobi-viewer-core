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
package de.intranda.digiverso.presentation.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.faces.validators.PIValidator;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;

/**
 * Servlet implementation class MetsResolver
 */
public class MetsResolver extends HttpServlet {

    private static final long serialVersionUID = 1663103361341321142L;

    private static final Logger logger = LoggerFactory.getLogger(MetsResolver.class);

    private static final String ERRTXT_DOC_NOT_FOUND = "No matching document could be found. ";
    private static final String ERRTXT_ILLEGAL_IDENTIFIER = "Illegal identifier";
    private static final String ERRTXT_MULTIMATCH = "Multiple documents matched the search query. No unambiguous mapping possible.";

    public static void main(String[] args) {
        String s = "/opt/digiverso/data/";
        URI p = URI.create(s);
        System.out.println(p + " is absolute: " + p.isAbsolute());
    }
    
    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     * @should return METS file correctly via pi
     * @should return METS file correctly via urn
     * @should return LIDO file correctly
     * @should return 404 if access denied
     * @should return 404 if file not found
     * @should return 409 if more than one record matched
     * @should return 500 if record identifier bad
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String id = request.getParameter("id");
        String urn = request.getParameter("urn");
        if (id != null || urn != null) {
            if (id != null && !PIValidator.validatePi(id)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERRTXT_ILLEGAL_IDENTIFIER + ": " + id);
                return;
            }
            StringBuilder sbPath = new StringBuilder();
            try {
                String query = null;
                if (id != null) {
                    query = SolrConstants.PI + ":\"" + id + '"';
                } else if (urn != null) {
                    query = SolrConstants.URN + ":\"" + urn + '"';
                }
                SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(query);
                if (hits == null || hits.isEmpty()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_DOC_NOT_FOUND);
                    return;
                }
                if (hits.getNumFound() > 1) {
                    // show multiple match, that indicates corrupted index
                    response.sendError(HttpServletResponse.SC_CONFLICT, ERRTXT_MULTIMATCH);
                    return;
                }

                // If the user has no listing privilege for this record, act as if it does not exist
                SolrDocument doc = hits.get(0);
                id = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
                boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(id, null, IPrivilegeHolder.PRIV_LIST, request);
                if (!access) {
                    logger.debug("User may not list {}", id);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_DOC_NOT_FOUND);
                    return;
                }

                String format = (String) doc.getFieldValue(SolrConstants.SOURCEDOCFORMAT);
                String dataRepository = ((String) doc.getFieldValue(SolrConstants.DATAREPOSITORY)).replace("file://", "");
                if (StringUtils.isNotEmpty(dataRepository)) {
                    String dataRepositoriesHome = DataManager.getInstance().getConfiguration().getDataRepositoriesHome();
                    if (StringUtils.isNotEmpty(dataRepositoriesHome) && !Paths.get(dataRepositoriesHome).isAbsolute()) {
                        sbPath.append(dataRepositoriesHome).append('/');
                    }
                    if (format != null) {
                        switch (format.toUpperCase()) {
                            case SolrConstants._METS:
                                sbPath.append(dataRepository).append('/').append(DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
                                break;
                            case SolrConstants._LIDO:
                                sbPath.append(dataRepository).append('/').append(DataManager.getInstance().getConfiguration().getIndexedLidoFolder());
                                break;
                        }
                    } else {
                        sbPath.append(dataRepository).append('/').append(DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
                    }
                } else {
                    // Backwards compatibility for old indexes
                    if (format != null) {
                        switch (format.toUpperCase()) {
                            case SolrConstants._METS:
                                sbPath.append(DataManager.getInstance().getConfiguration().getViewerHome())
                                        .append(DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
                                break;
                            case SolrConstants._LIDO:
                                sbPath.append(DataManager.getInstance().getConfiguration().getViewerHome())
                                        .append(DataManager.getInstance().getConfiguration().getIndexedLidoFolder());
                                break;
                            default: // nothing
                        }
                    } else {
                        sbPath.append(DataManager.getInstance().getConfiguration().getViewerHome())
                                .append(DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
                    }
                }
                if (sbPath.charAt(sbPath.length() - 1) != '/') {
                    sbPath.append('/');
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
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + file.getAbsolutePath());
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
