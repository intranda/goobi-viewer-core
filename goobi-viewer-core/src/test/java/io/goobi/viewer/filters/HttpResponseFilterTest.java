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

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;

class HttpResponseFilterTest extends AbstractTest {

    /**
     * Runs a request with the given servlet path through a fresh filter instance and returns the
     * response mock, so callers can verify which headers were set.
     */
    private static HttpServletResponse runFilter(String servletPath) throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getServletPath()).thenReturn(servletPath);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        new HttpResponseFilter().doFilter(request, response, chain);
        return response;
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies set long term cache headers for whitelisted assets
     */
    @Test
    void doFilter_shouldSetLongTermCacheHeadersForWhitelistedAssets() throws Exception {
        HttpServletResponse response = runFilter("/resources/css/dist/viewer.min.css");

        Mockito.verify(response).setHeader("Cache-Control", "public, max-age=12345");
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies set no store for account bound paths
     */
    @Test
    void doFilter_shouldSetNoStoreForAccountBoundPaths() throws Exception {
        HttpServletResponse response = runFilter("/admin/users/");

        Mockito.verify(response).setHeader("Cache-Control", "no-store");
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies set no store for account bound paths even when dynamic caching is off
     */
    @Test
    void doFilter_shouldSetNoStoreForAccountBoundPathsEvenWhenDynamicCachingIsOff() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("performance.caching.dynamic[@policy]", "off");

        HttpServletResponse response = runFilter("/user/dashboard/");

        Mockito.verify(response).setHeader("Cache-Control", "no-store");
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies apply the configured policy to dynamic pages
     */
    @Test
    void doFilter_shouldApplyTheConfiguredPolicyToDynamicPages() throws Exception {
        HttpServletResponse noStore = runFilter("/object/PPN123/1/");
        Mockito.verify(noStore).setHeader("Cache-Control", "no-store");

        DataManager.getInstance().getConfiguration().overrideValue("performance.caching.dynamic[@policy]", "no-cache");
        HttpServletResponse noCache = runFilter("/object/PPN123/1/");
        Mockito.verify(noCache).setHeader("Cache-Control", "private, no-cache");

        DataManager.getInstance().getConfiguration().overrideValue("performance.caching.dynamic[@policy]", "off");
        HttpServletResponse off = runFilter("/object/PPN123/1/");
        Mockito.verify(off, Mockito.never()).setHeader(ArgumentMatchers.eq("Cache-Control"), ArgumentMatchers.anyString());
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies set no cache headers for jsf resources and api paths
     */
    @Test
    void doFilter_shouldSetNoCacheHeadersForJsfResourcesAndApiPaths() throws Exception {
        HttpServletResponse jsf = runFilter("/jakarta.faces.resource/viewer.css.xhtml");
        Mockito.verify(jsf, Mockito.never()).setHeader(ArgumentMatchers.eq("Cache-Control"), ArgumentMatchers.anyString());

        HttpServletResponse api = runFilter("/api/v1/records/PPN123/manifest/");
        Mockito.verify(api, Mockito.never()).setHeader(ArgumentMatchers.eq("Cache-Control"), ArgumentMatchers.anyString());
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies set no headers when caching is disabled
     */
    @Test
    void doFilter_shouldSetNoHeadersWhenCachingIsDisabled() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("performance.caching[@enabled]", false);

        HttpServletResponse response = runFilter("/resources/css/dist/viewer.min.css");

        Mockito.verify(response, Mockito.never()).setHeader(ArgumentMatchers.eq("Cache-Control"), ArgumentMatchers.anyString());
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies skip character encoding for api paths
     */
    @Test
    void doFilter_shouldSkipCharacterEncodingForApiPaths() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getServletPath()).thenReturn("/api/v1/records/PPN123/manifest/");
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        new HttpResponseFilter().doFilter(request, response, Mockito.mock(FilterChain.class));

        Mockito.verify(request, Mockito.never()).setCharacterEncoding(ArgumentMatchers.anyString());
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies set no store for account bound paths reached through a forward
     */
    @Test
    void doFilter_shouldSetNoStoreForAccountBoundPathsReachedThroughAForward() throws Exception {
        // the pretty url rewrite filter has already forwarded /user/searches/ to this view id by
        // the time this filter runs, so only the preserved forward attribute still carries it
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getServletPath()).thenReturn("/userBackendSearches.xhtml");
        Mockito.when(request.getContextPath()).thenReturn("/viewer");
        Mockito.when(request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI)).thenReturn("/viewer/user/searches/");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        new HttpResponseFilter().doFilter(request, response, Mockito.mock(FilterChain.class));

        Mockito.verify(response).setHeader("Cache-Control", "no-store");
    }
}
