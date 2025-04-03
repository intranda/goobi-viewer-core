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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_COMMENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_NER_TAGS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_SEQUENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Sequence;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 *
 */
class RecordPageResourceTest extends AbstractRestApiTest {

    private static final String PI = "PPN743674162";
    private static final String PAGENO = "10";
    private static final String PI_ANNOTATIONS = "PI_1";
    private static final String PI_SPACE_IN_FILENAME = "ARVIErdm5";
    private static final String PAGENO_ANNOTATIONS = "1";

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testGetNER() {
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_NER_TAGS).params(PI, PAGENO).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject doc = new JSONObject(entity);
            assertNotNull(doc.getJSONArray("pages"));
            assertEquals(1, doc.getJSONArray("pages").length());
            assertEquals(3, doc.getJSONArray("pages").getJSONObject(0).getJSONArray("tags").length());
        }
    }

    @Test
    void testGetSequence() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_SEQUENCE).params(PI).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            Sequence sequence = mapper.readValue(entity, Sequence.class);
            assertEquals(URI.create(url), sequence.getId());
            assertEquals(322, sequence.getCanvases().size());
        }
    }

    @Test
    void testGetCanvas() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(PI, PAGENO).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            Canvas2 canvas = mapper.readValue(entity, Canvas2.class);
            assertEquals(URI.create(url), canvas.getId());
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getAnnotationsForRecord(java.lang.String)}.
     * 
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    @Test
    void testGetAnnotationsForPage() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(PI_ANNOTATIONS, PAGENO_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            AnnotationCollection collection = mapper.readValue(entity, AnnotationCollection.class);
            assertNotNull(collection);
            assertEquals(0, collection.getTotalItems()); //No annotations indexed
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getCommentsForRecord(java.lang.String)}.
     * 
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    @Test
    void testGetCommentsForPage() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_PAGES, RECORDS_PAGES_COMMENTS).params(PI_ANNOTATIONS, PAGENO_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            AnnotationList collection = mapper.readValue(entity, AnnotationList.class);
            assertNotNull(collection);
            assertEquals(3, collection.getResources().size());
        }
    }
    

    @Test
    void testEscapeFilenamesInUrls() {
        DataManager.getInstance().getConfiguration().overrideValue("webapi.iiif.rendering.viewer[@enabled]", true);
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer());
        DataManager.getInstance().getConfiguration().overrideValue("webapi.iiif.rendering.pdf[@enabled]", true);
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF());

        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(PI_SPACE_IN_FILENAME, "1").build();
        try (Response response = target(url)
                .request()
                .get()) {
            String entity = response.readEntity(String.class);
            assertEquals(200, response.getStatus(), response.getStatusInfo().getReasonPhrase());
            assertNotNull(entity);
            JSONObject canvas = new JSONObject(entity);
            JSONArray renderings = null;
            try {
                renderings = canvas.getJSONArray("rendering");
            } catch (JSONException e) {
                // Fallback for when "rendering" is not an array
                JSONObject rendering = canvas.getJSONObject("rendering");
                if (rendering != null) {
                    renderings = new JSONArray();
                    renderings.put(rendering);
                }
            }
            assertNotNull(renderings);
            assertFalse(renderings.isEmpty());
            Map pdfLink = renderings.toList()
                    .stream()
                    .map(Map.class::cast)
                    .filter(map -> "dcTypes:Image".equals(map.get("@type")))
                    .findAny()
                    .orElse(null);
            assertNotNull(pdfLink, "No PDF link in canvas");
            String id = (String) pdfLink.get("@id");
            Assertions.assertTrue(id.contains("erdmagnetisches+observatorium+vi_blatt_5.jpg"), "Wrong filename in " + id);
        }
    }
}
