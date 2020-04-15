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
package io.goobi.viewer.filters;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;

/**
 * <p>
 * SessionCounterFilter class.
 * </p>
 */
@WebFilter
public class SessionCounterFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SessionCounterFilter.class);

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    /** {@inheritDoc} */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    /** {@inheritDoc} */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc) throws IOException, ServletException {
        // logger.trace("doFilter");
        HttpServletRequest req = (HttpServletRequest) request;
        String id = req.getSession().getId();
        Map<String, String> metadataMap = DataManager.getInstance().getSessionMap().get(id);
        if (metadataMap == null) {
            metadataMap = new LinkedHashMap<>();
            DataManager.getInstance().getSessionMap().put(id, metadataMap);
            metadataMap.put("id", id);
            metadataMap.put("created", new Date().toString());
        }
        metadataMap.put("address", req.getRemoteAddr());
        metadataMap.put("x-forwarded-for", req.getHeader("x-forwarded-for"));
        Date now = new Date();
        metadataMap.put("last request", now.toString());
        metadataMap.put("previous request", new Date(req.getSession().getLastAccessedTime()).toString());
        metadataMap.put("timeout", String.valueOf(req.getSession().getMaxInactiveInterval()) + " s");

        //       Enumeration<String> sessionAttributes = req.getSession().getAttributeNames();
        Optional<Map<Object, Map>> logicalViews =
                Optional.ofNullable((Map) req.getSession().getAttribute("com.sun.faces.renderkit.ServerSideStateHelper.LogicalViewMap"));
        Integer numberOfLogicalViews = logicalViews.map(map -> map.keySet().size()).orElse(0);
        Integer numberOfTotalViews = logicalViews.map(map -> map.values().stream().mapToInt(value -> value.keySet().size()).sum()).orElse(0);
        metadataMap.put("Logical Views stored in session", numberOfLogicalViews.toString());
        metadataMap.put("Total views stored in session", numberOfTotalViews.toString());

        fc.doFilter(request, response); // continue
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#destroy()
     */
    /** {@inheritDoc} */
    @Override
    public void destroy() {
    }
}
