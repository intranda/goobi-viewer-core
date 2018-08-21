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

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

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
}