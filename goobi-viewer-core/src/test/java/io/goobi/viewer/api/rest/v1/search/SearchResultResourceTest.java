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
package io.goobi.viewer.api.rest.v1.search;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.TestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

class SearchResultResourceTest extends AbstractDatabaseEnabledTest {

    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;

    @BeforeEach
    void setup() throws Exception {
        super.setUp();
        this.servletRequest = Mockito.mock(HttpServletRequest.class);
        this.servletResponse = Mockito.mock(HttpServletResponse.class);
    }

    /**
     * @see SearchResultResource#getSearchHitChildren(List,String,int,Locale)
     * @verifies return null if searchHits null
     */
    @Test
    void getSearchHitChildren_shouldReturnNullIfSearchHitsNull() throws Exception {
        TestUtils.mockFacesContext();
        
        SearchResultResource resource = new SearchResultResource(this.servletRequest, this.servletResponse);
        Response response = resource.getRISAsFile("PI:AC13451894", "", "", 0);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
}