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
package io.goobi.viewer.api.rest.v1.records;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.goobi.viewer.api.rest.AbstractRestApiTest;
import io.goobi.viewer.api.rest.model.ErrorMessage;
import io.goobi.viewer.model.rss.Channel;
import io.goobi.viewer.model.rss.RssItem;
import static io.goobi.viewer.api.rest.v1.ApiUrls.*;
/**
 * @author florian
 *
 */
public class RSSResourceTest extends AbstractRestApiTest {

   
    @Test
    public void testRSSJsonMax() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS, RECORDS_RSS_JSON).build())
                .queryParam("max", 5)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            Channel channel = mapper.readValue(entity, Channel.class);
            Assert.assertEquals(5, channel.getItems().size());
        }
    }
    
    @Test
    public void testRSSJsonLang() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS, RECORDS_RSS_JSON).build())
                .queryParam("lang", "en")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            Channel channel = mapper.readValue(entity, Channel.class);
            Assert.assertEquals("en", channel.getLanguage());
        }
    }
    
    @Test
    public void testRSSJsonQuery() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS, RECORDS_RSS_JSON).build())
                .queryParam("query", "MD_TITLE:Berlin")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            Channel channel = mapper.readValue(entity, Channel.class);
            for (RssItem item : channel.getItems()) {
                assertTrue("Result doesn't match query 'MD_TITLE:Berlin'", item.getTitle().contains("Berlin"));
            }
        }
    }
    
    @Test
    public void testRSSJsonFacets() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS, RECORDS_RSS_JSON).build())
                .queryParam("query", "MD_TITLE:Berlin")
                .queryParam("facets", "DOCSTRCT:volume")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            Channel channel = mapper.readValue(entity, Channel.class);
            for (RssItem item : channel.getItems()) {
                assertTrue("Result doesn't match query 'MD_TITLE:Berlin'", item.getTitle().contains("Berlin"));
                assertTrue("Result doesn't match facet 'DOCSTRCT:volume'", item.getDocType().equalsIgnoreCase("band"));
            }
        }
    }
    
    @Test
    public void testRSSJsonSubtheme() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS, RECORDS_RSS_JSON).build())
                .queryParam("subtheme", "subtheme2")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            Channel channel = mapper.readValue(entity, Channel.class);
            Assert.assertEquals(3, channel.getItems().size());
        }
    }
    
    @Test
    public void testRSSXmlSubtheme() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS, RECORDS_RSS_XML).build())
                .queryParam("subtheme", "subtheme2")
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return entity", response.getEntity());
        }
    }
    
    @Test
    public void testRSSXml() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS, RECORDS_RSS_XML).build())
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return entity", response.getEntity());
        }
    }
    
    @Test
    public void testRSSInvalidType() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS, RECORDS_RSS_JSON).build())
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals("Should return status 406", 406, response.getStatus());
            String entity = response.readEntity(String.class);
            ErrorMessage message = mapper.readValue(entity, ErrorMessage.class);
            assertEquals(406, message.getStatus());
        }
    }

}
