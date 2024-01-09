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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.jdom2.Element;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.sitemap.Sitemap;

public class SitemapTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final Logger logger = LogManager.getLogger(SitemapTest.class);

    /**
     * @see Sitemap#createUrlElement(String,String,String,String)
     * @verifies create loc element correctly
     */
    @Test
    void createUrlElement_shouldCreateLocElementCorrectly() throws Exception {
        Sitemap sitemap = new Sitemap();
        Element eleUrl = sitemap.createUrlElement("https://foo.bar", null, null, null);
        Assertions.assertNotNull(eleUrl);
        Assertions.assertEquals("https://foo.bar", eleUrl.getChildText("loc", Sitemap.nsSitemap));
    }

    /**
     * @see Sitemap#createUrlElement(String,String,String,String)
     * @verifies create lastmod element correctly
     */
    @Test
    void createUrlElement_shouldCreateLastmodElementCorrectly() throws Exception {
        Sitemap sitemap = new Sitemap();
        Element eleUrl = sitemap.createUrlElement("https://foo.bar", "2018-08-21", null, null);
        Assertions.assertNotNull(eleUrl);
        Assertions.assertEquals("2018-08-21", eleUrl.getChildText("lastmod", Sitemap.nsSitemap));
    }

    @Test
    void testSitemap() throws IOException, InterruptedException {

        int timeout = 20; //minutes

        Sitemap sitemap = new Sitemap();

        Path path = Paths.get("sitemap_temp");
        try {
            if (!Files.isDirectory(path)) {
                Files.createDirectory(path);
            }
            long start = System.nanoTime();
            Thread thread = new Thread(() -> {
                try {
                    sitemap.generate(null, path.toAbsolutePath().toString());
                } catch (IOException | PresentationException | IndexUnreachableException | DAOException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            });
            thread.start();
            thread.join(TimeUnit.MINUTES.toMillis(timeout));
            if (thread.isAlive()) {
                thread.interrupt();
                thread.join();
            }
            long end = System.nanoTime();
            logger.info("Generating sitemap took {}s", toSeconds(end - start));
            logger.info("Written sitemap files to {}", path.toAbsolutePath());
            assertTrue(Files.list(path).count() > 0);
        } finally {
            if (Files.isDirectory(path)) {
                FileUtils.deleteDirectory(path.toFile());
            }
        }

    }

    /**
     * @param l
     * @return
     */
    private static double toSeconds(long nano) {
        return nano / 1E9;
    }
}
