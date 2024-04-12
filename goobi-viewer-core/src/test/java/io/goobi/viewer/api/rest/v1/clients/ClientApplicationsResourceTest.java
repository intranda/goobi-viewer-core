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

import static io.goobi.viewer.api.rest.v1.ApiUrls.CLIENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.CLIENTS_CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.clients.ClientApplication.AccessStatus;

/**
 * Unit tests for {@link ClientApplicationsResource}
 * 
 * @author florian
 *
 */
class ClientApplicationsResourceTest extends AbstractRestApiTest {

    @Test
    void test_noAccess() {
        try (Response response = target()
                .path(CLIENTS)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            String entity = response.readEntity(String.class);
            assertEquals(401, response.getStatus(), "Should return status 401; answer; " + entity);
        }

        try (Response response = target()
                .path(CLIENTS + CLIENTS_CLIENT.replace("{id}", "12345"))
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            String entity = response.readEntity(String.class);
            assertEquals(401, response.getStatus(), "Should return status 401; answer; " + entity);
        }

        ClientApplication client = new ClientApplication("12345");
        try (Response response = target()
                .path(CLIENTS + CLIENTS_CLIENT.replace("{id}", "12345"))
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(client, MediaType.APPLICATION_JSON))) {
            String entity = response.readEntity(String.class);
            assertEquals(401, response.getStatus(), "Should return status 401; answer; " + entity);
        }
    }

    @Test
    void test_listClients() {
        try (Response response = target(CLIENTS)
                .request()
                .header("token", "test")
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            String entity = response.readEntity(String.class);
            assertEquals(200, response.getStatus(), "Should return status 200; answer; " + entity);
            assertNotNull(entity);
            JSONArray clients = new JSONArray(entity);
            assertEquals(2, clients.length());
            assertEquals("First client", clients.getJSONObject(0).get("name"));
        }
    }

    @Test
    void test_getClient() {
        try (Response response = target()
                .path(CLIENTS + CLIENTS_CLIENT.replace("{id}", "1234-abcd-4321"))
                .request()
                .header("token", "test")
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            String entity = response.readEntity(String.class);
            assertEquals(200, response.getStatus(), "Should return status 200; answer; " + entity);
            JSONObject json = new JSONObject(entity);
            assertEquals("First client", json.get("name"));
        }

    }

    @Test
    void test_putClient() throws DAOException {

        ClientApplication databaseClient = DataManager.getInstance().getDao().getClientApplicationByClientId("1234-abcd-4321");
        assertEquals(AccessStatus.REQUESTED, databaseClient.getAccessStatus());
        assertNotEquals("100.200.10.20/2", databaseClient.getSubnetMask());

        ClientApplication client = new ClientApplication();
        client.setAccessStatus(AccessStatus.GRANTED);
        client.setSubnetMask("100.200.10.20/2");

        try (Response response = target()
                .path(CLIENTS + CLIENTS_CLIENT.replace("{id}", "1234-abcd-4321"))
                .request()
                .header("token", "test")
                .accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(client, MediaType.APPLICATION_JSON))) {
            String entity = response.readEntity(String.class);
            assertEquals(200, response.getStatus(), "Should return status 200; answer; " + entity);
        }

        databaseClient = DataManager.getInstance().getDao().getClientApplicationByClientId("1234-abcd-4321");
        assertEquals(AccessStatus.GRANTED, databaseClient.getAccessStatus());
        assertEquals("100.200.10.20/2", databaseClient.getSubnetMask());
    }
}
