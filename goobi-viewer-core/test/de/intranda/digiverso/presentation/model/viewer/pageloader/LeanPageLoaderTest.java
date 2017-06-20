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
package de.intranda.digiverso.presentation.model.viewer.pageloader;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

public class LeanPageLoaderTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see LeanPageLoader#getNumPages()
     * @verifies return size correctly
     */
    @Test
    public void getNumPages_shouldReturnSizeCorrectly() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        // Page number as a constructor arg
        LeanPageLoader pageLoader = new LeanPageLoader(se, 5);
        Assert.assertEquals(5, pageLoader.getNumPages());
        // Page number from the StructElement
        pageLoader = new LeanPageLoader(se, -1);
        Assert.assertEquals(16, pageLoader.getNumPages());
    }

    /**
     * @see LeanPageLoader#getPage(int)
     * @verifies return correct page
     */
    @Test
    public void getPage_shouldReturnCorrectPage() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.getPage(3);
        Assert.assertNotNull(pe);
        Assert.assertEquals(3, pe.getOrder());
    }

    /**
     * @see LeanPageLoader#getPage(int)
     * @verifies return null if pageOrder smaller than firstPageOrder
     */
    @Test
    public void getPage_shouldReturnNullIfPageOrderSmallerThanFirstPageOrder() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.getPage(-1);
        Assert.assertNull(pe);
    }

    /**
     * @see LeanPageLoader#getPage(int)
     * @verifies return null if pageOrder larger than lastPageOrder
     */
    @Test
    public void getPage_shouldReturnNullIfPageOrderLargerThanLastPageOrder() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.getPage(20);
        Assert.assertNull(pe);
    }

    /**
     * @see LeanPageLoader#getPageForFileName(String)
     * @verifies return the correct page
     */
    @Test
    public void getPageForFileName_shouldReturnTheCorrectPage() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.getPageForFileName("00000004.tif");
        Assert.assertNotNull(pe);
        Assert.assertEquals(3, pe.getOrder());
    }

    /**
     * @see LeanPageLoader#getPageForFileName(String)
     * @verifies return null if file name not found
     */
    @Test
    public void getPageForFileName_shouldReturnNullIfFileNameNotFound() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.getPageForFileName("NOTFOUND.tif");
        Assert.assertNull(pe);
    }

    /**
     * @see LeanPageLoader#setFirstAndLastPageOrder()
     * @verifies set first page order correctly
     */
    @Test
    public void setFirstAndLastPageOrder_shouldSetFirstPageOrderCorrectly() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, se.getNumPages());
        Assert.assertEquals(0, pageLoader.getFirstPageOrder());
    }

    /**
     * @see LeanPageLoader#setFirstAndLastPageOrder()
     * @verifies set last page order correctly
     */
    @Test
    public void setFirstAndLastPageOrder_shouldSetLastPageOrderCorrectly() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, se.getNumPages());
        Assert.assertEquals(15, pageLoader.getLastPageOrder());
    }

    /**
     * @see LeanPageLoader#loadPage(int,String)
     * @verifies load page correctly via page number
     */
    @Test
    public void loadPage_shouldLoadPageCorrectlyViaPageNumber() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.loadPage(3, null);
        Assert.assertNotNull(pe);
        Assert.assertEquals(3, pe.getOrder());
    }

    /**
     * @see LeanPageLoader#loadPage(int,String)
     * @verifies load page correctly via file name
     */
    @Test
    public void loadPage_shouldLoadPageCorrectlyViaFileName() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.loadPage(-1, "00000004.tif");
        Assert.assertNotNull(pe);
        Assert.assertEquals(3, pe.getOrder());
    }

    /**
     * @see LeanPageLoader#loadPage(int,String)
     * @verifies return null if page not found
     */
    @Test
    public void loadPage_shouldReturnNullIfPageNotFound() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        LeanPageLoader pageLoader = new LeanPageLoader(se, -1);
        PhysicalElement pe = pageLoader.loadPage(-1, "NOTFOUND.tif");
    }
}