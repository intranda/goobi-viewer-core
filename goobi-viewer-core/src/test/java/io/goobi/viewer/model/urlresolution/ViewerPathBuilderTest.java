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
package io.goobi.viewer.model.urlresolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.viewer.PageType;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Florian Alpers
 *
 */
class ViewerPathBuilderTest extends AbstractTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @verifies return true for given input
     */
    @Test
    void startsWith_shouldReturnTrueForGivenInput() {
        String url1 = "a";
        String url2 = "a/b";
        String url3 = "a/b/c";
        String url4 = "f";
        String url5 = "b/a";
        String url6 = "a/bc";

        URI uri = URI.create("a/b/cdef");

        Assertions.assertTrue(ViewerPathBuilder.startsWith(uri, url1));
        Assertions.assertTrue(ViewerPathBuilder.startsWith(uri, url2));
        Assertions.assertFalse(ViewerPathBuilder.startsWith(uri, url3));
        Assertions.assertFalse(ViewerPathBuilder.startsWith(uri, url4));
        Assertions.assertFalse(ViewerPathBuilder.startsWith(uri, url5));
        Assertions.assertFalse(ViewerPathBuilder.startsWith(uri, url6));

    }

    /**
     * @verifies create correct path w ith leading excamation mark
     * @see ViewerPathBuilder#createPath
     */
    @Test
    void createPath_shouldCreateCorrectPathWIthLeadingExcamationMark() throws DAOException {
        String url = "http://localhost:8082/viewer/!fulltext/AC03343066/13/";
        String serverUrl = "http://localhost:8082/viewer";
        String applicationName = "/viewer";
        Optional<ViewerPath> path = ViewerPathBuilder.createPath(serverUrl, applicationName, url, "");
        assertTrue(path.isPresent());
        assertEquals("fulltext/AC03343066/13/", path.get().getCombinedPath().toString());
    }

    /**
     * @see ViewerPathBuilder#createPath(HttpServletRequest)
     * @verifies strip scheme, host, port and context path from URL and return remaining path
     */
    @Test
    void createPath_shouldStripSchemeHostPortAndContextPathFromURLAndReturnRemainingPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("/viewer");
        request.setParameter("urn", "urn:nbn:de:gbv:9-g-4882158");
        Optional<ViewerPath> path = ViewerPathBuilder.createPath(request, "http://localhost:8080/viewer/!fulltext/AC03343066/13/");
        assertTrue(path.isPresent());
        assertEquals("fulltext/AC03343066/13/", path.get().getCombinedPath().toString());
    }

    /**
     * @see ViewerPathBuilder#createPath(HttpServletRequest, String)
     * @verifies remove server url or name correctly
     */
    @Test
    void createPath_shouldRemoveServerUrlOrNameCorrectly() throws Exception {
        // Verify that both full server URL prefix and context-path-only prefix are stripped
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("/viewer");

        // When the baseUrl starts with the full server URL, it should be stripped
        Optional<ViewerPath> path1 = ViewerPathBuilder.createPath(request, "http://localhost:8080/viewer/image/PPN123/1/");
        assertTrue(path1.isPresent());
        assertEquals("image/PPN123/1/", path1.get().getCombinedPath().toString());

        // When the baseUrl starts with just the context path (server name), it should also be stripped
        Optional<ViewerPath> path2 = ViewerPathBuilder.createPath(request, "/viewer/image/PPN456/2/");
        assertTrue(path2.isPresent());
        assertEquals("image/PPN456/2/", path2.get().getCombinedPath().toString());
    }

    /**
     * @verifies return correct type for configured name
     * @see ViewerPathBuilder#getPageType(final URI)
     */
    @Test
    void getPageType_shouldReturnCorrectTypeForConfiguredName() {
        // Test config maps viewImage to "viewImage_value"
        URI servicePath = URI.create("viewImage_value/PPN123/1/");
        Optional<PageType> result = ViewerPathBuilder.getPageType(servicePath);
        assertTrue(result.isPresent());
        assertEquals(PageType.viewImage, result.get());
    }

    /**
     * @verifies return correct type for raw name
     * @see ViewerPathBuilder#getPageType(final URI)
     */
    @Test
    void getPageType_shouldReturnCorrectTypeForRawName() {
        URI servicePath = URI.create("object/PPN123/1/");
        Optional<PageType> result = ViewerPathBuilder.getPageType(servicePath);
        assertTrue(result.isPresent());
        assertEquals(PageType.viewObject, result.get());
    }

    /**
     * @verifies return empty for unknown path
     * @see ViewerPathBuilder#getPageType(final URI)
     */
    @Test
    void getPageType_shouldReturnEmptyForUnknownPath() {
        URI servicePath = URI.create("nonexistent/PPN123/1/");
        Optional<PageType> result = ViewerPathBuilder.getPageType(servicePath);
        assertFalse(result.isPresent());
    }

    /**
     * When both viewImage and viewObject are configured to "object", a URL starting with "object/" should resolve to viewObject (whose raw name
     * matches), not viewImage.
     *
     * @verifies prefer raw name match when both types have same configured name
     */
    @Test
    void getPageType_shouldPreferRawNameMatchWhenBothTypesHaveSameConfiguredName() {
        DataManager.getInstance()
                .injectConfiguration(new Configuration(
                        new File("src/test/resources/config_viewer_pageTypes_overlap.test.xml").getAbsolutePath()));

        // Both viewImage and viewObject are configured to "object", so both match.
        // viewObject should win because its raw name "object" also matches the URL.
        URI servicePath = URI.create("object/PPN123/1/");
        Optional<PageType> result = ViewerPathBuilder.getPageType(servicePath);
        assertTrue(result.isPresent());
        assertEquals(PageType.viewObject, result.get());
    }

    /**
     * When both viewImage and viewObject are configured to "object", createPath should use the configured name "object" (not the raw name "image") as
     * the page path.
     *
     * @verifies use configured name as page path
     */
    @Test
    void createPath_shouldUseConfiguredNameAsPagePath() throws Exception {
        DataManager.getInstance()
                .injectConfiguration(new Configuration(
                        new File("src/test/resources/config_viewer_pageTypes_overlap.test.xml").getAbsolutePath()));

        Optional<ViewerPath> path = ViewerPathBuilder.createPath(
                "http://localhost:8080/viewer", "/viewer", "object/PPN123/1/", "");
        assertTrue(path.isPresent());
        assertEquals(PageType.viewObject, path.get().getPageType());
        // The page path must use the configured name "object", not switch to a raw name
        assertEquals("object", path.get().getPagePath().toString());
        assertEquals("object/PPN123/1/", path.get().getCombinedPath().toString());
    }
}
