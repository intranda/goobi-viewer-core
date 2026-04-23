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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.managedbeans.ContextMocker;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

class PhysicalElementTest extends AbstractDatabaseAndSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(PhysicalElementTest.class);

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        FacesContext facesContext = ContextMocker.mockFacesContext();
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        UIViewRoot viewRoot = Mockito.mock(UIViewRoot.class);
        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
    }

    /**
     * @see PhysicalElement#determineFileName(String)
     * @verifies cut off everything but the file name for normal file paths
     */
    @Test
    void determineFileName_shouldCutOffEverythingButTheFileNameForNormalFilePaths() throws Exception {
        Assertions.assertEquals("image.jpg", PhysicalElement.determineFileName("image.jpg"));
        Assertions.assertEquals("image.jpg", PhysicalElement.determineFileName("/opt/digiverso/viewer/media/123/image.jpg"));
    }

    /**
     * @see PhysicalElement#determineFileName(String)
     * @verifies leave external urls intact
     */
    @Test
    void determineFileName_shouldLeaveExternalUrlsIntact() throws Exception {
        Assertions.assertEquals("http://www.example.com/image.jpg", PhysicalElement.determineFileName("http://www.example.com/image.jpg"));
    }

    /**
     * @verifies return correct threshold values
     * @see PhysicalElement#getImageHeightRationThresholds
     */
    @Test
    void getImageHeightRationThresholds_shouldReturnCorrectThresholdValues() {
        PhysicalElement page =
                new PhysicalElement("PHYS_0001", "00000001.tif", 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);
        Assertions.assertEquals(0.2f, page.getImageHeightRationThresholds().get(0));
        Assertions.assertEquals(2f, page.getImageHeightRationThresholds().get(1));
    }

    /**
     * @see PhysicalElement#getFullMimeType(String,String)
     * @verifies return mimeType if already full mime type
     */
    @Test
    void getFullMimeType_shouldReturnMimeTypeIfAlreadyFullMimeType() throws Exception {
        Assertions.assertEquals("application/pdf", PhysicalElement.getFullMimeType("application/pdf", "foo.bar"));
    }

    /**
     * @see PhysicalElement#getFullMimeType(String,String)
     * @verifies return mimeType if not image
     */
    @Test
    void getFullMimeType_shouldReturnMimeTypeIfNotImage() throws Exception {
        Assertions.assertEquals("application", PhysicalElement.getFullMimeType("application", "foo.bar"));
    }

    /**
     * @see PhysicalElement#getFullMimeType(String,String)
     * @verifies return png image mime type from file name
     */
    @Test
    void getFullMimeType_shouldReturnPngImageMimeTypeFromFileName() throws Exception {
        Assertions.assertEquals("image/png", PhysicalElement.getFullMimeType("image", "foo.png"));
    }

    /**
     * @see PhysicalElement#getFullMimeType(String,String)
     * @verifies return jpeg if not png
     */
    @Test
    void getFullMimeType_shouldReturnJpegIfNotPng() throws Exception {
        Assertions.assertEquals("image/jpeg", PhysicalElement.getFullMimeType("image", "foo.bmp"));
    }

    /**
     * @see PhysicalElement#getMediaType()
     * @verifies return correct media type
     */
    @Test
    void getMediaType_shouldReturnCorrectMediaType() throws Exception {
        Assertions.assertTrue(new PhysicalElementBuilder().setMimeType("image/tiff").build().getMediaType().isImage());
        Assertions.assertTrue(new PhysicalElementBuilder().setMimeType("audio/mpeg3").build().getMediaType().isAudio());
        Assertions.assertTrue(new PhysicalElementBuilder().setMimeType("video/webm").build().getMediaType().isVideo());
        Assertions.assertTrue(new PhysicalElementBuilder().setMimeType("application/pdf").build().getMediaType().isPdf());
        Assertions.assertTrue(new PhysicalElementBuilder().setMimeType("text/html-sandboxed").build().getMediaType().isSandboxedHtml());
        Assertions.assertTrue(new PhysicalElementBuilder().setMimeType("model/gltf+json").build().getMediaType().is3DModel());
        Assertions.assertTrue(new PhysicalElementBuilder().setMimeType("model/object").build().getMediaType().is3DModel());
    }

    /**
     * @see PhysicalElement#getImageFilepath()
     * @verifies return filepath for image mime type
     */
    @Test
    void getImageFilepath_shouldReturnFilepathForImageMimeType() throws Exception {
        Assertions.assertEquals("001.tif", new PhysicalElementBuilder().setMimeType("image/tiff").setFilePath("001.tif").build().getImageFilepath());
    }

    /**
     * @see PhysicalElement#getImageFilepath()
     * @verifies return tiff if available
     */
    @Test
    void getImageFilepath_shouldReturnTiffIfAvailable() throws Exception {
        Assertions.assertEquals("001.tif",
                new PhysicalElementBuilder().setMimeType("audio/mpeg").setFilePath("001.mp3").build().setFilePathTiff("001.tif").getImageFilepath());
    }

    /**
     * @see PhysicalElement#getImageFilepath()
     * @verifies return jpeg if available
     */
    @Test
    void getImageFilepath_shouldReturnJpegIfAvailable() throws Exception {
        Assertions.assertEquals("001.jpg",
                new PhysicalElementBuilder().setMimeType("video/mpeg").setFilePath("001.mp4").build().setFilePathTiff("001.jpg").getImageFilepath());
    }

    /**
     * @verifies return true if access allowed for this page
     * @see PhysicalElement#isAccessPermissionFulltext()
     */
    @Test
    void isAccessPermissionFulltext_shouldReturnTrueIfAccessAllowedForThisPage() throws Exception {
        PhysicalElement pe = new PhysicalElementBuilder().setPi("PPN517154005").setFilePath("00000001.tif").build();
        Assertions.assertTrue(pe.isAccessPermissionFulltext());
    }

    /**
     * @verifies return false if access denied for this page
     * @see PhysicalElement#isAccessPermissionFulltext()
     */
    @Test
    void isAccessPermissionFulltext_shouldReturnFalseIfAccessDeniedForThisPage() throws Exception {
        PhysicalElement pe = new PhysicalElementBuilder().setPi("1164781693_1792000902").setFilePath("EPN_77071899X_0002.tif").build();
        Assertions.assertFalse(pe.isAccessPermissionFulltext());
    }

    /**
     * @see PhysicalElement#seedAccessPermission(String, AccessPermission)
     * @verifies cache prefetched permission without triggering lookup
     */
    @Test
    void seedAccessPermission_shouldCachePrefetchedPermissionWithoutTriggeringLookup() throws Exception {
        PhysicalElement page = new PhysicalElement("PHYS_0001", "00000001.tif", 1,
                "Seite 1", "urn:test:1", "http://purl", "PI_TEST", "image/tiff", null);

        AccessPermission prefetched = AccessPermission.granted();
        page.seedAccessPermission(IPrivilegeHolder.PRIV_VIEW_IMAGES, prefetched);

        assertSame(prefetched, page.getAccessPermission(IPrivilegeHolder.PRIV_VIEW_IMAGES));
    }

    /**
     * @see PhysicalElement#seedAccessPermission(String, AccessPermission)
     * @verifies not overwrite previously cached permission
     */
    @Test
    void seedAccessPermission_shouldNotOverwritePreviouslyCachedPermission() throws Exception {
        PhysicalElement page = new PhysicalElement("PHYS_0001", "00000001.tif", 1,
                "Seite 1", "urn:test:1", "http://purl", "PI_TEST", "image/tiff", null);

        AccessPermission first = AccessPermission.granted();
        AccessPermission second = AccessPermission.denied();
        page.seedAccessPermission(IPrivilegeHolder.PRIV_VIEW_IMAGES, first);
        page.seedAccessPermission(IPrivilegeHolder.PRIV_VIEW_IMAGES, second);

        assertSame(first, page.getAccessPermission(IPrivilegeHolder.PRIV_VIEW_IMAGES));
    }

    /**
     * @see PhysicalElement#seedAccessPermission(String, AccessPermission)
     * @verifies ignore null permission
     */
    @Test
    void seedAccessPermission_shouldIgnoreNullPermission() throws Exception {
        PhysicalElement page = new PhysicalElement("PHYS_0001", "00000001.tif", 1,
                "Seite 1", "urn:test:1", "http://purl", "PI_TEST", "image/tiff", null);

        page.seedAccessPermission(IPrivilegeHolder.PRIV_VIEW_IMAGES, null);
        // Should fall through to denied() default, not NPE
        assertNotNull(page.getAccessPermission(IPrivilegeHolder.PRIV_VIEW_IMAGES));
    }

    /**
     * @see PhysicalElement#isAccessPermissionImageZoom()
     * @verifies return seeded zoom permission without triggering lookup
     */
    @Test
    void isAccessPermissionImageZoom_shouldReturnSeededZoomPermissionWithoutTriggeringLookup() throws Exception {
        PhysicalElement page = new PhysicalElement("PHYS_0001", "00000001.tif", 1,
                "Seite 1", "urn:test:1", "http://purl", "PI_TEST", "image/tiff", null);
        page.seedAccessPermission(IPrivilegeHolder.PRIV_ZOOM_IMAGES, AccessPermission.granted());

        try (MockedStatic<AccessConditionUtils> acu = Mockito.mockStatic(AccessConditionUtils.class)) {
            assertTrue(page.isAccessPermissionImageZoom());
            acu.verifyNoInteractions();
        }
    }

    /**
     * @see PhysicalElement#isAccessPermissionImageDownload()
     * @verifies return seeded download permission without triggering lookup
     */
    @Test
    void isAccessPermissionImageDownload_shouldReturnSeededDownloadPermissionWithoutTriggeringLookup() throws Exception {
        PhysicalElement page = new PhysicalElement("PHYS_0001", "00000001.tif", 1,
                "Seite 1", "urn:test:1", "http://purl", "PI_TEST", "image/tiff", null);
        page.seedAccessPermission(IPrivilegeHolder.PRIV_DOWNLOAD_IMAGES, AccessPermission.denied());

        try (MockedStatic<AccessConditionUtils> acu = Mockito.mockStatic(AccessConditionUtils.class)) {
            assertFalse(page.isAccessPermissionImageDownload());
            acu.verifyNoInteractions();
        }
    }

    /**
     * @see PhysicalElement#isAccessPermissionPdf()
     * @verifies not trigger lookup when pdf permission is seeded
     */
    @Test
    void isAccessPermissionPdf_shouldNotTriggerLookupWhenPdfPermissionIsSeeded() throws Exception {
        PhysicalElement page = new PhysicalElement("PHYS_0001", "00000001.tif", 1,
                "Seite 1", "urn:test:1", "http://purl", "PI_TEST", "image/tiff", null);
        page.seedAccessPermission(IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF, AccessPermission.granted());

        try (MockedStatic<AccessConditionUtils> acu = Mockito.mockStatic(AccessConditionUtils.class)) {
            // Only asserts the fast path does NOT call AccessConditionUtils. The Configuration
            // gate (isPagePdfEnabled) and the mime-type gate still run locally; if either is
            // false in the default fixture configuration, isAccessPermissionPdf correctly
            // returns false without hitting AccessConditionUtils — acu.verifyNoInteractions()
            // is enough either way.
            page.isAccessPermissionPdf();
            acu.verifyNoInteractions();
        }
    }

}
