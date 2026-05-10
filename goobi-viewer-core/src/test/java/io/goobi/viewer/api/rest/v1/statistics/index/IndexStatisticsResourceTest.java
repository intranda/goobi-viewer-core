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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.goobi.viewer.api.rest.model.statistics.index.ImportSummary;
import io.goobi.viewer.api.rest.model.statistics.index.ImportTrendBucket;
import io.goobi.viewer.api.rest.model.statistics.index.PublicationTypeStatistic;
import io.goobi.viewer.model.statistics.index.IndexStatisticsService;
import io.goobi.viewer.model.statistics.index.StatisticsUnavailableException;
import jakarta.ws.rs.core.Response;

class IndexStatisticsResourceTest {

    /**
     * @see IndexStatisticsResource#getPublicationTypes(String, String)
     * @verifies return service result with cache control header
     */
    @Test
    @SuppressWarnings("unchecked")
    void getPublicationTypes_shouldReturnServiceResultWithCacheControlHeader() throws Exception {
        IndexStatisticsService svc = mock(IndexStatisticsService.class);
        when(svc.getPublicationTypes(any(), any()))
                .thenReturn(List.of(new PublicationTypeStatistic("Monograph", 5, "Monograph")));

        IndexStatisticsResource res = new IndexStatisticsResource(svc);
        Response response = res.getPublicationTypes(null, null);

        assertEquals(200, response.getStatus());
        List<PublicationTypeStatistic> body = (List<PublicationTypeStatistic>) response.getEntity();
        assertEquals(1, body.size());
        assertEquals("Monograph", body.get(0).label());
        // Cache-Control must be present for CDN-friendliness.
        String cc = response.getHeaderString("Cache-Control");
        assertNotNull(cc);
        assertTrue(cc.contains("max-age=3600"), "Cache-Control was: " + cc);
    }

    /**
     * @see IndexStatisticsResource#getPublicationTypes(String, String)
     * @verifies return 503 when service throws StatisticsUnavailableException
     */
    @Test
    void getPublicationTypes_shouldReturn503WhenServiceThrowsStatisticsUnavailableException() throws Exception {
        IndexStatisticsService svc = mock(IndexStatisticsService.class);
        when(svc.getPublicationTypes(any(), any()))
                .thenThrow(new StatisticsUnavailableException("solr down", new RuntimeException("x")));

        IndexStatisticsResource res = new IndexStatisticsResource(svc);
        Response response = res.getPublicationTypes(null, null);

        assertEquals(503, response.getStatus());
    }

    /**
     * @see IndexStatisticsResource#getPublicationTypes(String, String)
     * @verifies forward lang query parameter to service
     */
    @Test
    void getPublicationTypes_shouldForwardLangQueryParameterToService() throws Exception {
        IndexStatisticsService svc = mock(IndexStatisticsService.class);
        // ArgumentCaptor on the Locale parameter — the resource turns the lang string into a Locale
        // before calling the service, and we want to be sure the conversion didn't get dropped.
        ArgumentCaptor<Locale> localeCaptor = ArgumentCaptor.forClass(Locale.class);
        when(svc.getPublicationTypes(localeCaptor.capture(), any())).thenReturn(List.of());

        IndexStatisticsResource res = new IndexStatisticsResource(svc);
        res.getPublicationTypes("de", null);

        assertEquals(Locale.forLanguageTag("de"), localeCaptor.getValue());
    }

    /**
     * @see IndexStatisticsResource#getPublicationTypes(String, String)
     * @verifies forward filter query parameter to service
     */
    @Test
    void getPublicationTypes_shouldForwardFilterQueryParameterToService() throws Exception {
        IndexStatisticsService svc = mock(IndexStatisticsService.class);
        ArgumentCaptor<String> filterCaptor = ArgumentCaptor.forClass(String.class);
        when(svc.getPublicationTypes(any(), filterCaptor.capture())).thenReturn(List.of());

        IndexStatisticsResource res = new IndexStatisticsResource(svc);
        res.getPublicationTypes("de", "DC:zeitschriften");

        assertEquals("DC:zeitschriften", filterCaptor.getValue());
    }

    /**
     * @see IndexStatisticsResource#getImportTrend(int, int, String)
     * @verifies pass through query parameters
     */
    @Test
    @SuppressWarnings("unchecked")
    void getImportTrend_shouldPassThroughQueryParameters() throws Exception {
        IndexStatisticsService svc = mock(IndexStatisticsService.class);
        when(svc.getImportTrend(eq(180), eq(12), any())).thenReturn(List.of(new ImportTrendBucket(0, 0)));

        IndexStatisticsResource res = new IndexStatisticsResource(svc);
        Response response = res.getImportTrend(180, 12, null);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<ImportTrendBucket>) response.getEntity()).size());
    }

    /**
     * @see IndexStatisticsResource#getImportTrend(int, int, String)
     * @verifies return 503 when service throws StatisticsUnavailableException
     */
    @Test
    void getImportTrend_shouldReturn503WhenServiceThrowsStatisticsUnavailableException() throws Exception {
        IndexStatisticsService svc = mock(IndexStatisticsService.class);
        when(svc.getImportTrend(eq(180), eq(12), any()))
                .thenThrow(new StatisticsUnavailableException("solr down", new RuntimeException("x")));

        IndexStatisticsResource res = new IndexStatisticsResource(svc);
        Response response = res.getImportTrend(180, 12, null);

        assertEquals(503, response.getStatus());
    }

    /**
     * @see IndexStatisticsResource#getImportTrend(int, int, String)
     * @verifies forward filter query parameter to service
     */
    @Test
    void getImportTrend_shouldForwardFilterQueryParameterToService() throws Exception {
        IndexStatisticsService svc = mock(IndexStatisticsService.class);
        ArgumentCaptor<String> filterCaptor = ArgumentCaptor.forClass(String.class);
        when(svc.getImportTrend(eq(180), eq(12), filterCaptor.capture())).thenReturn(List.of());

        IndexStatisticsResource res = new IndexStatisticsResource(svc);
        res.getImportTrend(180, 12, "DC:bilder");

        assertEquals("DC:bilder", filterCaptor.getValue());
    }

    /**
     * @see IndexStatisticsResource#getImportSummary(String)
     * @verifies return summary from service
     */
    @Test
    void getImportSummary_shouldReturnSummaryFromService() throws Exception {
        IndexStatisticsService svc = mock(IndexStatisticsService.class);
        when(svc.getImportSummary(any())).thenReturn(new ImportSummary(100, 40));

        IndexStatisticsResource res = new IndexStatisticsResource(svc);
        Response response = res.getImportSummary(null);

        assertEquals(200, response.getStatus());
        ImportSummary summary = (ImportSummary) response.getEntity();
        assertEquals(100, summary.pages());
        assertEquals(40, summary.fulltexts());
    }

    /**
     * @see IndexStatisticsResource#getImportSummary(String)
     * @verifies return 503 when service throws StatisticsUnavailableException
     */
    @Test
    void getImportSummary_shouldReturn503WhenServiceThrowsStatisticsUnavailableException() throws Exception {
        IndexStatisticsService svc = mock(IndexStatisticsService.class);
        when(svc.getImportSummary(any()))
                .thenThrow(new StatisticsUnavailableException("solr down", new RuntimeException("x")));

        IndexStatisticsResource res = new IndexStatisticsResource(svc);
        Response response = res.getImportSummary(null);

        assertEquals(503, response.getStatus());
    }

    /**
     * @see IndexStatisticsResource#getImportSummary(String)
     * @verifies forward filter query parameter to service
     */
    @Test
    void getImportSummary_shouldForwardFilterQueryParameterToService() throws Exception {
        IndexStatisticsService svc = mock(IndexStatisticsService.class);
        ArgumentCaptor<String> filterCaptor = ArgumentCaptor.forClass(String.class);
        when(svc.getImportSummary(filterCaptor.capture())).thenReturn(new ImportSummary(0, 0));

        IndexStatisticsResource res = new IndexStatisticsResource(svc);
        res.getImportSummary("DC:test");

        assertEquals("DC:test", filterCaptor.getValue());
    }
}
