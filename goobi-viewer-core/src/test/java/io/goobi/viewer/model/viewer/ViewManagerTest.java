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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.TestUtils;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IDDOCNotFoundException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.model.download.DownloadOption;
import io.goobi.viewer.model.viewer.pageloader.EagerPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;

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
        String expect = "records/" + PI_KLEIUNIV + "/files/images/00000014.tif$00000015.tif$00000016.tif/full.pdf";
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
    
    @Test
    public void testDisplayDownloadWidget() throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        String pi = "PPN123";
        String docstructType = "Catalogue";

        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));

        FacesContext context = TestUtils.mockFacesContext();
        
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        boolean display = viewManager.isDisplayContentDownloadMenu();
        Assert.assertTrue(display);
    }
    
    @Test
    public void testListDownloadLinksForWork() throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException, IOException {
        String pi = "PPN123";
        String docstructType = "Catalogue";

        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));

        FacesContext context = TestUtils.mockFacesContext();
        
        ViewManager viewManager = new ViewManager(se, new EagerPageLoader(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        List<LabeledLink> links = viewManager.getContentDownloadLinksForWork();
        Assert.assertEquals(2, links.size());
    }
    
    @Test
    public void testGetPageDownloadUrl() throws IndexUnreachableException, DAOException, PresentationException, ViewerConfigurationException {
        
        String pi = "PPN123";
        String docstructType = "Catalogue";
        String filename = "00000001.tif";

        ViewManager viewManager = createViewManager(pi, docstructType, filename);
        
        
        String baseUrl = DataManager.getInstance().getRestApiManager().getContentApiUrl()
                + "records/" + pi + "/files/images/" + filename;
        
        DownloadOption maxSizeTiff = new DownloadOption("Master", "master", "max");
        String masterTiffUrl = baseUrl + "/full/max/0/default.tif";
        assertEquals(masterTiffUrl, viewManager.getPageDownloadUrl(maxSizeTiff).replaceAll("\\?.*", "")); //ignore query params
        
        DownloadOption scaledJpeg = new DownloadOption("Thumbnail", "jpg", new Dimension(800, 1200));
        String thumbnailUrl = baseUrl + "/full/!800,1200/0/default.jpg";
        assertEquals(thumbnailUrl, viewManager.getPageDownloadUrl(scaledJpeg).replaceAll("\\?.*", "")); //ignore query params

    }
    
    @Test
    public void testGetDownloadOptionsForImage() {
        
        DownloadOption tooLarge = new DownloadOption("", "master", new Dimension(10000, 10000));
        DownloadOption master = new DownloadOption("", "master", DownloadOption.MAX);
        DownloadOption thumb = new DownloadOption("", "jpg", "600");
        DownloadOption largeThumb = new DownloadOption("", "jpg", "2000");
        DownloadOption empty = new DownloadOption("", "jpg", DownloadOption.NONE);
        List<DownloadOption> configuredOptions = Arrays.asList(tooLarge, master, largeThumb, thumb, empty);
        
        Dimension maxSize =  new Dimension(20000, 5000);
        Dimension imageSize = new Dimension(1000, 2000);
        
        List<DownloadOption> options = ViewManager.getDownloadOptionsForImage(configuredOptions, imageSize, maxSize, "00000001.tif");
        assertEquals(2, options.size());
        
        DownloadOption masterOption = options.stream().filter(o -> o.getFormat().equalsIgnoreCase("tiff")).findFirst().orElse(null);
        assertNotNull(masterOption);
        assertEquals(imageSize, masterOption.getBoxSizeInPixel());
        
        DownloadOption thumbOption = options.stream().filter(o -> o.getFormat().equalsIgnoreCase("jpg")).findFirst().orElse(null);
        assertNotNull(thumbOption);
        assertEquals(new Dimension(1000*600/2000, 600), thumbOption.getBoxSizeInPixel());


    }


    private ViewManager createViewManager(String pi, String docstructType, String pageFilename)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));
        PhysicalElement page = Mockito.mock(PhysicalElement.class);
        Mockito.when(page.getFilename()).thenReturn(pageFilename);
        Mockito.when(page.getFilepath()).thenReturn(pi + "/" + pageFilename);
        Mockito.when(page.getMimeType()).thenReturn("image/tiff");
        
        IPageLoader pageLoader = Mockito.mock(EagerPageLoader.class);
        Mockito.when(pageLoader.getPage(Mockito.anyInt())).thenReturn(page);
        
        return new ViewManager(se, pageLoader, se.getLuceneId(), null, null, new ImageDeliveryBean());
    }
    
}