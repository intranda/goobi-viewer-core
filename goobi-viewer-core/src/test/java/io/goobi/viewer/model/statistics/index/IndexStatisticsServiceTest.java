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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.model.statistics.index.CollectionStatistic;
import io.goobi.viewer.api.rest.model.statistics.index.ImportSummary;
import io.goobi.viewer.api.rest.model.statistics.index.ImportTrendBucket;
import io.goobi.viewer.api.rest.model.statistics.index.LanguageStatistic;
import io.goobi.viewer.api.rest.model.statistics.index.PublicationCenturyStatistic;
import io.goobi.viewer.api.rest.model.statistics.index.PublicationTypeStatistic;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.solr.SolrSearchIndex;

// Extends AbstractTest because the production code calls ViewerResourceBundle.getTranslation(...),
// which requires the DataManager configuration to be initialised before resource bundles can load.
class IndexStatisticsServiceTest extends AbstractTest {

    /**
     * @see IndexStatisticsService#getPublicationTypes(java.util.Locale, String)
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
        List<PublicationTypeStatistic> result = svc.getPublicationTypes(null, null);

        assertEquals(2, result.size());
        assertEquals("Monograph", result.get(0).query());
        assertEquals(50L, result.get(0).count());
        verify(index).search(argThat(q -> q.startsWith("+PI:* +(ISWORK:true ISANCHOR:true) +DOCTYPE:DOCSTRCT")),
                eq(0), eq(0), isNull(), eq(Collections.singletonList("DOCSTRCT")), eq("count"),
                eq(Collections.singletonList("DOCSTRCT")), isNull(), isNull());
    }

    /**
     * @see IndexStatisticsService#getPublicationTypes(java.util.Locale, String)
     * @verifies append filter to solr query when non-blank
     */
    @Test
    void getPublicationTypes_shouldAppendFilterToSolrQueryWhenNonBlank() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        QueryResponse resp = mock(QueryResponse.class);
        FacetField field = mock(FacetField.class);
        when(field.getValues()).thenReturn(Collections.emptyList());
        when(resp.getFacetField("DOCSTRCT")).thenReturn(field);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any())).thenReturn(resp);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        svc.getPublicationTypes(null, "DC:zeitschriften");

        // The filter must appear wrapped in MUST so the surrounding query can only narrow, never broaden.
        verify(index).search(argThat(q -> q != null && q.contains(" +(DC:zeitschriften)")),
                eq(0), eq(0), isNull(), any(), any(), any(), isNull(), isNull());
    }

    /**
     * @see IndexStatisticsService#getPublicationTypes(java.util.Locale, String)
     * @verifies treat null and empty filter as same cache slot
     */
    @Test
    void getPublicationTypes_shouldTreatNullAndEmptyFilterAsSameCacheSlot() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        QueryResponse resp = mock(QueryResponse.class);
        FacetField field = mock(FacetField.class);
        when(field.getValues()).thenReturn(Collections.emptyList());
        when(resp.getFacetField("DOCSTRCT")).thenReturn(field);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any())).thenReturn(resp);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        svc.getPublicationTypes(null, null);
        svc.getPublicationTypes(null, "");

        // Both calls share the same cache slot — Solr must be hit only once.
        verify(index, times(1)).search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any());
    }

    /**
     * @see IndexStatisticsService#getPublicationTypes(java.util.Locale, String)
     * @verifies throw StatisticsUnavailableException on solr error if no cache
     */
    @Test
    void getPublicationTypes_shouldThrowStatisticsUnavailableExceptionOnSolrErrorIfNoCache() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IndexUnreachableException("solr down"));
        IndexStatisticsService svc = new IndexStatisticsService(index);

        assertThrows(StatisticsUnavailableException.class, () -> svc.getPublicationTypes(null, null));
    }

    /**
     * @see IndexStatisticsService#getImportTrend(int, int, String)
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
        List<ImportTrendBucket> result = svc.getImportTrend(180, 12, null);

        assertEquals(12, result.size());
        // The current epoch bucket is at index 0; its count must include both sample timestamps.
        assertEquals(2, result.get(0).count());
    }

    /**
     * @see IndexStatisticsService#getImportTrend(int, int, String)
     * @verifies use separate cache slot per filter
     */
    @Test
    void getImportTrend_shouldUseSeparateCacheSlotPerFilter() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        QueryResponse resp = mock(QueryResponse.class);
        FacetField field = mock(FacetField.class);
        when(field.getValues()).thenReturn(Collections.emptyList());
        when(resp.getFacetField("DATECREATED")).thenReturn(field);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any())).thenReturn(resp);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        svc.getImportTrend(180, 12, "DC:zeitschriften");
        svc.getImportTrend(180, 12, "DC:bilder");

        // Two different filters must NOT share a cache slot.
        verify(index, times(2)).search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any());
    }

    /**
     * @see IndexStatisticsService#getImportTrend(int, int, String)
     * @verifies throw StatisticsUnavailableException on solr error if no cache
     */
    @Test
    void getImportTrend_shouldThrowStatisticsUnavailableExceptionOnSolrErrorIfNoCache() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IndexUnreachableException("solr down"));
        IndexStatisticsService svc = new IndexStatisticsService(index);

        assertThrows(StatisticsUnavailableException.class, () -> svc.getImportTrend(180, 12, null));
    }

    /**
     * @see IndexStatisticsService#getImportSummary(String)
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
        ImportSummary summary = svc.getImportSummary(null);

        assertEquals(100L, summary.pages());
        assertEquals(40L, summary.fulltexts());
    }

    /**
     * @see IndexStatisticsService#getImportSummary(String)
     * @verifies apply filter to both page and fulltext queries
     */
    @Test
    void getImportSummary_shouldApplyFilterToBothPageAndFulltextQueries() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        when(index.getHitCount(any())).thenReturn(0L);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        svc.getImportSummary("DC:zeitschriften");

        // The filter must be appended to both the page-count and the fulltext-count queries — otherwise the fulltext
        // count could exceed the page count and break the chart's ratio display.
        verify(index).getHitCount(argThat(q -> q != null && q.contains("+DOCTYPE:PAGE")
                && !q.contains("FULLTEXTAVAILABLE")
                && q.contains(" +(DC:zeitschriften)")));
        verify(index).getHitCount(argThat(q -> q != null && q.contains("+FULLTEXTAVAILABLE:true")
                && q.contains(" +(DC:zeitschriften)")));
    }

    /**
     * @see IndexStatisticsService#getImportSummary(String)
     * @verifies throw StatisticsUnavailableException on solr error if no cache
     */
    @Test
    void getImportSummary_shouldThrowStatisticsUnavailableExceptionOnSolrErrorIfNoCache() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        when(index.getHitCount(any()))
                .thenThrow(new IndexUnreachableException("solr down"));
        IndexStatisticsService svc = new IndexStatisticsService(index);

        assertThrows(StatisticsUnavailableException.class, () -> svc.getImportSummary(null));
    }

    /**
     * @see IndexStatisticsService#getPublicationCenturies(String)
     * @verifies bucket years into centuries
     * @verifies sort centuries chronologically
     * @verifies drop pre-modern centuries
     */
    @Test
    void getPublicationCenturies_shouldBucketYearsIntoCenturiesAndSortChronologicallyAndDropPreModern() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        QueryResponse resp = mock(QueryResponse.class);
        FacetField field = mock(FacetField.class);
        // Sample years: 1873 + 1899 → century 18 (count 5); 1900 → century 19 (count 1); 2010 → century 20 (count 2);
        // -50 (BCE) and "" (empty) must be dropped.
        FacetField.Count y1873 = new FacetField.Count(field, "1873", 3);
        FacetField.Count y1899 = new FacetField.Count(field, "1899", 2);
        FacetField.Count y1900 = new FacetField.Count(field, "1900", 1);
        FacetField.Count y2010 = new FacetField.Count(field, "2010", 2);
        FacetField.Count yBCE = new FacetField.Count(field, "-50", 9);
        FacetField.Count yEmpty = new FacetField.Count(field, "", 4);
        when(field.getValues()).thenReturn(Arrays.asList(y1873, y1899, y1900, y2010, yBCE, yEmpty));
        when(resp.getFacetField("SORTNUM_YEAR")).thenReturn(field);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any())).thenReturn(resp);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        List<PublicationCenturyStatistic> result = svc.getPublicationCenturies(null);

        // Three buckets: 18 (1800–1899), 19 (1900–1999), 20 (2000–2099). BCE and empty are dropped.
        assertEquals(3, result.size());
        assertEquals(18, result.get(0).century());
        assertEquals(5L, result.get(0).count());
        assertEquals(19, result.get(1).century());
        assertEquals(1L, result.get(1).count());
        assertEquals(20, result.get(2).century());
        assertEquals(2L, result.get(2).count());
    }

    /**
     * @see IndexStatisticsService#getPublicationCenturies(String)
     * @verifies append filter to solr query when non-blank
     */
    @Test
    void getPublicationCenturies_shouldAppendFilterToSolrQueryWhenNonBlank() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        QueryResponse resp = mock(QueryResponse.class);
        FacetField field = mock(FacetField.class);
        when(field.getValues()).thenReturn(Collections.emptyList());
        when(resp.getFacetField("SORTNUM_YEAR")).thenReturn(field);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any())).thenReturn(resp);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        svc.getPublicationCenturies("DC:zeitschriften");

        verify(index).search(argThat(q -> q != null && q.contains(" +(DC:zeitschriften)")),
                eq(0), eq(0), isNull(), any(), any(), any(), isNull(), isNull());
    }

    /**
     * @see IndexStatisticsService#getPublicationCenturies(String)
     * @verifies throw StatisticsUnavailableException on solr error if no cache
     */
    @Test
    void getPublicationCenturies_shouldThrowStatisticsUnavailableExceptionOnSolrErrorIfNoCache() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IndexUnreachableException("solr down"));
        IndexStatisticsService svc = new IndexStatisticsService(index);

        assertThrows(StatisticsUnavailableException.class, () -> svc.getPublicationCenturies(null));
    }

    /**
     * @see IndexStatisticsService#getLanguages(java.util.Locale, String)
     * @verifies translate language codes via resource bundle
     * @verifies fall back to raw code when language translation is missing
     */
    @Test
    void getLanguages_shouldTranslateLanguageCodesViaResourceBundle() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        QueryResponse resp = mock(QueryResponse.class);
        FacetField field = mock(FacetField.class);
        // "en" exists in messages_en.properties as en=English; "xyz" has no translation and the bundle echoes the key.
        FacetField.Count en = new FacetField.Count(field, "en", 100);
        FacetField.Count xyz = new FacetField.Count(field, "xyz", 5);
        when(field.getValues()).thenReturn(Arrays.asList(en, xyz));
        when(resp.getFacetField("FACET_LANGUAGE")).thenReturn(field);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any())).thenReturn(resp);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        List<LanguageStatistic> result = svc.getLanguages(java.util.Locale.ENGLISH, null);

        assertEquals(2, result.size());
        assertEquals("en", result.get(0).code());
        assertEquals("English", result.get(0).label());
        // For unknown codes, ViewerResourceBundle.getTranslation echoes the key — that's the raw-code fallback.
        assertEquals("xyz", result.get(1).code());
        assertEquals("xyz", result.get(1).label());
    }

    /**
     * @see IndexStatisticsService#getLanguages(java.util.Locale, String)
     * @verifies append filter to solr query when non-blank
     */
    @Test
    void getLanguages_shouldAppendFilterToSolrQueryWhenNonBlank() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        QueryResponse resp = mock(QueryResponse.class);
        FacetField field = mock(FacetField.class);
        when(field.getValues()).thenReturn(Collections.emptyList());
        when(resp.getFacetField("FACET_LANGUAGE")).thenReturn(field);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any())).thenReturn(resp);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        svc.getLanguages(null, "DC:bilder");

        verify(index).search(argThat(q -> q != null && q.contains(" +(DC:bilder)")),
                eq(0), eq(0), isNull(), any(), any(), any(), isNull(), isNull());
    }

    /**
     * @see IndexStatisticsService#getLanguages(java.util.Locale, String)
     * @verifies throw StatisticsUnavailableException on solr error if no cache
     */
    @Test
    void getLanguages_shouldThrowStatisticsUnavailableExceptionOnSolrErrorIfNoCache() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IndexUnreachableException("solr down"));
        IndexStatisticsService svc = new IndexStatisticsService(index);

        assertThrows(StatisticsUnavailableException.class, () -> svc.getLanguages(null, null));
    }

    /**
     * @see IndexStatisticsService#getTopCollections(int, java.util.Locale, String)
     * @verifies limit collections to requested size
     */
    @Test
    void getTopCollections_shouldLimitCollectionsToRequestedSize() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        QueryResponse resp = mock(QueryResponse.class);
        FacetField field = mock(FacetField.class);
        when(field.getValues()).thenReturn(Collections.emptyList());
        when(resp.getFacetField("DC")).thenReturn(field);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any())).thenReturn(resp);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        svc.getTopCollections(5, null, null);

        // The 9th argument (params) must carry facet.limit=5; without that override SolrSearchIndex's hardcoded
        // setFacetLimit(-1) returns ALL DC values and the size parameter is silently ignored.
        verify(index).search(any(), eq(0), eq(0), isNull(), any(), any(), any(), isNull(),
                argThat((java.util.Map<String, String> p) -> p != null && "5".equals(p.get("facet.limit"))));
    }

    /**
     * @see IndexStatisticsService#getTopCollections(int, java.util.Locale, String)
     * @verifies append filter to solr query when non-blank
     */
    @Test
    void getTopCollections_shouldAppendFilterToSolrQueryWhenNonBlank() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        QueryResponse resp = mock(QueryResponse.class);
        FacetField field = mock(FacetField.class);
        FacetField.Count dc = new FacetField.Count(field, "newspapers", 42);
        when(field.getValues()).thenReturn(Arrays.asList(dc));
        when(resp.getFacetField("DC")).thenReturn(field);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any())).thenReturn(resp);

        IndexStatisticsService svc = new IndexStatisticsService(index);
        List<CollectionStatistic> result = svc.getTopCollections(10, null, "DC:zeitschriften");

        assertEquals(1, result.size());
        assertEquals("newspapers", result.get(0).name());
        verify(index).search(argThat(q -> q != null && q.contains(" +(DC:zeitschriften)")),
                eq(0), eq(0), isNull(), any(), any(), any(), isNull(), any());
    }

    /**
     * @see IndexStatisticsService#getTopCollections(int, java.util.Locale, String)
     * @verifies throw StatisticsUnavailableException on solr error if no cache
     */
    @Test
    void getTopCollections_shouldThrowStatisticsUnavailableExceptionOnSolrErrorIfNoCache() throws Exception {
        SolrSearchIndex index = mock(SolrSearchIndex.class);
        when(index.search(any(), eq(0), eq(0), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IndexUnreachableException("solr down"));
        IndexStatisticsService svc = new IndexStatisticsService(index);

        assertThrows(StatisticsUnavailableException.class, () -> svc.getTopCollections(10, null, null));
    }
}
