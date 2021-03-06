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

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.solr.SolrConstants;

/**
 * This Servlet maps a given lucene field value to a url and then either redirects there or forwards there, depending on the config.
 */
public class PpnResolver extends HttpServlet implements Serializable {

    private static final long serialVersionUID = -4508522532259964453L;

    private static final Logger logger = LoggerFactory.getLogger(PpnResolver.class);

    private static final String REQUEST_PARAM_NAME = "id";

    private static final String REQUEST_PAGE_PARAM_NAME = "page";

    // error messages

    private static final String ERRTXT_DOC_NOT_FOUND = "No matching document could be found. ";
    private static final String ERRTXT_NO_ARGUMENT =
            "You didnt not specify a source field value for the mapping. Append the value to the URL as a request parameter; expected param name is :";
    private static final String ERRTXT_ILLEGAL_IDENTIFIER = "Illegal identifier";
    private static final String ERRTXT_MULTIMATCH = "Multiple documents matched the search query. No unambiguous mapping possible.";
    private static final String ERRTXT_ILLEGAL_PAGE_NUMBER = "Illegal page number";

    /**
     * {@inheritDoc}
     *
     * For a given lucene field name parameter, this method either forwards or redirects to the target URL. The target URL is generated by inserting
     * the target lucene field into the target work url, if a document could be identified by the source field. Otherwise, a document is searched for
     * using the page field; if a document is found in this alternative way, target field and page field of the document are inserted into the target
     * page url. NOTE: If you forward, the target URL must be on the same server and must be below the context root of this servlet, e.g. this servlet
     * can not forward to a target above '/'. A redirect changes the URL displayed in the browser, a forward does not.
     * 
     * @should return 400 if record identifier missing
     * @should return 404 if record not found
     * @should return 500 if record identifier bad
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String identifier = request.getParameter(REQUEST_PARAM_NAME);
        if (identifier == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERRTXT_NO_ARGUMENT + REQUEST_PARAM_NAME);
            return;
        }
        if (!PIValidator.validatePi(identifier)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERRTXT_ILLEGAL_IDENTIFIER + ": " + identifier);
            return;
        }
        Integer page = null;
        String pageString = request.getParameter(REQUEST_PAGE_PARAM_NAME);
        if (StringUtils.isNotBlank(pageString)) {
            try {
                page = Integer.parseInt(pageString);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERRTXT_ILLEGAL_PAGE_NUMBER + ": " + identifier);
            }
        }

        // // 5. redirect or forward using the target field value
        // if (DO_REDIRECT) {
        // response.sendRedirect(result);
        // } else {
        // getServletContext().getRequestDispatcher(result).forward(request, response);
        // }

        // 3. evaluate the search
        try {
            String query = "+" + SolrConstants.PI + ":\"" + identifier + "\"" + SearchHelper.getAllSuffixes(request, false, false);
            SolrDocumentList hits = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query);
            logger.trace("Resolver query: {}", query);
            if (hits.getNumFound() == 0) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_DOC_NOT_FOUND);
                return;
            } else if (hits.getNumFound() > 1) {
                // 3.2 show multiple match, that indicates corrupted indexer
                response.sendError(HttpServletResponse.SC_CONFLICT, ERRTXT_MULTIMATCH);
                return;
            }

            // If the user has no listing privilege for this record, act as if it does not exist
            boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(identifier, null, IPrivilegeHolder.PRIV_LIST,
                    request);
            if (!access) {
                logger.debug("User may not list {}", identifier);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_DOC_NOT_FOUND);
                return;
            }

            // 4. extract the target field value of the single found document
            SolrDocument targetDoc = hits.get(0);

            String result;
            if (page == null) {
                result = IdentifierResolver.constructUrl(targetDoc, false);
            } else {
                result = IdentifierResolver.constructUrl(targetDoc, false, page);
            }
            if (DataManager.getInstance().getConfiguration().isUrnDoRedirect()) {
                response.sendRedirect(result);
            } else {
                getServletContext().getRequestDispatcher(result).forward(request, response);
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
        } catch (RecordNotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_DOC_NOT_FOUND);
            return;
        }
    }
}
