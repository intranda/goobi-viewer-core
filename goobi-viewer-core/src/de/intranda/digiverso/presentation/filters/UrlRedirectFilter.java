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

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyContext;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.servlets.utils.CombinedPath;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;
import de.intranda.digiverso.presentation.servlets.utils.UrlRedirectUtils;
import jdk.nashorn.internal.runtime.Context.ThrowErrorManager;

/**
 * Encodes responses into UTF-8 and prevents proxy caching.
 */
public class UrlRedirectFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UrlRedirectFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            DispatcherType dispatcherType = httpRequest.getDispatcherType();
            //            try {                
            //                PrettyContext context = PrettyContext.getCurrentInstance(httpRequest);
            //                System.out.println("Request url = " + context.getRequestURL().toURL());
            //            } catch(Throwable e) {
            //                
            //            }

            Optional<CombinedPath> currentPath = getCombinedPath(httpRequest);
            if (DispatcherType.REQUEST.equals(httpRequest.getDispatcherType())) {
                if (currentPath.isPresent()) {
                    UrlRedirectUtils.setCurrentView(currentPath.get(), httpRequest.getSession());
                    if (!currentPath.get().getPagePath().startsWith("cms/") && currentPath.get().getCmsPage() != null) {
                        CombinedPath cmsPagePath = new CombinedPath(currentPath.get());
                        cmsPagePath.setPagePath(Paths.get(currentPath.get().getCmsPage().getRelativeUrlPath(false)));
                        logger.debug("Forwarding " + currentPath.get().toString() + " to " + cmsPagePath.getCombinedUrl());
                        RequestDispatcher d = request.getRequestDispatcher(cmsPagePath.getCombinedUrl());
                        d.forward(request, response);
                        return;
                    } else if (currentPath.get().getCmsPage() != null) {
                        //path starts with 'cms'
                        //                        PrettyContext prettyContext = PrettyContext.getCurrentInstance(httpRequest);
                        //                        String prettyRequestUrl = prettyContext.getRequestURL().toString();
                        //                        String prettyContextPath = prettyContext.getContextPath();
                        //                        FacesContext facesContext = FacesContext.getCurrentInstance();
                        //                        CombinedPath cmsPagePath = new CombinedPath(
                        //                                currentPath.get().getHostUrl(), 
                        //                                Paths.get(currentPath.get().getCmsPage().getRelativeUrlPath(false)), 
                        //                                currentPath.get().getParameterPath());
                        //                        cmsPagePath.setCmsPage(currentPath.get().getCmsPage());
                        ////                        ExternalContext externalContext = facesContext.getExternalContext();
                        //                        String redirectUrl = cmsPagePath.getHostUrl() + cmsPagePath.getCombinedPrettyfiedUrl();
                        ////                        httpResponse.sendRedirect(redirectUrl);
                        ////                        externalContext.redirect(redirectUrl);
                        //                        final HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(httpRequest) {
                        //                            @Override
                        //                            public StringBuffer getRequestURL() {
                        //                                final StringBuffer originalUrl = ((HttpServletRequest) getRequest()).getRequestURL();
                        //                                return new StringBuffer(redirectUrl);
                        //                            }
                        //                        };
                        //                        request = wrapped;
                    }

                    //                    if (currentPath.get().isPage() && currentPath.get().getPageType().isHandledWithCms()) {
                    //                        Optional<CMSPage> oCmsPage = DataManager.getInstance().getDao().getAllCMSPages().stream().filter(page -> StringUtils
                    //                                .isNotBlank(page.getStaticPageName())).filter(page -> currentPath.get().getPageType().matches(page
                    //                                        .getStaticPageName())).findFirst();
                    //                        if (oCmsPage.isPresent()) {
                    //                        }
                    //                    } else if (currentPath.get().getCmsPage() != null) {
                    //                        CombinedPath cmsPagePath = new CombinedPath(currentPath.get().getHostUrl(), Paths.get(currentPath.get().getCmsPage()
                    //                                .getRelativeUrlPath(false)), currentPath.get().getParameterPath());
                    //
                    //                        logger.debug("Forwarding " + currentPath.get().toString() + " to " + cmsPagePath.getCombinedUrl());
                    //                        RequestDispatcher d = request.getRequestDispatcher(cmsPagePath.getCombinedUrl());
                    //                        d.forward(request, response);
                    //                        return;
                    //                    }
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
        String serverName = httpRequest.getContextPath();
        PrettyContext context = PrettyContext.getCurrentInstance(httpRequest);
        if (!serviceUrl.contains("/cms/") && context != null && context.getRequestURL() != null) {
            serviceUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + ("/".equals(context.getRequestURL().toURL()) ? "/index"
                    : context.getRequestURL().toURL());
        }
        serviceUrl = serviceUrl.replaceAll("\\/index\\.xhtml", "/");
        return UrlRedirectUtils.getCombinedUrl(serverUrl, serverName, serviceUrl, false);

    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

}