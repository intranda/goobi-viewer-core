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
package de.intranda.digiverso.presentation.servlets.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyContext;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.viewer.PageType;

/**
 * @author Florian Alpers
 *
 */
public class UrlRedirectUtils {

    private static Logger logger = LoggerFactory.getLogger(UrlRedirectUtils.class);

    private static final String PREVIOUS_URL = "previousURL";
    private static final String CURRENT_URL = "currentURL";

    private static final PageType[] IGNORED_VIEWS = new PageType[] { PageType.viewFullscreen };

    /**
     * Saves the current view url to the session map Also saves the previous view url to the session map if it represents a different view than the
     * current view
     * 
     * @param request
     * @throws DAOException
     */
    public synchronized static void setCurrentView(final ServletRequest request) {

        try {
            if (request != null) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpSession session = httpRequest.getSession();

                if (session != null) {
                    // http://localhost:8080/viewer/ || http://localhost:8080/
                    String hostUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest);

                    String serviceUrl = ((HttpServletRequest) request).getRequestURI();
                    PrettyContext context = PrettyContext.getCurrentInstance(httpRequest);
                    if (context != null && context.getRequestURL() != null) {
                        serviceUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + context.getRequestURL().toURL();
                    }

                    serviceUrl = serviceUrl.replace(hostUrl, "").replaceAll("^\\/", ""); 
                    final Path servicePath = getServicePath(serviceUrl);
                    
                    Path pagePath = null;
                    Path paramsPath = null;
                    
                    
                    if(servicePath.startsWith("cms") && servicePath.getName(1).toString().matches("\\d+")) {
                        pagePath = servicePath.subpath(0, 2);
                        paramsPath = pagePath.relativize(servicePath);
                    } else {
                        Optional<String> pageNameOfType = getPageTypePath(servicePath);
                        if(pageNameOfType.isPresent()) {
                            pagePath = Paths.get(pageNameOfType.get());
                            paramsPath = pagePath.relativize(servicePath);
                        } else {
                            Optional<String> pageNameOfCmsUrl = getCmsUrlPath(servicePath);
                            if(pageNameOfCmsUrl.isPresent()) {
                                pagePath = Paths.get(pageNameOfCmsUrl.get());
                                paramsPath = pagePath.relativize(servicePath);
                            }
                        }
                    }
                    if(pagePath != null) {
                        //viewer page url
                        if(!isIgnoredView(pagePath)) {
                            CombinedPath previousPath = (CombinedPath) session.getAttribute(CURRENT_URL);
                            CombinedPath currentPath = new CombinedPath(hostUrl, pagePath, paramsPath);
                            session.setAttribute(CURRENT_URL, currentPath);
                            logger.trace("Set session attribute {} to {}", CURRENT_URL, currentPath);
                            if(previousPath != null && !currentPath.getPagePath().equals(previousPath.getPagePath())) {
                                //different page
                                session.setAttribute(PREVIOUS_URL, previousPath);
                                logger.trace("Set session attribute {} to {}", PREVIOUS_URL, previousPath);
                            }
                        }
                    } else {
                        //some other url
                        return;
                    }
                }
            }
        } catch (Throwable e) {
            //catch all throwables to avoid constant redirects to error
            logger.error("Error saving page url", e);
        }
    }

    /**
     * @param servicePath
     * @return
     * @throws DAOException 
     */
    private static Optional<String> getCmsUrlPath(Path servicePath) throws DAOException {
        return DataManager.getInstance().getDao().getAllCMSPages().stream()
                .filter(cmsPage -> StringUtils.isNotBlank(cmsPage.getPersistentUrl()))
                .map(CMSPage::getPersistentUrl)
                .map(url -> url.replaceAll("^\\/|\\/$", ""))    //remove leading and trailing slashes
                .filter(url -> servicePath.startsWith(url))
                .findFirst();
    }

    /**
     * @param servicePath
     * @return
     */
    public static Optional<String> getPageTypePath(final Path servicePath) {
        Optional<String> pageNameOfType = Arrays.stream(PageType.values())
        .map(type -> type.getName())
        .map(name -> name.replaceAll("^\\/|\\/$", ""))    //remove leading and trailing slashes
//        .peek(name -> System.out.println("Found page type name " + name))
        .filter(name -> servicePath.startsWith(name))
        .findAny();
        return pageNameOfType;
    }

    /**
     * @param serviceUrl
     * @return
     * @throws DAOException
     */
    private static Path getServicePath(String serviceUrl) throws DAOException {
        Path servicePath = Paths.get(serviceUrl);
        if (servicePath.getName(0).toString().equals("cms")) {
            //cms page
            final String cmsPageId = servicePath.getName(1).toString();
            if (StringUtils.isNotBlank(cmsPageId)) {
                servicePath = getCmsAlternativePath(cmsPageId, servicePath.toString());
            }
        }
        return servicePath;
    }

    /**
     * @param servletUrl
     * @param cmsPageId
     * @param tempUrl
     * @return
     * @throws DAOException
     */
    public static Path getCmsAlternativePath(final String cmsPageId, final String tempUrl) throws DAOException {
        String serviceUrl = DataManager.getInstance().getDao().getAllCMSPages().stream()
                .filter(cmsPage -> cmsPage.getId().toString().equals(cmsPageId))
                .filter(cmsPage -> StringUtils.isNotBlank(cmsPage.getPersistentUrl()))
                .map(CMSPage::getPersistentUrl)
                .findFirst().orElseGet(() -> tempUrl);
        return Paths.get(serviceUrl);
    }

    /**
     * Returns true if the path matches one of the ignored views
     * 
     * @param previousURL
     * @return
     */
    private static boolean isIgnoredView(Path path) {
        for (PageType pageType : IGNORED_VIEWS) {
            if(path.equals(Paths.get(pageType.getName()))) {
                return true;
            }
        }
        return false;
    }

    public synchronized static String getPreviousView(ServletRequest request) {
        if (request != null) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession();

            if (session != null) {
                CombinedPath previousPath =  (CombinedPath)session.getAttribute(PREVIOUS_URL);
                if(previousPath != null) {
                    return previousPath.getCombinedUrl();
                }
            }
        }
        return "";
    }

    public synchronized static void redirectToUrl(String url) throws IOException {

        FacesContext.getCurrentInstance().getExternalContext().getFlash().setRedirect(true);
        FacesContext.getCurrentInstance().getExternalContext().redirect(url);
    }

    public synchronized static String getCurrentView(ServletRequest request) {
        if (request != null) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession();

            if (session != null) {
                return (String) session.getAttribute(CURRENT_URL);
            }
        }
        return "";
    }

}
