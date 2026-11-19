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

import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;

/**
 * @author Florian Alpers
 *
 */
class IIIFUrlHandlerTest extends AbstractTest {

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
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies url from file
     */
    @Test
    void getIIIFImageUrl_shouldUrlFromFile() {
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertEquals("/api/v1/records/1234/files/images/filename.tif/full/max/0/default.jpg", url);
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies return empty string for local url
     */
    @Test
    void getIIIFImageUrl_shouldReturnEmptyStringForLocalUrl() {
        for (String url : Arrays.asList("http://localhost/image/filename.tif", "http://127.0.0.1/image/filename.jpg",
                "http://10.0.0.5/image/filename.jpg", "http://169.254.169.254/latest/meta-data/x.jpg", "http://[::1]/image/filename.jpg",
                "https://viewer.goobi.io/viewer/api/v1/records/PPN123/files/source/00000001.tif")) {
            Assertions.assertEquals("", handler.getIIIFImageUrl(url, pi, region, size, rotation, quality, format), url);
        }
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies url from public external image url
     */
    @Test
    void getIIIFImageUrl_shouldUrlFromPublicExternalImageUrl() {
        fileUrl = "http://93.184.216.34/image/filename.tif";
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertEquals("/api/v1/images/external/http:U002FU002F93.184.216.34U002FimageU002Ffilename.tif/full/max/0/default.jpg",
                url);
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies url from configured restricted url
     */
    @Test
    void getIIIFImageUrl_shouldUrlFromConfiguredRestrictedUrl() {
        fileUrl = "http://exteral/restricted/filename.tif";
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertEquals("/api/v1/images/external/http:U002FU002FexteralU002FrestrictedU002Ffilename.tif/full/max/0/default.jpg",
                url);
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies url from external image url
     */
    @Test
    void getIIIFImageUrl_shouldUrlFromExternalImageUrl() {
        fileUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/full/0/native.jpg";
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertEquals("http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/max/0/default.jpg", url);
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies url from external image url containing nested url
     */
    @Test
    void getIIIFImageUrl_shouldUrlFromExternalImageUrlContainingNestedUrl() {
        fileUrl = "https://images.podigee-cdn.net/0x,sW-ya=/https://main.podigee-cdn.net/uploads/u56152/24b68377.jpg";
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertTrue(url.startsWith("/api/v1/images/external/https:U002FU002Fimages.podigee-cdn.net"), url);
        Assertions.assertTrue(url.endsWith("24b68377.jpg/full/max/0/default.jpg"), url);
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies url from local file url
     */
    @Test
    void getIIIFImageUrl_shouldUrlFromLocalFileUrl() {
        fileUrl = cmsMediaFileUrl("filename.tif");
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertEquals("/api/v1/images/external/" + StringTools.escapeCriticalUrlChracters(fileUrl, false) + "/full/max/0/default.jpg",
                url);
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies url from local file url with space
     */
    @Test
    void getIIIFImageUrl_shouldUrlFromLocalFileUrlWithSpace() {
        fileUrl = cmsMediaFileUrl("filename 01.tif");
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assertions.assertTrue(url.startsWith("/api/v1/images/external/file:U002FU002FU002F"), url);
        Assertions.assertTrue(url.endsWith("U002Ffilename%2001.tif/full/max/0/default.jpg"), url);
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies return empty string for file url outside public media folders
     */
    @Test
    void getIIIFImageUrl_shouldReturnEmptyStringForFileUrlOutsidePublicMediaFolders() {
        for (String url : Arrays.asList("file:///image/filename.tif", "file:///C:/opt/digiverso/viewer/data/1/media/PPN123/filename.tif",
                "/opt/digiverso/viewer/data/1/media/PPN123/00000001.tif", cmsMediaFileUrl("../data/1/media/PPN123/00000001.tif"))) {
            Assertions.assertEquals("", handler.getIIIFImageUrl(url, pi, region, size, rotation, quality, format), url);
        }
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies return empty string for record file without valid pi
     */
    @Test
    void getIIIFImageUrl_shouldReturnEmptyStringForRecordFileWithoutValidPi() {
        for (String identifier : Arrays.asList(null, "", " ", "-", "../PPN123")) {
            Assertions.assertEquals("", handler.getIIIFImageUrl(fileUrl, identifier, region, size, rotation, quality, format), identifier);
        }
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies url from pi and file name in file url
     */
    @Test
    void getIIIFImageUrl_shouldUrlFromPiAndFileNameInFileUrl() {
        String url = handler.getIIIFImageUrl("PPN123/filename.tif", "-", region, size, rotation, quality, format);
        Assertions.assertEquals("/api/v1/records/PPN123/files/images/filename.tif/full/max/0/default.jpg", url);
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String)
     * @verifies return empty string for invalid record file name
     */
    @Test
    void getIIIFImageUrl_shouldReturnEmptyStringForInvalidRecordFileName() {
        for (String file : Arrays.asList("..", "PPN123/../filename.tif", "a/b/filename.tif", "PPN123/..", "filenameU002F..U002Fx.tif")) {
            Assertions.assertEquals("", handler.getIIIFImageUrl(file, pi, region, size, rotation, quality, format), file);
        }
    }

    /**
     * @see IIIFUrlHandler#getIIIFImageUrl(String, String, String, String, String, String, String, String)
     * @verifies validate urls without url manager
     */
    @Test
    void getIIIFImageUrl_shouldValidateUrlsWithoutUrlManager() {
        IIIFUrlHandler plain = new IIIFUrlHandler();
        String api = "https://viewer.goobi.io/viewer/rest/";
        Assertions.assertEquals("https://viewer.goobi.io/viewer/rest/image/1234/filename.tif/full/max/0/default.jpg",
                plain.getIIIFImageUrl(api, fileUrl, pi, region, size, rotation, quality, format));
        Assertions.assertEquals("", plain.getIIIFImageUrl(api, fileUrl, "-", region, size, rotation, quality, format));
        Assertions.assertEquals("", plain.getIIIFImageUrl(api, "http://127.0.0.1/filename.jpg", pi, region, size, rotation, quality, format));
        Assertions.assertEquals("", plain.getIIIFImageUrl(api, "file:///etc/filename.tif", pi, region, size, rotation, quality, format));
    }

    private static String cmsMediaFileUrl(String fileName) {
        Configuration config = DataManager.getInstance().getConfiguration();
        return StringTools.appendTrailingSlash(Paths.get(config.getViewerHome(), config.getCmsMediaFolder()).toAbsolutePath().toUri().toString())
                + fileName.replace(" ", "%20");
    }

    /**
     * @verifies return false for given input
     * @see IIIFUrlHandler#isIIIFUrl
     */
    @Test
    void isIIIFUrl_shouldReturnFalseForGivenInput() {
        fileUrl = "Eine Abrechnung mit den Rechtssozialisten_0018.tif";
        Assertions.assertFalse(handler.isIIIFUrl(fileUrl));
    }

    /**
     * @see IIIFUrlHandler#getModifiedIIIFFUrl(String,RegionRequest,Scale,Rotation,Colortype,ImageFileFormat)
     * @verifies replace dimensions correctly
     */
    @Test
    void getModifiedIIIFFUrl_shouldReplaceDimensionsCorrectly() throws Exception {
        // Valid IIIF image URL with full/max/0/default.jpg parameters
        String iiifUrl = "https://example.com/image/full/max/0/default.jpg";
        // Replace region to square and format to png; pass null for unchanged parameters
        String result = handler.getModifiedIIIFFUrl(iiifUrl, RegionRequest.SQUARE, null, null, null, ImageFileFormat.PNG);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("square"), "Region should be replaced to square");
        Assertions.assertTrue(result.contains(".png"), "Format should be replaced to png");
        // Size, rotation, and quality remain unchanged
        Assertions.assertTrue(result.contains("/max/"), "Size should remain unchanged");
        Assertions.assertTrue(result.contains("/0/"), "Rotation should remain unchanged");
    }

    /**
     * @see IIIFUrlHandler#getModifiedIIIFFUrl(String,RegionRequest,Scale,Rotation,Colortype,ImageFileFormat)
     * @verifies do nothing if not iiif url
     */
    @Test
    void getModifiedIIIFFUrl_shouldDoNothingIfNotIiifUrl() throws Exception {
        // Non-IIIF URL should be returned unchanged
        String nonIiifUrl = "https://example.com/images/photo.jpg";
        String result = handler.getModifiedIIIFFUrl(nonIiifUrl, RegionRequest.FULL, Scale.MAX, Rotation.NONE, Colortype.DEFAULT, ImageFileFormat.JPG);
        Assertions.assertEquals(nonIiifUrl, result);
    }

}
