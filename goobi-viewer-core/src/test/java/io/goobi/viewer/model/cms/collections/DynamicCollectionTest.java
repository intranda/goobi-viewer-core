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
package io.goobi.viewer.model.cms.collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import jakarta.servlet.http.HttpServletRequest;

class DynamicCollectionTest extends AbstractTest {

    /**
     * A dynamic collection without a custom collection URL must resolve to a records search built directly from the request, without relying on a
     * FacesContext / PrettyContext. This is required for the IIIF collection REST endpoint, which runs outside of JSF.
     *
     * @see DynamicCollection#getLinkURI(HttpServletRequest)
     * @verifies build a records search url from the request without a faces context
     */
    @Test
    void getLinkURI_shouldBuildRecordsSearchUrlFromRequestWithoutFacesContext() {
        DynamicCollection dc = new DynamicCollection("test_link");
        dc.setSolrQuery("YEAR:[1980 TO 2000]");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/viewer");

        URI uri = dc.getLinkURI(request);
        assertNotNull(uri, "A records search URL should be built even without a FacesContext");
        String url = uri.toString();
        assertTrue(url.startsWith("http://localhost:8080/viewer/search/-/"), "Unexpected URL: " + url);
        assertTrue(url.endsWith("/1/-/-/"), "Unexpected URL: " + url);
    }

    /**
     * A custom collection URL takes precedence over the default records search.
     *
     * @see DynamicCollection#getLinkURI(HttpServletRequest)
     * @verifies honor a custom collection url
     */
    @Test
    void getLinkURI_shouldHonorCustomCollectionUrl() {
        DynamicCollection dc = new DynamicCollection("test_link");
        dc.setSolrQuery("YEAR:[1980 TO 2000]");
        dc.setCollectionUrl("/mypage/");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/viewer");

        URI uri = dc.getLinkURI(request);
        assertNotNull(uri);
        assertTrue(uri.toString().endsWith("/viewer/mypage/"), "Unexpected URL: " + uri);
    }

    /**
     * With neither a custom URL nor a stored query there is no link.
     *
     * @see DynamicCollection#getLinkURI(HttpServletRequest)
     * @verifies return null when no query and no custom url
     */
    @Test
    void getLinkURI_shouldReturnNullWhenNoQueryAndNoCustomUrl() {
        DynamicCollection dc = new DynamicCollection("test_link");
        dc.setSolrQuery("");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/viewer");

        assertNull(dc.getLinkURI(request));
    }
}
