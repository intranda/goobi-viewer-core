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
import java.util.List;

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

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.servlets.utils.UrlRedirectUtils;

/**
 * Encodes responses into UTF-8 and prevents proxy caching.
 */
public class UrlRedirectFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UrlRedirectFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getServletPath();
        path = StringUtils.removeStart(path, "/");
        path = StringUtils.removeEnd(path, "/");
        if(!path.startsWith("index.xhtml") & !path.startsWith("resources") && path.endsWith("index.xhtml")) {
            path = StringUtils.remove(path, "/index.xhtml");
            String urlSuffix = "";
            if(path.contains("/search/")) {                
                urlSuffix = path.substring(path.indexOf("/search/"));
                path = path.substring(0, path.indexOf("/search/"));
            }
           logger.trace("Attempting to find cms page for " + path);
            try {
                List<CMSPage> cmsPages = DataManager.getInstance().getDao().getAllCMSPages();
                for (CMSPage cmsPage : cmsPages) {
                    if(path.equalsIgnoreCase(cmsPage.getPersistentUrl())) {                        
                        String redirectPath = ("/" + cmsPage.getRelativeUrlPath(false) + urlSuffix).replace("//", "/");
                        logger.debug("Forwarding " + path + " to " + redirectPath);
                        RequestDispatcher d=request.getRequestDispatcher(redirectPath);
                        d.forward(request, response);
                        return;
                    }
                    
                }
            } catch (DAOException e) {
                throw new ServletException(e);
            }
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