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
package io.goobi.viewer.model.viewer.pageloader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.EagerPageLoader;

class EagerPageLoaderTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @verifies return size correctly
     */
    @Test
    void getNumPages_shouldReturnSizeCorrectly() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        EagerPageLoader pageLoader = new EagerPageLoader(se);
        Assertions.assertEquals(16, pageLoader.getNumPages());
    }

    /**
     * @verifies return correct page
     */
    @Test
    void getPage_shouldReturnCorrectPage() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        EagerPageLoader pageLoader = new EagerPageLoader(se);
        PhysicalElement pe = pageLoader.getPage(3);
        Assertions.assertNotNull(pe);
        Assertions.assertEquals(3, pe.getOrder());
    }

    /**
     * @verifies return the correct page
     */
    @Test
    void getPageForFileName_shouldReturnTheCorrectPage() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        EagerPageLoader pageLoader = new EagerPageLoader(se);
        PhysicalElement pe = pageLoader.getPageForFileName("00000004.tif");
        Assertions.assertNotNull(pe);
        Assertions.assertEquals(4, pe.getOrder());
    }

    /**
     * @verifies return null if file name not found
     */
    @Test
    void getPageForFileName_shouldReturnNullIfFileNameNotFound() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        EagerPageLoader pageLoader = new EagerPageLoader(se);
        PhysicalElement pe = pageLoader.getPageForFileName("NOTFOUND.tif");
        Assertions.assertNull(pe);
    }

    /**
     * @verifies set first page order to 1 for loaded struct element
     */
    @Test
    void setFirstAndLastPageOrder_shouldSetFirstPageOrderTo1ForLoadedStructElement() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        EagerPageLoader pageLoader = new EagerPageLoader(se);
        Assertions.assertEquals(1, pageLoader.getFirstPageOrder());
    }

    /**
     * @verifies set last page order to total number of pages for loaded struct element
     */
    @Test
    void setFirstAndLastPageOrder_shouldSetLastPageOrderToTotalNumberOfPagesForLoadedStructElement() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        EagerPageLoader pageLoader = new EagerPageLoader(se);
        Assertions.assertEquals(16, pageLoader.getLastPageOrder());
    }
}
