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
package io.goobi.viewer.api.rest.v1.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;

/**
 * Tests for {@link CMSPageResource}.
 * Verifies that requesting a non-existent CMS page returns HTTP 404 rather than
 * HTTP 500 (which would occur when null is passed to the ViewerPage constructor).
 */
class CMSPageResourceTest extends AbstractRestApiTest {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Requesting a CMS page that does not exist in the DAO must return HTTP 404.
     * Before the fix, getCMSPage() returned null which was passed to new ViewerPage(null),
     * causing a NullPointerException and HTTP 500.
     */
    @Test
    void testGetPage_nonExistentIdReturns404() {
        // Use an ID that is very unlikely to exist in the test database
        try (Response response = target("/cms/pages/999999")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(404, response.getStatus(),
                    "Non-existent CMS page id should return HTTP 404, not 500");
        }
    }
}
