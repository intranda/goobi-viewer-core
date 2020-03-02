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
package io.goobi.viewer.filters;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyContext;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * Filter class for redirecting from protected pages that either require a user login or superuser privileges to the login page.
 */
public class LoginFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoginFilter.class);

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    /** {@inheritDoc} */
    @Override
    public void destroy() {
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    /** {@inheritDoc} */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // Create new personal filter query suffix        

        if (httpRequest.getSession().getAttribute(SearchHelper.PARAM_NAME_FILTER_QUERY_SUFFIX) == null) {
            try {
                SearchHelper.updateFilterQuerySuffix(httpRequest);
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                //                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                //                return;
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
                //                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                //                return;
            } catch (DAOException e) {
                logger.debug("DAOException thrown here: {}", e.getMessage());
                //                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                //                httpResponse.sendRedirect("/viewer/error/");
                //                return;
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
        User user = (User) httpRequest.getSession().getAttribute("user");
        if (user == null) {
            logger.debug("No user found, redirecting to login...");
            ((HttpServletResponse) response).sendRedirect(
                    ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + "/login/?from=" + URLEncoder.encode(fullRequestURI, "UTF-8"));
        } else if (httpRequest.getRequestURI().contains("/admin")) {
            try {
                if (user.isSuperuser() || user.isHasCmsPrivilege(IPrivilegeHolder.PRIV_CMS_PAGES)) {
                    chain.doFilter(request, response); // continue
                    return;
                }
                logger.debug("User '{}' not superuser, redirecting to login...", user.getDisplayName());
            } catch (PresentationException e) {
                logger.error(e.getMessage(), e);
            } catch (IndexUnreachableException e) {
                logger.error(e.getMessage(), e);
            } catch (DAOException e) {
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
     */
    public static boolean isRestrictedUri(String uri) {
        if (uri == null) {
            return false;
        }

        if (uri.matches("/?viewer/.*")) {
            uri = uri.replaceAll("/?viewer/", "/");
        }
        logger.trace("uri: {}", uri);
        switch (uri) {
            case "/myactivity/":
            case "/mysearches/":
                return true;
            default:
                // Allow activation URLs
                if (uri.startsWith("/user/activate/")) {
                    return false;
                }
                // any URIs starting with /user/ are supposed to be only accessible to logged in users
                if (uri.startsWith("/user/")) {
                    return true;
                }

                //any URIs leading to campaign annotation/review
                if (uri.matches(".*/campaigns/\\d+/(review|annotate)/.*")) {
                    return true;
                }

                //make an exception for session bookmarks search list or share key
                if (uri.contains("bookmarks/search/") || uri.contains("bookmarks/session/") || uri.contains("bookmarks/key/")
                        || uri.contains("bookmarks/send/") || uri.contains("bookmarks/search/session")) {
                    return false;
                }
                // Regular URLs
                if ((uri.contains("/crowd") && !(uri.contains("about")) || uri.contains("/admin") || uri.contains("/userBackend"))) {
                    return true;
                }
        }

        return false;
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    /** {@inheritDoc} */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }
}
