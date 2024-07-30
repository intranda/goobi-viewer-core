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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import io.goobi.viewer.model.security.CopyrightIndicatorLicense;
import io.goobi.viewer.model.security.CopyrightIndicatorStatus;
import io.goobi.viewer.model.security.CopyrightIndicatorStatus.Status;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.model.viewer.pageloader.EagerPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;
import io.goobi.viewer.solr.SolrConstants;

class ViewManagerTest extends AbstractDatabaseAndSolrEnabledTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

    }

    /**
     * @see ViewManager#getPage(int)
     * @verifies return correct page
     */
    @Test
    void getPage_shouldReturnCorrectPage() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
        PhysicalElement pe = viewManager.getPage(3).orElse(null);
        Assertions.assertNotNull(pe);
        Assertions.assertEquals(3, pe.getOrder());
    }

    /**
     * @see ViewManager#getPage(int)
     * @verifies return null if order less than zero
     */
    @Test
    void getPage_shouldReturnNullIfOrderLessThanZero() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
        PhysicalElement pe = viewManager.getPage(-1).orElse(null);
        Assertions.assertNull(pe);
    }

    /**
     * @see ViewManager#getPage(int)
     * @verifies return null if order larger than number of pages
     */
    @Test
    void getPage_shouldReturnNullIfOrderLargerThanNumberOfPages() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
        PhysicalElement pe = viewManager.getPage(17).orElse(null);
        Assertions.assertNull(pe);
    }

    /**
     * @see ViewManager#getPage(int)
     * @verifies return null if pageLoader is null
     */
    @Test
    void getPage_shouldReturnNullIfPageLoaderIsNull() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, null, se.getLuceneId(), null, null, null);
        PhysicalElement pe = viewManager.getPage(0).orElse(null);
        Assertions.assertNull(pe);
    }

    /**
     * @see ViewManager#getImagesSection()
     * @verifies return correct PhysicalElements for a thumbnail page
     */
    @Test
    void getImagesSection_shouldReturnCorrectPhysicalElementsForAThumbnailPage() throws Exception {
        int thumbnailsPerPage = 10;

        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
        Assertions.assertEquals(16, viewManager.getImagesCount());

        viewManager.setCurrentThumbnailPage(1);
        List<PhysicalElement> pages = viewManager.getImagesSection(thumbnailsPerPage);
        Assertions.assertEquals(10, pages.size());
        Assertions.assertEquals(1, pages.get(0).getOrder());
        Assertions.assertEquals(10, pages.get(9).getOrder());

        viewManager.setCurrentThumbnailPage(2);
        pages = viewManager.getImagesSection(thumbnailsPerPage);
        Assertions.assertEquals(6, pages.size());
        Assertions.assertEquals(11, pages.get(0).getOrder());
        Assertions.assertEquals(15, pages.get(4).getOrder());
    }

    @Test
    void getImagesSection_shouldReturnCorrectPhysicalElementsForAThumbnailPageWithStartPageTwo() throws Exception {
        int thumbnailsPerPage = 30;

        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);

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
        Assertions.assertEquals(180, viewManager.getImagesCount());

        viewManager.setCurrentThumbnailPage(1);
        List<PhysicalElement> pages = viewManager.getImagesSection(thumbnailsPerPage);
        Assertions.assertEquals(30, pages.size());
        Assertions.assertEquals(2, pages.get(0).getOrder());
        Assertions.assertEquals(31, pages.get(pages.size() - 1).getOrder());

        viewManager.setCurrentThumbnailPage(2);
        pages = viewManager.getImagesSection(thumbnailsPerPage);
        Assertions.assertEquals(30, pages.size());
        Assertions.assertEquals(32, pages.get(0).getOrder());
        Assertions.assertEquals(61, pages.get(pages.size() - 1).getOrder());

        viewManager.setCurrentThumbnailPage(6);
        pages = viewManager.getImagesSection(thumbnailsPerPage);
        Assertions.assertEquals(30, pages.size());
        Assertions.assertEquals(152, pages.get(0).getOrder());
        Assertions.assertEquals(181, pages.get(pages.size() - 1).getOrder());
    }

    /**
     * @see ViewManager#resetImage()
     * @verifies reset rotation
     */
    @Test
    void resetImage_shouldResetRotation() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
        Assertions.assertEquals(0, viewManager.getCurrentRotate());
        viewManager.rotateRight();
        Assertions.assertEquals(90, viewManager.getCurrentRotate());
        viewManager.resetImage();
        Assertions.assertEquals(0, viewManager.getCurrentRotate());
    }

    /**
     * @see ViewManager#rotateLeft()
     * @verifies rotate correctly
     */
    @Test
    void rotateLeft_shouldRotateCorrectly() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
        Assertions.assertEquals(0, viewManager.getCurrentRotate());
        viewManager.rotateLeft();
        Assertions.assertEquals(270, viewManager.getCurrentRotate());
        viewManager.rotateLeft();
        Assertions.assertEquals(180, viewManager.getCurrentRotate());
        viewManager.rotateLeft();
        Assertions.assertEquals(90, viewManager.getCurrentRotate());
        viewManager.rotateLeft();
        Assertions.assertEquals(0, viewManager.getCurrentRotate());
    }

    /**
     * @see ViewManager#rotateRight()
     * @verifies rotate correctly
     */
    @Test
    void rotateRight_shouldRotateCorrectly() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, null);
        Assertions.assertEquals(0, viewManager.getCurrentRotate());
        viewManager.rotateRight();
        Assertions.assertEquals(90, viewManager.getCurrentRotate());
        viewManager.rotateRight();
        Assertions.assertEquals(180, viewManager.getCurrentRotate());
        viewManager.rotateRight();
        Assertions.assertEquals(270, viewManager.getCurrentRotate());
        viewManager.rotateRight();
        Assertions.assertEquals(0, viewManager.getCurrentRotate());
    }

    /**
     * @see ViewManager#getPdfPartDownloadLink()
     * @verifies construct url correctly
     */
    @Test
    void getPdfPartDownloadLink_shouldConstructUrlCorrectly() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        Assertions.assertEquals(16, viewManager.getImagesCount());

        viewManager.setFirstPdfPage("14");
        viewManager.setLastPdfPage("16");
        String url = viewManager.getPdfPartDownloadLink();
        String expect = "records/" + PI_KLEIUNIV + "/files/images/00000014.tif$00000015.tif$00000016.tif/full.pdf";
        Assertions.assertTrue(url.contains(expect), "Expeted URL to contain '" + expect + "' but was: " + url);
    }

    /**
     * @see ViewManager#getCiteLinkDocstruct()
     * @verifies return correct url
     */
    @Test
    void getCiteLinkDocstruct_shouldReturnCorrectUrl() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV, true);
        viewManager.setCurrentImageOrder(10);

        String purl = viewManager.getCiteLinkDocstruct();
        Assertions.assertEquals("/object/" + PI_KLEIUNIV + "/5/LOG_0003/", purl);
    }

    /**
     * @see ViewManager#getCiteLinkPage()
     * @verifies return correct url
     */
    @Test
    void getCiteLinkPage_shouldReturnCorrectUrl() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV, true);
        viewManager.setCurrentImageOrder(2);

        String purl = viewManager.getCiteLinkPage();
        Assertions.assertEquals("/object/" + PI_KLEIUNIV + "/2/", purl);
    }

    /**
     * @see ViewManager#getCiteLinkWork()
     * @verifies return correct url
     */
    @Test
    void getCiteLinkWork_shouldReturnCorrectUrl() throws Exception {
        String pi = "PPN123";
        String docstructType = "Catalogue";

        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));

        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV, true);

        String purl = viewManager.getCiteLinkWork();
        Assertions.assertEquals("/object/" + PI_KLEIUNIV + "/1/", purl);
    }

    /**
     * @see ViewManager#isBelowFulltextThreshold(double)
     * @verifies return true if there are no pages
     */
    @Test
    void isBelowFulltextThreshold_shouldReturnTrueIfThereAreNoPages() throws Exception {
        String pi = "PPN123";
        String docstructType = "Catalogue";

        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));

        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assertions.assertTrue(viewManager.isBelowFulltextThreshold(0));
    }

    @Test
    void testDisplayDownloadWidget() throws IndexUnreachableException, PresentationException, DAOException {
        String pi = "PPN123";
        String docstructType = "Catalogue";

        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));

        FacesContext context = TestUtils.mockFacesContext();

        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        boolean display = viewManager.isDisplayContentDownloadMenu();
        Assertions.assertTrue(display);
    }

    @Test
    void testListDownloadLinksForWork()
            throws IndexUnreachableException, PresentationException, DAOException, IOException {
        String pi = "PPN123";
        String docstructType = "Catalogue";

        StructElement se = new StructElement(123L);
        se.setDocStructType(docstructType);
        se.getMetadataFields().put(SolrConstants.PI_TOPSTRUCT, Collections.singletonList(pi));

        FacesContext context = TestUtils.mockFacesContext();

        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        List<LabeledLink> links = viewManager.getContentDownloadLinksForWork();
        Assertions.assertEquals(2, links.size());
    }

    @Test
    void testGetPageDownloadUrl() throws IndexUnreachableException, DAOException, PresentationException, ViewerConfigurationException {

        String pi = "PPN123";
        String docstructType = "Catalogue";
        String filename = "00000001.tif";

        ViewManager viewManager = createViewManager(pi, docstructType, filename);

        String baseUrl = DataManager.getInstance().getRestApiManager().getContentApiUrl()
                + "records/" + pi + "/files/images/" + filename;

        DownloadOption maxSizeTiff = new DownloadOption("Master", "master", "max");
        String masterTiffUrl = baseUrl + "/full/max/0/default.tif";
        assertEquals(masterTiffUrl, viewManager.getPageDownloadUrl(maxSizeTiff, viewManager.getCurrentPage()).replaceAll("\\?.*", "")); //ignore query params

        DownloadOption scaledJpeg = new DownloadOption("Thumbnail", "jpg", new Dimension(800, 1200));
        String thumbnailUrl = baseUrl + "/full/!800,1200/0/default.jpg";
        assertEquals(thumbnailUrl, viewManager.getPageDownloadUrl(scaledJpeg, viewManager.getCurrentPage()).replaceAll("\\?.*", "")); //ignore query params

    }

    @Test
    void testGetDownloadOptionsForImage() {

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
    void test_setCurrentImageOrderString()
            throws IndexUnreachableException, PresentationException, IDDOCNotFoundException, DAOException {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
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
        Mockito.when(page.getBaseMimeType()).thenReturn(BaseMimeType.IMAGE);

        IPageLoader pageLoader = Mockito.mock(EagerPageLoader.class);
        Mockito.when(pageLoader.getPage(Mockito.anyInt())).thenReturn(page);

        return new ViewManager(se, pageLoader, se.getLuceneId(), null, null, new ImageDeliveryBean());
    }

    @Test
    void test_getElementsAroundPage() throws IndexUnreachableException, PresentationException, DAOException {
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
    void isFilesOnly_shouldReturnTrueIfMimeTypeApplication() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, "application/pdf", null);
        Assertions.assertTrue(viewManager.isFilesOnly());
    }

    @Test
    void test_getPdfDownloadLink()
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException, URISyntaxException {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());
        String test = String.format("%srecords/%s/pdf/", DataManager.getInstance().getConfiguration().getIIIFApiUrl(), se.getPi());
        String link = viewManager.getPdfDownloadLink();
        assertEquals(test, link);
    }

    @Test
    void test_getPdfDownloadLink_queryParams()
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException, URISyntaxException {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
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
    void getLinkForDFGViewer_shouldConstructDefaultUrlCorrectly() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assertions.assertEquals("dfg-viewer_valuesourcefile_valuePPN517154005&set[image]=-1", viewManager.getLinkForDFGViewer());
    }

    /**
     * @see ViewManager#getLinkForDFGViewer()
     * @verifies construct url from custom field correctly
     */
    @Test
    void getLinkForDFGViewer_shouldConstructUrlFromCustomFieldCorrectly() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        se.metadataFields.put("MD2_DFGVIEWERURL", Collections.singletonList("https://foo.bar/PPN517154004.xml"));
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assertions.assertEquals("dfg-viewer_valuehttps%3A%2F%2Ffoo.bar%2FPPN517154004.xml&set[image]=-1", viewManager.getLinkForDFGViewer());
    }

    /**
     * @see ViewManager#getExternalDownloadUrl()
     * @verifies return correct value
     */
    @Test
    void getExternalDownloadUrl_shouldReturnCorrectValue() throws Exception {
        String url = "https://example.com/download";
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        se.metadataFields.put(SolrConstants.DOWNLOAD_URL_EXTERNAL, Collections.singletonList(url));
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assertions.assertEquals(url, viewManager.getExternalDownloadUrl());
    }

    /**
     * @see ViewManager#getCopyrightIndicatorStatusName()
     * @verifies return locked status if locked most restrictive status found
     */
    @Test
    void getCopyrightIndicatorStatusName_shouldReturnLockedStatusIfLockedMostRestrictiveStatusFound() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        se.metadataFields.put("MD_ACCESSCONDITION", Arrays.asList("Freier Zugang", "Eingeschränker Zugang", "Gesperrter Zugang"));
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assertions.assertEquals(Status.LOCKED.name(), viewManager.getCopyrightIndicatorStatusName());
    }

    /**
     * @see ViewManager#getCopyrightIndicatorStatusName()
     * @verifies return partial status if partial most restrictive status found
     */
    @Test
    void getCopyrightIndicatorStatusName_shouldReturnPartialStatusIfPartialMostRestrictiveStatusFound() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        se.metadataFields.put("MD_ACCESSCONDITION", Arrays.asList("Freier Zugang", "Eingeschränker Zugang"));
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assertions.assertEquals(Status.PARTIAL.name(), viewManager.getCopyrightIndicatorStatusName());
    }

    /**
     * @see ViewManager#getCopyrightIndicatorStatusName()
     * @verifies return open status if no restrictive statuses found
     */
    @Test
    void getCopyrightIndicatorStatusName_shouldReturnOpenStatusIfNoRestrictiveStatusesFound() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        Assertions.assertEquals(Status.OPEN.name(), viewManager.getCopyrightIndicatorStatusName());
    }

    /**
     * @see ViewManager#getCopyrightIndicatorStatuses()
     * @verifies return correct statuses
     */
    @Test
    void getCopyrightIndicatorStatuses_shouldReturnCorrectStatuses() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        se.metadataFields.put("MD_ACCESSCONDITION", Arrays.asList("Eingeschränker Zugang", "Gesperrter Zugang"));
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        List<CopyrightIndicatorStatus> result = viewManager.getCopyrightIndicatorStatuses();
        assertEquals(2, result.size());
    }

    /**
     * @see ViewManager#getCopyrightIndicatorStatuses()
     * @verifies return open status if no statuses found
     */
    @Test
    void getCopyrightIndicatorStatuses_shouldReturnOpenStatusIfNoStatusesFound() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        List<CopyrightIndicatorStatus> result = viewManager.getCopyrightIndicatorStatuses();
        assertEquals(1, result.size());
        assertEquals(Status.OPEN, result.get(0).getStatus());
    }

    /**
     * @see ViewManager#getCopyrightIndicatorLicense()
     * @verifies return correct license
     */
    @Test
    void getCopyrightIndicatorLicense_shouldReturnCorrectLicense() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        se.metadataFields.put("MD_ACCESSCONDITIONCOPYRIGHT", Arrays.asList("VGWORT"));
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        CopyrightIndicatorLicense license = viewManager.getCopyrightIndicatorLicense();
        assertNotNull(license);
        assertEquals("COPYRIGHT_DESCRIPTION_VGWORT", license.getDescription());
    }

    /**
     * @see ViewManager#getCopyrightIndicatorLicense()
     * @verifies return default license if no licenses found
     */
    @Test
    void getCopyrightIndicatorLicense_shouldReturnDefaultLicenseIfNoLicensesFound() throws Exception {
        StructElement se = new StructElement(iddocKleiuniv);
        Assertions.assertNotNull(se);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        CopyrightIndicatorLicense license = viewManager.getCopyrightIndicatorLicense();
        assertNotNull(license);
        assertEquals("", license.getDescription());
    }

    @Test
    void test_getLinkToDownloadFile()
            throws UnsupportedEncodingException, URISyntaxException, IndexUnreachableException, PresentationException, DAOException {
        String filename = "INN 2_Gutenzell.pdf";
        String filenameEncoded = "INN%202_Gutenzell.pdf";

        StructElement se = new StructElement(iddocKleiuniv);
        ViewManager viewManager = new ViewManager(se, AbstractPageLoader.create(se), se.getLuceneId(), null, null, new ImageDeliveryBean());

        LabeledLink link = viewManager.getLinkToDownloadFile(filename);
        assertTrue(link.getUrl().endsWith(filenameEncoded));

    }

}
