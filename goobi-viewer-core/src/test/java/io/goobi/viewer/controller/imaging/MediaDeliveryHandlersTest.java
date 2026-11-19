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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;

class MediaDeliveryHandlersTest extends AbstractTest {

    private static final String SERVLET_PATH = "https://viewer.example.org/viewer";

    private Configuration config;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        config = Mockito.mock(Configuration.class);
        Mockito.when(config.getViewerHome()).thenReturn("/opt/digiverso/viewer/");
        Mockito.when(config.getCmsMediaFolder()).thenReturn("cms_media");
        Mockito.when(config.getTempMediaFolder()).thenReturn("temp_media");
        Mockito.when(config.getTheme()).thenReturn("mytheme");
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("viewer.example.org");
        request.setServerPort(443);
        request.setContextPath("/viewer");
        return request;
    }

    /**
     * @see MediaDeliveryHandlers#forRequest(jakarta.servlet.http.HttpServletRequest)
     * @verifies not create session
     */
    @Test
    void forRequest_shouldNotCreateSession() {
        MockHttpServletRequest request = request();
        MediaDeliveryHandlers handlers = MediaDeliveryHandlers.forRequest(request);
        assertNotNull(handlers.getThumbs());
        assertNotNull(handlers.getMedia());
        assertNotNull(handlers.getPresentation());
        assertNull(request.getSession(false));
    }

    /**
     * @see MediaDeliveryHandlers#forRequest(jakarta.servlet.http.HttpServletRequest)
     * @verifies cache instance in request
     */
    @Test
    void forRequest_shouldCacheInstanceInRequest() {
        MockHttpServletRequest request = request();
        assertSame(MediaDeliveryHandlers.forRequest(request), MediaDeliveryHandlers.forRequest(request));
    }

    /**
     * @see MediaDeliveryHandlers#forRequest(jakarta.servlet.http.HttpServletRequest)
     * @verifies use servlet path of request
     */
    @Test
    void forRequest_shouldUseServletPathOfRequest() {
        MediaDeliveryHandlers handlers = MediaDeliveryHandlers.forRequest(request());
        assertEquals(SERVLET_PATH, handlers.getServletPath());
        assertTrue(handlers.getStaticImagesURI().startsWith(SERVLET_PATH + "/resources/"));
    }

    /**
     * @see MediaDeliveryHandlers#isExternalUrl(String, String)
     * @verifies return true for absolute http url of other host
     */
    @Test
    void isExternalUrl_shouldReturnTrueForAbsoluteHttpUrlOfOtherHost() {
        assertTrue(MediaDeliveryHandlers.isExternalUrl("https://other.example.org/image.jpg", SERVLET_PATH));
    }

    /**
     * @see MediaDeliveryHandlers#isExternalUrl(String, String)
     * @verifies return false for url within servlet path
     */
    @Test
    void isExternalUrl_shouldReturnFalseForUrlWithinServletPath() {
        assertFalse(MediaDeliveryHandlers.isExternalUrl(SERVLET_PATH + "/resources/images/foo.png", SERVLET_PATH));
    }

    /**
     * @see MediaDeliveryHandlers#isExternalUrl(String, String)
     * @verifies return false for file url
     */
    @Test
    void isExternalUrl_shouldReturnFalseForFileUrl() {
        assertFalse(MediaDeliveryHandlers.isExternalUrl("file:///opt/digiverso/viewer/cms_media/foo.jpg", SERVLET_PATH));
    }

    /**
     * @see MediaDeliveryHandlers#isExternalUrl(String, String)
     * @verifies return false for relative url
     */
    @Test
    void isExternalUrl_shouldReturnFalseForRelativeUrl() {
        assertFalse(MediaDeliveryHandlers.isExternalUrl("00000001.tif", SERVLET_PATH));
        assertFalse(MediaDeliveryHandlers.isExternalUrl(null, SERVLET_PATH));
    }

    /**
     * @see MediaDeliveryHandlers#isUnrestrictedUrl(String, String, Configuration)
     * @verifies return true for external url
     */
    @Test
    void isUnrestrictedUrl_shouldReturnTrueForExternalUrl() {
        assertTrue(MediaDeliveryHandlers.isUnrestrictedUrl("https://other.example.org/image.jpg", SERVLET_PATH, config));
        assertTrue(MediaDeliveryHandlers.isUnrestrictedUrl("file:///opt/digiverso/viewer/cms_media/foo.jpg", SERVLET_PATH, config));
    }

    /**
     * @see MediaDeliveryHandlers#isUnrestrictedUrl(String, String, Configuration)
     * @verifies return true for static image url
     */
    @Test
    void isUnrestrictedUrl_shouldReturnTrueForStaticImageUrl() {
        assertTrue(MediaDeliveryHandlers.isUnrestrictedUrl(SERVLET_PATH + "/resources/themes/mytheme/images/logo.png", SERVLET_PATH, config));
    }

    /**
     * @see MediaDeliveryHandlers#isUnrestrictedUrl(String, String, Configuration)
     * @verifies return false for record image file name
     */
    @Test
    void isUnrestrictedUrl_shouldReturnFalseForRecordImageFileName() {
        assertFalse(MediaDeliveryHandlers.isUnrestrictedUrl("00000001.tif", SERVLET_PATH, config));
        assertFalse(MediaDeliveryHandlers.isUnrestrictedUrl("file:///opt/digiverso/viewer/other/foo.jpg", SERVLET_PATH, config));
    }

    /**
     * @see MediaDeliveryHandlers#isAllowedExternalImageUrl(String, String, Configuration)
     * @verifies allow http url of public host
     */
    @Test
    void isAllowedExternalImageUrl_shouldAllowHttpUrlOfPublicHost() {
        assertTrue(MediaDeliveryHandlers.isAllowedExternalImageUrl("https://93.184.216.34/images/foo.jpg", SERVLET_PATH, config));
        assertTrue(MediaDeliveryHandlers.isAllowedExternalImageUrl("http://93.184.216.34:8080/images/foo.jpg", SERVLET_PATH, config));
    }

    /**
     * @see MediaDeliveryHandlers#isAllowedExternalImageUrl(String, String, Configuration)
     * @verifies reject loopback and private addresses
     */
    @Test
    void isAllowedExternalImageUrl_shouldRejectLoopbackAndPrivateAddresses() {
        for (String url : List.of("http://localhost:8080/viewer/api/v1/records/PPN123/files/images/00000001.tif",
                "http://127.0.0.1/foo.jpg", "http://127.1.2.3/foo.jpg", "http://2130706433/foo.jpg", "http://0.0.0.0/foo.jpg",
                "http://[::1]/foo.jpg", "http://[::ffff:127.0.0.1]/foo.jpg", "http://10.0.0.5/foo.jpg", "http://192.168.1.1/foo.jpg",
                "http://172.16.0.1/foo.jpg", "http://169.254.169.254/latest/meta-data/foo.jpg", "http://[fe80::1]/foo.jpg",
                "http://224.0.0.1/foo.jpg", "http:U002FU002F127.0.0.1U002Ffoo.jpg", "http://100.64.0.1/foo.jpg", "http://[fd00::1]/foo.jpg")) {
            assertFalse(MediaDeliveryHandlers.isAllowedExternalImageUrl(url, SERVLET_PATH, config), url);
        }
    }

    /**
     * @see MediaDeliveryHandlers#isAllowedExternalImageUrl(String, String, Configuration)
     * @verifies reject url of own host
     */
    @Test
    void isAllowedExternalImageUrl_shouldRejectUrlOfOwnHost() {
        assertFalse(MediaDeliveryHandlers.isAllowedExternalImageUrl(SERVLET_PATH + "/api/v1/records/PPN123/files/images/00000001.tif",
                SERVLET_PATH, config));
        assertFalse(MediaDeliveryHandlers.isAllowedExternalImageUrl("http://VIEWER.example.org:8080/foo.jpg", SERVLET_PATH, config));
    }

    /**
     * @see MediaDeliveryHandlers#isAllowedExternalImageUrl(String, String, Configuration)
     * @verifies allow static theme image of own host
     */
    @Test
    void isAllowedExternalImageUrl_shouldAllowStaticThemeImageOfOwnHost() {
        assertTrue(MediaDeliveryHandlers.isAllowedExternalImageUrl(SERVLET_PATH + "/resources/themes/mytheme/images/logo.png", SERVLET_PATH,
                config));
        assertFalse(MediaDeliveryHandlers.isAllowedExternalImageUrl(
                SERVLET_PATH + "/resources/themes/mytheme/images/../../../../api/v1/records/PPN123/files/images/00000001.tif", SERVLET_PATH,
                config));
    }

    /**
     * @see MediaDeliveryHandlers#isAllowedExternalImageUrl(String, String, Configuration)
     * @verifies allow configured restricted url even on internal host
     */
    @Test
    void isAllowedExternalImageUrl_shouldAllowConfiguredRestrictedUrlEvenOnInternalHost() {
        Mockito.when(config.getRestrictedImageUrls()).thenReturn(List.of("https?://10.0.0.5/restricted/.*", "https?://images.internal/.*\\.tif"));
        assertTrue(MediaDeliveryHandlers.isAllowedExternalImageUrl("http://10.0.0.5/restricted/foo.jpg", SERVLET_PATH, config));
        assertTrue(MediaDeliveryHandlers.isAllowedExternalImageUrl("http:U002FU002F10.0.0.5U002FrestrictedU002Ffoo.jpg", SERVLET_PATH, config));
        assertTrue(MediaDeliveryHandlers.isAllowedExternalImageUrl("https://images.internal/a/b/00000001.tif", SERVLET_PATH, config));
        // other urls of the same internal host are still rejected
        assertFalse(MediaDeliveryHandlers.isAllowedExternalImageUrl("http://10.0.0.5/other/foo.jpg", SERVLET_PATH, config));
    }

    /**
     * @see MediaDeliveryHandlers#isAllowedExternalImageUrl(String, String, Configuration)
     * @verifies reject non http restricted url
     */
    @Test
    void isAllowedExternalImageUrl_shouldRejectNonHttpRestrictedUrl() {
        Mockito.when(config.getRestrictedImageUrls()).thenReturn(List.of(".*\\.tif"));
        assertFalse(MediaDeliveryHandlers.isAllowedExternalImageUrl("file:///opt/digiverso/viewer/data/1/media/PPN123/00000001.tif", SERVLET_PATH,
                config));
        assertFalse(MediaDeliveryHandlers.isAllowedExternalImageUrl("s3://bucket/00000001.tif", SERVLET_PATH, config));
    }

    /**
     * @see MediaDeliveryHandlers#isAllowedExternalImageUrl(String, String, Configuration)
     * @verifies allow file url only in public media folders
     */
    @Test
    void isAllowedExternalImageUrl_shouldAllowFileUrlOnlyInPublicMediaFolders() {
        assertTrue(MediaDeliveryHandlers.isAllowedExternalImageUrl("file:///opt/digiverso/viewer/cms_media/foo.jpg", SERVLET_PATH, config));
        assertTrue(MediaDeliveryHandlers.isAllowedExternalImageUrl("file:///opt/digiverso/viewer/temp_media/foo.jpg", SERVLET_PATH, config));
        assertFalse(MediaDeliveryHandlers.isAllowedExternalImageUrl("file:///opt/digiverso/viewer/data/1/media/PPN123/00000001.tif",
                SERVLET_PATH, config));
        assertFalse(MediaDeliveryHandlers.isAllowedExternalImageUrl("file:///opt/digiverso/viewer/cms_media/../data/foo.jpg", SERVLET_PATH,
                config));
        assertFalse(MediaDeliveryHandlers.isAllowedExternalImageUrl("/opt/digiverso/viewer/data/foo.jpg", SERVLET_PATH, config));
    }

    /**
     * @see MediaDeliveryHandlers#isAllowedExternalImageUrl(String, String, Configuration)
     * @verifies reject other schemes and malformed urls
     */
    @Test
    void isAllowedExternalImageUrl_shouldRejectOtherSchemesAndMalformedUrls() {
        for (String url : Arrays.asList(null, "", "s3://bucket/foo.jpg", "jar:file:///opt/foo.jar!/foo.jpg", "ftp://93.184.216.34/foo.jpg",
                "http:///foo.jpg", "http://unresolvable-host.invalid/foo.jpg", "00000001.tif")) {
            assertFalse(MediaDeliveryHandlers.isAllowedExternalImageUrl(url, SERVLET_PATH, config), String.valueOf(url));
        }
    }
}
