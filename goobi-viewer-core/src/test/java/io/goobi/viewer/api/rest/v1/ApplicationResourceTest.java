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
package io.goobi.viewer.api.rest.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiInfo;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author florian
 *
 */
class ApplicationResourceTest extends AbstractRestApiTest {

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.ApplicationResource#getApiInfo()}.
     */
    @Test
    void testGetApiInfo() {
        try (Response response = target(urls.path().build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            ApiInfo info = response.readEntity(ApiInfo.class);
            assertNotNull(info);
            assertEquals("v1", info.getVersion());
        }
    }

}
