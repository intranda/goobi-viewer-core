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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;

import io.goobi.viewer.api.rest.model.statistics.index.ImportSummary;
import io.goobi.viewer.api.rest.model.statistics.index.ImportTrendBucket;
import io.goobi.viewer.api.rest.model.statistics.index.PublicationTypeStatistic;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * Aggregations over the Solr index for index-level statistic charts. Constructor takes a {@link SolrSearchIndex}
 * dependency so unit tests can mock it; production callers use the no-arg constructor that pulls the singleton.
 *
 * <p>
 * Each method caches its result for one day and throws {@link StatisticsUnavailableException} when the upstream is
 * down and no cached snapshot is available, so the resource layer can translate to HTTP 503 instead of silently
 * returning empty data.
 * </p>
 */
public class IndexStatisticsService {

    private static final Logger logger = LogManager.getLogger(IndexStatisticsService.class);

    private static final long CACHE_TTL_MS = 24L * 60L * 60L * 1000L;

    private final SolrSearchIndex searchIndex;

    // Each method caches per filter (and additionally per locale or per (days, dataPoints) configuration). Separator
    // between key components is a literal space because BCP-47 language tags never contain whitespace, so the
    // composition is unambiguous and stays human-readable in log output.
    private final Map<String, CachedSnapshot<List<PublicationTypeStatistic>>> publicationTypesCachePerLocale = new ConcurrentHashMap<>();
    private final Map<String, CachedTrend> importTrendCache = new ConcurrentHashMap<>();
    private final Map<String, CachedSnapshot<ImportSummary>> importSummaryCache = new ConcurrentHashMap<>();

    public IndexStatisticsService() {
        this(DataManager.getInstance().getSearchIndex());
    }

    public IndexStatisticsService(SolrSearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    /**
     * Wraps an admin-supplied Solr filter in a MUST clause so it can only narrow the surrounding query. This is a
     * security-relevant invariant: even a clever filter like {@code "*:* OR -DC:hidden"} cannot escape the surrounding
     * {@code "+(...)"} envelope and broaden access rules established by {@link SearchHelper#getAllSuffixes()}.
     *
     * @param filter raw Solr sub-query supplied by a CMS admin; may be null or blank
     * @return either an empty string or {@code " +(<filter>)"} ready to splice into an existing query
     */
    private static String appendFilter(String filter) {
        return (filter != null && !filter.isBlank()) ? " +(" + filter + ")" : "";
    }

    /** Canonical cache-key fragment for a filter. Treats null and blank as the same "no filter" slot. */
    private static String cacheKey(String filter) {
        return (filter != null && !filter.isBlank()) ? filter : "";
    }

    /**
     * Lists each top-level docstruct type with its record count, with labels translated to the given locale.
     *
     * @param locale the user's locale for label translation; falls back to default if null
     * @param filter optional Lucene sub-query to constrain the docstructs; wrapped in MUST so it can only narrow.
     *            null or blank means no constraint.
     * @return DTOs in Solr's facet order (by count, descending), never null
     * @throws StatisticsUnavailableException if Solr is unreachable and no cached snapshot exists for this locale+filter
     * @should issue exact docstruct facet query and map facet values to dtos
     * @should append filter to solr query when non-blank
     * @should treat null and empty filter as same cache slot
     * @should return cached snapshot on solr error if cache available
     * @should throw StatisticsUnavailableException on solr error if no cache
     */
    public List<PublicationTypeStatistic> getPublicationTypes(Locale locale, String filter) throws StatisticsUnavailableException {
        // Key composition: BCP-47 language tag (or "") plus a space plus the canonical filter slot. Whitespace is
        // never part of a language tag, so the two halves are always cleanly separable.
        String key = (locale != null ? locale.toLanguageTag() : "") + " " + cacheKey(filter);
        CachedSnapshot<List<PublicationTypeStatistic>> snap = publicationTypesCachePerLocale.get(key);
        if (snap != null && snap.isFresh(CACHE_TTL_MS)) {
            return snap.getValue();
        }
        try {
            String query = "+" + SolrConstants.PI + ":*"
                    + " +(" + SolrConstants.ISWORK + ":true " + SolrConstants.ISANCHOR + ":true)"
                    + " +" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name()
                    + appendFilter(filter)
                    + SearchHelper.getAllSuffixes();
            QueryResponse resp = searchIndex.search(query, 0, 0, null,
                    Collections.singletonList(SolrConstants.DOCSTRCT), "count",
                    Collections.singletonList(SolrConstants.DOCSTRCT), null, null);
            if (resp == null || resp.getFacetField(SolrConstants.DOCSTRCT) == null
                    || resp.getFacetField(SolrConstants.DOCSTRCT).getValues() == null) {
                List<PublicationTypeStatistic> empty = Collections.emptyList();
                publicationTypesCachePerLocale.put(key, new CachedSnapshot<>(empty, System.currentTimeMillis()));
                return empty;
            }
            List<Count> counts = resp.getFacetField(SolrConstants.DOCSTRCT).getValues();
            List<PublicationTypeStatistic> out = new ArrayList<>(counts.size());
            for (Count count : counts) {
                String label = ViewerResourceBundle.getTranslation(count.getName(), locale).replace(",", "");
                out.add(new PublicationTypeStatistic(label, count.getCount(), count.getName()));
            }
            publicationTypesCachePerLocale.put(key, new CachedSnapshot<>(out, System.currentTimeMillis()));
            return out;
        } catch (PresentationException | IndexUnreachableException e) {
            logger.warn("Solr query for publication types failed; will serve cache if available: {}", e.getMessage());
            if (snap != null) {
                return snap.getValue();
            }
            throw new StatisticsUnavailableException("Solr query for publication types failed", e);
        }
    }

    /**
     * Builds a cumulative-count time-series suitable for a line chart.
     *
     * @param days size of the look-back window
     * @param dataPoints number of buckets to produce; capped at {@code days}
     * @param filter optional Lucene sub-query to constrain the time-series; wrapped in MUST so it can only narrow.
     *            null or blank means no constraint.
     * @return DTOs ordered newest first, never null
     * @throws StatisticsUnavailableException if Solr is unreachable and no compatible cached snapshot exists
     * @should return one bucket per data point with cumulative counts
     * @should clamp dataPoints to days when dataPoints exceeds days
     * @should use separate cache slot per filter
     * @should return cached snapshot on solr error if cache available
     * @should throw StatisticsUnavailableException on solr error if no cache
     */
    public List<ImportTrendBucket> getImportTrend(int days, int dataPoints, String filter) throws StatisticsUnavailableException {
        String key = cacheKey(filter);
        CachedTrend snap = importTrendCache.get(key);
        if (snap != null && snap.matches(days, dataPoints) && snap.isFresh(CACHE_TTL_MS)) {
            return snap.getValue();
        }
        try {
            List<ImportTrendBucket> out = computeImportTrend(days, dataPoints, filter);
            importTrendCache.put(key, new CachedTrend(days, dataPoints, out, System.currentTimeMillis()));
            return out;
        } catch (PresentationException | IndexUnreachableException e) {
            logger.warn("Solr query for import trend failed; will serve cache if available: {}", e.getMessage());
            if (snap != null && snap.matches(days, dataPoints)) {
                return snap.getValue();
            }
            throw new StatisticsUnavailableException("Solr query for import trend failed", e);
        }
    }

    /** Solr-query body, separated so the public method can wrap it in cache+throw logic. */
    private List<ImportTrendBucket> computeImportTrend(int days, int dataPoints, String filter)
            throws PresentationException, IndexUnreachableException {
        String query = "+" + SolrConstants.PI + ":*" + appendFilter(filter) + SearchHelper.getAllSuffixes();
        QueryResponse resp = searchIndex.search(query, 0, 0, null,
                Collections.singletonList(SolrConstants.DATECREATED), "count",
                Collections.singletonList(SolrConstants.DATECREATED), null, null);
        List<String> dateList = new ArrayList<>();
        if (resp != null && resp.getFacetField(SolrConstants.DATECREATED) != null
                && resp.getFacetField(SolrConstants.DATECREATED).getValues() != null) {
            for (Count count : resp.getFacetField(SolrConstants.DATECREATED).getValues()) {
                dateList.add(ViewerResourceBundle.getTranslation(count.getName(), null));
            }
        }

        int buckets = Math.min(dataPoints, days);
        int dayDiv = days / Math.max(buckets, 1);
        List<Long> timestamps = new ArrayList<>(buckets);
        long[] counts = new long[buckets];
        timestamps.add(System.currentTimeMillis());
        GregorianCalendar cal = new GregorianCalendar();
        for (int i = 1; i < buckets; i++) {
            cal.add(Calendar.DAY_OF_MONTH, -dayDiv);
            timestamps.add(cal.getTime().getTime());
        }
        // Sort newest-first.
        timestamps.sort(Comparator.reverseOrder());

        for (String s : dateList) {
            long created = Long.parseLong(s);
            for (int i = 0; i < timestamps.size(); i++) {
                if (created < timestamps.get(i)) {
                    counts[i]++;
                }
            }
        }
        List<ImportTrendBucket> out = new ArrayList<>(buckets);
        for (int i = 0; i < buckets; i++) {
            out.add(new ImportTrendBucket(timestamps.get(i), counts[i]));
        }
        return out;
    }

    /**
     * Total page and full-text counts in the index.
     *
     * @param filter optional Lucene sub-query to constrain both counts; wrapped in MUST so it can only narrow.
     *            null or blank means no constraint.
     * @return summary; both numbers populated from Solr or returned from cache on error
     * @throws StatisticsUnavailableException if Solr is unreachable and no cached snapshot exists
     * @should return aggregated page and fulltext counts
     * @should apply filter to both page and fulltext queries
     * @should treat null and empty filter as same cache slot
     * @should return cached snapshot on solr error if cache available
     * @should throw StatisticsUnavailableException on solr error if no cache
     */
    public ImportSummary getImportSummary(String filter) throws StatisticsUnavailableException {
        String key = cacheKey(filter);
        CachedSnapshot<ImportSummary> snap = importSummaryCache.get(key);
        if (snap != null && snap.isFresh(CACHE_TTL_MS)) {
            return snap.getValue();
        }
        try {
            long pages = searchIndex.getHitCount(
                    "+" + SolrConstants.DOCTYPE + ":" + DocType.PAGE.name()
                            + appendFilter(filter)
                            + SearchHelper.getAllSuffixes());
            long fulltexts = searchIndex.getHitCount(
                    "+" + SolrConstants.DOCTYPE + ":" + DocType.PAGE.name()
                            + " +" + SolrConstants.FULLTEXTAVAILABLE + ":true"
                            + appendFilter(filter)
                            + SearchHelper.getAllSuffixes());
            ImportSummary summary = new ImportSummary(pages, fulltexts);
            importSummaryCache.put(key, new CachedSnapshot<>(summary, System.currentTimeMillis()));
            return summary;
        } catch (PresentationException | IndexUnreachableException e) {
            logger.warn("Solr query for import summary failed; will serve cache if available: {}", e.getMessage());
            if (snap != null) {
                return snap.getValue();
            }
            throw new StatisticsUnavailableException("Solr query for import summary failed", e);
        }
    }

    /**
     * Cached value with timestamp. Volatile-guarded for safe publication; replacement is whole-snapshot, no torn
     * reads. (Same pattern the legacy bean uses, just typed and consolidated.)
     */
    static final class CachedSnapshot<T> {
        // Private fields with accessors to satisfy the project Checkstyle "VisibilityModifier" rule.
        private final T value;
        private final long timestamp;

        CachedSnapshot(T value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        T getValue() {
            return value;
        }

        boolean isFresh(long ttlMillis) {
            return (System.currentTimeMillis() - timestamp) < ttlMillis;
        }
    }

    /** Cache slot keyed on (days, dataPoints) so different chart configurations don't collide. */
    private static final class CachedTrend {
        private final int days;
        private final int dataPoints;
        private final List<ImportTrendBucket> value;
        private final long timestamp;

        CachedTrend(int days, int dataPoints, List<ImportTrendBucket> value, long timestamp) {
            this.days = days;
            this.dataPoints = dataPoints;
            this.value = value;
            this.timestamp = timestamp;
        }

        List<ImportTrendBucket> getValue() {
            return value;
        }

        boolean matches(int d, int p) {
            return this.days == d && this.dataPoints == p;
        }

        boolean isFresh(long ttlMillis) {
            return (System.currentTimeMillis() - timestamp) < ttlMillis;
        }
    }
}
