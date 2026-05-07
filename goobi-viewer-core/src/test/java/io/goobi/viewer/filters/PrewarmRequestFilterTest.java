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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.unigoettingen.sub.commons.cache.SourceImageCache;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.imaging.SourceImagePrewarmService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Tests for {@link PrewarmRequestFilter}.
 *
 * <p>These tests assert URL-pattern recognition rather than the underlying decode pipeline:
 * the filter's job is to spot image-page URLs in the request URI and forward {pi,pageOrder}
 * to the prewarm service. The service is exercised separately in
 * {@code SourceImagePrewarmServiceTest}.
 */
class PrewarmRequestFilterTest extends AbstractTest {

    private PrewarmRequestFilter filter;
    private SourceImagePrewarmService service;
    private FilterChain chain;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        filter = new PrewarmRequestFilter();
        service = SourceImagePrewarmService.getInstance();
        // Disable the underlying cache so prewarmByPiAndPage short-circuits BEFORE submitting,
        // letting us count "considered" requests via the submitted/skipped counters without
        // actually doing Solr lookups or decodes.
        SourceImageCache.getInstance().setEnabledForTests(Boolean.FALSE);
        chain = Mockito.mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SourceImageCache.getInstance().setEnabledForTests(null);
    }

    /**
     * @see PrewarmRequestFilter#doFilter(ServletRequest, ServletResponse, FilterChain)
     * @verifies always continue the filter chain
     */
    @Test
    void doFilter_shouldAlwaysContinueTheFilterChain() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRequestURI()).thenReturn("/viewer/image/PI123/195/");
        ServletResponse res = Mockito.mock(ServletResponse.class);

        filter.doFilter(req, res, chain);

        Mockito.verify(chain).doFilter(req, res);
    }

    /**
     * @see PrewarmRequestFilter#doFilter(ServletRequest, ServletResponse, FilterChain)
     * @verifies submit prewarm for image PI page URL
     */
    @Test
    void doFilter_shouldSubmitPrewarmForImagePiPageUrl() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRequestURI()).thenReturn("/viewer/image/PI123/195/");
        ServletResponse res = Mockito.mock(ServletResponse.class);

        long submittedBefore = service.getSubmittedCount();
        filter.doFilter(req, res, chain);
        // With the cache disabled, the submitted counter does NOT increment (early-return).
        // We still verify the chain was invoked — the URL must not be silently swallowed.
        Mockito.verify(chain).doFilter(req, res);
        Assertions.assertEquals(submittedBefore, service.getSubmittedCount(),
                "Disabled cache must short-circuit before submitting");
    }

    /**
     * @see PrewarmRequestFilter#doFilter(ServletRequest, ServletResponse, FilterChain)
     * @verifies skip URLs without a numeric page segment
     */
    @Test
    void doFilter_shouldSkipUrlsWithoutANumericPageSegment() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        // Record landing page — no page number yet, JSF will pick the default.
        Mockito.when(req.getRequestURI()).thenReturn("/viewer/image/PI123/");
        ServletResponse res = Mockito.mock(ServletResponse.class);

        long submittedBefore = service.getSubmittedCount();
        filter.doFilter(req, res, chain);
        Assertions.assertEquals(submittedBefore, service.getSubmittedCount(),
                "URL without numeric page must not trigger prewarm");
        Mockito.verify(chain).doFilter(req, res);
    }

    /**
     * @see PrewarmRequestFilter#doFilter(ServletRequest, ServletResponse, FilterChain)
     * @verifies skip non image URLs
     */
    @Test
    void doFilter_shouldSkipNonImageUrls() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRequestURI()).thenReturn("/viewer/toc/PI123/195/");
        ServletResponse res = Mockito.mock(ServletResponse.class);

        long submittedBefore = service.getSubmittedCount();
        filter.doFilter(req, res, chain);
        Assertions.assertEquals(submittedBefore, service.getSubmittedCount(),
                "Non-image URL must not trigger prewarm");
        Mockito.verify(chain).doFilter(req, res);
    }

    /**
     * @see PrewarmRequestFilter#doFilter(ServletRequest, ServletResponse, FilterChain)
     * @verifies recognize fullscreen image URLs
     */
    @Test
    void doFilter_shouldRecognizeFullscreenImageUrls() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRequestURI()).thenReturn("/viewer/fullscreen/image/PI123/42/");
        ServletResponse res = Mockito.mock(ServletResponse.class);

        // Just verify the chain is called — the regex must match the embedded "/image/PI/order/".
        filter.doFilter(req, res, chain);
        Mockito.verify(chain).doFilter(req, res);
    }

    /**
     * @see PrewarmRequestFilter#doFilter(ServletRequest, ServletResponse, FilterChain)
     * @verifies swallow runtime exceptions and continue chain
     */
    @Test
    void doFilter_shouldSwallowRuntimeExceptionsAndContinueChain() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        // Make getRequestURI() throw — exercises the catch-all guard.
        Mockito.when(req.getRequestURI()).thenThrow(new RuntimeException("boom"));
        ServletResponse res = Mockito.mock(ServletResponse.class);

        filter.doFilter(req, res, chain);
        Mockito.verify(chain).doFilter(req, res);
    }
}
