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
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageBinding;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.bindings.MediaResourceBinding;
import io.goobi.viewer.api.rest.v1.records.media.ObjectResource;
import io.goobi.viewer.controller.DataManager;

class ApiCacheControlResponseFilterTest extends AbstractTest {

    /** Sample endpoints whose annotations drive the classification. */
    static class Endpoints {

        @ContentServerImageBinding
        public void image() {
            //
        }

        @MediaResourceBinding
        public void media() {
            //
        }

        public void data() {
            //
        }
    }

    /**
     * Runs one response through the filter and returns the emitted Cache-Control value, or null.
     */
    private static String runFilter(String methodName, int status, String presetOnRawResponse) throws Exception {
        Method method = Endpoints.class.getMethod(methodName);

        ResourceInfo resourceInfo = Mockito.mock(ResourceInfo.class);
        Mockito.when(resourceInfo.getResourceMethod()).thenReturn(method);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ContainerResponseContext responseContext = Mockito.mock(ContainerResponseContext.class);
        Mockito.when(responseContext.getHeaders()).thenReturn(headers);
        Mockito.when(responseContext.getStatus()).thenReturn(status);

        HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        Mockito.when(servletResponse.getHeader("Cache-Control")).thenReturn(presetOnRawResponse);

        ApiCacheControlResponseFilter filter = new ApiCacheControlResponseFilter();
        filter.setResourceInfoForTest(resourceInfo);
        filter.setServletResponseForTest(servletResponse);
        filter.filter(Mockito.mock(ContainerRequestContext.class), responseContext);

        Object value = headers.getFirst("Cache-Control");
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
     * @verifies keep the header of a not modified response
     */
    @Test
    void filter_shouldKeepTheHeaderOfANotModifiedResponse() throws Exception {
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
     * @verifies set no header when caching is disabled
     */
    @Test
    void filter_shouldSetNoHeaderWhenCachingIsDisabled() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("performance.caching[@enabled]", false);

        Assertions.assertNull(runFilter("image", 200, null));
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

    /** Fails unless exactly one method of that name carries the media binding. */
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
}
