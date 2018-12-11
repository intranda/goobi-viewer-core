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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meterware.pseudoserver.HttpRequest;

import de.intranda.digiverso.presentation.controller.DataManager;

/**
 * Encodes responses into UTF-8 and prevents proxy caching.
 */
public class HttpResponseFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(HttpResponseFilter.class);

    private static boolean preventProxyCaching = DataManager.getInstance().getConfiguration().isPreventProxyCaching();
    private static String alwaysCacheRegex = "/css|jquery|primefaces|\\.js|\\.gif|\\.png|\\.ico|\\.jpg|\\.jpeg";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        /*
         * Firefox browser tries to precache all urls in links with rel="next" or rel="prefetch".
         * This changes the session state and thus shall not pass
         * Fortunately Firefox marks all precaching-request with a X-Moz : prefetch header
         * (https://developer.mozilla.org/en-US/docs/Web/HTTP/Link_prefetching_FAQ)
         * However this header is not standardized and may change in the future
         */
        String xmoz = httpRequest.getHeader("X-Moz");
        if(xmoz == null) {
            xmoz = httpRequest.getHeader("X-moz");
        }
        if(xmoz != null && xmoz.equalsIgnoreCase("prefetch")) {
            logger.trace("Refuse prefetch request");
            return;
        }
        
        //rest calls should not carry character encoding
        String path = httpRequest.getServletPath();
        if(!path.equals("/rest")) {            
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
        }
        
        if (preventProxyCaching) {
            //            if (httpRequest.getRequestURI().contains("OpenLayers"))
            //            logger.debug(httpRequest.getRequestURI());
            

            // Only disable caching if the URI doesn't match the regex
            Pattern p = Pattern.compile(alwaysCacheRegex);
            Matcher m = p.matcher(httpRequest.getRequestURI());
            if (!m.find()) {
                //                logger.debug(httpRequest.getRequestURI());
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setHeader("Expires", "Tue, 03 Jul 2001 06:00:00 GMT");
                httpResponse.setHeader("Last-Modified", new java.util.Date().toString());
                httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
                httpResponse.setHeader("Pragma", "no-cache");
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