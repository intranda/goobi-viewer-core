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
package io.goobi.viewer.model.sitemap;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SitemapBuilder}. Intentionally does not extend a Solr/DB-backed base class
 * — the assertion only inspects the static workerThread field via reflection and must not pay the
 * cost of spinning up embedded infrastructure.
 */
class SitemapBuilderTest {

    /**
     * Reflection helper to read the private static workerThread field.
     *
     * @return current value of {@code SitemapBuilder.workerThread} or null
     * @throws ReflectiveOperationException when the field cannot be accessed
     */
    private static Thread readWorkerThread() throws ReflectiveOperationException {
        Field f = SitemapBuilder.class.getDeclaredField("workerThread");
        f.setAccessible(true);
        return (Thread) f.get(null);
    }

    /**
     * @see SitemapBuilder#updateSitemap(String, String)
     * @verifies null worker thread after completion
     */
    @Test
    void updateSitemap_shouldNullWorkerThreadAfterCompletion() throws Exception {
        SitemapBuilder builder = new SitemapBuilder(null);
        // Use a non-existing output path; the resulting exception signals completion of the worker
        // thread. We only care about the thread reference being cleared in the finally block,
        // not the generation result.
        try {
            builder.updateSitemap("/nonexistent/path-that-will-fail", "http://localhost:8080/viewer");
        } catch (Exception expected) {
            // swallow — goal is to verify the thread reference was cleared
        }
        assertNull(readWorkerThread(), "workerThread must be null after updateSitemap completes");
    }
}
