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

import javax.servlet.http.HttpServletRequest;

public class ServletUtils {

    /**
     * retrieve complete Servlet url from servlet context, including Url, Port, Servletname etc. callable without jsf context
     *
     * @return complete url as string
     */
    public static String getServletPathWithHostAsUrlFromRequest(HttpServletRequest request) {
        String scheme = request.getScheme(); // http
        String serverName = request.getServerName(); // hostname.com
        int serverPort = request.getServerPort(); // 80
        String contextPath = request.getContextPath(); // /mywebapp
        if ("http".equals(scheme) && serverPort == 80) {
            return scheme + "://" + serverName + contextPath;
        }
        if ("https".equals(scheme) && serverPort == 443) {
            return scheme + "://" + serverName + contextPath;
        }

        return scheme + "://" + serverName + ":" + serverPort + contextPath;
    }
    
    /**
     * retrieve complete Servlet url from servlet context, including Url, Port, callable without jsf servletName
     *
     * @return complete url as string
     */
    public static String getServletPathWithoutHostAsUrlFromRequest(HttpServletRequest request) {
        String scheme = request.getScheme(); // http
        String serverName = request.getServerName(); // hostname.com
        int serverPort = request.getServerPort(); // 80
        if ("http".equals(scheme) && serverPort == 80) {
            return scheme + "://" + serverName;
        }
        if ("https".equals(scheme) && serverPort == 443) {
            return scheme + "://" + serverName;
        }

        return scheme + "://" + serverName + ":" + serverPort;
    }
}
