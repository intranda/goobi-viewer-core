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

import java.lang.reflect.Method;
import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.bindings.MediaResourceBinding;
import io.goobi.viewer.api.rest.bindings.RecordFileDownloadBinding;
import io.goobi.viewer.api.rest.v1.records.media.ObjectResource;
import io.goobi.viewer.controller.DataManager;

class ApiCacheControlResponseFilterTest extends AbstractTest {

    /** Sample endpoints whose annotations drive the classification. */
    static class Endpoints {

        @ContentServerImageBinding
        public void image() {
            //
        }

        /** Mirrors the real image tile endpoint: several producible types, but the format is pinned by a path parameter. */
        @ContentServerImageBinding
        @Produces({ "image/jpeg", "image/png", "image/tif" })
        public void imageTile(@PathParam("format") String format) {
            //
        }

        /** Mirrors the real image info endpoint: several producible types negotiated purely via Accept. */
        @ContentServerImageInfoBinding
        @Produces({ "application/ld+json", MediaType.APPLICATION_JSON })
        public void imageInfo() {
            //
        }

        @MediaResourceBinding
        public void media() {
            //
        }

        /** Mirrors RecordFileResource#getMediaFile, which carries both bindings at once. */
        @MediaResourceBinding
        @RecordFileDownloadBinding
        public void mediaDownload() {
            //
        }

        public void data() {
            //
        }
    }

    /**
     * Runs one response through the filter and returns the resulting JAX-RS response headers.
     */
    private static MultivaluedMap<String, Object> runFilterHeaders(String methodName, int status, String cacheControlOnRaw,
            String expiresOnRaw, boolean committed, String cacheControlInJaxRsHeaders) throws Exception {
        Method method = resolveEndpoint(methodName);

        ResourceInfo resourceInfo = Mockito.mock(ResourceInfo.class);
        Mockito.when(resourceInfo.getResourceMethod()).thenReturn(method);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        if (cacheControlInJaxRsHeaders != null) {
            headers.putSingle(HttpHeaders.CACHE_CONTROL, cacheControlInJaxRsHeaders);
        }
        ContainerResponseContext responseContext = Mockito.mock(ContainerResponseContext.class);
        Mockito.when(responseContext.getHeaders()).thenReturn(headers);
        Mockito.when(responseContext.getStatus()).thenReturn(status);

        HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        Mockito.when(servletResponse.getHeader(HttpHeaders.CACHE_CONTROL)).thenReturn(cacheControlOnRaw);
        Mockito.when(servletResponse.getHeader(HttpHeaders.EXPIRES)).thenReturn(expiresOnRaw);
        Mockito.when(servletResponse.isCommitted()).thenReturn(committed);

        ApiCacheControlResponseFilter filter = new ApiCacheControlResponseFilter();
        filter.setResourceInfoForTest(resourceInfo);
        filter.setServletResponseForTest(servletResponse);
        filter.filter(Mockito.mock(ContainerRequestContext.class), responseContext);

        return headers;
    }

    /** Looks up the single fixture method with this name, regardless of its parameter list. */
    private static Method resolveEndpoint(String methodName) {
        return Arrays.stream(Endpoints.class.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such fixture endpoint: " + methodName));
    }

    /**
     * Runs one response through the filter and returns the emitted Cache-Control value, or null.
     */
    private static String runFilter(String methodName, int status, String presetOnRawResponse) throws Exception {
        Object value = runFilterHeaders(methodName, status, presetOnRawResponse, null, false, null).getFirst(HttpHeaders.CACHE_CONTROL);
        return value == null ? null : value.toString();
    }

    /** Returns the emitted Vary value, or null. */
    private static String vary(MultivaluedMap<String, Object> headers) {
        Object value = headers.getFirst(HttpHeaders.VARY);
        return value == null ? null : value.toString();
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies set a short private max age for image responses
     */
    @Test
    void filter_shouldSetAShortPrivateMaxAgeForImageResponses() throws Exception {
        Assertions.assertEquals("private, max-age=77", runFilter("image", 200, null));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies revalidate image responses when max age is zero
     */
    @Test
    void filter_shouldRevalidateImageResponsesWhenMaxAgeIsZero() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("performance.caching.api.image[@maxAge]", 0);

        Assertions.assertEquals("private, no-cache", runFilter("image", 200, null));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies revalidate media responses
     */
    @Test
    void filter_shouldRevalidateMediaResponses() throws Exception {
        Assertions.assertEquals("private, no-cache", runFilter("media", 200, null));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies revalidate image info responses
     */
    @Test
    void filter_shouldRevalidateImageInfoResponses() throws Exception {
        Assertions.assertEquals("private, no-cache", runFilter("imageInfo", 200, null));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies never store plain data responses
     */
    @Test
    void filter_shouldNeverStorePlainDataResponses() throws Exception {
        Assertions.assertEquals("no-store", runFilter("data", 200, null));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies never store error responses
     */
    @Test
    void filter_shouldNeverStoreErrorResponses() throws Exception {
        Assertions.assertEquals("no-store", runFilter("image", 403, null));
        Assertions.assertEquals("no-store", runFilter("image", 404, null));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies override an existing header on an error response
     */
    @Test
    void filter_shouldOverrideAnExistingHeaderOnAnErrorResponse() throws Exception {
        Assertions.assertEquals("no-store", runFilter("image", 500, "private, max-age=300"));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies never store a download even when the media binding is also present
     */
    @Test
    void filter_shouldNeverStoreADownloadEvenWhenTheMediaBindingIsAlsoPresent() throws Exception {
        Assertions.assertEquals("no-store", runFilter("mediaDownload", 200, null));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies set the image max age on a not modified response
     */
    @Test
    void filter_shouldSetTheImageMaxAgeOnANotModifiedResponse() throws Exception {
        Assertions.assertEquals("private, max-age=77", runFilter("image", 304, null));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies not override a header set on the raw servlet response
     */
    @Test
    void filter_shouldNotOverrideAHeaderSetOnTheRawServletResponse() throws Exception {
        Assertions.assertNull(runFilter("data", 200, "private, max-age=300"));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies not override a header already present in the jax rs headers
     */
    @Test
    void filter_shouldNotOverrideAHeaderAlreadyPresentInTheJaxRsHeaders() throws Exception {
        MultivaluedMap<String, Object> headers = runFilterHeaders("data", 200, null, null, false, "public, max-age=60");

        Assertions.assertEquals("public, max-age=60", headers.getFirst(HttpHeaders.CACHE_CONTROL));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies not override a response with an expires header on the raw servlet response
     */
    @Test
    void filter_shouldNotOverrideAResponseWithAnExpiresHeaderOnTheRawServletResponse() throws Exception {
        MultivaluedMap<String, Object> headers = runFilterHeaders("data", 200, null, "Wed, 21 Oct 2026 07:28:00 GMT", false, null);

        Assertions.assertNull(headers.getFirst(HttpHeaders.CACHE_CONTROL));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies not touch an already committed response
     */
    @Test
    void filter_shouldNotTouchAnAlreadyCommittedResponse() throws Exception {
        MultivaluedMap<String, Object> headers = runFilterHeaders("image", 200, null, null, true, null);

        Assertions.assertNull(headers.getFirst(HttpHeaders.CACHE_CONTROL));
        Assertions.assertNull(headers.getFirst(HttpHeaders.VARY));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies set no header when caching is disabled
     */
    @Test
    void filter_shouldSetNoHeaderWhenCachingIsDisabled() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("performance.caching[@enabled]", false);

        Assertions.assertNull(runFilter("image", 200, null));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies vary only by cookie for a private response with a fixed format
     */
    @Test
    void filter_shouldVaryOnlyByCookieForAPrivateResponseWithAFixedFormat() throws Exception {
        MultivaluedMap<String, Object> headers = runFilterHeaders("imageTile", 200, null, null, false, null);

        Assertions.assertEquals("Cookie", vary(headers));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies vary by accept and cookie for a negotiated private response
     */
    @Test
    void filter_shouldVaryByAcceptAndCookieForANegotiatedPrivateResponse() throws Exception {
        MultivaluedMap<String, Object> headers = runFilterHeaders("imageInfo", 200, null, null, false, null);

        Assertions.assertEquals("Accept, Cookie", vary(headers));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies find the media binding on every static media endpoint
     */
    @Test
    void filter_shouldFindTheMediaBindingOnEveryStaticMediaEndpoint() throws Exception {
        assertMediaBinding(io.goobi.viewer.api.rest.v1.cms.CMSMediaResource.class, "getSvgContent");
        assertMediaBinding(io.goobi.viewer.api.rest.v1.cms.CMSMediaResource.class, "getIcoContent");
        assertMediaBinding(io.goobi.viewer.api.rest.v1.cms.CMSMediaResource.class, "getPDFMediaItemContent");
        assertMediaBinding(io.goobi.viewer.api.rest.v2.cms.CMSMediaResource.class, "getSvgContent");
        assertMediaBinding(io.goobi.viewer.api.rest.v2.cms.CMSMediaResource.class, "getIcoContent");
        assertMediaBinding(io.goobi.viewer.api.rest.v2.cms.CMSMediaResource.class, "getPDFMediaItemContent");

        assertMediaBinding(ObjectResource.class, "getObject", HttpServletRequest.class, HttpServletResponse.class);
        assertMediaBinding(ObjectResource.class, "getObjectResource",
                HttpServletRequest.class, HttpServletResponse.class, String.class, String.class, String.class);
        assertMediaBinding(ObjectResource.class, "getObjectResource2",
                HttpServletRequest.class, HttpServletResponse.class, String.class, String.class, String.class);
        assertMediaBinding(ObjectResource.class, "getObjectResource",
                HttpServletRequest.class, HttpServletResponse.class, String.class, String.class, String.class, String.class);
        assertMediaBinding(ObjectResource.class, "getObjectResource2",
                HttpServletRequest.class, HttpServletResponse.class, String.class, String.class, String.class, String.class);
    }

    /** Fails unless at least one method of that name carries the media binding. */
    private static void assertMediaBinding(Class<?> resourceClass, String methodName) {
        boolean found = Arrays.stream(resourceClass.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .anyMatch(m -> m.getAnnotation(MediaResourceBinding.class) != null);
        Assertions.assertTrue(found, methodName + " on " + resourceClass.getSimpleName() + " must carry @MediaResourceBinding");
    }

    /** Fails unless the method with the given signature carries the media binding. Disambiguates overloaded method names. */
    private static void assertMediaBinding(Class<?> resourceClass, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = resourceClass.getDeclaredMethod(methodName, parameterTypes);
        Assertions.assertNotNull(method.getAnnotation(MediaResourceBinding.class),
                methodName + " on " + resourceClass.getSimpleName() + " must carry @MediaResourceBinding");
    }

    /**
     * The Vary suppression in {@link ApiCacheControlResponseFilter} keys off a path parameter literally
     * named "format"; this pins that name down on the real image tile endpoints instead of only the
     * fixture, so a rename or a differently named format parameter fails loudly here.
     *
     * @see ApiCacheControlResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
     * @verifies find a format path parameter on every image tile endpoint
     */
    @Test
    void filter_shouldFindAFormatPathParameterOnEveryImageTileEndpoint() throws Exception {
        assertFormatPathParam(io.goobi.viewer.api.rest.v1.records.media.RecordsFilesImageResource.class, "getImage",
                String.class, String.class, String.class, String.class, String.class);
        assertFormatPathParam(io.goobi.viewer.api.rest.v2.records.media.RecordsFilesImageResource.class, "getImage",
                String.class, String.class, String.class, String.class, String.class);
    }

    /** Fails unless one of the method's parameters carries {@code @PathParam("format")}. */
    private static void assertFormatPathParam(Class<?> resourceClass, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = resourceClass.getDeclaredMethod(methodName, parameterTypes);
        boolean found = Arrays.stream(method.getParameters())
                .anyMatch(p -> p.isAnnotationPresent(PathParam.class) && "format".equals(p.getAnnotation(PathParam.class).value()));
        Assertions.assertTrue(found, methodName + " on " + resourceClass.getSimpleName() + " must carry @PathParam(\"format\")");
    }
}
