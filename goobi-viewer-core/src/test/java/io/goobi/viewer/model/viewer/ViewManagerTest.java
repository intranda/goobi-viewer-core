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
package io.goobi.viewer.model.viewer;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.IDDOCNotFoundException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.model.viewer.pageloader.EagerPageLoader;

public class ViewManagerTest extends AbstractDatabaseAndSolrEnabledTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));
    }

    /**
     * @see ViewManager#getPage(int)
     * @verifies return correct page
     */
    @Test
    public void getPage_shouldReturnCorrectPage() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
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
        StructElement se = new StructElement(iddocKleiuniv);
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
        StructElement se = new StructElement(iddocKleiuniv);
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
        StructElement se = new StructElement(iddocKleiuniv);
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

        StructElement se = new StructElement(iddocKleiuniv);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, null);
        Assert.assertEquals(16, viewManager.getImagesCount());

        viewManager.setCurrentThumbnailPage(1);
        List<PhysicalElement> pages = viewManager.getImagesSection(thumbnailsPerPage);
        Assert.assertEquals(10, pages.size());
        Assert.assertEquals(1, pages.get(0).getOrder());
        Assert.assertEquals(10, pages.get(9).getOrder());

        viewManager.setCurrentThumbnailPage(2);
        pages = viewManager.getImagesSection(thumbnailsPerPage);
        Assert.assertEquals(6, pages.size());
        Assert.assertEquals(11, pages.get(0).getOrder());
        Assert.assertEquals(15, pages.get(4).getOrder());
    }

    /**
     * @see ViewManager#resetImage()
     * @verifies reset rotation
     */
    @Test
    public void resetImage_shouldResetRotation() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
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
        StructElement se = new StructElement(iddocKleiuniv);
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
        StructElement se = new StructElement(iddocKleiuniv);
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
        StructElement se = new StructElement(iddocKleiuniv);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        Assert.assertEquals(16, viewManager.getImagesCount());

        viewManager.setFirstPdfPage("14");
        viewManager.setLastPdfPage("16");
        String url = viewManager.getPdfPartDownloadLink();
        String expect = "image/" + PI_KLEIUNIV + "/00000014.tif$00000015.tif$00000016.tif/full/max/0/PPN517154005_14-16.pdf";
        Assert.assertTrue("expeted url to contain " + expect + " but was " + url, url.contains(expect));
    }

    /**
     * @see ViewManager#getPersistentUrl(String)
     * @verifies generate purl via urn correctly
     */
    @Test
    public void getPersistentUrl_shouldGeneratePurlViaUrnCorrectly() throws Exception {
        StructElement se = new StructElement();
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        String purl = viewManager.getPersistentUrl("urn:nbn:foo:bar-1234");
        Assert.assertEquals("urnResolver_valueurn:nbn:foo:bar-1234", purl);
    }

    /**
     * @see ViewManager#getPersistentUrl(String)
     * @verifies generate purl without urn correctly
     */
    @Test
    public void getPersistentUrl_shouldGeneratePurlWithoutUrnCorrectly() throws Exception {
        String pi = "PPN123";
        String docstructType = "Catalogue";

        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));

        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        try {
            viewManager.setCurrentImageNo(1);
        } catch (IDDOCNotFoundException e) {
        }
        Assert.assertEquals(docstructType, viewManager.getTopDocument().getDocStructType());
        Assert.assertEquals(pi, viewManager.getPi());
        Assert.assertEquals(1, viewManager.getCurrentImageNo());

        String purl = viewManager.getPersistentUrl(null);
        Assert.assertEquals("/toc/PPN123/1/", purl);
    }

    /**
     * @see ViewManager#isBelowFulltextThreshold(double)
     * @verifies return true if there are no pages
     */
    @Test
    public void isBelowFulltextThreshold_shouldReturnTrueIfThereAreNoPages() throws Exception {
        String pi = "PPN123";
        String docstructType = "Catalogue";

        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));

        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assert.assertTrue(viewManager.isBelowFulltextThreshold(0));
    }
}