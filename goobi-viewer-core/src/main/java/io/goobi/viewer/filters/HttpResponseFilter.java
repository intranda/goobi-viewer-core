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
 * Servlet filter that controls HTTP response caching for the viewer webapp.
 *
 * <p>Behavior is gated by {@code performance.preventProxyCaching} (default
 * {@code false}). When the option is enabled, the filter splits requests
 * into two groups:</p>
 *
 * <ul>
 *   <li><b>Dynamic responses</b> (XHTML pages, REST/JSON, anything else):
 *       receive {@code Cache-Control: no-store, no-cache, must-revalidate,
 *       max-age=0, post-check=0, pre-check=0} plus {@code Pragma: no-cache}
 *       and a stale {@code Expires}. This prevents stale JSF view-state,
 *       stale post-redirect content, and shared-proxy caching of
 *       personalized responses.</li>
 *   <li><b>Static, content-addressable resources</b> (matched by
 *       {@link #ALWAYS_CACHE_PATTERN} — e.g. {@code .js}, {@code .css},
 *       {@code .svg}, {@code .woff}, {@code .png}, ...): receive
 *       {@code Cache-Control: public, max-age=2592000} so browsers can
 *       cache them for 30 days without revalidation. The TTL matches the
 *       historical Apache {@code mod_expires} default for image types and
 *       the IIIF image endpoint.</li>
 * </ul>
 *
 * <p>The filter also normalises the request and response character
 * encoding to UTF-8 for non-API paths.</p>
 *
 * <p>The {@code preventProxyCaching} flag is read live from
 * {@link io.goobi.viewer.controller.Configuration} on every request, so
 * administrators can change the value in {@code config_viewer.xml} without
 * restarting Tomcat. Apache Commons Configuration's reloading strategy
 * stat()s the file only when the strategy says it's time (poll-based,
 * not on every read), so the per-request cost is a HashMap lookup.</p>
 */
public class HttpResponseFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(HttpResponseFilter.class);

    // Skip-list for static, content-addressable resources. Substring matches
    // (`/css`, `jquery`, `primefaces`) preserve backward compatibility with
    // JSF resource URLs that have no file extension. Extension matches cover
    // all modern static formats served by the viewer and its themes.
    //
    // Extension tokens are followed by `\b` (word boundary) to prevent false
    // matches where a known extension is a substring of a longer word — most
    // importantly to keep `.js` from matching the `.js` substring of `.json`,
    // which would route REST-API responses into the long-term cache branch.
    private static String alwaysCacheRegex =
            "/css|jquery|primefaces"
                    + "|\\.(?:js|css|map|gif|png|ico|jpg|jpeg"
                    + "|svg|woff2|woff|ttf|eot|otf)\\b";
    // Pre-compiled pattern for cache regex — avoids Pattern.compile() on every HTTP request
    private static final Pattern ALWAYS_CACHE_PATTERN = Pattern.compile(alwaysCacheRegex);

    /**
     * Positive Cache-Control header value for static resources matched by
     * {@link #ALWAYS_CACHE_PATTERN}. 30 days mirrors the existing IIIF image
     * endpoint TTL and the historical mod_expires setting for image types —
     * long enough to materially reduce repeat traffic, short enough to
     * recover from a bad asset deploy without forcing users to clear their
     * cache. If production HARs ever show 30 days is wrong, expose this as
     * a config option in {@code <performance><staticResourceCacheMaxAge>}.
     */
    private static final String STATIC_RESOURCE_CACHE_CONTROL =
            "public, max-age=2592000";

    /**
     * {@inheritDoc}
     *
     * @should set no-store cache headers for dynamic XHTML responses
     * @should set long-term cache headers for modern static resource extensions
     * @should set long-term cache headers for already matched static resource
     *         extensions
     * @should set long-term cache headers for path substring matches
     * @should set no-store cache headers for api json responses
     * @should not set any cache headers when prevent proxy caching is disabled
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        //rest calls should not carry character encoding
        String path = httpRequest.getServletPath();
        if (!path.startsWith("/api/") && !path.equals("/rest")) {
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
        }

        // Read the flag live per request so admins can flip
        // <preventProxyCaching> in config_viewer.xml without restarting
        // Tomcat. Apache Commons Configuration's reloading strategy stat()s
        // the file only when the strategy says it's time (poll-based, not on
        // every read), so the per-request cost is a HashMap lookup. If
        // profiling later shows this as a hotspot, a TTL cache local to the
        // filter is the obvious mitigation.
        if (DataManager.getInstance().getConfiguration()
                .isPreventProxyCaching()) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            // Static, content-addressable URIs get a positive long-term cache
            // header so that browsers do not have to revalidate them on every
            // navigation. Dynamic URIs get the protective no-store/no-cache
            // trio that prevents JSF view-state corruption and stale
            // post-redirect content.
            Matcher m = ALWAYS_CACHE_PATTERN.matcher(httpRequest.getRequestURI());
            if (m.find()) {
                httpResponse.setHeader("Cache-Control",
                        STATIC_RESOURCE_CACHE_CONTROL);
            } else {
                httpResponse.setHeader("Expires", "Tue, 03 Jul 2001 06:00:00 GMT");
                httpResponse.setHeader("Last-Modified",
                        LocalDateTime.now().atZone(ZoneId.systemDefault()).format(DateTools.FORMATTERJAVAUTILDATETOSTRING));
                httpResponse.setHeader("Cache-Control",
                        "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
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
