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
package io.goobi.viewer.controller.imaging;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.intranda.api.iiif.image.ImageInformation;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.TestUtils;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.PhysicalElementBuilder;

/**
 * @author Florian Alpers
 *
 */
class ImageHandlerTest extends AbstractTest {

    ImageHandler handler;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        AbstractApiUrlManager urls = new ApiUrls(TestUtils.APPLICATION_ROOT_URL + "api/v1/");
        handler = new ImageHandler(urls);
    }

    //    @Test
    void testGetImageInformation() throws URISyntaxException, ContentLibException {
        String url1 = "http://localhost:8081/ics/iiif/image/18979459-1830/00375666.png/info.json";
        String url2 = "18979459-1830/00375666.png";

        ImageInformation info = handler.getImageInformation(url1);
        Assertions.assertNotNull(info);

        info = handler.getImageInformation(url2);
        Assertions.assertNotNull(info);

    }

    @Test
    void testGetImageUrlLocal() {
        PhysicalElement page = new PhysicalElementBuilder().setPi("1234")
                .setPhysId("PHYS_0001")
                .setFilePath("00000001.tif")
                .setOrder(1)
                .setOrderLabel("Seite 1")
                .setUrn("urn:234235:3423")
                .setPurlPart("http://purl")
                .setMimeType("image/tiff")
                .setDataRepository(null)
                .build();

        String url = handler.getImageUrl(page);
        Assertions.assertEquals(TestUtils.APPLICATION_ROOT_URL + "api/v1/records/1234/files/images/00000001.tif/info.json?pageType=viewImage", url);
    }

    @Test
    void testGetImageUrlLocal_handleSpecialCharacters() {
        PhysicalElement page = new PhysicalElementBuilder().setPi("PI 1234")
                .setPhysId("PHYS_0001")
                .setFilePath("ab 00000001.tif")
                .setOrder(1)
                .setOrderLabel("Seite 1")
                .setUrn("urn:234235:3423")
                .setPurlPart("http://purl")
                .setMimeType("image/tiff")
                .setDataRepository(null)
                .build();

        String url = handler.getImageUrl(page);
        URI uri = URI.create(url);
        Assertions.assertEquals(TestUtils.APPLICATION_ROOT_URL + "api/v1/records/PI+1234/files/images/ab+00000001.tif/info.json?pageType=viewImage",
                url);
        Assertions.assertEquals(url, uri.toString());

    }

    @Test
    void testGetImageUrlExternal() {
        PhysicalElement page = new PhysicalElementBuilder().setPi("1234")
                .setPhysId("PHYS_0001")
                .setFilePath("http://otherServer/images/00000001.tif/info.json")
                .setOrder(1)
                .setOrderLabel("Seite 1")
                .setUrn("urn:234235:3423")
                .setPurlPart("http://purl")
                .setMimeType("image/tiff")
                .setDataRepository(null)
                .build();

        String url = handler.getImageUrl(page);
        Assertions.assertEquals("http://otherServer/images/00000001.tif/info.json", url);
    }

    @Test
    void testGetImageUrlInternal() {
        PhysicalElement page = new PhysicalElementBuilder().setPi("1234")
                .setPhysId("PHYS_0001")
                .setFilePath("http://exteral/restricted/images/00000001.tif")
                .setOrder(1)
                .setOrderLabel("Seite 1")
                .setUrn("urn:234235:3423")
                .setPurlPart("http://purl")
                .setMimeType("image/tiff")
                .setDataRepository(null)
                .build();

        String url = handler.getImageUrl(page);
        Assertions.assertEquals(
                TestUtils.APPLICATION_ROOT_URL
                        + "api/v1/images/external/http:U002FU002FexteralU002FrestrictedU002FimagesU002F00000001.tif/info.json?pageType=viewImage",
                url);
    }

    @Test
    void testGetImageInformationFromPage()
            throws URISyntaxException, ContentLibException, IndexUnreachableException, ViewerConfigurationException, DAOException {
        PhysicalElement page = Mockito.mock(PhysicalElement.class);
        Mockito.when(page.getFilepath()).thenReturn("00000318.tif");
        Mockito.when(page.getImageWidth()).thenReturn(800);
        Mockito.when(page.getImageHeight()).thenReturn(1200);
        Mockito.when(page.getMimeType()).thenReturn("image/tiff");
        Mockito.when(page.getPi()).thenReturn("PPN1234");

        ImageInformation info = handler.getImageInformation(page, PageType.viewObject);
        Assertions.assertEquals(TestUtils.APPLICATION_ROOT_URL + "api/v1/records/PPN1234/files/images/00000318.tif", info.getId().toString());
        Assertions.assertEquals(page.getImageWidth(), info.getWidth());
        Assertions.assertEquals(page.getImageHeight(), info.getHeight());
        Assertions.assertEquals(600, info.getSizes().get(0).getWidth());
        Assertions.assertEquals(900, info.getSizes().get(0).getHeight());
        Assertions.assertEquals(512, info.getTiles().get(0).getWidth());
        Assertions.assertEquals(3, info.getTiles().get(0).getScaleFactors().get(2));
    }

    @Test
    void testResolveURIs() throws URISyntaxException {
        String stringExternal = "https://localhost:8080/a/b/c d";
        String stringInternal = "file:/a/b/c d#yxwg=123,52,564,213";
        String stringRelative = "a/b/c d [1]-falls.jpg";

        URI uriExternal = PathConverter.toURI(stringExternal);
        Assertions.assertEquals("https://localhost:8080/a/b/c%20d", uriExternal.toString());
        URI uriInternal = PathConverter.toURI(stringInternal);
        Assertions.assertEquals("file:///a/b/c%20d#yxwg=123,52,564,213", uriInternal.toString());
        URI uriRelative = PathConverter.toURI(stringRelative);
        Assertions.assertEquals("a/b/c%20d%20%5B1%5D-falls.jpg", uriRelative.toString());

        Path pathExternal = PathConverter.getPath(uriExternal);
        Assertions.assertEquals("/a/b/c d", pathExternal.toString());
        Path pathInternal = PathConverter.getPath(uriInternal);
        Assertions.assertEquals("/a/b/c d", pathInternal.toString());
        Path pathRelative = PathConverter.getPath(uriRelative);
        Assertions.assertEquals("a/b/c d [1]-falls.jpg", pathRelative.toString());

    }

}
