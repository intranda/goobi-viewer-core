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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.imaging.SourceImagePrewarmService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Servlet filter that fires an asynchronous source-image cache pre-warm as soon as a request for
 * an image-page URL hits the servlet container — i.e. before any JSF lifecycle phase has run.
 * The {@link SourceImagePrewarmService} resolves the master file path (Solr lookup) and decodes
 * the JPEG on a background worker, so the OpenSeadragon tile burst that the browser issues a
 * second or two later finds the decoded {@code BufferedImage} already in cache.
 *
 * <p>The earlier we trigger pre-warm, the more lifecycle latency (RestoreView, ApplyRequestValues,
 * etc.) we get to overlap with the decode. Compared to the {@code ViewManager.setCurrentImageOrder}
 * hook, this filter buys ~100–300 ms of head start, which can mean the difference between an
 * already-cached master and a still-decoding one when the tile burst arrives.
 *
 * <p>Recognised URL shape: {@code /<context>/image/{PI}/{pageOrder}/...}. The first 1-based
 * integer path segment after the PI is treated as the page order. URLs without a numeric page
 * segment (e.g. the record landing page {@code /image/{PI}/}) are ignored — there is no specific
 * page to pre-warm yet, and JSF will resolve the default page later via the existing hook.
 *
 * <p>Filter mapping is restricted to {@code /image/*} and {@code /fullscreen/*} so the parsing
 * cost (a single regex match on the URI) is not paid for every static-resource or REST-API
 * request.
 */
@WebFilter(urlPatterns = { "/image/*", "/fullscreen/*" })
public class PrewarmRequestFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(PrewarmRequestFilter.class);

    // Captures: group 1 = PI, group 2 = pageOrder (1-based).
    // Anchored at start-of-string with at most one optional context-path segment before
    // {/image/ or /fullscreen/image/}. Stricter than a global "/image/" match: prevents
    // accidental matches against URIs that happen to embed "image/PI/N/" deep inside a longer
    // path (e.g. an unrelated REST endpoint that uses "image" as a parameter value).
    private static final Pattern URL_PATTERN =
            Pattern.compile("^(?:/[^/]+)?/(?:fullscreen/)?image/([^/]+)/(\\d+)(?:/|$)");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No init state needed — the SourceImagePrewarmService is a JVM-wide singleton.
    }

    /**
     * @should always continue the filter chain
     * @should submit prewarm for image PI page URL
     * @should skip URLs without a numeric page segment
     * @should skip non image URLs
     * @should recognize fullscreen image URLs
     * @should swallow runtime exceptions and continue chain
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpReq) {
            tryPrewarm(httpReq);
        }
        // Always continue the chain — the filter is non-blocking and never alters the response.
        chain.doFilter(request, response);
    }

    /**
     * Best-effort URL parse + prewarm submission. Any failure (malformed URL, parse error,
     * service rejection) is logged at trace level and swallowed — the filter must never break
     * the user-facing request.
     */
    private void tryPrewarm(HttpServletRequest req) {
        // Captured outside the try so the catch block can log it without re-invoking
        // getRequestURI() — that re-invocation could itself throw and propagate out of the
        // filter, which is exactly what this guard is meant to prevent.
        String uri = null;
        try {
            uri = req.getRequestURI();
            if (uri == null) {
                return;
            }
            Matcher m = URL_PATTERN.matcher(uri);
            if (!m.find()) {
                return;
            }
            String pi = m.group(1);
            int pageOrder;
            try {
                pageOrder = Integer.parseInt(m.group(2));
            } catch (NumberFormatException e) {
                // Regex matched \d+ so this should not happen, but guard anyway.
                return;
            }
            SourceImagePrewarmService.getInstance().prewarmByPiAndPage(pi, pageOrder);
        } catch (RuntimeException e) {
            // Catch-all guard so a parsing bug or service hiccup never breaks request handling.
            logger.trace("Prewarm filter ignored exception for {}: {}", uri, e.getMessage());
        }
    }

    @Override
    public void destroy() {
        // Nothing to dispose — the prewarm executor outlives individual filter instances.
    }
}
