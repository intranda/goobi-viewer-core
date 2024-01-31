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
package io.goobi.viewer.api.rest.v2.records;

import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_COMMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.wa.collection.AnnotationPage;
import io.goobi.viewer.api.rest.v2.AbstractRestApiTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;

/**
 * @author florian
 *
 */
class RecordPagesResourceTest extends AbstractRestApiTest {

    private static final String PI = "PPN743674162";
    private static final String PAGENO = "10";
    private static final String PI_ANNOTATIONS = "PI_1";
    private static final String PAGENO_ANNOTATIONS = "1";


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

    @Test
    void testGetCanvas() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(PI, PAGENO).build();
        try(Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject canvas = new JSONObject(entity);
            assertEquals(url, canvas.getString("id"));
            assertEquals("Canvas", canvas.getString("type"));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v2.records.RecordResource#getAnnotationsForRecord(java.lang.String)}.
     * @throws JsonProcessingException
     * @throws JsonMappingException
     * @throws DAOException 
     * @throws NumberFormatException 
     */
    @Test
    void testGetAnnotationsForPage() throws JsonMappingException, JsonProcessingException, NumberFormatException, DAOException {
        long annoCount = DataManager.getInstance().getDao().getAnnotationCountForTarget(PI_ANNOTATIONS, Integer.parseInt(PAGENO_ANNOTATIONS));
        try(Response response = target(urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(PI_ANNOTATIONS, PAGENO_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            AnnotationPage annoPage = mapper.readValue(entity, AnnotationPage.class);
            assertNotNull(annoPage);
            assertEquals("AnnotationPage", annoPage.getType());
            assertEquals(annoCount, annoPage.getItems().size());
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v2.records.RecordResource#getCommentsForRecord(java.lang.String)}.
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    @Test
    void testGetCommentsForPage() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS_PAGES, RECORDS_PAGES_COMMENTS).params(PI_ANNOTATIONS, PAGENO_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            AnnotationPage annoPage = mapper.readValue(entity, AnnotationPage.class);
            assertNotNull(annoPage);
            assertEquals(3, annoPage.getItems().size());
        }
    }
}
