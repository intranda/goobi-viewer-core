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

                    Optional<CombinedPath> oCurrentPath = getCombinedUrl(hostUrl, serviceUrl, true);
                    if(oCurrentPath.isPresent()) {
                        //viewer page url
                        CombinedPath currentPath = oCurrentPath.get();
                        if(!isIgnoredView(currentPath.getPagePath())) {
                            CombinedPath previousPath = (CombinedPath) session.getAttribute(CURRENT_URL);
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
     * Create a combined path from the given url. If the url leads to a known PageType, associates the PageType with the combined path.
     * If the path matches the alternative ('persistent') url of a cmsPage, this cmsPage is associated with the path
     * 
     * @param hostUrl       The absolute path of the servlet including the host part ('viewer')
     * @param serviceUrl    The concrete requested url, optionally including the hostUrl 
     * @return
     * @throws DAOException
     */
    public static Optional<CombinedPath> getCombinedUrl(String hostUrl, String serviceUrl, boolean usePretty) throws DAOException {
        serviceUrl = serviceUrl.replace(hostUrl, "").replaceAll("^\\/", ""); 
        final Path servicePath = getServicePath(serviceUrl, usePretty);
        
        CombinedPath currentPath = new CombinedPath();
        currentPath.setHostUrl(hostUrl);
        
        if(servicePath.startsWith("cms") && servicePath.getName(1).toString().matches("\\d+")) {
            currentPath.setPagePath(servicePath.subpath(0, 2));
            currentPath.setParameterPath(currentPath.getPagePath().relativize(servicePath));
        } else {
            Optional<PageType> pageType = getPageType(servicePath);
            if(pageType.isPresent()) {
                currentPath.setPagePath(Paths.get(pageType.get().getName()));
                currentPath.setParameterPath(currentPath.getPagePath().relativize(servicePath));
                currentPath.setPageType(pageType.get());
            } else {
                Optional<CMSPage> cmsPage = getCmsPage(servicePath);
                if(cmsPage.isPresent()) {
                    currentPath.setPagePath(Paths.get(cmsPage.get().getPersistentUrl()));
                    currentPath.setParameterPath(currentPath.getPagePath().relativize(servicePath));
                    currentPath.setCmsPage(cmsPage.get());
                }
            }
        }
        if(StringUtils.isNotBlank(currentPath.getPagePath().toString())) {            
            return Optional.of(currentPath);
        }
        return Optional.empty();
    }

    /**
     * Gets the best matching CMSPage which alternative url ('persistent url') matches the beginning of the given path
     * 
     * @param servicePath
     * @return
     * @throws DAOException 
     */
    public static Optional<CMSPage> getCmsPage(Path servicePath) throws DAOException {
        return DataManager.getInstance().getDao().getAllCMSPages().stream()
                .filter(cmsPage -> StringUtils.isNotBlank(cmsPage.getPersistentUrl()))
                .filter(page -> servicePath.startsWith(page.getPersistentUrl().replaceAll("^\\/|\\/$", "")))
                .sorted((page1, page2) -> Integer.compare(page1.getPersistentUrl().length(), page2.getPersistentUrl().length()))
                .findFirst();
    }
    
    /**
     * @param servicePath
     * @return
     * @throws DAOException 
     */
    public static Optional<String> getCmsUrlPath(Path servicePath) throws DAOException {
        return DataManager.getInstance().getDao().getAllCMSPages().stream()
                .filter(cmsPage -> StringUtils.isNotBlank(cmsPage.getPersistentUrl()))
                .map(CMSPage::getPersistentUrl)
                .map(url -> url.replaceAll("^\\/|\\/$", ""))    //remove leading and trailing slashes
                .filter(url -> servicePath.startsWith(url))
                .sorted((url1, url2) -> Integer.compare(url1.length(), url2.length()))
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
        .filter(name -> servicePath.startsWith(name))
        .sorted((name1, name2) -> Integer.compare(name1.length(), name2.length()))
        .findFirst();
        return pageNameOfType;
    }
    
    /**
     * @param servicePath
     * @return
     */
    public static Optional<PageType> getPageType(final Path servicePath) {
        Optional<PageType> pageNameOfType = Arrays.stream(PageType.values())
        .filter(type -> type.matches(servicePath))
        .sorted((type1, type2) -> Integer.compare(type1.getName().length(), type2.getName().length()))
        .findFirst();
        return pageNameOfType;
    }

    /**
     * @param serviceUrl
     * @return
     * @throws DAOException
     */
    private static Path getServicePath(String serviceUrl, boolean usePretty) throws DAOException {
        Path servicePath = Paths.get(serviceUrl);
        if (usePretty && servicePath.getName(0).toString().equals("cms")) {
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
