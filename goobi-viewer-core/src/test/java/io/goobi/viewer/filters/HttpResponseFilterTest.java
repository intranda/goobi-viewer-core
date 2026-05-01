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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;

class HttpResponseFilterTest extends AbstractTest {

    /**
     * Builds a request mock with the given URI and runs it through a fresh filter instance.
     * Returns the response mock so callers can verify which headers were set.
     */
    private static HttpServletResponse runFilter(String requestUri) throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn(requestUri);
        // getServletPath is consulted for the API-charset short-circuit only; default to a non-API path
        Mockito.when(request.getServletPath()).thenReturn(requestUri);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        new HttpResponseFilter().doFilter(request, response, chain);
        return response;
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies set no-store cache headers for dynamic XHTML responses
     */
    @Test
    void doFilter_shouldSetNoStoreCacheHeadersForDynamicXhtmlResponses() throws Exception {
        HttpServletResponse response = runFilter("/viewer/somepage.xhtml");

        Mockito.verify(response).setHeader("Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
        Mockito.verify(response).setHeader("Pragma", "no-cache");
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies set long-term cache headers for modern static resource extensions
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "/viewer/resources/icons/outline/arrow.svg",
            "/viewer/resources/themes/zlb/fonts/DejaVuSans-webfont.woff",
            "/viewer/resources/themes/zlb/fonts/Foo.woff2",
            "/viewer/resources/themes/zlb/fonts/Foo.ttf",
            "/viewer/resources/themes/zlb/fonts/Foo.eot",
            "/viewer/resources/themes/zlb/fonts/Foo.otf",
            "/viewer/resources/styles/main.css",
            "/viewer/resources/javascript/dist/viewer.min.js.map"
    })
    void doFilter_shouldSetLongTermCacheHeadersForModernStaticResourceExtensions(String uri) throws Exception {
        HttpServletResponse response = runFilter(uri);

        Mockito.verify(response).setHeader("Cache-Control", "public, max-age=2592000");
        Mockito.verify(response, Mockito.never()).setHeader(
                Mockito.eq("Cache-Control"),
                Mockito.argThat(v -> v != null && v.contains("no-store")));
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies set long-term cache headers for already matched static resource extensions
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "/viewer/resources/javascript/libs/jquery/jquery.min.js",
            "/viewer/resources/themes/zlb/images/template/fullscreen_logo.png",
            "/viewer/resources/themes/zlb/images/example.gif",
            "/viewer/resources/themes/zlb/images/example.jpg",
            "/viewer/resources/themes/zlb/images/example.jpeg",
            "/viewer/resources/themes/zlb/images/favicon.ico"
    })
    void doFilter_shouldSetLongTermCacheHeadersForAlreadyMatchedStaticResourceExtensions(String uri) throws Exception {
        HttpServletResponse response = runFilter(uri);

        Mockito.verify(response).setHeader("Cache-Control", "public, max-age=2592000");
        // Symmetry with the modern-extensions test: ensure no-store cannot sneak in via
        // a future regex regression that reorders the if/else branches.
        Mockito.verify(response, Mockito.never()).setHeader(
                Mockito.eq("Cache-Control"),
                Mockito.argThat(v -> v != null && v.contains("no-store")));
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies set long-term cache headers for path substring matches
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "/viewer/resources/javascript/libs/jquery/jquery-no-extension",
            "/viewer/javax.faces.resource/primefaces.css.xhtml",
            "/viewer/css/something-without-extension"
    })
    void doFilter_shouldSetLongTermCacheHeadersForPathSubstringMatches(String uri) throws Exception {
        HttpServletResponse response = runFilter(uri);

        Mockito.verify(response).setHeader("Cache-Control", "public, max-age=2592000");
        Mockito.verify(response, Mockito.never()).setHeader(
                Mockito.eq("Cache-Control"),
                Mockito.argThat(v -> v != null && v.contains("no-store")));
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies set no-store cache headers for api json responses
     */
    @Test
    void doFilter_shouldSetNoStoreCacheHeadersForApiJsonResponses() throws Exception {
        // /api/* paths are dynamic — REST responses must not be long-term cached even though
        // their URI carries a familiar-looking ".json" suffix.
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("/viewer/api/v1/records/123.json");
        Mockito.when(request.getServletPath()).thenReturn("/api/v1/records/123.json");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        new HttpResponseFilter().doFilter(request, response, chain);

        Mockito.verify(response).setHeader("Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
        // Charset short-circuit: API paths must skip setCharacterEncoding on the request
        Mockito.verify(request, Mockito.never()).setCharacterEncoding(Mockito.anyString());
    }

    /**
     * @see HttpResponseFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     * @verifies not set any cache headers when prevent proxy caching is disabled
     */
    @Test
    void doFilter_shouldNotSetAnyCacheHeadersWhenPreventProxyCachingIsDisabled() throws Exception {
        // Flip the runtime config off — the filter must read the live value, not a value
        // it cached at class-load time.
        DataManager.getInstance().getConfiguration().overrideValue("performance.preventProxyCaching", false);

        HttpServletResponse response = runFilter("/viewer/somepage.xhtml");

        Mockito.verify(response, Mockito.never()).setHeader(Mockito.eq("Cache-Control"), Mockito.anyString());
        Mockito.verify(response, Mockito.never()).setHeader(Mockito.eq("Pragma"), Mockito.anyString());
        Mockito.verify(response, Mockito.never()).setHeader(Mockito.eq("Expires"), Mockito.anyString());
    }
}
