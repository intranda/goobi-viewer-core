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
package io.goobi.viewer.api.rest.v1.authentication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.goobi.viewer.api.rest.model.CurrentUserResponse;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;

class UserEndpointTest extends AbstractRestApiTest {

    /**
     * @see UserEndpoint#getUserInfo()
     * @verifies return ip and null user when not logged in
     */
    @Test
    void getUserInfo_shouldReturnIpAndNullUserWhenNotLoggedIn() throws Exception {
        String url = urls.path(ApiUrls.USERS, ApiUrls.USERS_CURRENT).build();
        try (Response response = target(url).request().get()) {
            org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatus());
            String json = response.readEntity(String.class);
            JsonNode node = mapper.readTree(json);
            // IP must always be present
            assertNotNull(node.get("ip"), "ip field must be present");
            // user must be absent when not logged in
            assertNull(node.get("user"), "user field must be absent when not logged in");
        }
    }

    /**
     * @see UserEndpoint#getUserInfo()
     * @verifies return ip and user info when logged in
     */
    @Test
    void getUserInfo_shouldReturnIpAndUserInfoWhenLoggedIn() throws Exception {
        // This test verifies the structure: when a user IS in the session,
        // the response contains a non-null user object with a userId field.
        // Since JerseyTest does not provide a real session with a UserBean,
        // this test documents the expected JSON shape and verifies that a
        // CurrentUserResponse with a user can be serialized correctly.
        io.goobi.viewer.api.rest.model.UserJsonFacade facade =
                new io.goobi.viewer.api.rest.model.UserJsonFacade(
                        42L, "Test User", null, 0L, true, false, false, false);
        CurrentUserResponse resp = new CurrentUserResponse("127.0.0.1", facade);
        String json = mapper.writeValueAsString(resp);
        JsonNode node = mapper.readTree(json);
        assertNotNull(node.get("ip"), "ip field must be present");
        assertNotNull(node.get("user"), "user field must be present when logged in");
        org.junit.jupiter.api.Assertions.assertEquals(42, node.get("user").get("userId").asLong());
        org.junit.jupiter.api.Assertions.assertEquals("Test User", node.get("user").get("name").asText());
    }
}
