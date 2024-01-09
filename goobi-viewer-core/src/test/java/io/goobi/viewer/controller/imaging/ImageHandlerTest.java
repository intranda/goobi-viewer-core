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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.intranda.api.iiif.image.ImageInformation;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.util.PathConverter;
import de.unigoettingen.sub.commons.util.datasource.media.PageSource.IllegalPathSyntaxException;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.ConfigurationTest;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.PhysicalElementBuilder;

/**
 * @author Florian Alpers
 *
 */
public class ImageHandlerTest extends AbstractTest{

    ImageHandler handler;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        handler = new ImageHandler();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
    }

    //    @Test
    void testGetImageInformation() throws IllegalPathSyntaxException, URISyntaxException, ContentLibException {
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
        Assertions.assertEquals(ConfigurationTest.APPLICATION_ROOT_URL + "api/v1/image/1234/00000001.tif/info.json", url);
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
        Assertions.assertEquals(ConfigurationTest.APPLICATION_ROOT_URL + "api/v1/image/PI+1234/ab+00000001.tif/info.json", url);
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
                ConfigurationTest.APPLICATION_ROOT_URL + "api/v1/image/-/http:U002FU002FexteralU002FrestrictedU002FimagesU002F00000001.tif/info.json",
                url);
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
