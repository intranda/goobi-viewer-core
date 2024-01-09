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
import io.goobi.viewer.model.viewer.pageloader.LeanPageLoader;

public class LeanPageLoaderTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see LeanPageLoader#getNumPages()
     * @verifies return size correctly
     */
    @Test
    void getNumPages_shouldReturnSizeCorrectly() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        // Page number as a constructor arg
        LeanPageLoader pageLoader = new LeanPageLoader(se, 5);
        Assertions.assertEquals(5, pageLoader.getNumPages());
        // Page number from the StructElement
        pageLoader = new LeanPageLoader(se, -1);
        Assertions.assertEquals(16, pageLoader.getNumPages());
    }

    /**
     * @see LeanPageLoader#getPage(int)
     * @verifies return correct page
     */
    @Test
    void getPage_shouldReturnCorrectPage() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.getPage(3);
        Assertions.assertNotNull(pe);
        Assertions.assertEquals(3, pe.getOrder());
    }

    /**
     * @see LeanPageLoader#getPage(int)
     * @verifies return null if pageOrder smaller than firstPageOrder
     */
    @Test
    void getPage_shouldReturnNullIfPageOrderSmallerThanFirstPageOrder() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.getPage(-1);
        Assertions.assertNull(pe);
    }

    /**
     * @see LeanPageLoader#getPage(int)
     * @verifies return null if pageOrder larger than lastPageOrder
     */
    @Test
    void getPage_shouldReturnNullIfPageOrderLargerThanLastPageOrder() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.getPage(20);
        Assertions.assertNull(pe);
    }

    /**
     * @see LeanPageLoader#getPageForFileName(String)
     * @verifies return the correct page
     */
    @Test
    void getPageForFileName_shouldReturnTheCorrectPage() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.getPageForFileName("00000004.tif");
        Assertions.assertNotNull(pe);
        Assertions.assertEquals(4, pe.getOrder());
    }

    /**
     * @see LeanPageLoader#getPageForFileName(String)
     * @verifies return null if file name not found
     */
    @Test
    void getPageForFileName_shouldReturnNullIfFileNameNotFound() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.getPageForFileName("NOTFOUND.tif");
        Assertions.assertNull(pe);
    }

    /**
     * @see LeanPageLoader#setFirstAndLastPageOrder()
     * @verifies set first page order correctly
     */
    @Test
    void setFirstAndLastPageOrder_shouldSetFirstPageOrderCorrectly() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, se.getNumPages());
        Assertions.assertEquals(1, pageLoader.getFirstPageOrder());
    }

    /**
     * @see LeanPageLoader#setFirstAndLastPageOrder()
     * @verifies set last page order correctly
     */
    @Test
    void setFirstAndLastPageOrder_shouldSetLastPageOrderCorrectly() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, se.getNumPages());
        Assertions.assertEquals(16, pageLoader.getLastPageOrder());
    }

    /**
     * @see LeanPageLoader#loadPage(int,String)
     * @verifies load page correctly via page number
     */
    @Test
    void loadPage_shouldLoadPageCorrectlyViaPageNumber() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.loadPage(3, null);
        Assertions.assertNotNull(pe);
        Assertions.assertEquals(3, pe.getOrder());
    }

    /**
     * @see LeanPageLoader#loadPage(int,String)
     * @verifies load page correctly via file name
     */
    @Test
    void loadPage_shouldLoadPageCorrectlyViaFileName() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.loadPage(-1, "00000004.tif");
        Assertions.assertNotNull(pe);
        Assertions.assertEquals(4, pe.getOrder());
    }

    /**
     * @see LeanPageLoader#loadPage(int,String)
     * @verifies return null if page not found
     */
    @Test
    void loadPage_shouldReturnNullIfPageNotFound() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.loadPage(-1, "NOTFOUND.tif");
    }
}
