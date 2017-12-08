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
import java.nio.file.Path;
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

import com.ocpsoft.pretty.PrettyContext;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.servlets.utils.CombinedPath;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;
import de.intranda.digiverso.presentation.servlets.utils.UrlRedirectUtils;

/**
 * Encodes responses into UTF-8 and prevents proxy caching.
 */
public class UrlRedirectFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UrlRedirectFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            Optional<CombinedPath> currentPath = getCombinedPath(httpRequest);
            if (currentPath.isPresent() && currentPath.get().isPage()) {

                Optional<PageType> pageType = UrlRedirectUtils.getPageType(currentPath.get().getPagePath());
                if (pageType.isPresent() && pageType.get().isHandledWithCms()) {
                    Optional<CMSPage> oCmsPage = DataManager.getInstance().getDao().getAllCMSPages().stream()
                            .filter(page -> StringUtils.isNotBlank(page.getStaticPageName()))
                            .filter(page -> page.getStaticPageName().equalsIgnoreCase(pageType.get().getName()))
                            .findFirst();
                    if (oCmsPage.isPresent()) {
                        CMSPage cmsPage = oCmsPage.get();
                        CombinedPath cmsPagePath = new CombinedPath(currentPath.get().getHostUrl(), Paths.get(cmsPage.getRelativeUrlPath(false)), currentPath
                                .get().getParameterPath());
                        logger.debug("Forwarding " + currentPath.get().toString() + " to " + cmsPagePath.getCombinedUrl());
                        RequestDispatcher d = request.getRequestDispatcher(cmsPagePath.getCombinedUrl());
                        d.forward(request, response);
                        return;
                    }
                } else if (!pageType.isPresent() && currentPath.get().isPage()) {
                    Optional<CMSPage> oCmsPage = DataManager.getInstance().getDao().getAllCMSPages().stream().filter(page -> StringUtils.isNotBlank(
                            page.getPersistentUrl())).filter(page -> page.getPersistentUrl().equalsIgnoreCase(currentPath.get().getPagePath()
                                    .toString())).findFirst();
                    if (oCmsPage.isPresent()) {
                        CMSPage cmsPage = oCmsPage.get();
                        CombinedPath cmsPagePath = new CombinedPath(currentPath.get().getHostUrl(), Paths.get(cmsPage.getRelativeUrlPath(false)), currentPath
                                .get().getParameterPath());
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

    /**
     * @param httpRequest
     * @return
     * @throws DAOException
     */
    @SuppressWarnings("unused")
    private Optional<CombinedPath> getCombinedPath(HttpServletRequest httpRequest) throws DAOException {
        String serverUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest); // http://localhost:8080/viewer
        String serviceUrl = httpRequest.getServletPath(); // /resources/.../index.xhtml
//        String serviceUrl = httpRequest.getRequestURI(); // /viewer/resources/.../(index.xhtml)
        PrettyContext context = PrettyContext.getCurrentInstance(httpRequest);
        if (!serviceUrl.contains("/cms/") &&  context != null && context.getRequestURL() != null) {
            System.out.println(context.getRequestURL().toURL());
            serviceUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + ("/".equals(context.getRequestURL().toURL()) ? "/index" : context.getRequestURL().toURL());
        }
        serviceUrl = serviceUrl.replaceAll("\\/index\\.xhtml", "/");
        return UrlRedirectUtils.getCombinedUrl(serverUrl, serviceUrl);

    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

}