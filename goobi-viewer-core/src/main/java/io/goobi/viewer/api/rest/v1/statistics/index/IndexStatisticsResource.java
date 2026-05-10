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
package io.goobi.viewer.api.rest.v1.statistics.index;

import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Map;

import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.statistics.index.IndexStatisticsService;
import io.goobi.viewer.model.statistics.index.StatisticsUnavailableException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Index-level statistics for the new Chart.js-driven CMS components. All endpoints emit JSON; the JAX-RS/Jersey stack
 * registers this class via package scanning ({@code io.goobi.viewer.api.rest.v1.*}). Public access (no
 * {@code @AuthorizationBinding}) — see Phase 1 design notes in plan #15809.
 *
 * <p>
 * Each endpoint translates {@link StatisticsUnavailableException} (raised by {@link IndexStatisticsService} when Solr
 * is unreachable AND no cached snapshot is available) to HTTP 503 with a small JSON error body, so the frontend can
 * differentiate "really down" from "empty result". On success the response carries
 * {@code Cache-Control: public, max-age=3600}; the service layer keeps an additional day-long internal cache.
 * </p>
 */
@Path("/statistics/index")
@ViewerRestServiceBinding
public class IndexStatisticsResource {

    /** Public CDN-cacheable for one hour; service has its own day-long internal cache on top of that. */
    private static final int CACHE_MAX_AGE_SECONDS = 3600;

    private final IndexStatisticsService service;

    public IndexStatisticsResource() {
        this(new IndexStatisticsService());
    }

    /** Test-only constructor that injects a mock service. */
    IndexStatisticsResource(IndexStatisticsService service) {
        this.service = service;
    }

    /**
     * Resolves the locale for label translation. Prefers an explicit {@code lang} query parameter (the composite
     * forwards {@code #{navigationHelper.localeString}}, which is the user's session-bound UI language) because
     * JAX-RS requests do not always have an active CDI session-scope and {@link BeanUtils#getLocale()} would then
     * silently fall back to the FacesContext default. Falls back to {@code BeanUtils.getLocale()} when the parameter
     * is absent — keeps the endpoint usable from non-JSF callers (curl, sitemap probes, etc.).
     */
    private Locale resolveLocale(String langParam) {
        if (langParam != null && !langParam.isBlank()) {
            try {
                return Locale.forLanguageTag(langParam);
            } catch (IllformedLocaleException e) {
                // Bad client input — ignore and fall through to the BeanUtils path.
            }
        }
        return BeanUtils.getLocale();
    }

    /**
     * Lists each top-level docstruct type with its record count.
     *
     * @param lang IETF BCP 47 language tag (e.g. {@code de}, {@code en}); typically supplied by the composite as
     *            {@code #{navigationHelper.localeString}} so the response matches the user's UI language
     * @param filter optional Lucene sub-query forwarded from the CMS-admin filter input; restricts the docstruct
     *            facet to a subset of the index. No validation here — the backend editor's {@code solrQueryValidator}
     *            gate-keeps before persistence, and the service wraps the value in MUST so it can only narrow.
     * @return 200 + JSON list on success, 503 + JSON error body when the service signals unavailability
     * @should return service result with cache control header
     * @should return 503 when service throws StatisticsUnavailableException
     * @should forward lang query parameter to service
     * @should forward filter query parameter to service
     */
    @GET
    @Path("/publication-types")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPublicationTypes(@QueryParam("lang") String lang, @QueryParam("filter") String filter) {
        try {
            return ok(service.getPublicationTypes(resolveLocale(lang), filter));
        } catch (StatisticsUnavailableException e) {
            return unavailable(e);
        }
    }

    /**
     * Cumulative-count time-series of imported records for a line chart.
     *
     * @param days size of the look-back window
     * @param buckets number of data points to produce
     * @param filter optional Lucene sub-query forwarded from the CMS-admin filter input; restricts the time-series.
     * @return 200 + JSON list on success, 503 + JSON error body when the service signals unavailability
     * @should pass through query parameters
     * @should return 503 when service throws StatisticsUnavailableException
     * @should forward filter query parameter to service
     */
    @GET
    @Path("/import-trend")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportTrend(
            @QueryParam("days") @DefaultValue("180") int days,
            @QueryParam("buckets") @DefaultValue("12") int buckets,
            @QueryParam("filter") String filter) {
        try {
            return ok(service.getImportTrend(days, buckets, filter));
        } catch (StatisticsUnavailableException e) {
            return unavailable(e);
        }
    }

    /**
     * Total page and full-text counts in the index.
     *
     * @param filter optional Lucene sub-query forwarded from the CMS-admin filter input; applies to both counts.
     * @return 200 + JSON summary on success, 503 + JSON error body when the service signals unavailability
     * @should return summary from service
     * @should return 503 when service throws StatisticsUnavailableException
     * @should forward filter query parameter to service
     */
    @GET
    @Path("/imports-summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportSummary(@QueryParam("filter") String filter) {
        try {
            return ok(service.getImportSummary(filter));
        } catch (StatisticsUnavailableException e) {
            return unavailable(e);
        }
    }

    private Response ok(Object body) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(CACHE_MAX_AGE_SECONDS);
        cc.setPrivate(false);
        return Response.ok(body, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    }

    private Response unavailable(StatisticsUnavailableException e) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", e.getMessage()))
                .build();
    }
}
