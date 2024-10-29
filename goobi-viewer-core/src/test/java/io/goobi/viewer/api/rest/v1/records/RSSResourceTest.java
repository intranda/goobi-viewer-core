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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.goobi.viewer.api.rest.model.ErrorMessage;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.model.rss.Channel;
import io.goobi.viewer.model.rss.RssItem;

/**
 * @author florian
 *
 */
class RSSResourceTest extends AbstractRestApiTest {

    @Test
    void testRSSJsonMax() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_RSS, RECORDS_RSS_JSON).build())
                .queryParam("max", 5)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            Channel channel = mapper.readValue(entity, Channel.class);
            Assertions.assertEquals(5, channel.getItems().size());
        }
    }

    @Test
    void testRSSJsonLang() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_RSS, RECORDS_RSS_JSON).build())
                .queryParam("lang", "en")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            Channel channel = mapper.readValue(entity, Channel.class);
            Assertions.assertEquals("en", channel.getLanguage());
        }
    }

    @Test
    void testRSSJsonQuery() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_RSS, RECORDS_RSS_JSON).build())
                .queryParam("query", "MD_TITLE:Berlin")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            Channel channel = mapper.readValue(entity, Channel.class);
            for (RssItem item : channel.getItems()) {
                assertTrue(item.getTitle().contains("Berlin"), "Result doesn't match query 'MD_TITLE:Berlin': " + item.getTitle());
            }
        }
    }

    @Test
    void testRSSJsonFacets() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_RSS, RECORDS_RSS_JSON).build())
                .queryParam("query", "MD_TITLE:Berlin")
                .queryParam("facets", "DOCSTRCT:volume")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            Channel channel = mapper.readValue(entity, Channel.class);
            for (RssItem item : channel.getItems()) {
                assertTrue(item.getTitle().contains("Berlin"), "Result doesn't match query 'MD_TITLE:Berlin'");
                assertTrue(item.getDocType().equalsIgnoreCase("band"), "Result doesn't match facet 'DOCSTRCT:volume'");
            }
        }
    }

    @Test
    void testRSSJsonSubtheme() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_RSS, RECORDS_RSS_JSON).build())
                .queryParam("subtheme", "subtheme2")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            Channel channel = mapper.readValue(entity, Channel.class);
            Assertions.assertEquals(3, channel.getItems().size());
        }
    }

    @Test
    void testRSSXmlSubtheme() {
        try (Response response = target(urls.path(RECORDS_RSS).build())
                .queryParam("subtheme", "subtheme2")
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return entity");
        }
    }

    @Test
    void testRSSXml() {
        try (Response response = target(urls.path(RECORDS_RSS).build())
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return entity");
        }
    }

    @Test
    void testRSSInvalidType() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_RSS, RECORDS_RSS_JSON).build())
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(406, response.getStatus(), "Should return status 406");
            String entity = response.readEntity(String.class);
            ErrorMessage message = mapper.readValue(entity, ErrorMessage.class);
            assertEquals(406, message.getStatus());
        }
    }
}
