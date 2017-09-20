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
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import com.ocpsoft.pretty.PrettyContext;
import com.sun.swing.internal.plaf.synth.resources.synth;

/**
 * @author Florian Alpers
 *
 */
public class UrlRedirectUtils {

    /**
     * 
     */
    private static final String VIEW_REGEX = "https?://.*?/.*?/.*?/";
    private static final String PREVIOUS_URL = "previousURL";
    private static final String CURRENT_URL = "currentURL";

    /**
     * Saves the current view url to the session map Also saves the previous view url to the session map if it represents a different view than the
     * current view
     * 
     * @param request
     */
    public synchronized static void setCurrentView(ServletRequest request) {
        if (request != null) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession();

            if (session != null) {
                String currentURL = ((HttpServletRequest) request).getRequestURI();
                PrettyContext context = PrettyContext.getCurrentInstance(httpRequest);
                if (context != null && context.getRequestURL() != null) {
                    currentURL = ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest) + context.getRequestURL().toURL();
                }
                if(!currentURL.endsWith(".xhtml")) {                    
                    String previousURL = (String) session.getAttribute(CURRENT_URL);
                    previousURL = previousURL == null ? "" : previousURL;
                    session.setAttribute(CURRENT_URL, currentURL);
                    if (isDifferentView(previousURL, currentURL)) {
                        session.setAttribute(PREVIOUS_URL, previousURL);
                    }
                }
            }
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
            previousURL = getMatch(previousURL, VIEW_REGEX);
            currentURL = getMatch(currentURL, VIEW_REGEX);
            return !previousURL.equals(currentURL);
    }

    /**
     * @param previousURL
     * @param string
     * @return
     */
    private static String getMatch(String text, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher matcher = p.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }
    
}
