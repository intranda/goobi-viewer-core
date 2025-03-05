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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import io.goobi.viewer.exceptions.DAOException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Florian Alpers
 *
 */
class ViewerPathBuilderTest {

    @Test
    void testStartsWith() {
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

    @Test
    void testCreateCorrectPathWIthLeadingExcamationMark() throws DAOException {
        String url = "http://localhost:8082/viewer/!fulltext/AC03343066/13/";
        String serverUrl = "http://localhost:8082/viewer";
        String applicationName = "/viewer";
        Optional<ViewerPath> path = ViewerPathBuilder.createPath(serverUrl, applicationName, url, "");
        assertTrue(path.isPresent());
        assertEquals("fulltext/AC03343066/13/", path.get().getCombinedPath().toString());
    }

    /**
     * @see ViewerPathBuilder#createPath(HttpServletRequest,String)
     * @verifies remove server url and name correctly
     */
    @Test
    void createPath_shouldRemoveServerUrlAndNameCorrectly() throws Exception {
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
}
