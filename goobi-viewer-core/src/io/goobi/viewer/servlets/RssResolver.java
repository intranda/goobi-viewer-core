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
import java.io.OutputStreamWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.rss.RSSFeed;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * Servlet implementation class RssResolver
 */
@Deprecated
public class RssResolver extends HttpServlet {
    private static final long serialVersionUID = -8188360280492927624L;

    private static final Logger logger = LoggerFactory.getLogger(RssResolver.class);

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String query = null;
        if (request.getParameterMap().get("q") != null) {
            query = "(" + request.getParameterMap().get("q")[0] + ")";
        }
        String language = "de";
        if (request.getParameterMap().get("lang") != null && request.getParameterMap().get("lang").length > 0) {
            language = request.getParameterMap().get("lang")[0];
        }
        if (request.getParameterMap().get("language") != null && request.getParameterMap().get("language").length > 0) {
            language = request.getParameterMap().get("language")[0];
        }
        logger.trace("RSS request language: {}", language);
        Long bookshelfId = null;
        if (request.getParameterMap().get("bookshelfId") != null) {
            try {
                bookshelfId = Long.valueOf(request.getParameterMap().get("bookshelfId")[0]);
            } catch (NumberFormatException e) {
                logger.warn("Received invalid bookshelf ID: {}", bookshelfId);
            }
        }
        try {
            response.setContentType("text/xml");
            // Build query
            if (StringUtils.isEmpty(query)) {
                if (bookshelfId != null) {
                    // Bookshelf RSS feed
                    BookmarkList bookshelf = DataManager.getInstance().getDao().getBookmarkList(bookshelfId);
                    if (bookshelf == null) {
                        logger.warn("Requested bookshelf not found: {}", bookshelfId);
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    if (!bookshelf.isIsPublic()) {
                        logger.warn("Requested bookshelf not public: {}", bookshelfId);
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    query = bookshelf.generateSolrQueryForItems();
                } else {
                    // Main RSS feed
                    StringBuilder sbQuery = new StringBuilder();
                    sbQuery.append(SolrConstants.ISWORK).append(":true");
                    query = sbQuery.toString();
                }
            }

            logger.trace("RSS query: {}", query);
            if (StringUtils.isNotEmpty(query)) {
                SyndFeedOutput output = new SyndFeedOutput();
                output.output(
                        RSSFeed.createRss(ServletUtils.getServletPathWithHostAsUrlFromRequest(request),
                                query + SearchHelper.getAllSuffixes(request, true, true,
                                        DataManager.getInstance().getConfiguration().isSubthemeAddFilterQuery()),
                                null, language),
                        new OutputStreamWriter(response.getOutputStream(), "utf-8"));
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Insufficient parameters");
                return;
            }
        } catch (ClientAbortException e) {
            //let them
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        } catch (FeedException e) {
            logger.error(e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
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
            //            response.isCommitted()
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        } catch (ViewerConfigurationException e) {
            logger.error(e.getMessage());
            return;
        }

    }

    /** {@inheritDoc} */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
