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
package de.intranda.digiverso.presentation.filters;

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

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * Filter class for redirecting from protected pages that either require a user login or superuser privileges to the login page.
 */
public class LoginFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoginFilter.class);

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
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
        //        PrettyContext prettyContext = PrettyContext.getCurrentInstance(httpRequest);
        if (isRestrictedUri(httpRequest.getRequestURI())) {
            String requestURI = httpRequest.getRequestURI();
            // Use Pretty URL, if available
            PrettyContext prettyContext = PrettyContext.getCurrentInstance(httpRequest);
            if (prettyContext != null && prettyContext.getRequestURL() != null) {
                requestURI = ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + prettyContext.getRequestURL().toURL();
            }
            User user = (User) httpRequest.getSession().getAttribute("user");
            if (user == null) {
                logger.debug("No user found, redirecting to login...");
                ((HttpServletResponse) response).sendRedirect(ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + "/user/?from="
                        + URLEncoder.encode(requestURI, "UTF-8"));
            } else if (httpRequest.getRequestURI().contains("/admin") && !user.isSuperuser()) {
                logger.debug("User '" + user.getEmail() + "' not superuser, redirecting to login...");
                ((HttpServletResponse) response).sendRedirect(ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + "/user/?from="
                        + URLEncoder.encode(requestURI, "UTF-8"));
            } else {
                chain.doFilter(request, response); // continue
            }
        } else {
            chain.doFilter(request, response); // continue
        }
    }


    /**
     * Checks whether the given URI requires a logged in user.
     *
     * @param uri
     * @return true if restricted; false otherwise.
     * @should return true for certain pretty uris
     * @should return true for crowdsourcing uris
     * @should return false for crowdsourcing about page
     * @should return true for admin uris
     * @should return true for user backend uris
     * @should return true for bookshelf uris
     */
    public static boolean isRestrictedUri(String uri) {
        if (uri != null) {
            //            logger.trace("URL: {}", uri);
            switch (uri) {
                case "/myactivity/":
                case "/mysearches/":
                case "/mybookshelves/":
                case "/otherbookshelves/":
                case "/bookshelf/":
                case "/editbookshelf/":
                    return true;
                default:
                    // Regular URLs
                    if ((uri.contains("/crowd") && !(uri.contains("about")) || uri.contains("/admin") || uri.contains("/userBackend") || uri.contains(
                            "/bookshel"))) {
                        return true;
                    }
            }

        }

        return false;
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }
}
