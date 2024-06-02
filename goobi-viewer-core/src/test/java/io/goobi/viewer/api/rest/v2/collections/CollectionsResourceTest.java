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
package io.goobi.viewer.api.rest.v2.collections;

import static io.goobi.viewer.api.rest.v2.ApiUrls.COLLECTIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.COLLECTIONS_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.goobi.viewer.api.rest.v2.AbstractRestApiTest;

/**
 * @author florian
 *
 */
class CollectionsResourceTest extends AbstractRestApiTest {

    private static final String SOLR_FIELD = "DC";
    private static final String COLLECTION = "dctei";
    private static final String GROUP = "MD2_VIEWERSUBTHEME";

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

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.collections.CollectionsResource#getAllCollections(java.lang.String)}.
     * 
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    @Test
    void testGetAllCollections() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(COLLECTIONS).params(SOLR_FIELD).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            String entity = response.readEntity(String.class);
            assertEquals(200, response.getStatus(), "Should return status 200; answer; " + entity);
            assertNotNull(entity);
            JSONObject collection = new JSONObject(entity);
            assertEquals(url, collection.getString("id"));
            assertTrue(collection.getJSONArray("items").length() > 0);
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.collections.CollectionsResource#getCollection(java.lang.String, java.lang.String)}.
     */
    @Test
    void testGetCollection() {
        String url = urls.path(COLLECTIONS, COLLECTIONS_COLLECTION).params(SOLR_FIELD, COLLECTION).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            String entity = response.readEntity(String.class);
            assertEquals(200, response.getStatus(), "Should return status 200; answer; " + entity);
            assertNotNull(entity);
            JSONObject collection = new JSONObject(entity);
            assertEquals(url, collection.getString("id"));
            assertEquals(4, collection.getJSONArray("items").length());
        }
    }
}
