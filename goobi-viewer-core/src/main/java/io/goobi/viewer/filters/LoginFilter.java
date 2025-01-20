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
package io.goobi.viewer.filters;

import java.io.IOException;
import java.net.URLEncoder;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ocpsoft.pretty.PrettyContext;

import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.comments.CommentManager;
import io.goobi.viewer.model.crowdsourcing.CrowdsourcingTools;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * Filter class for redirecting from protected pages that either require a user login or superuser privileges to the login page.
 */
public class LoginFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(LoginFilter.class);

    /* (non-Javadoc)
     * @see jakarta.servlet.Filter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     */
    /** {@inheritDoc} */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // Create new personal filter query suffix

        if (httpRequest.getSession().getAttribute(SearchHelper.PARAM_NAME_FILTER_QUERY_SUFFIX) == null) {
            try {
                SearchHelper.updateFilterQuerySuffix(httpRequest, IPrivilegeHolder.PRIV_LIST);
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            } catch (DAOException e) {
                logger.debug("DAOException thrown here: {}", e.getMessage());
            }
        }
        PrettyContext prettyContext = PrettyContext.getCurrentInstance(httpRequest);
        String requestURI = httpRequest.getRequestURI();
        String fullRequestURI = requestURI;
        // Use Pretty URL, if available
        if (prettyContext != null && prettyContext.getRequestURL() != null) {
            requestURI = prettyContext.getRequestURL().toURL();
            fullRequestURI = ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + prettyContext.getRequestURL().toURL();
        }
        if (!isRestrictedUri(requestURI)) {
            chain.doFilter(request, response); // continue
            return;
        }

        logger.trace("request uri: {}", requestURI);
        logger.trace("Checking session ID {} for user object", httpRequest.getSession().getId());
        User user = (User) httpRequest.getSession().getAttribute("user");
        if (user == null) {
            logger.debug("No user found, redirecting to login...");
            ((HttpServletResponse) response).sendRedirect(
                    ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + "/login/?from=" + URLEncoder.encode(fullRequestURI, "UTF-8"));
        } else if (httpRequest.getRequestURI().contains("/admin")) {
            try {
                if (user.isSuperuser() || user.isHasCmsPrivilege(IPrivilegeHolder.PRIV_CMS_PAGES)
                        || CrowdsourcingTools.isUserOwnsAnyCampaigns(user) || CommentManager.isUserHasAccessToCommentGroups(user)) {
                    chain.doFilter(request, response); // continue
                    return;
                }
                logger.debug("User '{}' not superuser, redirecting to login...", user.getDisplayName());
            } catch (DAOException | IndexUnreachableException | PresentationException e) {
                logger.error(e.getMessage(), e);
            }
            ((HttpServletResponse) response).sendRedirect(
                    ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + "/login/?from=" + URLEncoder.encode(requestURI, "UTF-8"));
        } else {
            chain.doFilter(request, response); // continue
        }
    }

    /**
     * Checks whether the given URI requires a logged in user.
     *
     * @param uri a {@link java.lang.String} object.
     * @return true if restricted; false otherwise.
     * @should return true for certain pretty uris
     * @should return true for crowdsourcing uris
     * @should return false for crowdsourcing about page
     * @should return true for admin uris
     * @should return true for user backend uris
     * @should return true for user bookmarks uris
     * @should return false for bookmarks list uri
     * @should return false for bookmarks session uris
     * @should return false for bookmarks share key uris
     * @should return false for bookmarks send list uris
     * @should return false for user account activation uris
     * @should return false for user password reset uris
     */
    public static boolean isRestrictedUri(final String uri) {
        if (uri == null) {
            return false;
        }

        String localUri = uri;
        if (localUri.matches("/?viewer/.*")) {
            localUri = localUri.replaceAll("/?viewer/", "/");
        }
        logger.trace("uri: {}", localUri);
        switch (localUri.trim()) {
            case "/myactivity/":
                return true;
            default:
                // Allow activation URLs
                if (localUri.startsWith("/user/activate/")) {
                    return false;
                }
                // Password reset URLs
                if (localUri.startsWith("/user/resetpw/")) {
                    return false;
                }
                // any URIs starting with /user/ are supposed to be only accessible to logged in users
                if (localUri.startsWith("/user/")) {
                    return true;
                }

                //any URIs leading to campaign annotation/review
                if (localUri.matches(".*/campaigns/\\d+/(review|annotate)/.*")) { //NOSONAR no catastrophic backtracking detected
                    return true;
                }

                //make an exception for session bookmarks search list or share key
                if (localUri.contains("bookmarks/search/") || localUri.contains("bookmarks/session/") || localUri.contains("bookmarks/key/")
                        || localUri.contains("bookmarks/send/") || localUri.contains("bookmarks/search/session")) {
                    return false;
                }
                // Regular URLs
                if ((localUri.contains("/crowd") && !(localUri.contains("about")) || localUri.contains("/admin")
                        || localUri.contains("/userBackend"))) {
                    return true;
                }
        }

        return false;
    }
}
