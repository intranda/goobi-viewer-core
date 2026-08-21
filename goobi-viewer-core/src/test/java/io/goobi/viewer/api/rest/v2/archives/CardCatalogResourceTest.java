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
package io.goobi.viewer.api.rest.v2.archives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v2.AbstractRestApiTest;
import jakarta.ws.rs.core.Response;

/**
 * Integration tests for the card catalog endpoints against the EAD archive of the test index
 * ("Akte_Koch_-_Humboldt_Universitaet").
 */
class CardCatalogResourceTest extends AbstractRestApiTest {

    private static final String PI = "Akte_Koch_-_Humboldt_Universitaet";

    /**
     * @verifies return catalog info with drawers
     */
    @Test
    void getInfo_shouldReturnCatalogInfoWithDrawers() {
        try (Response response = target("/archives/" + PI + "/cardcatalog/").request().get()) {
            assertEquals(200, response.getStatus());
            JSONObject info = new JSONObject(response.readEntity(String.class));
            assertEquals(PI, info.getString("pi"));
            assertTrue(info.getInt("totalEntries") > 0);
            assertFalse(info.getJSONArray("drawers").isEmpty());
            assertTrue(info.getJSONObject("config").getInt("zoomFactor") > 0);
        }
    }

    /**
     * @verifies return boundaries and entries for a drawer
     */
    @Test
    void getBoundariesAndEntries_shouldReturnCardsForADrawer() {
        String drawerId;
        try (Response response = target("/archives/" + PI + "/cardcatalog/").request().get()) {
            assertEquals(200, response.getStatus());
            JSONObject info = new JSONObject(response.readEntity(String.class));
            drawerId = info.getJSONArray("drawers").getJSONObject(0).getString("id");
        }
        try (Response response = target("/archives/" + PI + "/cardcatalog/drawers/" + drawerId + "/boundaries").request().get()) {
            assertEquals(200, response.getStatus());
            JSONObject section = new JSONObject(response.readEntity(String.class));
            assertFalse(section.getJSONArray("items").isEmpty());
            assertEquals(1, section.getJSONArray("items").getJSONObject(0).getInt("position"));
        }
        try (Response response = target("/archives/" + PI + "/cardcatalog/drawers/" + drawerId + "/entries")
                .queryParam("from", 1)
                .queryParam("to", 3)
                .request()
                .get()) {
            assertEquals(200, response.getStatus());
            JSONObject section = new JSONObject(response.readEntity(String.class));
            assertTrue(section.getJSONArray("items").length() <= 3);
        }
    }

    /**
     * @verifies return register below the configured threshold
     */
    @Test
    void getRegister_shouldReturnRegisterBelowThreshold() {
        // test config: registerThreshold=2000, the test archive has a few hundred nodes
        try (Response response = target("/archives/" + PI + "/cardcatalog/register").request().get()) {
            assertEquals(200, response.getStatus());
            JSONObject register = new JSONObject(response.readEntity(String.class));
            assertTrue(register.getBoolean("available"));
            assertFalse(register.getJSONArray("drawers").isEmpty());
        }
    }

    /**
     * @verifies return 404 for unknown archives and entries
     */
    @Test
    void endpoints_shouldReturn404ForUnknownArchivesAndEntries() {
        try (Response response = target("/archives/NOSUCHARCHIVE/cardcatalog/").request().get()) {
            assertEquals(404, response.getStatus());
        }
        try (Response response = target("/archives/" + PI + "/cardcatalog/lookup")
                .queryParam("entryId", "nosuchentry")
                .request()
                .get()) {
            assertEquals(404, response.getStatus());
        }
    }
}
