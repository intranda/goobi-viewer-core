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
package io.goobi.viewer.model.viewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
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
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IDDOCNotFoundException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.model.job.download.DownloadOption;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.model.viewer.pageloader.EagerPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;
import io.goobi.viewer.solr.SolrConstants;

public class ViewManagerTest extends AbstractDatabaseAndSolrEnabledTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

    }

    /**
     * @see ViewManager#getPage(int)
     * @verifies return correct page
     */
    @Test
    public void getPage_shouldReturnCorrectPage() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
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
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
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
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
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
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
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

    @Test
    public void getImagesSection_shouldReturnCorrectPhysicalElementsForAThumbnailPageWithStartPageTwo() throws Exception {
        int thumbnailsPerPage = 30;

        StructElement se = new StructElement(iddocKleiuniv);
        Assert.assertNotNull(se);

        IPageLoader pageLoader = Mockito.mock(IPageLoader.class);
        Mockito.when(pageLoader.getFirstPageOrder()).thenReturn(2);
        Mockito.when(pageLoader.getLastPageOrder()).thenReturn(181);
        Mockito.when(pageLoader.getNumPages()).thenReturn(180);
        Mockito.when(pageLoader.getPage(Mockito.anyInt())).thenAnswer(i -> {
            int pageNo = i.getArgument(0);
            PhysicalElement page = new PhysicalElement("" + pageNo, "" + pageNo, pageNo, "" + pageNo, "", "", "", "", "");
            return page;
        });

        ViewManager viewManager = new ViewManager(se, pageLoader, se.getLuceneId(), null, null, null);
        Assert.assertEquals(180, viewManager.getImagesCount());

        viewManager.setCurrentThumbnailPage(1);
        List<PhysicalElement> pages = viewManager.getImagesSection(thumbnailsPerPage);
        Assert.assertEquals(30, pages.size());
        Assert.assertEquals(2, pages.get(0).getOrder());
        Assert.assertEquals(31, pages.get(pages.size() - 1).getOrder());

        viewManager.setCurrentThumbnailPage(2);
        pages = viewManager.getImagesSection(thumbnailsPerPage);
        Assert.assertEquals(30, pages.size());
        Assert.assertEquals(32, pages.get(0).getOrder());
        Assert.assertEquals(61, pages.get(pages.size() - 1).getOrder());

        viewManager.setCurrentThumbnailPage(6);
        pages = viewManager.getImagesSection(thumbnailsPerPage);
        Assert.assertEquals(30, pages.size());
        Assert.assertEquals(152, pages.get(0).getOrder());
        Assert.assertEquals(181, pages.get(pages.size() - 1).getOrder());
    }

    /**
     * @see ViewManager#resetImage()
     * @verifies reset rotation
     */
    @Test
    public void resetImage_shouldResetRotation() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
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
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
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
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
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
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        Assert.assertEquals(16, viewManager.getImagesCount());

        viewManager.setFirstPdfPage("14");
        viewManager.setLastPdfPage("16");
        String url = viewManager.getPdfPartDownloadLink();
        String expect = "records/" + PI_KLEIUNIV + "/files/images/00000014.tif$00000015.tif$00000016.tif/full.pdf";
        Assert.assertTrue("expeted url to contain " + expect + " but was " + url, url.contains(expect));
    }

    /**
     * @see ViewManager#getCiteLinkDocstruct()
     * @verifies return correct url
     */
    @Test
    public void getCiteLinkDocstruct_shouldReturnCorrectUrl() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        viewManager.setCurrentImageOrder(10);

        String purl = viewManager.getCiteLinkDocstruct();
        Assert.assertEquals("/object/" + PI_KLEIUNIV + "/5/LOG_0003/", purl);
    }

    /**
     * @see ViewManager#getCiteLinkPage()
     * @verifies return correct url
     */
    @Test
    public void getCiteLinkPage_shouldReturnCorrectUrl() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        viewManager.setCurrentImageOrder(2);

        String purl = viewManager.getCiteLinkPage();
        Assert.assertEquals("/object/" + PI_KLEIUNIV + "/2/", purl);
    }

    /**
     * @see ViewManager#getCiteLinkWork()
     * @verifies return correct url
     */
    @Test
    public void getCiteLinkWork_shouldReturnCorrectUrl() throws Exception {
        String pi = "PPN123";
        String docstructType = "Catalogue";

        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));

        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);

        String purl = viewManager.getCiteLinkWork();
        Assert.assertEquals("/object/" + PI_KLEIUNIV + "/1/", purl);
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

        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assert.assertTrue(viewManager.isBelowFulltextThreshold(0));
    }

    @Test
    public void testDisplayDownloadWidget() throws IndexUnreachableException, PresentationException, DAOException {
        String pi = "PPN123";
        String docstructType = "Catalogue";

        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));

        FacesContext context = TestUtils.mockFacesContext();

        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        boolean display = viewManager.isDisplayContentDownloadMenu();
        Assert.assertTrue(display);
    }

    @Test
    public void testListDownloadLinksForWork()
            throws IndexUnreachableException, PresentationException, DAOException, IOException {
        String pi = "PPN123";
        String docstructType = "Catalogue";

        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));

        FacesContext context = TestUtils.mockFacesContext();

        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        List<LabeledLink> links = viewManager.getContentDownloadLinksForWork();
        Assert.assertEquals(2, links.size());
    }

    @Test
    public void testGetPageDownloadUrl() throws IndexUnreachableException, DAOException, PresentationException {

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

        Dimension maxSize = new Dimension(20000, 5000);
        Dimension imageSize = new Dimension(1000, 2000);

        List<DownloadOption> options = ViewManager.getDownloadOptionsForImage(configuredOptions, imageSize, maxSize, "00000001.tif");
        assertEquals(2, options.size());

        DownloadOption masterOption = options.stream().filter(o -> o.getFormat().equalsIgnoreCase("tiff")).findFirst().orElse(null);
        assertNotNull(masterOption);
        assertEquals(imageSize, masterOption.getBoxSizeInPixel());

        DownloadOption thumbOption = options.stream().filter(o -> o.getFormat().equalsIgnoreCase("jpg")).findFirst().orElse(null);
        assertNotNull(thumbOption);
        assertEquals(new Dimension(1000 * 600 / 2000, 600), thumbOption.getBoxSizeInPixel());

    }

    @Test
    public void test_setCurrentImageOrderString()
            throws IndexUnreachableException, PresentationException, IDDOCNotFoundException, DAOException {
        StructElement se = new StructElement(iddocKleiuniv);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
        {
            viewManager.setCurrentImageOrderString(null);
            assertEquals(1, viewManager.getCurrentImageOrder());
        }
        {
            viewManager.setCurrentImageOrderString("");
            assertEquals(1, viewManager.getCurrentImageOrder());
        }
        {
            viewManager.setCurrentImageOrderString("\"\"");
            assertEquals(1, viewManager.getCurrentImageOrder());
        }
        {
            viewManager.setCurrentImageOrderString("fgsdfgdf");
            assertEquals(1, viewManager.getCurrentImageOrder());
        }
        {
            viewManager.setCurrentImageOrderString("12");
            assertEquals(12, viewManager.getCurrentImageOrder());
        }
        {
            viewManager.setCurrentImageOrderString("5-8");
            assertEquals(5, viewManager.getCurrentImageOrder());
        }
        {
            viewManager.setCurrentImageOrderString("235674");
            assertEquals(viewManager.getPageLoader().getLastPageOrder(), viewManager.getCurrentImageOrder());
        }
        {
            viewManager.setCurrentImageOrderString("-8");
            assertEquals(viewManager.getPageLoader().getFirstPageOrder(), viewManager.getCurrentImageOrder());
        }
    }

    private static ViewManager createViewManager(String pi, String docstructType, String pageFilename)
            throws IndexUnreachableException, PresentationException {
        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));
        PhysicalElement page = Mockito.mock(PhysicalElement.class);
        Mockito.when(page.getFirstFileName()).thenReturn(pageFilename);
        Mockito.when(page.getFilepath()).thenReturn(pi + "/" + pageFilename);
        Mockito.when(page.getMimeType()).thenReturn("image/tiff");
        Mockito.when(page.getBaseMimeType()).thenReturn("image");

        IPageLoader pageLoader = Mockito.mock(EagerPageLoader.class);
        Mockito.when(pageLoader.getPage(Mockito.anyInt())).thenReturn(page);

        return new ViewManager(se, pageLoader, se.getLuceneId(), null, null, new ImageDeliveryBean());
    }

    @Test
    public void test_getElementsAroundPage() throws IndexUnreachableException, PresentationException, DAOException {
        StructElement se = new StructElement(iddocKleiuniv);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
        assertEquals(16, viewManager.getAllPages().size());
        assertEquals(1, viewManager.getFirstPageOrder());
        assertEquals(16, viewManager.getLastPageOrder());

        {
            List<Integer> pages = viewManager.getPageRangeAroundPage(3, 2, false);
            assertEquals(5, pages.size());
            assertEquals(1, pages.get(0).intValue());
            assertEquals(5, pages.get(4).intValue());
        }
        {
            List<Integer> pages = viewManager.getPageRangeAroundPage(1, 2, false);
            assertEquals(5, pages.size());
            assertEquals(1, pages.get(0).intValue());
            assertEquals(5, pages.get(4).intValue());
        }
        {
            List<Integer> pages = viewManager.getPageRangeAroundPage(3, 3, false);
            assertEquals(7, pages.size());
            assertEquals(1, pages.get(0).intValue());
            assertEquals(7, pages.get(6).intValue());
        }
        {
            List<Integer> pages = viewManager.getPageRangeAroundPage(16, 2, false);
            assertEquals(5, pages.size());
            assertEquals(12, pages.get(0).intValue());
            assertEquals(16, pages.get(4).intValue());
        }
        {
            List<Integer> pages = viewManager.getPageRangeAroundPage(15, 2, false);
            assertEquals(5, pages.size());
            assertEquals(12, pages.get(0).intValue());
            assertEquals(16, pages.get(4).intValue());
        }
        {
            List<Integer> pages = viewManager.getPageRangeAroundPage(10, 3, false);
            assertEquals(7, pages.size());
            assertEquals(7, pages.get(0).intValue());
            assertEquals(13, pages.get(6).intValue());
        }

        {
            List<Integer> pages = viewManager.getPageRangeAroundPage(1, 8, false);
            assertEquals(16, pages.size());
            assertEquals(1, pages.get(0).intValue());
            assertEquals(16, pages.get(15).intValue());
        }

        {
            List<Integer> pages = viewManager.getPageRangeAroundPage(1, 8, true);
            assertEquals(17, pages.size());
            assertEquals(1, pages.get(0).intValue());
            assertEquals(17, pages.get(16).intValue());
        }

    }

    /**
     * @see ViewManager#isFilesOnly()
     * @verifies return true if mime type application
     */
    @Test
    public void isFilesOnly_shouldReturnTrueIfMimeTypeApplication() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, "application/pdf", null);
        Assert.assertTrue(viewManager.isFilesOnly());
    }

    @Test
    public void test_getPdfDownloadLink()
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException, URISyntaxException {
        StructElement se = new StructElement(iddocKleiuniv);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        String test = String.format("%srecords/%s/pdf/", DataManager.getInstance().getConfiguration().getIIIFApiUrl(), se.getPi());
        String link = viewManager.getPdfDownloadLink();
        assertEquals(test, link);
    }

    @Test
    public void test_getPdfDownloadLink_queryParams()
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException, URISyntaxException {
        StructElement se = new StructElement(iddocKleiuniv);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        String test = String.format("%srecords/%s/pdf/?%s=%s",
                DataManager.getInstance().getConfiguration().getIIIFApiUrl(), se.getPi(), "usePdfSource", "true");
        String link = viewManager.getPdfDownloadLink(List.of(List.of("usePdfSource", "true")));
        assertEquals(test, link);

        String test2 = String.format(test + "&%s=%s&%s=%s", "queryA", "a", "queryB", "b");
        String link2 = viewManager.getPdfDownloadLink(List.of(List.of("usePdfSource", "true"), List.of("queryA", "a"), List.of("queryB", "b")));
        assertEquals(test2, link2);
    }

    /**
     * @see ViewManager#getLinkForDFGViewer()
     * @verifies construct default url correctly
     */
    @Test
    public void getLinkForDFGViewer_shouldConstructDefaultUrlCorrectly() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assert.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assert.assertEquals("dfg-viewer_valuesourcefile_valuePPN517154005&set[image]=-1", viewManager.getLinkForDFGViewer());
    }

    /**
     * @see ViewManager#getLinkForDFGViewer()
     * @verifies construct url from custom field correctly
     */
    @Test
    public void getLinkForDFGViewer_shouldConstructUrlFromCustomFieldCorrectly() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assert.assertNotNull(se);
        se.metadataFields.put("MD2_DFGVIEWERURL", Collections.singletonList("https://foo.bar/PPN517154004.xml"));
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assert.assertEquals("dfg-viewer_valuehttps%3A%2F%2Ffoo.bar%2FPPN517154004.xml&set[image]=-1", viewManager.getLinkForDFGViewer());
    }

    /**
     * @see ViewManager#getExternalDownloadUrl()
     * @verifies return correct value
     */
    @Test
    public void getExternalDownloadUrl_shouldReturnCorrectValue() throws Exception {
        String url = "https://example.com/download";
        StructElement se = new StructElement(iddocKleiuniv);
        Assert.assertNotNull(se);
        se.metadataFields.put(SolrConstants.DOWNLOAD_URL_EXTERNAL, Collections.singletonList(url));
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assert.assertEquals(url, viewManager.getExternalDownloadUrl());
    }
}
