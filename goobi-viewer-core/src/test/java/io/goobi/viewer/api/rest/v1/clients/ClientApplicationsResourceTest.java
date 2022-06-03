/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest.v1.clients;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;
import static org.junit.Assert.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;

/**
 * @author florian
 *
 */
public class ClientApplicationsResourceTest  extends AbstractRestApiTest {

    @Test
    public void testListClients() {
        String url = urls.path(CLIENTS).build();
        try(Response response = target()
                .path(CLIENTS)
                .request()
                .header("token", "test")
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            String entity = response.readEntity(String.class);
            assertEquals("Should return status 200; answer; " + entity, 200, response.getStatus());
            assertNotNull(entity);
            JSONArray clients = new JSONArray(entity);
            assertEquals(2, clients.length());
            assertEquals("First client", clients.getJSONObject(0).get("name"));
        }
    }

}
