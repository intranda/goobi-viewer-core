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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RSS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RSS_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.goobi.viewer.api.rest.model.ErrorMessage;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.model.rss.Channel;
import io.goobi.viewer.model.rss.RssItem;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author florian
 *
 */
class RSSResourceTest extends AbstractRestApiTest {

    /**
     * @verifies limit results to max parameter
     */
    @Test
    void getRssJsonFeed_shouldLimitResultsToMaxParameter() throws JsonProcessingException {
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

    /**
     * @verifies set channel language from lang parameter
     */
    @Test
    void getRssJsonFeed_shouldSetChannelLanguageFromLangParameter() throws JsonProcessingException {
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

    /**
     * @verifies filter results by query parameter
     */
    @Test
    void getRssJsonFeed_shouldFilterResultsByQueryParameter() throws JsonProcessingException {
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

    /**
     * @verifies filter results by query and facet parameters
     */
    @Test
    void getRssJsonFeed_shouldFilterResultsByQueryAndFacetParameters() throws JsonProcessingException {
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

    /**
     * @verifies filter results by subtheme parameter
     */
    @Test
    void getRssJsonFeed_shouldFilterResultsBySubthemeParameter() throws JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_RSS, RECORDS_RSS_JSON).build())
                .queryParam("subtheme", "subtheme2")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            Channel channel = mapper.readValue(entity, Channel.class);
            Assertions.assertEquals(2, channel.getItems().size());
        }
    }

    /**
     * @verifies return xml filtered by subtheme
     */
    @Test
    void getRssFeed_shouldReturnXmlFilteredBySubtheme() {
        try (Response response = target(urls.path(RECORDS_RSS).build())
                .queryParam("subtheme", "subtheme2")
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return entity");
        }
    }

    /**
     * @verifies return status 200 with xml entity
     */
    @Test
    void getRssFeed_shouldReturnStatus200WithXmlEntity() {
        try (Response response = target(urls.path(RECORDS_RSS).build())
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return entity");
        }
    }

    /**
     * @verifies return status 400 for invalid solr query
     */
    @Test
    void getRssFeed_shouldReturnStatus400ForInvalidSolrQuery() {
        // A syntactically invalid Solr query must return 400, not 500
        try (Response response = target(urls.path(RECORDS_RSS).build())
                .queryParam("query", ":::")
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(400, response.getStatus(), "Invalid Solr query should return status 400");
        }
    }

    /**
     * @verifies return status 400 for invalid solr query
     */
    @Test
    void getRssJsonFeed_shouldReturnStatus400ForInvalidSolrQuery() throws JsonProcessingException {
        // A syntactically invalid Solr query must return 400, not 500
        try (Response response = target(urls.path(RECORDS_RSS, RECORDS_RSS_JSON).build())
                .queryParam("query", ":::")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(400, response.getStatus(), "Invalid Solr query should return status 400");
        }
    }

    /**
     * @verifies return status 400 for slash query
     */
    @Test
    void getRssFeed_shouldReturnStatus400ForSlashQuery() {
        // query=/ triggers IndexUnreachableException("Error from server ...") in Solr,
        // which must map to HTTP 400, not 500
        try (Response response = target(urls.path(RECORDS_RSS).build())
                .queryParam("query", "/")
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(400, response.getStatus(), "Solr syntax error (/) should return status 400");
        }
    }

    /**
     * @verifies return status 400 for slash query
     */
    @Test
    void getRssJsonFeed_shouldReturnStatus400ForSlashQuery() {
        // query=/ triggers IndexUnreachableException("Error from server ...") in Solr,
        // which must map to HTTP 400, not 500
        try (Response response = target(urls.path(RECORDS_RSS, RECORDS_RSS_JSON).build())
                .queryParam("query", "/")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(400, response.getStatus(), "Solr syntax error (/) should return status 400");
        }
    }

    /**
     * @verifies return status 406 when accepting xml on json endpoint
     */
    @Test
    void getRssJsonFeed_shouldReturnStatus406WhenAcceptingXmlOnJsonEndpoint() throws JsonProcessingException {
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
