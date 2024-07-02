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
package io.goobi.viewer.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Servlet implementation class MetsResolver
 */
public class MetsResolver extends HttpServlet {

    private static final long serialVersionUID = 1663103361341321142L;

    private static final Logger logger = LogManager.getLogger(MetsResolver.class);

    static final String ERRTXT_DOC_NOT_FOUND = "No matching document could be found.";
    static final String ERRTXT_ILLEGAL_IDENTIFIER = "Illegal identifier";
    static final String ERRTXT_MULTIMATCH = "Multiple documents matched the search query. No unambiguous mapping possible.";

    private static final String[] FIELDS =
            { SolrConstants.ACCESSCONDITION, SolrConstants.DATAREPOSITORY, SolrConstants.PI_TOPSTRUCT, SolrConstants.SOURCEDOCFORMAT };

    /**
     * {@inheritDoc}
     * @should return METS file correctly via pi
     * @should return METS file correctly via urn
     * @should return LIDO file correctly
     * @should return EAD file correctly
     * @should return 404 if record not in index
     * @should return 404 if file not found
     * @should return 409 if more than one record matched
     * @should return 400 if record identifier bad
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String id = request.getParameter("id");
        String urn = request.getParameter("urn");
        if (id == null && urn == null) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERRTXT_ILLEGAL_IDENTIFIER + ": " + id);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            return;
        }
        if (id != null && !PIValidator.validatePi(id)) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERRTXT_ILLEGAL_IDENTIFIER + ": " + id);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            return;
        }

        String query = null;
        if (id != null) {
            query = SolrConstants.PI + ":\"" + id + '"';
        } else {
            query = SolrConstants.URN + ":\"" + urn + '"';
        }
        SolrDocumentList hits = null;
        try {
            hits = DataManager.getInstance().getSearchIndex().search(query, Arrays.asList(FIELDS));
        } catch (PresentationException | IndexUnreachableException e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
            return;
        }
        if (hits == null || hits.isEmpty()) {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_DOC_NOT_FOUND);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            return;
        }
        if (hits.getNumFound() > 1) {
            // show multiple match, that indicates corrupted index
            try {
                response.sendError(HttpServletResponse.SC_CONFLICT, ERRTXT_MULTIMATCH);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            return;
        }

        SolrDocument doc = hits.get(0);
        id = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);

        // If the user has no listing privilege for this record, act as if it does not exist
        boolean access = false;
        try {
            access =
                    AccessConditionUtils.checkAccessPermissionBySolrDoc(doc, query, IPrivilegeHolder.PRIV_DOWNLOAD_METADATA, request).isGranted();
        } catch (IndexUnreachableException | DAOException e) {
            logger.error(e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
            return;
        }
        if (!access) {
            logger.debug("User may not download metadata for {}", id);
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_DOC_NOT_FOUND);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            return;
        }

        String format = (String) doc.getFieldValue(SolrConstants.SOURCEDOCFORMAT);
        String dataRepository = (String) doc.getFieldValue(SolrConstants.DATAREPOSITORY);

        String filePath =
                DataFileTools.getSourceFilePath(id + ".xml", dataRepository,
                        format != null ? format.toUpperCase() : SolrConstants.SOURCEDOCFORMAT_METS);

        response.setContentType(StringConstants.MIMETYPE_TEXT_XML);
        File file = new File(filePath);
        response.setHeader("Content-Disposition", "filename=\"" + file.getName() + "\"");
        try (FileInputStream fis = new FileInputStream(file); ServletOutputStream out = response.getOutputStream()) {
            int bytesRead = 0;
            byte[] byteArray = new byte[300];
            try {
                while ((bytesRead = fis.read(byteArray)) != -1) {
                    out.write(byteArray, 0, bytesRead);
                }
                out.flush();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        } catch (FileNotFoundException e) {
            logger.debug(e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + file.getName());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
        }
    }
}
