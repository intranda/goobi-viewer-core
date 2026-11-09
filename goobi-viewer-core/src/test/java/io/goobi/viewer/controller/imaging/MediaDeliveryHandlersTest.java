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
}
