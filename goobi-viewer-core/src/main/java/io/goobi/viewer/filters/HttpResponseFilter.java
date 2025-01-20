/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.filters;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;

/**
 * Encodes responses into UTF-8 and prevents proxy caching.
 */
public class HttpResponseFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(HttpResponseFilter.class);

    private static boolean preventProxyCaching = DataManager.getInstance().getConfiguration().isPreventProxyCaching();
    private static String alwaysCacheRegex = "/css|jquery|primefaces|\\.js|\\.gif|\\.png|\\.ico|\\.jpg|\\.jpeg";

    /** {@inheritDoc} */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        //rest calls should not carry character encoding
        String path = httpRequest.getServletPath();
        if (!path.equals("/rest") && !path.equals("/api/v1")) {
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
                httpResponse.setHeader("Last-Modified",
                        LocalDateTime.now().atZone(ZoneId.systemDefault()).format(DateTools.FORMATTERJAVAUTILDATETOSTRING));
                httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
                httpResponse.setHeader("Pragma", "no-cache");
            }
        }
        chain.doFilter(request, response);
        //        chain.doFilter(request, new HttpServletResponseWrapper((HttpServletResponse) response) {
        //            public void setHeader(String name, String value) {
        //                if (!"etag".equalsIgnoreCase(name)) {
        //                    super.setHeader(name, value);
        //                }
        //            }
        //        });
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
    }

    /** {@inheritDoc} */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

}
