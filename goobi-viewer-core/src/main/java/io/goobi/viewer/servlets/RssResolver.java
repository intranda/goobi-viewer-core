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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetAction;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.rss.RSSFeed;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Servlet implementation class RssResolver
 *
 * TODO: Removed deprecation marker because this servlet is still required for delivering RSS feeds for sidebar widget. The alternative, the rss
 * REST-resource delivers a json which need to be parsed and turned into html
 */
public class RssResolver extends HttpServlet {
    private static final long serialVersionUID = -8188360280492927624L;

    private static final Logger logger = LogManager.getLogger(RssResolver.class);

    private static final String PARAM_FILTERQUERY = "filterQuery";
    private static final String PARAM_LANGUAGE = "language";

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
        if (request.getParameterMap().get(PARAM_LANGUAGE) != null && request.getParameterMap().get(PARAM_LANGUAGE).length > 0) {
            language = request.getParameterMap().get(PARAM_LANGUAGE)[0];
        }
        int maxHits;
        if (request.getParameterMap().get("max") != null && request.getParameterMap().get("max").length > 0) {
            try {
                maxHits = Integer.parseInt(request.getParameterMap().get("max")[0]);
            } catch (NumberFormatException e) {
                try {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                } catch (IOException e1) {
                    logger.error(e1.getMessage());
                }
                return;
            }
        } else {
            maxHits = DataManager.getInstance().getConfiguration().getRssFeedItems();
        }

        String filterQuery = "";
        if (request.getParameterMap().get(PARAM_FILTERQUERY) != null && request.getParameterMap().get(PARAM_FILTERQUERY).length > 0) {
            filterQuery = request.getParameterMap().get(PARAM_FILTERQUERY)[0];
        }
        // logger.trace("RSS request filter query: {}", filterQuery);

        Long bookshelfId = null;
        if (request.getParameterMap().get("bookshelfId") != null) {
            try {
                bookshelfId = Long.valueOf(request.getParameterMap().get("bookshelfId")[0]);
            } catch (NumberFormatException e) {
                logger.warn("Received invalid bookshelf ID: {}", bookshelfId);
            }
        }
        try {
            response.setContentType(StringConstants.MIMETYPE_TEXT_XML);
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

            // logger.trace("RSS query: {}", query);
            if (StringUtils.isNotEmpty(query)) {
                SyndFeedOutput output = new SyndFeedOutput();
                output.output(
                        RSSFeed.createRss(ServletUtils.getServletPathWithHostAsUrlFromRequest(request),
                                query + SearchHelper.getAllSuffixes(request, true, true),
                                Collections.singletonList(filterQuery), language, maxHits, null, true),
                        new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8.name()));
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Insufficient parameters");
            }
        } catch (IOException e) {
            if (GetAction.isClientAbort(e)) {
                //let them
            } else {
                logger.error(e.getMessage(), e);
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                } catch (IOException e1) {
                    logger.error(e1.getMessage());
                }
            }
        } catch (FeedException e) {
            logger.error(e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
        } catch (DAOException e) {
            logger.debug("DAOException thrown here: {}", e.getMessage());
            //            response.isCommitted()
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
        } catch (ViewerConfigurationException e) {
            logger.error(e.getMessage());
        }

    }
}
