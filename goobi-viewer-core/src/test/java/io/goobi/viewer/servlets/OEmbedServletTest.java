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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.servlets.oembed.OEmbedRecord;

class OEmbedServletTest extends AbstractSolrEnabledTest {

    /**
     * @see OEmbedServlet#parseUrl(String)
     * @verifies parse url with page number correctly
     */
    @Test
    void parseUrl_shouldParseUrlWithPageNumberCorrectly() throws Exception {
        OEmbedRecord rec = OEmbedServlet.parseUrl("/image/PPN517154005/2/");
        Assertions.assertNotNull(rec);
        Assertions.assertNotNull(rec.getPhysicalElement());
        Assertions.assertEquals("PPN517154005", rec.getPhysicalElement().getPi());
        Assertions.assertEquals(2, rec.getPhysicalElement().getOrder());

    }

    /**
     * @see OEmbedServlet#parseUrl(String)
     * @verifies parse url without page number correctly
     */
    @Test
    void parseUrl_shouldParseUrlWithoutPageNumberCorrectly() throws Exception {
        OEmbedRecord rec = OEmbedServlet.parseUrl("/image/PPN517154005/");
        Assertions.assertNotNull(rec);
        Assertions.assertNotNull(rec.getPhysicalElement());
        Assertions.assertEquals(1, rec.getPhysicalElement().getOrder());
    }

    /**
     * @see OEmbedServlet#parseUrl(String)
     * @verifies return null if url contains no pi
     */
    @Test
    void parseUrl_shouldReturnNullIfUrlContainsNoPi() throws Exception {
        Assertions.assertNull(OEmbedServlet.parseUrl("/image/"));
    }
}