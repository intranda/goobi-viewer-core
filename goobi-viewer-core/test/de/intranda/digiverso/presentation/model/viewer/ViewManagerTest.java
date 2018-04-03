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
package de.intranda.digiverso.presentation.model.viewer;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.managedbeans.ImageDeliveryBean;
import de.intranda.digiverso.presentation.model.viewer.pageloader.EagerPageLoader;

public class ViewManagerTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see ViewManager#getPage(int)
     * @verifies return correct page
     */
    @Test
    public void getPage_shouldReturnCorrectPage() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, null);
        PhysicalElement pe = viewManager.getPage(3).orElse(null);
        Assert.assertNotNull(pe);
        Assert.assertEquals(3, pe.getOrder());
    }

    /**
     * @see ViewManager#getPage(int)
     * @verifies return null if order less than zero
     */
    @Test
    public void getPage_shouldReturnNullIfOrderLessThanZero() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, null);
        PhysicalElement pe = viewManager.getPage(-1).orElse(null);
        Assert.assertNull(pe);
    }

    /**
     * @see ViewManager#getPage(int)
     * @verifies return null if order larger than number of pages
     */
    @Test
    public void getPage_shouldReturnNullIfOrderLargerThanNumberOfPages() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, null);
        PhysicalElement pe = viewManager.getPage(17).orElse(null);
        Assert.assertNull(pe);
    }

    /**
     * @see ViewManager#getPage(int)
     * @verifies return null if pageLoader is null
     */
    @Test
    public void getPage_shouldReturnNullIfPageLoaderIsNull() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, null, se.getLuceneId(), null, null, null);
        PhysicalElement pe = viewManager.getPage(0).orElse(null);
        Assert.assertNull(pe);
    }

    /**
     * @see ViewManager#getImagesSection()
     * @verifies return correct PhysicalElements for a thumbnail page
     */
    @Test
    public void getImagesSection_shouldReturnCorrectPhysicalElementsForAThumbnailPage() throws Exception {
        int thumbnailsPerPage = 10;

        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, null);
        Assert.assertEquals(16, viewManager.getImagesCount());

        viewManager.setCurrentThumbnailPage(1);
        List<PhysicalElement> pages = viewManager.getImagesSection(thumbnailsPerPage);
        Assert.assertEquals(10, pages.size());
        // Old test index - ORDER values are off by one
        Assert.assertEquals(0, pages.get(0).getOrder());
        Assert.assertEquals(9, pages.get(9).getOrder());

        viewManager.setCurrentThumbnailPage(2);
        pages = viewManager.getImagesSection(thumbnailsPerPage);
        // Old test index - ORDER values are off by one
        Assert.assertEquals(5, pages.size());
        Assert.assertEquals(11, pages.get(0).getOrder());
        Assert.assertEquals(15, pages.get(4).getOrder());
    }

    /**
     * @see ViewManager#resetImage()
     * @verifies reset rotation
     */
    @Test
    public void resetImage_shouldResetRotation() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, null);
        Assert.assertEquals(0, viewManager.getCurrentRotate());
        viewManager.rotateRight();
        Assert.assertEquals(90, viewManager.getCurrentRotate());
        viewManager.resetImage();
        Assert.assertEquals(0, viewManager.getCurrentRotate());
    }

    /**
     * @see ViewManager#rotateLeft()
     * @verifies rotate correctly
     */
    @Test
    public void rotateLeft_shouldRotateCorrectly() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, null);
        Assert.assertEquals(0, viewManager.getCurrentRotate());
        viewManager.rotateLeft();
        Assert.assertEquals(270, viewManager.getCurrentRotate());
        viewManager.rotateLeft();
        Assert.assertEquals(180, viewManager.getCurrentRotate());
        viewManager.rotateLeft();
        Assert.assertEquals(90, viewManager.getCurrentRotate());
        viewManager.rotateLeft();
        Assert.assertEquals(0, viewManager.getCurrentRotate());
    }

    /**
     * @see ViewManager#rotateRight()
     * @verifies rotate correctly
     */
    @Test
    public void rotateRight_shouldRotateCorrectly() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, null);
        Assert.assertEquals(0, viewManager.getCurrentRotate());
        viewManager.rotateRight();
        Assert.assertEquals(90, viewManager.getCurrentRotate());
        viewManager.rotateRight();
        Assert.assertEquals(180, viewManager.getCurrentRotate());
        viewManager.rotateRight();
        Assert.assertEquals(270, viewManager.getCurrentRotate());
        viewManager.rotateRight();
        Assert.assertEquals(0, viewManager.getCurrentRotate());
    }

    /**
     * @see ViewManager#getPdfPartDownloadLink()
     * @verifies construct url correctly
     */
    @Test
    public void getPdfPartDownloadLink_shouldConstructUrlCorrectly() throws Exception {
        StructElement se = new StructElement(1387459019047L);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        Assert.assertEquals(16, viewManager.getImagesCount());

        // The test index has a 0-based page count
        viewManager.setFirstPdfPage("13");
        viewManager.setLastPdfPage("15");
        String url = viewManager.getPdfPartDownloadLink();
        String expect = "image/PPN517154005/00000014.tif$00000015.tif$00000016.tif/full/max/0/PPN517154005_13-15.pdf";
        Assert.assertTrue("expeted url to contain " + expect + " but was " + url, url.contains(expect));
//                "?action=pdf&images=PPN517154005/00000014.tif$PPN517154005/00000015.tif$PPN517154005/00000016.tif&targetFileName=PPN517154005_13-15.pdf"));
    }
}