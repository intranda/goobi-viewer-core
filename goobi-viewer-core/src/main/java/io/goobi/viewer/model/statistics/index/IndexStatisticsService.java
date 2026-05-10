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

import io.goobi.viewer.api.rest.model.statistics.index.CollectionStatistic;
import io.goobi.viewer.api.rest.model.statistics.index.ImportSummary;
import io.goobi.viewer.api.rest.model.statistics.index.ImportTrendBucket;
import io.goobi.viewer.api.rest.model.statistics.index.LanguageStatistic;
import io.goobi.viewer.api.rest.model.statistics.index.PublicationCenturyStatistic;
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
    // Centuries: keyed by filter only (no locale-specific labels — the JS renderer formats the X-axis tick).
    private final Map<String, CachedSnapshot<List<PublicationCenturyStatistic>>> publicationCenturiesCache = new ConcurrentHashMap<>();
    // Languages and top-collections both translate labels server-side, so the cache key includes the locale tag.
    private final Map<String, CachedSnapshot<List<LanguageStatistic>>> languagesCache = new ConcurrentHashMap<>();
    private final Map<String, CachedSnapshot<List<CollectionStatistic>>> topCollectionsCache = new ConcurrentHashMap<>();

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
     * Histogram of works by publication century (e.g. century 18 = 1800–1899). Pre-modern centuries are dropped.
     *
     * @param filter optional Lucene sub-query to constrain the histogram; wrapped in MUST so it can only narrow.
     * @return DTOs ordered chronologically (oldest century first), never null
     * @throws StatisticsUnavailableException if Solr is unreachable and no cached snapshot exists for this filter
     * @should bucket years into centuries
     * @should sort centuries chronologically
     * @should drop pre-modern centuries
     * @should append filter to solr query when non-blank
     * @should treat null and empty filter as same cache slot
     * @should return cached snapshot on solr error if cache available
     * @should throw StatisticsUnavailableException on solr error if no cache
     */
    public List<PublicationCenturyStatistic> getPublicationCenturies(String filter) throws StatisticsUnavailableException {
        String key = cacheKey(filter);
        CachedSnapshot<List<PublicationCenturyStatistic>> snap = publicationCenturiesCache.get(key);
        if (snap != null && snap.isFresh(CACHE_TTL_MS)) {
            return snap.getValue();
        }
        try {
            String query = "+" + SolrConstants.PI + ":*"
                    + " +(" + SolrConstants.ISWORK + ":true " + SolrConstants.ISANCHOR + ":true)"
                    + appendFilter(filter)
                    + SearchHelper.getAllSuffixes();
            QueryResponse resp = searchIndex.search(query, 0, 0, null,
                    Collections.singletonList(SolrConstants.SORTNUM_YEAR), "count",
                    Collections.singletonList(SolrConstants.SORTNUM_YEAR), null, null);
            // Group facet rows by floor-divided century. Empty values, BCE/garbage years and pre-modern centuries are
            // dropped — the chart targets the modern publishing record and the X-axis label "-1. Jh." reads oddly.
            Map<Integer, Long> byCentury = new java.util.TreeMap<>();
            if (resp != null && resp.getFacetField(SolrConstants.SORTNUM_YEAR) != null
                    && resp.getFacetField(SolrConstants.SORTNUM_YEAR).getValues() != null) {
                for (Count c : resp.getFacetField(SolrConstants.SORTNUM_YEAR).getValues()) {
                    int year;
                    try {
                        year = Integer.parseInt(c.getName());
                    } catch (NumberFormatException e) {
                        // Empty or non-numeric — ignore.
                        continue;
                    }
                    int century = Math.floorDiv(year, 100);
                    if (century < 0) {
                        continue;
                    }
                    byCentury.merge(century, c.getCount(), Long::sum);
                }
            }
            List<PublicationCenturyStatistic> out = new ArrayList<>(byCentury.size());
            for (Map.Entry<Integer, Long> e : byCentury.entrySet()) {
                out.add(new PublicationCenturyStatistic(e.getKey(), e.getValue()));
            }
            publicationCenturiesCache.put(key, new CachedSnapshot<>(out, System.currentTimeMillis()));
            return out;
        } catch (PresentationException | IndexUnreachableException e) {
            logger.warn("Solr query for publication centuries failed; will serve cache if available: {}", e.getMessage());
            if (snap != null) {
                return snap.getValue();
            }
            throw new StatisticsUnavailableException("Solr query for publication centuries failed", e);
        }
    }

    /**
     * Distribution of works by language with locale-translated display names.
     *
     * @param locale the user's locale for label translation; falls back to default if null
     * @param filter optional Lucene sub-query to constrain the distribution; wrapped in MUST.
     * @return DTOs in Solr's facet order (by count, descending), never null
     * @throws StatisticsUnavailableException if Solr is unreachable and no cached snapshot exists
     * @should translate language codes via resource bundle
     * @should fall back to raw code when language translation is missing
     * @should append filter to solr query when non-blank
     * @should treat null and empty filter as same cache slot
     * @should throw StatisticsUnavailableException on solr error if no cache
     */
    public List<LanguageStatistic> getLanguages(Locale locale, String filter) throws StatisticsUnavailableException {
        String key = (locale != null ? locale.toLanguageTag() : "") + " " + cacheKey(filter);
        CachedSnapshot<List<LanguageStatistic>> snap = languagesCache.get(key);
        if (snap != null && snap.isFresh(CACHE_TTL_MS)) {
            return snap.getValue();
        }
        try {
            String query = "+" + SolrConstants.PI + ":*"
                    + " +(" + SolrConstants.ISWORK + ":true " + SolrConstants.ISANCHOR + ":true)"
                    + appendFilter(filter)
                    + SearchHelper.getAllSuffixes();
            // FACET_LANGUAGE is the indexer's facet-enabled twin of MD_LANGUAGE — same convention as FACET_DC for
            // collections (see SolrConstants.FACET_DC). The bare "LANGUAGE" / "MD_LANGUAGE" fields are stored but
            // not necessarily docValues-enabled, so faceting on them comes back empty on real instances.
            String facetField = "FACET_LANGUAGE";
            QueryResponse resp = searchIndex.search(query, 0, 0, null,
                    Collections.singletonList(facetField), "count",
                    Collections.singletonList(facetField), null, null);
            List<LanguageStatistic> out = new ArrayList<>();
            if (resp != null && resp.getFacetField(facetField) != null
                    && resp.getFacetField(facetField).getValues() != null) {
                for (Count c : resp.getFacetField(facetField).getValues()) {
                    String code = c.getName();
                    // The viewer's message bundles use the bare ISO code as the resource-bundle key (e.g. en=English).
                    // ViewerResourceBundle.getTranslation echoes the key back when no translation is found, which is
                    // exactly the raw-code fallback we want for unknown languages.
                    String label = ViewerResourceBundle.getTranslation(code, locale);
                    out.add(new LanguageStatistic(code, label, c.getCount()));
                }
            }
            languagesCache.put(key, new CachedSnapshot<>(out, System.currentTimeMillis()));
            return out;
        } catch (PresentationException | IndexUnreachableException e) {
            logger.warn("Solr query for languages failed; will serve cache if available: {}", e.getMessage());
            if (snap != null) {
                return snap.getValue();
            }
            throw new StatisticsUnavailableException("Solr query for languages failed", e);
        }
    }

    /**
     * Top {@code size} collections by record count, with locale-translated labels.
     *
     * @param size maximum number of collections to return
     * @param locale the user's locale for label translation; falls back to default if null
     * @param filter optional Lucene sub-query to constrain the collections; wrapped in MUST.
     * @return DTOs in Solr's facet order (by count, descending), never null
     * @throws StatisticsUnavailableException if Solr is unreachable and no cached snapshot exists
     * @should limit collections to requested size
     * @should translate collection names via resource bundle
     * @should append filter to solr query when non-blank
     * @should use separate cache slot per size
     * @should throw StatisticsUnavailableException on solr error if no cache
     */
    public List<CollectionStatistic> getTopCollections(int size, Locale locale, String filter) throws StatisticsUnavailableException {
        String key = size + " " + (locale != null ? locale.toLanguageTag() : "") + " " + cacheKey(filter);
        CachedSnapshot<List<CollectionStatistic>> snap = topCollectionsCache.get(key);
        if (snap != null && snap.isFresh(CACHE_TTL_MS)) {
            return snap.getValue();
        }
        try {
            String query = "+" + SolrConstants.PI + ":*"
                    + " +(" + SolrConstants.ISWORK + ":true " + SolrConstants.ISANCHOR + ":true)"
                    + appendFilter(filter)
                    + SearchHelper.getAllSuffixes();
            // SolrSearchIndex.search hardcodes setFacetLimit(-1); the params map runs after that and overrides it,
            // letting Solr return only the top {@code size} buckets directly instead of post-filtering in Java.
            Map<String, String> params = Map.of("facet.limit", String.valueOf(size));
            QueryResponse resp = searchIndex.search(query, 0, 0, null,
                    Collections.singletonList(SolrConstants.DC), "count",
                    Collections.singletonList(SolrConstants.DC), null, params);
            List<CollectionStatistic> out = new ArrayList<>();
            if (resp != null && resp.getFacetField(SolrConstants.DC) != null
                    && resp.getFacetField(SolrConstants.DC).getValues() != null) {
                for (Count c : resp.getFacetField(SolrConstants.DC).getValues()) {
                    String name = c.getName();
                    String label = ViewerResourceBundle.getTranslation(name, locale);
                    out.add(new CollectionStatistic(name, label, c.getCount()));
                }
            }
            topCollectionsCache.put(key, new CachedSnapshot<>(out, System.currentTimeMillis()));
            return out;
        } catch (PresentationException | IndexUnreachableException e) {
            logger.warn("Solr query for top collections failed; will serve cache if available: {}", e.getMessage());
            if (snap != null) {
                return snap.getValue();
            }
            throw new StatisticsUnavailableException("Solr query for top collections failed", e);
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
