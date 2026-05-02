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
package io.goobi.viewer.controller.imaging;

import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.unigoettingen.sub.commons.cache.SourceImageCache;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

/**
 * Asynchronously triggers the ContentServer's source-image cache for the page the user just
 * navigated to, so the OpenSeadragon tile burst that follows finds the master image already
 * decoded and resident in the {@link SourceImageCache}.
 *
 * <p>Without prewarm, the first viewer of a cold-cache page pays the full source-decode latency
 * spread across the parallel tile requests: every tile thread either ROI-decodes the source
 * itself (cache disabled) or waits for the burst-coalesce decoder (cache enabled but cold —
 * the first tile triggers the decode, every subsequent tile waits). With prewarm, the decoder
 * runs once during HTML render — by the time the browser starts requesting tiles, the cache
 * already holds the decoded {@code BufferedImage} and every tile is served via cheap
 * {@link java.awt.image.BufferedImage#getSubimage}.
 *
 * <p>The service is a JVM-wide singleton because the underlying {@link SourceImageCache} is
 * itself a singleton and a single bounded executor avoids one Tomcat session monopolising
 * decode threads at the expense of others. Tasks are submitted to a small pool (4 threads) with
 * a bounded queue; overflow is dropped silently — prewarm is best-effort, never load-bearing.
 *
 * <p>The decode runs on a worker thread that calls {@code getRenderedImage(null)} on a fresh
 * {@link ImageManager}. That call hits {@code AbstractImageInterpreter}'s cache hook which —
 * after the prewarm thread's own {@code registerRequestAndIsBurst} primer — takes the
 * burst-coalesce path and lands in {@link SourceImageCache#getOrDecodeFull}. The first thread
 * (us) wins the race, decodes the master, and populates the cache. Tile threads arriving
 * concurrently see the in-flight {@link java.util.concurrent.CompletableFuture} and wait,
 * which is exactly the desired coalescing behavior.
 */
public final class SourceImagePrewarmService {

    private static final Logger logger = LogManager.getLogger(SourceImagePrewarmService.class);

    private static final int POOL_SIZE = 4;
    // Bounded queue: a viewer that page-flips faster than the decoder can keep up should not
    // accumulate unbounded backlog. 64 is enough for normal browsing while keeping memory bounded.
    private static final int QUEUE_CAPACITY = 64;
    private static final long IDLE_THREAD_KEEP_ALIVE_SECONDS = 60L;

    private static final SourceImagePrewarmService INSTANCE = new SourceImagePrewarmService();

    public static SourceImagePrewarmService getInstance() {
        return INSTANCE;
    }

    private final ThreadPoolExecutor executor;
    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong skippedAlreadyCached = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    private SourceImagePrewarmService() {
        this.executor = new ThreadPoolExecutor(
                POOL_SIZE, POOL_SIZE,
                IDLE_THREAD_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new PrewarmThreadFactory(),
                // Drop on overflow: prewarm is best-effort. The tile burst will fall back to
                // the regular per-request decode path, which still works (just slower).
                new ThreadPoolExecutor.DiscardPolicy());
        // Allow the core threads to time out so an idle viewer JVM doesn't keep 4 threads alive forever.
        this.executor.allowCoreThreadTimeOut(true);
    }

    /**
     * Resolves the source-image URI of the given page and submits an async prewarm. Silently
     * returns when the page is null, has no usable file path, or fails to resolve — prewarm is
     * best-effort and never throws to the caller.
     *
     * @param page page whose master image should be pre-decoded; may be null
     */
    public void prewarm(PhysicalElement page) {
        if (page == null) {
            return;
        }
        try {
            URI uri = resolveSourceUri(page);
            if (uri != null) {
                prewarm(uri);
            }
        } catch (PresentationException | IndexUnreachableException | IllegalArgumentException e) {
            logger.debug("Prewarm URI resolution failed for page {} of {}: {}",
                    page.getOrder(), page.getPi(), e.getMessage());
        }
    }

    /**
     * Resolves the local source-image URI of a page, mirroring the lookup done by
     * {@code ImageHandler#getImageInformation(PhysicalElement)}. Returns null for external URLs
     * (those handled by a remote IIIF server, where prewarming a local cache is meaningless) and
     * for blank file paths.
     */
    private URI resolveSourceUri(PhysicalElement page) throws PresentationException, IndexUnreachableException {
        String filepath = page.getFilepath();
        if (StringUtils.isBlank(filepath)) {
            return null;
        }
        // External (http/https) sources are served by a remote IIIF endpoint; the local
        // SourceImageCache cannot help with those.
        if (ImageHandler.isExternalUrl(filepath)) {
            return null;
        }
        Path dataPath = DataFileTools.getDataFilePath(page.getPi(),
                DataManager.getInstance().getConfiguration().getMediaFolder(), null, filepath);
        return dataPath.toUri();
    }

    /**
     * Submits an async prewarm for the page identified by record PI and 1-based page order.
     * Returns immediately. The Solr lookup that resolves PI+order to the master file path runs
     * on a worker thread, never on the caller's thread (typically a servlet request thread that
     * must hand the response back to the user as fast as possible).
     *
     * <p>Used by {@code PrewarmRequestFilter} which fires before JSF is even involved — earliest
     * possible point in the request lifecycle to start an async master-image decode.
     *
     * @param pi record PI; null/blank short-circuits
     * @param pageOrder 1-based page order; values &lt; 1 short-circuit
     */
    public void prewarmByPiAndPage(String pi, int pageOrder) {
        if (StringUtils.isBlank(pi) || pageOrder < 1) {
            return;
        }
        if (!DataManager.getInstance().getConfiguration().isPrewarmSourceImageCache()) {
            return;
        }
        if (!SourceImageCache.getInstance().isEnabled()) {
            return;
        }
        try {
            executor.execute(() -> resolveAndPrewarm(pi, pageOrder));
            submitted.incrementAndGet();
        } catch (RejectedExecutionException e) {
            dropped.incrementAndGet();
            logger.debug("Prewarm submit dropped for {} page {}: {}", pi, pageOrder, e.getMessage());
        }
    }

    /**
     * Worker-side companion to {@link #prewarmByPiAndPage}. Resolves the master URI via Solr,
     * then runs the decode through the same pipeline as the URI-direct path. Failures are
     * swallowed and counted because prewarm is best-effort.
     */
    private void resolveAndPrewarm(String pi, int pageOrder) {
        try {
            String filename = lookupFilename(pi, pageOrder);
            if (StringUtils.isBlank(filename) || ImageHandler.isExternalUrl(filename)) {
                // No local file to decode (record might not exist, page might be remote-IIIF).
                return;
            }
            Path dataPath = DataFileTools.getDataFilePath(pi,
                    DataManager.getInstance().getConfiguration().getMediaFolder(), null, filename);
            URI uri = dataPath.toUri();
            // Same short-circuits as prewarm(URI) — mirroring them here saves an executor
            // round-trip when the URI is already cached.
            SourceImageCache cache = SourceImageCache.getInstance();
            if (cache.get(uri) != null) {
                skippedAlreadyCached.incrementAndGet();
                return;
            }
            runPrewarm(uri);
        } catch (PresentationException | IndexUnreachableException | IllegalArgumentException e) {
            failed.incrementAndGet();
            logger.debug("Prewarm resolution failed for {} page {}: {}", pi, pageOrder, e.getMessage());
        }
    }

    /**
     * Solr lookup: PI + page order → master FILENAME. Returns null when no PAGE doc matches.
     * Runs in the worker thread, never blocks a request thread.
     */
    private String lookupFilename(String pi, int pageOrder) throws PresentationException, IndexUnreachableException {
        String query = SolrConstants.PI_TOPSTRUCT + ":" + pi
                + " AND " + SolrConstants.DOCTYPE + ":" + DocType.PAGE
                + " AND " + SolrConstants.ORDER + ":" + pageOrder;
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query,
                Collections.singletonList(SolrConstants.FILENAME));
        if (doc == null) {
            return null;
        }
        Object value = doc.getFieldValue(SolrConstants.FILENAME);
        return value == null ? null : value.toString();
    }

    /**
     * Submits an async prewarm for the given source-image URI. Returns immediately. The actual
     * decode happens on a worker thread some time later. Safe to call repeatedly with the same
     * URI: a duplicate submission while a previous prewarm is still in flight short-circuits in
     * {@link SourceImageCache#getOrDecodeFull} (one decoder runs, the other waits and serves
     * the same image), and a submission for an already-cached URI returns without doing work.
     *
     * @param sourceUri URI of the master image file (typically {@code file://...}). May be null —
     *            null is silently ignored to keep callers free of preconditions.
     * @should do nothing when uri is null
     * @should do nothing when source image cache is disabled
     * @should skip submit when uri is already cached
     * @should populate cache when uri is decodable
     * @should swallow decode failures silently
     */
    public void prewarm(URI sourceUri) {
        if (sourceUri == null) {
            return;
        }
        // Feature switch: even if the underlying cache is enabled, the operator can disable
        // prewarm explicitly (e.g. on a memory-constrained instance where the extra decode
        // threads compete with regular request handling).
        if (!DataManager.getInstance().getConfiguration().isPrewarmSourceImageCache()) {
            return;
        }
        SourceImageCache cache = SourceImageCache.getInstance();
        // Cache disabled in ContentServer config: prewarm has no place to put the image.
        if (!cache.isEnabled()) {
            return;
        }
        // Already cached: nothing to do. This is the hot path on every page revisit
        // within the cache TTL window.
        if (cache.get(sourceUri) != null) {
            skippedAlreadyCached.incrementAndGet();
            return;
        }
        try {
            executor.execute(() -> runPrewarm(sourceUri));
            submitted.incrementAndGet();
        } catch (RejectedExecutionException e) {
            // DiscardPolicy itself never throws, but a shutdown executor would. Counting the
            // miss makes operator-visible whether the pool is saturated or the service was
            // disabled at runtime.
            dropped.incrementAndGet();
            logger.debug("Prewarm submit dropped for {}: {}", sourceUri, e.getMessage());
        }
    }

    private void runPrewarm(URI sourceUri) {
        long start = System.nanoTime();
        try {
            SourceImageCache cache = SourceImageCache.getInstance();
            // Re-check inside the worker: another worker (or a tile request that hit
            // decodeFullCoalesced first) may have populated the cache while this task waited
            // in the queue.
            if (cache.get(sourceUri) != null) {
                skippedAlreadyCached.incrementAndGet();
                return;
            }
            // Prime the burst-tracking map with this URI so concurrent tile requests landing
            // in AbstractImageInterpreter#getRenderedImage observe registerRequestAndIsBurst()
            // returning true and take the burst-coalesce path. Without this prime, the very
            // first concurrent tile would fall through to the legacy ROI-decode branch and miss
            // the cache fill happening on this thread.
            cache.registerRequestAndIsBurst(sourceUri);

            try (ImageManager mgr = new ImageManager(sourceUri)) {
                // ROI=null triggers a full-image decode. The interpreter's cache hook takes the
                // burst-coalesce path (because registerRequestAndIsBurst returns true on this
                // second call), enters SourceImageCache.getOrDecodeFull, wins the inflight
                // race for this URI, runs the underlying decoder and populates the cache as a
                // side effect.
                mgr.getMyInterpreter().getRenderedImage(null);
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            logger.debug("Prewarm decoded {} in {} ms", sourceUri, elapsedMs);
            completed.incrementAndGet();
        } catch (ImageManagerException | ImageInterpreterException | FileNotFoundException
                | IllegalArgumentException e) {
            // Prewarm is best-effort. Failures (file missing, decode error, etc.) must not
            // impact the user-facing request. Log at debug because the same error will surface
            // again — louder — when the actual tile request runs.
            failed.incrementAndGet();
            logger.debug("Prewarm failed for {}: {}", sourceUri, e.getMessage());
        }
    }

    /** Test helper: blocks until all queued + active prewarm tasks have finished. */
    void awaitQuiescence(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getActiveCount() == 0 && executor.getQueue().isEmpty()) {
                return;
            }
            Thread.sleep(10);
        }
    }

    /** @return number of prewarm tasks ever submitted to the executor */
    public long getSubmittedCount() {
        return submitted.get();
    }

    /** @return number of prewarm tasks that ran to a successful decode */
    public long getCompletedCount() {
        return completed.get();
    }

    /** @return number of prewarm submissions rejected by the executor (e.g. saturated queue) */
    public long getDroppedCount() {
        return dropped.get();
    }

    /** @return number of prewarm calls short-circuited because the URI was already cached */
    public long getSkippedAlreadyCachedCount() {
        return skippedAlreadyCached.get();
    }

    /** @return number of prewarm tasks that failed (file missing, decode error, Solr lookup) */
    public long getFailedCount() {
        return failed.get();
    }

    private static final class PrewarmThreadFactory implements ThreadFactory {
        private final AtomicInteger seq = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            // Daemon threads so an orderly Tomcat shutdown is not blocked by an in-flight decode.
            Thread t = new Thread(r, "viewer-prewarm-" + seq.incrementAndGet());
            t.setDaemon(true);
            // Below default so prewarm never starves request-handling threads under CPU pressure.
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    }
}
