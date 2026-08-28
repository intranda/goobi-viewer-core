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

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.goobi.viewer.controller.DataManager;

/**
 * Servlet filter that controls HTTP response caching for the viewer webapp.
 *
 * <p>Requests are classified by {@link ResourceCacheCategory}; each category maps to one
 * {@code Cache-Control} value:
 *
 * <ul>
 *   <li>{@code STATIC}: {@code public, max-age=<performance.caching.static[@maxAge]>}
 *   <li>{@code ACCOUNT}: {@code no-store}, regardless of the dynamic policy
 *   <li>{@code DYNAMIC}: per {@code performance.caching.dynamic[@policy]}
 *   <li>{@code API}: nothing, {@code ApiCacheControlResponseFilter} owns those responses
 *   <li>{@code SKIP}: nothing, Mojarra owns JSF resource responses via
 *       {@code com.sun.faces.defaultResourceMaxAge}
 * </ul>
 *
 * <p>On an {@code ERROR} dispatch the filter always emits {@code no-store}: a response whose
 * status is not a success must not inherit the freshness of the path it was requested under.
 *
 * <p>The filter also normalises request and response character encoding to UTF-8 for non-API paths.
 */
public class HttpResponseFilter implements Filter {

    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String NO_STORE = "no-store";
    private static final String POLICY_NO_STORE = "no-store";
    private static final String POLICY_OFF = "off";

    /** {@inheritDoc} */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //
    }

    /**
     * {@inheritDoc}
     *
     * @should set long term cache headers for whitelisted assets
     * @should set no store for account bound paths
     * @should set no store for account bound paths even when dynamic caching is off
     * @should apply the configured policy to dynamic pages
     * @should set no cache headers for jsf resources and api paths
     * @should set no headers when caching is disabled
     * @should skip character encoding for api paths
     * @should set no store for account bound paths reached through a forward
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getServletPath();
        ResourceCacheCategory category = ResourceCacheCategory.classify(path, getOriginalPath(httpRequest));

        // REST calls carry their own encoding negotiated by JAX-RS.
        if (category != ResourceCacheCategory.API) {
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
        }

        // Read the flag live per request so admins can change the configuration without
        // restarting Tomcat. Commons Configuration polls the file, so the per-request cost is a
        // map lookup.
        if (DataManager.getInstance().getConfiguration().isCachingEnabled()) {
            if (DispatcherType.ERROR.equals(request.getDispatcherType())) {
                httpResponse.setHeader(HEADER_CACHE_CONTROL, NO_STORE);
            } else {
                applyCacheControl(httpResponse, category);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Returns the request uri the current dispatch originated from, without the context path.
     *
     * <p>The PrettyFaces rewrite filter is ordered ahead of this one and forwards a matched pretty
     * url to its backing view id via a genuine {@code RequestDispatcher.forward()}. From that point
     * on {@link HttpServletRequest#getRequestURI()} reflects the forward target, i.e. the view id,
     * not the pretty url the browser sent; the servlet container preserves the pre-forward uri in
     * the standard {@link RequestDispatcher#FORWARD_REQUEST_URI} attribute instead, and PrettyFaces
     * itself relies on that same attribute to recover the pretty url after its own forward. Falling
     * back to {@link HttpServletRequest#getRequestURI()} covers a dispatch that was never forwarded,
     * where the two are identical anyway.
     */
    private static String getOriginalPath(HttpServletRequest request) {
        String uri = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        if (uri == null) {
            uri = request.getRequestURI();
        }
        if (uri == null) {
            return null;
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }

    /** Emits the header for the given category, or nothing where another layer owns it. */
    private static void applyCacheControl(HttpServletResponse response, ResourceCacheCategory category) {
        switch (category) {
            case STATIC:
                response.setHeader(HEADER_CACHE_CONTROL,
                        "public, max-age=" + DataManager.getInstance().getConfiguration().getStaticResourceCacheMaxAge());
                break;
            case ACCOUNT:
                // Not subject to the dynamic policy: account bound pages must never be written to disk.
                response.setHeader(HEADER_CACHE_CONTROL, NO_STORE);
                break;
            case DYNAMIC:
                applyDynamicPolicy(response);
                break;
            default:
                break;
        }
    }

    /**
     * Emits the dynamic page header.
     *
     * <p>{@code private, no-cache} is the default rather than {@code no-store} because
     * {@code no-store} disqualifies a page from the browsers' back/forward cache, which turns every
     * backward navigation into a full request.
     */
    private static void applyDynamicPolicy(HttpServletResponse response) {
        String policy = DataManager.getInstance().getConfiguration().getDynamicCachePolicy();
        if (POLICY_OFF.equalsIgnoreCase(policy)) {
            return;
        }
        if (POLICY_NO_STORE.equalsIgnoreCase(policy)) {
            response.setHeader(HEADER_CACHE_CONTROL, NO_STORE);
            return;
        }
        response.setHeader(HEADER_CACHE_CONTROL, "private, no-cache");
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
        //
    }
}
