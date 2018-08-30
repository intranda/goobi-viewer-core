/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.model.sitemap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;

public class SitemapTest {

    /**
     * @see Sitemap#createUrlElement(String,String,String,String)
     * @verifies create loc element correctly
     */
    @Test
    public void createUrlElement_shouldCreateLocElementCorrectly() throws Exception {
        Sitemap sitemap = new Sitemap();
        Element eleUrl = sitemap.createUrlElement("https://foo.bar", null, null, null);
        Assert.assertNotNull(eleUrl);
        Assert.assertEquals("https://foo.bar", eleUrl.getChildText("loc", Sitemap.nsSitemap));
    }

    /**
     * @see Sitemap#createUrlElement(String,String,String,String)
     * @verifies create lastmod element correctly
     */
    @Test
    public void createUrlElement_shouldCreateLastmodElementCorrectly() throws Exception {
        Sitemap sitemap = new Sitemap();
        Element eleUrl = sitemap.createUrlElement("https://foo.bar", "2018-08-21", null, null);
        Assert.assertNotNull(eleUrl);
        Assert.assertEquals("2018-08-21", eleUrl.getChildText("lastmod", Sitemap.nsSitemap));
    }
    
    @Test
    public void testSitemap() throws IOException, PresentationException, IndexUnreachableException, DAOException, InterruptedException {
        
        int timeout = 20; //minutes
        
        Sitemap sitemap = new Sitemap();
        
        
        Path path = Paths.get("sitemap_temp");
        if(!Files.isDirectory(path)) {            
            Files.createDirectory(path); 
        }
        long start = System.nanoTime();
        Thread thread = new Thread(() ->
            {
                try {
                    sitemap.generate("https://viewer-demo01.intranda.com", path.toAbsolutePath().toString());
                } catch (IOException | PresentationException | IndexUnreachableException | DAOException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        );
        thread.start();
        thread.join(TimeUnit.MINUTES.toMillis(timeout));
        if(thread.isAlive()) {
            thread.interrupt();
            thread.join();
        }
        long end = System.nanoTime();
        System.out.println("Generating sitemap took " + toSeconds(end-start) + "s" );
        System.out.println("Written sitemap files to " + path.toAbsolutePath());

    }

    /**
     * @param l
     * @return
     */
    private double toSeconds(long nano) {
        return nano / 1E9;
    }
}