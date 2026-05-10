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
package io.goobi.viewer.model.statistics.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.model.statistics.index.ImportSummary;
import io.goobi.viewer.api.rest.model.statistics.index.ImportTrendBucket;
import io.goobi.viewer.api.rest.model.statistics.index.PublicationTypeStatistic;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.solr.SolrSearchIndex;

// Extends AbstractTest because the production code calls ViewerResourceBundle.getTranslation(...),
// which requires the DataManager configuration to be initialised before resource bundles can load.
class IndexStatisticsServiceTest extends AbstractTest {

    /**
     * @see IndexStatisticsService#getPublicationTypes()
     * @verifies issue exact docstruct facet query and map facet values to dtos
     */
    @Test
    void getPublicationTypes_shouldIssueExactDocstructFacetQueryAndMapFacetValuesToDtos() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        QueryResponse resp = mock(QueryResponse.class);
        FacetField field = mock(FacetField.class);
        FacetField.Count monograph = new FacetField.Count(field, "Monograph", 50);
        FacetField.Count periodical = new FacetField.Count(field, "Periodical", 30);
        when(field.getValues()).thenReturn(Arrays.asList(monograph, periodical));
        when(resp.getFacetField("DOCSTRCT")).thenReturn(field);
        when(index.search(argThat(q -> q != null && q.startsWith("+PI:* +(ISWORK:true ISANCHOR:true) +DOCTYPE:DOCSTRCT")),
                eq(0), eq(0), isNull(),
                eq(Collections.singletonList("DOCSTRCT")), eq("count"),
                eq(Collections.singletonList("DOCSTRCT")), isNull(), isNull()))
                        .thenReturn(resp);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        List<PublicationTypeStatistic> result = svc.getPublicationTypes(null);

        assertEquals(2, result.size());
        assertEquals("Monograph", result.get(0).query());
        assertEquals(50L, result.get(0).count());
        verify(index).search(argThat(q -> q.startsWith("+PI:* +(ISWORK:true ISANCHOR:true) +DOCTYPE:DOCSTRCT")),
                eq(0), eq(0), isNull(), eq(Collections.singletonList("DOCSTRCT")), eq("count"),
                eq(Collections.singletonList("DOCSTRCT")), isNull(), isNull());
    }

    /**
     * @see IndexStatisticsService#getPublicationTypes()
     * @verifies throw StatisticsUnavailableException on solr error if no cache
     */
    @Test
    void getPublicationTypes_shouldThrowStatisticsUnavailableExceptionOnSolrErrorIfNoCache() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IndexUnreachableException("solr down"));
        IndexStatisticsService svc = new IndexStatisticsService(index);

        assertThrows(StatisticsUnavailableException.class, () -> svc.getPublicationTypes(null));
    }

    /**
     * @see IndexStatisticsService#getImportTrend(int, int)
     * @verifies return one bucket per data point with cumulative counts
     */
    @Test
    void getImportTrend_shouldReturnOneBucketPerDataPointWithCumulativeCounts() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        QueryResponse resp = mock(QueryResponse.class);
        FacetField field = mock(FacetField.class);
        long now = System.currentTimeMillis();
        // Both creation timestamps lie in the past, so they should fall into the newest-first bucket at index 0.
        FacetField.Count first = new FacetField.Count(field, Long.toString(now - 86_400_000L * 30L), 1);
        FacetField.Count second = new FacetField.Count(field, Long.toString(now - 86_400_000L * 5L), 1);
        when(field.getValues()).thenReturn(Arrays.asList(first, second));
        when(resp.getFacetField("DATECREATED")).thenReturn(field);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any())).thenReturn(resp);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        List<ImportTrendBucket> result = svc.getImportTrend(180, 12);

        assertEquals(12, result.size());
        // The current epoch bucket is at index 0; its count must include both sample timestamps.
        assertEquals(2, result.get(0).count());
    }

    /**
     * @see IndexStatisticsService#getImportTrend(int, int)
     * @verifies throw StatisticsUnavailableException on solr error if no cache
     */
    @Test
    void getImportTrend_shouldThrowStatisticsUnavailableExceptionOnSolrErrorIfNoCache() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IndexUnreachableException("solr down"));
        IndexStatisticsService svc = new IndexStatisticsService(index);

        assertThrows(StatisticsUnavailableException.class, () -> svc.getImportTrend(180, 12));
    }

    /**
     * @see IndexStatisticsService#getImportSummary()
     * @verifies return aggregated page and fulltext counts
     */
    @Test
    void getImportSummary_shouldReturnAggregatedPageAndFulltextCounts() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        // Two argThat matchers differentiate the two queries:
        //   pagesQuery: contains "+DOCTYPE:PAGE" but NOT "+FULLTEXTAVAILABLE:true"
        //   fulltextQuery: contains "+FULLTEXTAVAILABLE:true"
        when(index.getHitCount(argThat(q -> q != null && q.contains("+DOCTYPE:PAGE") && !q.contains("FULLTEXTAVAILABLE"))))
                .thenReturn(100L);
        when(index.getHitCount(argThat(q -> q != null && q.contains("+FULLTEXTAVAILABLE:true"))))
                .thenReturn(40L);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        ImportSummary summary = svc.getImportSummary();

        assertEquals(100L, summary.pages());
        assertEquals(40L, summary.fulltexts());
    }

    /**
     * @see IndexStatisticsService#getImportSummary()
     * @verifies throw StatisticsUnavailableException on solr error if no cache
     */
    @Test
    void getImportSummary_shouldThrowStatisticsUnavailableExceptionOnSolrErrorIfNoCache() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        when(index.getHitCount(any()))
                .thenThrow(new IndexUnreachableException("solr down"));
        IndexStatisticsService svc = new IndexStatisticsService(index);

        assertThrows(StatisticsUnavailableException.class, svc::getImportSummary);
    }
}
