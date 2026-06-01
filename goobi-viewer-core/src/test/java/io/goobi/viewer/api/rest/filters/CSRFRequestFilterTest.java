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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;

class CSRFRequestFilterTest extends AbstractTest {

    private Configuration originalConfig;
    private Configuration configSpy;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        originalConfig = DataManager.getInstance().getConfiguration();
        configSpy = spy(originalConfig);
        DataManager.getInstance().injectConfiguration(configSpy);
    }

    @AfterEach
    public void csrfTearDown() {
        DataManager.getInstance().injectConfiguration(originalConfig);
    }

    private ContainerRequestContext requestMock(String origin, String referer, String authHeader, String path) {
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        when(ctx.getHeaderString("Origin")).thenReturn(origin);
        when(ctx.getHeaderString("Referer")).thenReturn(referer);
        when(ctx.getHeaderString("Authorization")).thenReturn(authHeader);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn(path);
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        return ctx;
    }

    /**
     * @see CSRFRequestFilter#filter(ContainerRequestContext)
     * @verifies do nothing when filter is disabled
     */
    @Test
    void filter_shouldDoNothingWhenFilterIsDisabled() throws Exception {
        Mockito.doReturn(false).when(configSpy).isCsrfFilterEnabled();
        ContainerRequestContext ctx = requestMock("https://evil.example.com", null, null, "media/files/folder");

        new CSRFRequestFilter().filter(ctx);

        verify(ctx, never()).abortWith(any());
    }

    /**
     * @see CSRFRequestFilter#filter(ContainerRequestContext)
     * @verifies bypass when bearer token present
     */
    @Test
    void filter_shouldBypassWhenBearerTokenPresent() throws Exception {
        Mockito.doReturn(true).when(configSpy).isCsrfFilterEnabled();
        Mockito.doReturn("https://viewer.example.com").when(configSpy).getViewerBaseUrl();
        ContainerRequestContext ctx = requestMock(null, null, "Bearer abcdef123456", "media/files/folder");

        new CSRFRequestFilter().filter(ctx);

        verify(ctx, never()).abortWith(any());
    }

    /**
     * @see CSRFRequestFilter#filter(ContainerRequestContext)
     * @verifies reject when origin and referer missing
     */
    @Test
    void filter_shouldRejectWhenOriginAndRefererMissing() throws Exception {
        Mockito.doReturn(true).when(configSpy).isCsrfFilterEnabled();
        Mockito.doReturn("https://viewer.example.com").when(configSpy).getViewerBaseUrl();
        Mockito.doReturn(Collections.emptyList()).when(configSpy).getCsrfAdditionalAllowedOrigins();
        ContainerRequestContext ctx = requestMock(null, null, null, "media/files/folder");

        new CSRFRequestFilter().filter(ctx);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).abortWith(captor.capture());
        assertEquals(403, captor.getValue().getStatus());
    }

    /**
     * @see CSRFRequestFilter#filter(ContainerRequestContext)
     * @verifies reject foreign origin
     */
    @Test
    void filter_shouldRejectForeignOrigin() throws Exception {
        Mockito.doReturn(true).when(configSpy).isCsrfFilterEnabled();
        Mockito.doReturn("https://viewer.example.com").when(configSpy).getViewerBaseUrl();
        Mockito.doReturn(Collections.emptyList()).when(configSpy).getCsrfAdditionalAllowedOrigins();
        ContainerRequestContext ctx = requestMock("https://evil.example.com", null, null, "media/files/folder");

        new CSRFRequestFilter().filter(ctx);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).abortWith(captor.capture());
        assertEquals(403, captor.getValue().getStatus());
    }

    /**
     * @see CSRFRequestFilter#filter(ContainerRequestContext)
     * @verifies accept same origin
     */
    @Test
    void filter_shouldAcceptSameOrigin() throws Exception {
        Mockito.doReturn(true).when(configSpy).isCsrfFilterEnabled();
        Mockito.doReturn("https://viewer.example.com").when(configSpy).getViewerBaseUrl();
        Mockito.doReturn(Collections.emptyList()).when(configSpy).getCsrfAdditionalAllowedOrigins();
        ContainerRequestContext ctx = requestMock("https://viewer.example.com", null, null, "media/files/folder");

        new CSRFRequestFilter().filter(ctx);

        verify(ctx, never()).abortWith(any());
    }

    /**
     * @see CSRFRequestFilter#filter(ContainerRequestContext)
     * @verifies accept whitelisted origin
     */
    @Test
    void filter_shouldAcceptWhitelistedOrigin() throws Exception {
        Mockito.doReturn(true).when(configSpy).isCsrfFilterEnabled();
        Mockito.doReturn("https://viewer.example.com").when(configSpy).getViewerBaseUrl();
        Mockito.doReturn(List.of("https://cdn.example.com")).when(configSpy).getCsrfAdditionalAllowedOrigins();
        ContainerRequestContext ctx = requestMock("https://cdn.example.com", null, null, "media/files/folder");

        new CSRFRequestFilter().filter(ctx);

        verify(ctx, never()).abortWith(any());
    }

    /**
     * @see CSRFRequestFilter#filter(ContainerRequestContext)
     * @verifies extract origin from referer when origin header missing
     */
    @Test
    void filter_shouldExtractOriginFromRefererWhenOriginHeaderMissing() throws Exception {
        Mockito.doReturn(true).when(configSpy).isCsrfFilterEnabled();
        Mockito.doReturn("https://viewer.example.com").when(configSpy).getViewerBaseUrl();
        Mockito.doReturn(Collections.emptyList()).when(configSpy).getCsrfAdditionalAllowedOrigins();
        ContainerRequestContext ctx = requestMock(null, "https://viewer.example.com/some/path?q=1", null, "media/files/folder");

        new CSRFRequestFilter().filter(ctx);

        verify(ctx, never()).abortWith(any());
    }

    /**
     * @see CSRFRequestFilter#filter(ContainerRequestContext)
     * @verifies reject literal null origin
     */
    @Test
    void filter_shouldRejectLiteralNullOrigin() throws Exception {
        Mockito.doReturn(true).when(configSpy).isCsrfFilterEnabled();
        Mockito.doReturn("https://viewer.example.com").when(configSpy).getViewerBaseUrl();
        Mockito.doReturn(Collections.emptyList()).when(configSpy).getCsrfAdditionalAllowedOrigins();
        ContainerRequestContext ctx = requestMock("null", null, null, "media/files/folder");

        new CSRFRequestFilter().filter(ctx);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).abortWith(captor.capture());
        assertEquals(403, captor.getValue().getStatus());
    }

    /**
     * @see CSRFRequestFilter#filter(ContainerRequestContext)
     * @verifies reject malformed origin
     */
    @Test
    void filter_shouldRejectMalformedOrigin() throws Exception {
        Mockito.doReturn(true).when(configSpy).isCsrfFilterEnabled();
        Mockito.doReturn("https://viewer.example.com").when(configSpy).getViewerBaseUrl();
        Mockito.doReturn(Collections.emptyList()).when(configSpy).getCsrfAdditionalAllowedOrigins();
        ContainerRequestContext ctx = requestMock("<script>alert(1)</script>", null, null, "media/files/folder");

        new CSRFRequestFilter().filter(ctx);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).abortWith(captor.capture());
        assertEquals(403, captor.getValue().getStatus());
    }

    /**
     * @see CSRFRequestFilter#filter(ContainerRequestContext)
     * @verifies reject when self origin is unparseable
     */
    @Test
    void filter_shouldRejectWhenSelfOriginIsUnparseable() throws Exception {
        Mockito.doReturn(true).when(configSpy).isCsrfFilterEnabled();
        Mockito.doReturn("localhost:8080/default-viewer/").when(configSpy).getViewerBaseUrl();
        Mockito.doReturn(Collections.emptyList()).when(configSpy).getCsrfAdditionalAllowedOrigins();
        ContainerRequestContext ctx = requestMock("https://viewer.example.com", null, null, "media/files/folder");

        new CSRFRequestFilter().filter(ctx);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).abortWith(captor.capture());
        assertEquals(403, captor.getValue().getStatus());
    }

    /**
     * @see CSRFRequestFilter#filter(ContainerRequestContext)
     * @verifies bypass when bearer token uses lowercase scheme
     */
    @Test
    void filter_shouldBypassWhenBearerTokenUsesLowercaseScheme() throws Exception {
        Mockito.doReturn(true).when(configSpy).isCsrfFilterEnabled();
        Mockito.doReturn("https://viewer.example.com").when(configSpy).getViewerBaseUrl();
        ContainerRequestContext ctx = requestMock(null, null, "bearer abcdef123456", "media/files/folder");

        new CSRFRequestFilter().filter(ctx);

        verify(ctx, never()).abortWith(any());
    }
}
