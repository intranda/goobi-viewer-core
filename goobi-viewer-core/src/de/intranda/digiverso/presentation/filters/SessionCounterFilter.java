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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

import de.intranda.digiverso.presentation.controller.DataManager;

/**
 * 
 */
@WebFilter
public class SessionCounterFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionCounterFilter.class);

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc) throws IOException, ServletException {
        // logger.trace("doFilter");
        HttpServletRequest req = (HttpServletRequest) request;
        String id = req.getSession().getId();
        Map<String, String> metadataMap = DataManager.getInstance().getSessionMap().get(id);
        if (metadataMap == null) {
            metadataMap = new HashMap<>();
            DataManager.getInstance().getSessionMap().put(id, metadataMap);
            metadataMap.put("id", id);
            metadataMap.put("created", new Date().toString());
        }
        metadataMap.put("address", req.getRemoteAddr());
        metadataMap.put("x-forwarded-for", req.getHeader("x-forwarded-for"));
        Date now = new Date();
        metadataMap.put("last", now.toString());
        metadataMap.put("timeout", String.valueOf(req.getSession().getMaxInactiveInterval()) + " s");
        
        fc.doFilter(request, response); // continue
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }
}
