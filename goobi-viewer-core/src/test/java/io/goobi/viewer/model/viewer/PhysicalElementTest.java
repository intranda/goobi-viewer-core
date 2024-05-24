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

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.managedbeans.ContextMocker;

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

    @Test
    void getImageHeightRationThresholds_test() {
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
     * @see PhysicalElement#getBaseMimeType()
     * @verifies return correct base mime type
     */
    @Test
    void getBaseMimeType_shouldReturnCorrectBaseMimeType() throws Exception {
        Assertions.assertEquals(BaseMimeType.IMAGE, new PhysicalElementBuilder().setMimeType("image/tiff").build().getBaseMimeType());
        Assertions.assertEquals(BaseMimeType.AUDIO, new PhysicalElementBuilder().setMimeType("audio/mpeg3").build().getBaseMimeType());
        Assertions.assertEquals(BaseMimeType.VIDEO, new PhysicalElementBuilder().setMimeType("video/webm").build().getBaseMimeType());
        Assertions.assertEquals(BaseMimeType.APPLICATION,
                new PhysicalElementBuilder().setMimeType("application/pdf").build().getBaseMimeType());
        Assertions.assertEquals(BaseMimeType.SANDBOXED_HTML,
                new PhysicalElementBuilder().setMimeType("text/html").build().getBaseMimeType());
        Assertions.assertEquals(BaseMimeType.MODEL, new PhysicalElementBuilder().setMimeType("model/gltf+json").build().getBaseMimeType());
        Assertions.assertEquals(BaseMimeType.MODEL, new PhysicalElementBuilder().setMimeType("model/object").build().getBaseMimeType());
    }

    /**
     * @see PhysicalElement#getBaseMimeType()
     * @verifies return image if base mime type not found
     */
    @Test
    void getBaseMimeType_shouldReturnImageIfBaseMimeTypeNotFound() throws Exception {
        Assertions.assertEquals(BaseMimeType.IMAGE, new PhysicalElementBuilder().setMimeType("foo/bar").build().getBaseMimeType());
    }

    /**
     * @see PhysicalElement#getImageFilepath()
     * @verifies return filePath if mime type image
     */
    @Test
    void getImageFilepath_shouldReturnImageIfBaseMimeTypeNotFound() throws Exception {
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
}
