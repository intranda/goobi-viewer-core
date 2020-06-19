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

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;
import static org.junit.Assert.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import io.goobi.viewer.api.rest.AbstractRestApiTest;
import io.goobi.viewer.model.rss.Channel;

/**
 * @author florian
 *
 */
public class RecordResourceTest extends AbstractRestApiTest{

    private static final String PI = "74241";
    private static final String PI_ANNOTATIONS = "PI_1";
    private static final String DIVID ="LOG_0004";
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getRISAsFile()}.
     */
    @Test
    public void testGetRISAsFile() {
        try(Response response = target(urls.path(RECORDS_RECORD, RECORDS_RIS_FILE).params(PI).build())
                .request()
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            assertTrue(entity.contains("TY  - BOOK"));
            assertTrue(entity.contains("CN  - 74241"));
            String fileName = PI + "_LOG_0000.ris";
            assertEquals( "attachment; filename=\"" + fileName + "\"", response.getHeaderString("Content-Disposition"));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getRISAsText()}.
     */
    @Test
    public void testGetRISAsText() {
        try(Response response = target(urls.path(RECORDS_RECORD, RECORDS_RIS_TEXT).params(PI).build())
                .request()
                .accept(MediaType.TEXT_PLAIN)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            assertTrue(entity.contains("TY  - BOOK"));
            assertTrue(entity.contains("CN  - 74241"));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getTOCAsText()}.
     */
    @Test
    public void testGetTOCAsText() {
        try(Response response = target(urls.path(RECORDS_RECORD, RECORDS_TOC).params(PI).build())
                .request()
                .accept(MediaType.TEXT_PLAIN)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            assertTrue(entity.contains("NOBILTÀ PISANA OSSERVATA"));
            assertTrue(entity.contains("Wappen"));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getAnnotationsForRecord(java.lang.String)}.
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void testGetAnnotationsForRecord() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(PI_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            AnnotationCollection collection = mapper.readValue(entity, AnnotationCollection.class);
            assertNotNull(collection);
            assertEquals(3l, collection.getTotalItems());
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getAnnotationPageForRecord()}.
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void testGetAnnotationPageForRecord() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(PI_ANNOTATIONS).build() + "/1")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            AnnotationPage page = mapper.readValue(entity, AnnotationPage.class);
            assertNotNull(page);
            assertEquals(3l, page.getItems().size());
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getCommentsForRecord(java.lang.String)}.
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void testGetCommentsForRecord() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(PI_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            AnnotationCollection collection = mapper.readValue(entity, AnnotationCollection.class);
            assertNotNull(collection);
            assertEquals(2l, collection.getTotalItems());
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getCommentPageForRecord()}.
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void testGetCommentPageForRecord() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(PI_ANNOTATIONS).build() + "/1")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            AnnotationPage page = mapper.readValue(entity, AnnotationPage.class);
            assertNotNull(page);
            assertEquals(2l, page.getItems().size());
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getSource(java.lang.String)}.
     */
    @Test
    public void testGetSource() {
        try(Response response = target(urls.path(RECORDS_RECORD, RECORDS_METADATA_SOURCE).params(PI).build())
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
        }
    }

}
