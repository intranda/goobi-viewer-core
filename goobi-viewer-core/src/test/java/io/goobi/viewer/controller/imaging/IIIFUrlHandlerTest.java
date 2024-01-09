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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;

/**
 * @author Florian Alpers
 *
 */
public class IIIFUrlHandlerTest extends AbstractTest {

    IIIFUrlHandler handler;
    String fileUrl = "filename.tif";
    String pi = "1234";
    String region = "full";
    String size = "max";
    String rotation = "0";
    String quality = "default";
    String format = "jpg";

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        ApiUrls urls = new ApiUrls(ApiUrls.API);
        handler = new IIIFUrlHandler(urls);
    }

    /**
     * Test method for
     * {@link io.goobi.viewer.controller.imaging.IIIFUrlHandler#getIIIFImageUrl(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)}.
     */
    @Test
    void testUrlFromFile() {
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertEquals("/api/v1/records/1234/files/images/filename.tif/full/max/0/default.jpg", url);
    }

    @Test
    void testUrlFromLocalUrl() {
        fileUrl = "http://localhost/image/filename.tif";
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertEquals("/api/v1/images/external/http:U002FU002FlocalhostU002FimageU002Ffilename.tif/full/max/0/default.jpg",
                url);
    }

    @Test
    void testUrlFromExternalImageUrl() {
        fileUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/full/0/native.jpg";
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertEquals("http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/max/0/default.jpg", url);
    }

    @Test
    void testUrlFromLocalFileUrl() {
        fileUrl = "file:///image/filename.tif";
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertEquals("/api/v1/images/external/file:U002FU002FU002FimageU002Ffilename.tif/full/max/0/default.jpg",
                url);
    }

    @Test
    void testUrlFromLocalFileUrlWithSpace() {
        fileUrl = "file:///image/filename 01.tif";
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertEquals("/api/v1/images/external/file:U002FU002FU002FimageU002Ffilename%2001.tif/full/max/0/default.jpg", url);
    }

    @Test
    void testUrlFromWindowsFileUrl() {
        fileUrl = "file:///C:/opt/digiverso/viewer/cms_media/filename.tif";
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertEquals(
                "/api/v1/images/external/file:U002FU002FU002FC:U002FoptU002FdigiversoU002FviewerU002Fcms_mediaU002Ffilename.tif/full/max/0/default.jpg",
                url);

    }

}
