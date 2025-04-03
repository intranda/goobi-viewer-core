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
package io.goobi.viewer.api.rest.v2.cms;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v2.AbstractRestApiTest;
import io.goobi.viewer.api.rest.v2.ApiUrls;

/**
 * @author florian
 *
 */
class CMSMediaImageResourceTest extends AbstractRestApiTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testCallImageUrlCapitalSuffix() {
        String filename = "image4.JPG";
        String url = urls.path(ApiUrls.CMS_MEDIA, ApiUrls.CMS_MEDIA_FILES_FILE).params(filename).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            String entity = response.readEntity(String.class);
            //since no file image4.JPG exists, 404 is returned. But that is ok as long as the method was called (otherwise 405 would be thrown)
            assertEquals(404, response.getStatus(), "Should return status 404; answer; " + entity);
        }
    }
}
