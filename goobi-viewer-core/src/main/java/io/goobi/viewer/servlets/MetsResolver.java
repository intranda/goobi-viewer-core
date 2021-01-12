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
package io.goobi.viewer.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;

/**
 * Servlet implementation class MetsResolver
 */
public class MetsResolver extends HttpServlet {

    private static final long serialVersionUID = 1663103361341321142L;

    private static final Logger logger = LoggerFactory.getLogger(MetsResolver.class);

    private static final String ERRTXT_DOC_NOT_FOUND = "No matching document could be found. ";
    private static final String ERRTXT_ILLEGAL_IDENTIFIER = "Illegal identifier";
    private static final String ERRTXT_MULTIMATCH = "Multiple documents matched the search query. No unambiguous mapping possible.";
    private static final String[] FIELDS =
            { SolrConstants.ACCESSCONDITION, SolrConstants.DATAREPOSITORY, SolrConstants.PI_TOPSTRUCT, SolrConstants.SOURCEDOCFORMAT };

    /**
     * <p>
     * main.
     * </p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(String[] args) {
        String s = "/opt/digiverso/data/";
        URI p = URI.create(s);
        System.out.println(p + " is absolute: " + p.isAbsolute());
    }

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String id = request.getParameter("id");
        String urn = request.getParameter("urn");
        if (id == null && urn == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERRTXT_ILLEGAL_IDENTIFIER + ": " + id);
            return;
        }
        if (id != null && !PIValidator.validatePi(id)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERRTXT_ILLEGAL_IDENTIFIER + ": " + id);
            return;
        }

        try {
            String query = null;
            if (id != null) {
                query = SolrConstants.PI + ":\"" + id + '"';
            } else if (urn != null) {
                query = SolrConstants.URN + ":\"" + urn + '"';
            }
            SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(query, Arrays.asList(FIELDS));
            if (hits == null || hits.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_DOC_NOT_FOUND);
                return;
            }
            if (hits.getNumFound() > 1) {
                // show multiple match, that indicates corrupted index
                response.sendError(HttpServletResponse.SC_CONFLICT, ERRTXT_MULTIMATCH);
                return;
            }

            SolrDocument doc = hits.get(0);
            id = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);

            // If the user has no listing privilege for this record, act as if it does not exist
            boolean access =
                    AccessConditionUtils.checkAccessPermissionBySolrDoc(doc, query, IPrivilegeHolder.PRIV_DOWNLOAD_METADATA, request);
            if (!access) {
                logger.debug("User may not download metadata for {}", id);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_DOC_NOT_FOUND);
                return;
            }

            String format = (String) doc.getFieldValue(SolrConstants.SOURCEDOCFORMAT);
            String dataRepository = (String) doc.getFieldValue(SolrConstants.DATAREPOSITORY);

            String filePath =
                    DataFileTools.getSourceFilePath(id + ".xml", dataRepository, format != null ? format.toUpperCase() : SolrConstants._METS);

            response.setContentType("text/xml");
            File file = new File(filePath);
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

    }

    /** {@inheritDoc} */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
