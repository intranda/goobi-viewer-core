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
import java.net.URI;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.bindings.CSRFGuarded;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;

/**
 * JAX-RS request filter that enforces an Origin/Referer whitelist on simple-request REST
 * endpoints (multipart upload) that bypass the browser's CORS preflight protection.
 *
 * <p><b>Activation:</b> opt-in via {@code webapi.csrf[@enabled]=true} in the configuration.
 * When disabled (default), the filter performs no checks - pre-existing behavior is preserved.
 *
 * <p><b>Bypass:</b> requests carrying an {@code Authorization: Bearer ...} header are always
 * accepted (case-insensitive per RFC 6750 section 2.1). A bearer token is never sent
 * automatically by the browser, so its presence proves the request is a deliberate API
 * call and not a cross-site forgery.
 *
 * <p><b>Validation:</b> when enabled and not bypassed, the filter extracts the request's origin
 * from the {@code Origin} header, falling back to the host part of the {@code Referer} header.
 * The origin is compared against {@link Configuration#getViewerBaseUrl()} (self-origin) and
 * the list returned by {@link Configuration#getCsrfAdditionalAllowedOrigins()}. Mismatches
 * and missing headers both abort the request with HTTP 403.
 *
 * @see CSRFGuarded
 */
@Provider
@CSRFGuarded
@Priority(Priorities.AUTHORIZATION)
public class CSRFRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(CSRFRequestFilter.class);

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        Configuration config = DataManager.getInstance().getConfiguration();
        if (!config.isCsrfFilterEnabled()) {
            return;
        }
        // Bearer-Token requests are CSRF-immune: the browser never sends the Authorization
        // header automatically on cross-origin requests, so its presence proves a deliberate
        // call by the client (mobile app, REST script, etc.). RFC 6750 section 2.1 declares the
        // scheme case-insensitive - match "Bearer ", "bearer ", "BEARER " etc.
        String auth = ctx.getHeaderString("Authorization");
        if (auth != null && auth.length() >= 7 && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return;
        }
        String requestOrigin = normalizeOrigin(ctx.getHeaderString("Origin"));
        if (requestOrigin == null) {
            requestOrigin = normalizeOrigin(ctx.getHeaderString("Referer"));
        }
        if (requestOrigin == null) {
            reject(ctx, "Origin and Referer header missing");
            return;
        }
        if (!isAllowedOrigin(requestOrigin, config)) {
            reject(ctx, "Origin " + requestOrigin + " is not allowed");
        }
    }

    private static boolean isAllowedOrigin(String origin, Configuration config) {
        String self = normalizeOrigin(config.getViewerBaseUrl());
        if (self == null) {
            // Operator misconfig: urls.base unset and urls.rest fallback yielded a string
            // that URI cannot parse to a scheme+host. Fail closed and shout so operators
            // notice quickly; the alternative (accept everything) would defeat the filter.
            logger.warn("CSRF filter cannot derive self-origin from Configuration.getViewerBaseUrl()={}",
                    config.getViewerBaseUrl());
            return false;
        }
        if (origin.equals(self)) {
            return true;
        }
        for (String additional : config.getCsrfAdditionalAllowedOrigins()) {
            if (origin.equals(normalizeOrigin(additional))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reduces a URL or origin string to {@code scheme://host[:port]} (no path, no trailing slash).
     * Returns {@code null} when input is null, empty, or unparseable.
     *
     * @param urlOrOrigin URL or origin string
     * @return normalized origin or {@code null}
     */
    static String normalizeOrigin(String urlOrOrigin) {
        if (urlOrOrigin == null || urlOrOrigin.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(urlOrOrigin);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder()
                    .append(uri.getScheme())
                    .append("://")
                    .append(uri.getHost());
            if (uri.getPort() != -1) {
                sb.append(':').append(uri.getPort());
            }
            return sb.toString();
        } catch (IllegalArgumentException e) {
            // Malformed URL/Origin - treat as unknown, caller rejects.
            return null;
        }
    }

    private static void reject(ContainerRequestContext ctx, String reason) {
        // Log the specific reason for forensic analysis; return a generic body so the
        // wire response does not leak the configured whitelist policy to a probing
        // attacker.
        logger.warn("CSRF filter rejected request to /{}: {}", ctx.getUriInfo().getPath(), reason);
        ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity("CSRF protection violated")
                .build());
    }
}
