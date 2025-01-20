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
import java.net.URI;
import java.util.Optional;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.urlresolution.ViewerPath;
import io.goobi.viewer.model.urlresolution.ViewerPathBuilder;

/**
 * Filter for redirecting prettified calls to cmsPages.
 * <p>
 * Forwarding is handled by {@link jakarta.servlet.RequestDispatcher#forward(ServletRequest, ServletResponse)}, so the url displayed to the user
 * doesn't change, but the internal handling of the request is according to the forwarded url
 * </p>
 * <p>
 * 'prettified' in this context refers to calling CMSPages by either their 'alternative url' or the url of the static page they replace.
 * </p>
 * <p>
 * This filter needs to be placed in the filter chain before the {@link org.ocpsoft.rewrite.servlet.RewriteFilter} because the RewriteFilter (former
 * PrettyFilter) needs to handle the actual CMSPage mapping (the PrettyFilter won't handle the request if it has been called already for this request,
 * despite the forward)
 * </p>
 * <p>
 * This filter also stores the called url to the session map using
 * {@link io.goobi.viewer.model.urlresolution.ViewHistory#setCurrentView(ViewerPath, HttpSession)}. This is essential to leaving a view to return to a
 * previous view (for example when leaving the reading mode)
 * </p>
 */
public class UrlRedirectFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(UrlRedirectFilter.class);

    /**
     * {@inheritDoc}
     *
     * Redirects prettified calls to cmsPages (either using alternative url or static url of a cmsPage) to the actual page url (The cmsPage pretty-url
     * that is) Also stores the actually requested path in the current http session using {@link ViewHistory#setCurrentView(ViewerPath, HttpSession)}
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // logger.trace("doFilter"); //NOSONAR Debug
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            // Important: If prefetching requests are not refused here, the status of the backend beans (ActiveDocumentBean in particular)
            // will point to the prefetched page rather than the actual current page
            if (isPrefetchingRequest(httpRequest)) {
                return;
            }

            Optional<ViewerPath> currentPath = ViewerPathBuilder.createPath(httpRequest);
            if (currentPath.isPresent()) {
                logger.trace("currentPath: {}", currentPath.get());

                ViewHistory.setCurrentView(currentPath.get(), httpRequest.getSession());
                if (!ViewerPathBuilder.startsWith(currentPath.get().getPagePath(), "cms") && currentPath.get().getCmsPage() != null) {
                    if (currentPath.get().getCmsPage().isMayContainUrlParameters()
                            || StringUtils.isBlank(currentPath.get().getParameterPath().toString().replaceAll("/?\\d+/?", ""))) {
                        ViewerPath cmsPagePath = new ViewerPath(currentPath.get());
                        cmsPagePath.setPagePath(URI.create(currentPath.get().getCmsPage().getRelativeUrlPath(false)));
                        logger.debug("Forwarding {} to {}", currentPath.get(), cmsPagePath.getCombinedUrl());
                        RequestDispatcher d = request.getRequestDispatcher(cmsPagePath.getCombinedUrl());
                        d.forward(request, response);
                        return;
                    }
                } else if (!ViewerPathBuilder.startsWith(currentPath.get().getPagePath(), "campaigns") && currentPath.get().getCampaign() != null) {
                    ViewerPath cmsPagePath = new ViewerPath(currentPath.get());
                    cmsPagePath.setPagePath(URI.create(currentPath.get().getCmsPage().getRelativeUrlPath(false)));
                    logger.debug("Forwarding {} to {}", currentPath.get(), cmsPagePath.getCombinedUrl());
                    RequestDispatcher d = request.getRequestDispatcher(cmsPagePath.getCombinedUrl());
                    d.forward(request, response);
                    return;
                }
            }
        } catch (DAOException e) {
            throw new ServletException(e);
        }

        chain.doFilter(request, response);
    }

    /**
     * Firefox browser tries to precache all urls in links with rel="next" or rel="prefetch". This changes the session state and thus shall not pass
     * Fortunately Firefox marks all precaching-request with a X-Moz : prefetch header
     * (https://developer.mozilla.org/en-US/docs/Web/HTTP/Link_prefetching_FAQ) However this header is not standardized and may change in the future
     *
     * @param httpRequest
     * @return true if X-Moz:prefetch or sec-purpose:prefetch; false otherwise
     */
    private static boolean isPrefetchingRequest(HttpServletRequest httpRequest) {

        String xmoz = httpRequest.getHeader("X-Moz");
        if (xmoz == null) {
            xmoz = httpRequest.getHeader("X-moz");
        }
        if (xmoz != null && xmoz.equalsIgnoreCase("prefetch")) {
            logger.trace("Refuse prefetch request");
            return true;
        }

        String purpose = httpRequest.getHeader("sec-purpose");
        if (purpose != null && "prefetch".equalsIgnoreCase(purpose)) {
            logger.trace("Refuse prefetch request");
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
        //
    }
}
