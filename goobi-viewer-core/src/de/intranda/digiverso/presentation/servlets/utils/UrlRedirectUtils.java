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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/**
 * @author Florian Alpers
 *
 */
public class UrlRedirectUtils {

    private static Logger logger = LoggerFactory.getLogger(UrlRedirectUtils.class);

    /**
     * 
     */
    private static final String VIEW_REGEX = "https?://.*?/.*?/.*?/";
    private static final String VIEWERBASE_REGEX = "https?:\\/\\/.*?\\/[^\\/]+";
    private static final String CMSPAGE_REGEX = "https?://.*?/.*?/cms/.*";
    private static final String CMSID_REGEX = "https?://.*?/.*?/cms/(.*?)/.*";
    private static final String PREVIOUS_URL = "previousURL";
    private static final String CURRENT_URL = "currentURL";

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
                String currentURL = ((HttpServletRequest) request).getRequestURI();
                PrettyContext context = PrettyContext.getCurrentInstance(httpRequest);
                if (context != null && context.getRequestURL() != null) {
                    currentURL = ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + context.getRequestURL().toURL();
                }
                try {
                    if (currentURL.matches(CMSPAGE_REGEX)) {
                        String cmsPageId = getMatch(currentURL, CMSID_REGEX, 1);
                        if (StringUtils.isNotBlank(cmsPageId)) {
                            final String requestURL = currentURL;
                            currentURL = DataManager.getInstance().getDao().getAllCMSPages().stream()
                                    .filter(cmsPage -> cmsPage.getId().toString().equals(cmsPageId))
                                    .filter(cmsPage -> StringUtils.isNotBlank(cmsPage.getPersistentUrl()))
                                    .map(CMSPage::getPersistentUrl)
                                    .map(url -> getMatch(requestURL, VIEWERBASE_REGEX, 0) + "/" + url + "/")
                                    .findFirst()
                                    .orElseGet(() -> requestURL);
                        }
                    }
                } catch (DAOException e) {
                    logger.warn("Unable to map cms url to persistent url ", e);
                }
                if (!currentURL.endsWith(".xhtml")) {
                    String previousURL = (String) session.getAttribute(CURRENT_URL);
                    previousURL = previousURL == null ? "" : previousURL;
                    session.setAttribute(CURRENT_URL, currentURL);
                    logger.trace("Set session attribute {} to {}", CURRENT_URL, currentURL);
                    if (isDifferentView(previousURL, currentURL)) {
                        session.setAttribute(PREVIOUS_URL, previousURL);
                        logger.trace("Set session attribute {} to {}", PREVIOUS_URL, previousURL);
                    }
                }
            }
        }
        } catch(Throwable e) {
            //catch all throwables to avoid constant redirects to error
            logger.error("Error saving page url", e);
        }
    }

    public synchronized static String getPreviousView(ServletRequest request) {
        if (request != null) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession();

            if (session != null) {
                return (String) session.getAttribute(PREVIOUS_URL);
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

    /**
     * @param previousURL
     * @param currentURL
     * @return
     */
    private static boolean isDifferentView(String previousURL, String currentURL) {
        previousURL = getMatch(previousURL, VIEW_REGEX, 0);
        currentURL = getMatch(currentURL, VIEW_REGEX, 0);
        return !previousURL.equals(currentURL);
    }

    /**
     * @param previousURL
     * @param string
     * @return
     */
    private static String getMatch(String text, String pattern, int group) {
        Pattern p = Pattern.compile(pattern);
        Matcher matcher = p.matcher(text);
        if (matcher.find()) {
            return matcher.group(group);
        }
        return "";
    }

}
