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
package io.goobi.viewer.servlets;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.servlets.oembed.OEmbedRecord;

public class OEmbedServletTest extends AbstractSolrEnabledTest {

    /**
     * @see OEmbedServlet#parseUrl(String)
     * @verifies parse url with page number correctly
     */
    @Test
    public void parseUrl_shouldParseUrlWithPageNumberCorrectly() throws Exception {
        OEmbedRecord rec = OEmbedServlet.parseUrl("/image/PPN517154005/2/");
        Assert.assertNotNull(rec);
        Assert.assertNotNull(rec.getPhysicalElement());
        Assert.assertEquals("PPN517154005", rec.getPhysicalElement().getPi());
        Assert.assertEquals(2, rec.getPhysicalElement().getOrder());

    }

    /**
     * @see OEmbedServlet#parseUrl(String)
     * @verifies parse url without page number correctly
     */
    @Test
    public void parseUrl_shouldParseUrlWithoutPageNumberCorrectly() throws Exception {
        OEmbedRecord rec = OEmbedServlet.parseUrl("/image/PPN517154005/");
        Assert.assertNotNull(rec);
        Assert.assertNotNull(rec.getPhysicalElement());
        Assert.assertEquals(1, rec.getPhysicalElement().getOrder());
    }
}