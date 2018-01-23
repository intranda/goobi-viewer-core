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
import java.nio.file.Paths;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.urlresolution.ViewHistory;
import de.intranda.digiverso.presentation.model.urlresolution.ViewerPath;
import de.intranda.digiverso.presentation.model.urlresolution.ViewerPathBuilder;

/**
 * Filter for redirecting prettified calls to cmsPages 
 * <p>
 * Forwarding is handled by {@link RequestDispatcher#forward(ServletRequest, ServletResponse)}, so the url displayed to the user doesn't change, 
 * but the internal handling of the request is according to the forwarded url
 * </p>
 * <p>
 * 'prettified' in this context refers to calling CMSPages by either their 'alternative url' or the url of the static page they replace.
 * </p>
 * <p>
 * This filter needs to be placed in the filter chain before the {@link com.ocpsoft.pretty.PrettyFilter PrettyFilter} because the PrettyFilter
 * needs to handle the actual CMSPage mapping (the PrettyFilter won't handle the request if it has been called already for this request, despite the forward)
 * </p>
 * <p>
 * This filter also stores the called url to the session map using {@link ViewHistory#setCurrentView(ViewerPath, HttpSession)}. 
 * This is essential to leaving a view to return to a previous view (for example when leaving the reading mode)
 * </p>
 */
public class UrlRedirectFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UrlRedirectFilter.class);

    /**
     * Redirects prettified calls to cmsPages (either using alternative url or static url of a cmsPage) to the actual page url 
     * (The cmsPage pretty-url that is)
     * Also stores the actually requested path in the current http session using 
     * {@link ViewHistory#setCurrentView(ViewerPath, HttpSession)}
     * 
     * @throws IOException          If forwarding fails due to IOException
     * @throws ServletException     If the path could not be evaluated due to a DAOException or if forwarding fails due to ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            Optional<ViewerPath> currentPath = ViewerPathBuilder.createPath(httpRequest);
            
                if (currentPath.isPresent()) {
                    ViewHistory.setCurrentView(currentPath.get(), httpRequest.getSession());
                    if (!currentPath.get().getPagePath().startsWith("cms") && currentPath.get().getCmsPage() != null) {
                        if(currentPath.get().getCmsPage().mayContainURLParameters() || StringUtils.isBlank(currentPath.get().getParameterPath().toString().replaceAll("/?\\d+/?", ""))) {
                            ViewerPath cmsPagePath = new ViewerPath(currentPath.get());
                            cmsPagePath.setPagePath(Paths.get(currentPath.get().getCmsPage().getRelativeUrlPath(false)));
                            logger.debug("Forwarding " + currentPath.get().toString() + " to " + cmsPagePath.getCombinedUrl());
                            RequestDispatcher d = request.getRequestDispatcher(cmsPagePath.getCombinedUrl());
                            d.forward(request, response);
                            return;
                        }
                    }
                }
        } catch (DAOException e) {
            throw new ServletException(e);
        }

        chain.doFilter(request, response);
    }


    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

}