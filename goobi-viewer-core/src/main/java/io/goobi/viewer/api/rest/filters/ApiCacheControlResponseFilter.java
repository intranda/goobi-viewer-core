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
package io.goobi.viewer.api.rest.filters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;
import io.goobi.viewer.api.rest.bindings.DownloadBinding;
import io.goobi.viewer.api.rest.bindings.MediaResourceBinding;
import io.goobi.viewer.api.rest.bindings.RecordFileDownloadBinding;
import io.goobi.viewer.controller.DataManager;

/**
 * Sets the {@code Cache-Control} header for every REST API response.
 *
 * <p>Classification is binding based, not media type based: the response media type is null for
 * methods that only declare {@code @Produces} and for not-modified responses, so it cannot carry
 * the decision.
 *
 * <p>A response already committed by the time this filter runs is left untouched outright; nothing can
 * be set on it anymore.
 *
 * <p>An error or redirect response is forced to {@code no-store} before any override guard runs, so
 * that a failing endpoint can never hand out the freshness it preset for its success path.
 *
 * <p>Everything else that already carries a caching decision is left alone: endpoints setting their own
 * header, and the audio and video delivery which writes {@code Cache-Control: private, no-cache} straight
 * onto the servlet response.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class ApiCacheControlResponseFilter implements ContainerResponseFilter {

    private static final String NO_STORE = "no-store";
    private static final String PRIVATE_NO_CACHE = "private, no-cache";

    @Context
    private ResourceInfo resourceInfo;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * {@inheritDoc}
     *
     * @should set a short private max age for image responses
     * @should revalidate image responses when max age is zero
     * @should revalidate media responses
     * @should revalidate image info responses
     * @should never store plain data responses
     * @should never store error responses
     * @should override an existing header on an error response
     * @should never store a download even when the media binding is also present
     * @should set the image max age on a not modified response
     * @should not override a header set on the raw servlet response
     * @should not override a header already present in the jax rs headers
     * @should not override a response with an expires header on the raw servlet response
     * @should not touch an already committed response
     * @should set no header when caching is disabled
     * @should find the media binding on every static media endpoint
     * @should find a format path parameter on every image tile endpoint
     * @should vary only by cookie for a private response with a fixed format
     * @should vary by accept and cookie for a negotiated private response
     * @should keep a header set on the raw servlet response inside a container
     * @should apply the data policy inside a container
     * @should replace a header preset before an error response inside a container
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (!DataManager.getInstance().getConfiguration().isCachingEnabled()) {
            return;
        }
        if (servletResponse != null && servletResponse.isCommitted()) {
            return;
        }
        if (!isCacheableStatus(responseContext.getStatus())) {
            responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, NO_STORE);
            return;
        }
        if (hasCachingDecision(responseContext)) {
            return;
        }

        String value = cacheControlFor();
        responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, value);
        addVary(responseContext, value);
    }

    /** Returns true if some other layer already decided how this response may be cached. */
    private boolean hasCachingDecision(ContainerResponseContext responseContext) {
        if (responseContext.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL)) {
            return true;
        }
        if (servletResponse == null) {
            return false;
        }
        return servletResponse.getHeader(HttpHeaders.CACHE_CONTROL) != null || servletResponse.getHeader(HttpHeaders.EXPIRES) != null;
    }

    /**
     * Maps the matched resource method to a Cache-Control value.
     *
     * <p>The download bindings outrank the media binding: a record file download and a static media
     * delivery can sit on the very same method, but a download must never be handed out as
     * {@code no-cache}, which still permits disk storage. An access restricted file would otherwise
     * stay readable from a shared cache folder after the session that was allowed to fetch it ends.
     */
    private String cacheControlFor() {
        if (hasBinding(RecordFileDownloadBinding.class) || hasBinding(DownloadBinding.class)) {
            return NO_STORE;
        }
        if (hasBinding(ContentServerImageBinding.class)) {
            int maxAge = DataManager.getInstance().getConfiguration().getApiImageCacheMaxAge();
            return maxAge > 0 ? "private, max-age=" + maxAge : PRIVATE_NO_CACHE;
        }
        if (hasBinding(ContentServerImageInfoBinding.class) || hasBinding(MediaResourceBinding.class)) {
            return PRIVATE_NO_CACHE;
        }
        return NO_STORE;
    }

    /** Only successful and not-modified responses may carry a positive freshness lifetime. */
    private static boolean isCacheableStatus(int status) {
        return Response.Status.Family.SUCCESSFUL.equals(Response.Status.Family.familyOf(status))
                || status == Response.Status.NOT_MODIFIED.getStatusCode();
    }

    /**
     * Returns true if the matched resource method carries the given binding.
     *
     * <p>The check runs on the method rather than the class because JAX-RS name bindings are not
     * inherited: several image resources inherit their endpoints from a superclass in the content
     * library, and a class level lookup would miss exactly those.
     */
    private boolean hasBinding(Class<? extends Annotation> binding) {
        Method method = resourceInfo == null ? null : resourceInfo.getResourceMethod();
        return method != null && method.getAnnotation(binding) != null;
    }

    /**
     * Adds {@code Vary} where the response really varies.
     *
     * <p>{@code Accept} only matters where the resource negotiates the representation: it declares
     * more than one producible type and does not already pin the format through a path parameter.
     * Image tiles pick their format from a {@code {quality}.{format}} path segment instead, so a
     * plain {@code <img>} tag, {@code fetch()} and OpenSeadragon would each fragment the cache key on
     * their own Accept header for no reason. {@code Cookie} matters on every private response so that
     * the cache key changes on login and logout.
     */
    private void addVary(ContainerResponseContext responseContext, String cacheControl) {
        if (NO_STORE.equals(cacheControl)) {
            return;
        }
        StringBuilder vary = new StringBuilder();
        Method method = resourceInfo == null ? null : resourceInfo.getResourceMethod();
        if (variesByAccept(method)) {
            vary.append(HttpHeaders.ACCEPT);
        }
        if (cacheControl.startsWith("private")) {
            if (vary.length() > 0) {
                vary.append(", ");
            }
            vary.append(HttpHeaders.COOKIE);
        }
        if (vary.length() > 0) {
            responseContext.getHeaders().putSingle(HttpHeaders.VARY, vary.toString());
        }
    }

    /** Returns true if the resource negotiates its representation via the Accept header rather than a path parameter. */
    private static boolean variesByAccept(Method method) {
        if (method == null || hasPathParam(method, "format")) {
            return false;
        }
        return producesCount(method) > 1;
    }

    /** Returns true if one of the method's parameters is annotated {@code @PathParam(name)}. */
    private static boolean hasPathParam(Method method, String name) {
        for (Parameter parameter : method.getParameters()) {
            PathParam pathParam = parameter.getAnnotation(PathParam.class);
            if (pathParam != null && name.equals(pathParam.value())) {
                return true;
            }
        }
        return false;
    }

    /** Returns the number of media types the resource method produces. */
    private static int producesCount(Method method) {
        Produces produces = method.getAnnotation(Produces.class);
        return produces == null ? 0 : produces.value().length;
    }

    /** Injects the resource info in tests; the container injects it in production. */
    void setResourceInfoForTest(ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    /** Injects the servlet response in tests; the container injects it in production. */
    void setServletResponseForTest(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }
}
