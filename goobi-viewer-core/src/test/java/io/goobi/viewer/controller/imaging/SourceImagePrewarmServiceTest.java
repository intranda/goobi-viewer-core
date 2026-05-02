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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.unigoettingen.sub.commons.cache.SourceImageCache;
import io.goobi.viewer.AbstractTest;

/**
 * Unit tests for {@link SourceImagePrewarmService}.
 *
 * <p>Notes for future maintainers:
 * <ul>
 *   <li>The service is a JVM-wide singleton. Tests share counter state across runs, so we snapshot
 *       counters before each call and assert deltas rather than absolute values.</li>
 *   <li>{@link SourceImageCache} is also a singleton; we use its test-only override
 *       {@link SourceImageCache#setEnabledForTests(Boolean)} to flip it on/off and clear it via
 *       {@link SourceImageCache#clear()} between tests.</li>
 * </ul>
 */
class SourceImagePrewarmServiceTest extends AbstractTest {

    private static final Path TEST_TIFF =
            Paths.get("src/test/resources/data/viewer/images/PPN615391702/00000002.tif").toAbsolutePath();

    private SourceImagePrewarmService service;
    private SourceImageCache cache;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        service = SourceImagePrewarmService.getInstance();
        cache = SourceImageCache.getInstance();
        cache.clear();
        cache.setEnabledForTests(null); // restore config-driven default
    }

    @AfterEach
    void tearDown() {
        cache.clear();
        cache.setEnabledForTests(null);
    }

    /**
     * @see SourceImagePrewarmService#prewarm(URI)
     * @verifies do nothing when uri is null
     */
    @Test
    void prewarm_shouldDoNothingWhenUriIsNull() throws Exception {
        long submittedBefore = service.getSubmittedCount();
        service.prewarm((URI) null);
        Assertions.assertEquals(submittedBefore, service.getSubmittedCount(),
                "null URI must not enqueue a prewarm task");
    }

    /**
     * @see SourceImagePrewarmService#prewarm(URI)
     * @verifies do nothing when source image cache is disabled
     */
    @Test
    void prewarm_shouldDoNothingWhenSourceImageCacheIsDisabled() throws Exception {
        cache.setEnabledForTests(Boolean.FALSE);
        long submittedBefore = service.getSubmittedCount();
        service.prewarm(TEST_TIFF.toUri());
        Assertions.assertEquals(submittedBefore, service.getSubmittedCount(),
                "Disabled SourceImageCache must short-circuit submit");
    }

    /**
     * @see SourceImagePrewarmService#prewarm(URI)
     * @verifies skip submit when uri is already cached
     */
    @Test
    void prewarm_shouldSkipSubmitWhenUriIsAlreadyCached() throws Exception {
        cache.setEnabledForTests(Boolean.TRUE);
        URI uri = TEST_TIFF.toUri();
        // Populate cache directly with a stub image so the next prewarm sees a hit.
        cache.put(uri, new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB));

        long submittedBefore = service.getSubmittedCount();
        long skippedBefore = service.getSkippedAlreadyCachedCount();
        service.prewarm(uri);

        Assertions.assertEquals(submittedBefore, service.getSubmittedCount(),
                "Cached URI must not enqueue a prewarm task");
        Assertions.assertEquals(skippedBefore + 1, service.getSkippedAlreadyCachedCount(),
                "Cached URI must increment the skip counter");
    }

    /**
     * @see SourceImagePrewarmService#prewarm(URI)
     * @verifies populate cache when uri is decodable
     */
    @Test
    void prewarm_shouldPopulateCacheWhenUriIsDecodable() throws Exception {
        cache.setEnabledForTests(Boolean.TRUE);
        URI uri = TEST_TIFF.toUri();
        Assertions.assertTrue(TEST_TIFF.toFile().exists(), "Test fixture must exist: " + TEST_TIFF);

        Assertions.assertNull(cache.get(uri), "Cache must start empty for the test URI");
        service.prewarm(uri);
        service.awaitQuiescence(10_000L);

        Assertions.assertNotNull(cache.get(uri),
                "After prewarm + quiescence, cache must hold a decoded image for the URI");
    }

    /**
     * @see SourceImagePrewarmService#prewarm(URI)
     * @verifies swallow decode failures silently
     */
    @Test
    void prewarm_shouldSwallowDecodeFailuresSilently() throws Exception {
        cache.setEnabledForTests(Boolean.TRUE);
        URI bogus = Paths.get("src/test/resources/__nonexistent_image__.tif").toAbsolutePath().toUri();
        long failedBefore = service.getFailedCount();

        // Must not throw.
        service.prewarm(bogus);
        service.awaitQuiescence(5_000L);

        Assertions.assertEquals(failedBefore + 1, service.getFailedCount(),
                "Missing source file must increment the failed counter, not throw");
        Assertions.assertNull(cache.get(bogus), "Failed prewarm must leave cache untouched");
    }
}
