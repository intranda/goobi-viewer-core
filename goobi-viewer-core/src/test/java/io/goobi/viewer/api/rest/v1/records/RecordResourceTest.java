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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_COMMENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_LAYER;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_METADATA_SOURCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_NER_TAGS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RIS_FILE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RIS_TEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_TOC;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.v2.Layer;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author florian
 *
 */
class RecordResourceTest extends AbstractRestApiTest {

    private static final String PI = "74241";
    private static final String PI_ANNOTATIONS = "PI_1";
    private static final String PI_NER = "PPN743674162";

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
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getRISAsFile()}.
     * @verifies return non null result
     * @see RecordResource#getRISAsFile
     */
    @Test
    void getRISAsFile_shouldReturnNonNullResult() {
        try (Response response = target(urls.path(RECORDS_RECORD, RECORDS_RIS_FILE).params(PI).build())
                .request()
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertTrue(entity.contains("TY  - BOOK"));
            assertTrue(entity.contains("CN  - 74241"));
            String fileName = PI + "_LOG_0000.ris";
            assertEquals("attachment; filename=\"" + fileName + "\"", response.getHeaderString("Content-Disposition"));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getRISAsText()}.
     * @verifies return non null result
     * @see RecordResource#getRISAsText()
     */
    @Test
    void getRISAsText_shouldReturnNonNullResult() {
        try (Response response = target(urls.path(RECORDS_RECORD, RECORDS_RIS_TEXT).params(PI).build())
                .request()
                .accept(MediaType.TEXT_PLAIN)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertTrue(entity.contains("TY  - BOOK"));
            assertTrue(entity.contains("CN  - 74241"));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getTOCAsText()}.
     * @verifies return non null result
     * @see RecordResource#getTOCAsText
     */
    @Test
    void getTOCAsText_shouldReturnNonNullResult() {
        try (Response response = target(urls.path(RECORDS_RECORD, RECORDS_TOC).params(PI).build())
                .request()
                .accept(MediaType.TEXT_PLAIN)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertTrue(entity.contains("NOBILTÀ PISANA OSSERVATA"));
            assertTrue(entity.contains("Wappen"));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getAnnotationsForRecord(java.lang.String)}.
     *
     * @throws JsonProcessingException
     * @throws JsonMappingException
     * @verifies return non null result
     * @see RecordResource#getAnnotationsForRecord
     */
    @Test
    void getAnnotationsForRecord_shouldReturnNonNullResult() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(PI_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            AnnotationCollection collection = mapper.readValue(entity, AnnotationCollection.class);
            assertNotNull(collection);
            assertEquals(0l, collection.getTotalItems()); //no annotations indexed
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getCommentPageForRecord()}.
     *
     * @throws JsonProcessingException
     * @throws JsonMappingException
     * @verifies return non null result
     * @see RecordResource#getCommentPageForRecord
     */
    @Test
    void getCommentPageForRecord_shouldReturnNonNullResult() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(PI_ANNOTATIONS).build() + "1/")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            AnnotationPage page = mapper.readValue(entity, AnnotationPage.class);
            assertNotNull(page);
            assertEquals(4l, page.getItems().size());
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getSource(java.lang.String)}.
     */
    //TODO: read some actual mets file from test index
    /**
     * @verifies return non null result
     * @see RecordResource#getSource
     */
    @Test
    void getSource_shouldReturnNonNullResult() {
        try (Response response = target(urls.path(RECORDS_RECORD, RECORDS_METADATA_SOURCE).params(PI).build())
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(404, response.getStatus(), "Should return status 404");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject error = new JSONObject(entity);
            assertEquals("No source file found for 74241", error.getString("message"));
        }
    }

    /**
     * @verifies return non null result
     * @see RecordResource#getManifest
     */
    @Test
    void getManifest_shouldReturnNonNullResult() {
        String url = urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(PI).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject manifest = new JSONObject(entity);
            String id = manifest.getString("@id");
            assertEquals(url, id);

            //            Manifest manifest = mapper.readValue(entity, Manifest.class);
            //            assertEquals(URI.create(url), manifest.getId());
        }
    }

    /**
     * @verifies return non null result
     * @see RecordResource#getLayer
     */
    @Test
    void getLayer_shouldReturnNonNullResult() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_RECORD, RECORDS_LAYER).params(PI, "ALTO").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            Layer layer = mapper.readValue(entity, Layer.class);
            assertEquals(URI.create(url), layer.getId());
        }
    }

    /**
     * @verifies return non null result
     * @see RecordResource#getNERTags
     */
    @Test
    void getNERTags_shouldReturnNonNullResult() {
        String url = urls.path(RECORDS_RECORD, RECORDS_NER_TAGS).params(PI_NER).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200: " + response.getStatusInfo().getReasonPhrase());
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject doc = new JSONObject(entity);
            assertNotNull(doc.getJSONArray("pages"));
            assertEquals(322, doc.getJSONArray("pages").length());
        }
    }

    //    @Test
    void testGetRequiredPrivilege() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("/viewer/api/v1/records/PPN615391702/manifest/");
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8082/viewer/api/v1/records/PPN615391702/manifest/"));

        ApiUrls urls = new ApiUrls("http://localhost:8082/viewer/api/v1");

        // Use a valid PI (non-empty, alphanumeric) to avoid IllegalRequestException from validatePi
        new RecordResource(request, "PPN615391702");
    }

    /**
     * Requests with PIs containing illegal characters must return HTTP 400.
     * Characters in the PI blocklist (ILLEGAL_CHARS) should be rejected by validatePi()
     * in the constructor.
     * Note: some characters like space (%20) or pipe (%7C) are rejected at the HTTP routing
     * level before reaching our constructor; we test characters that are valid in URL paths
     * but in the PI blocklist and do reach the constructor.
     * @verifies invalid pi returns 400
     * @see RecordResource#try
     */
    @Test
    void try_shouldInvalidPiReturns400() {
        // exclamation mark - valid URL path character, in PI blocklist
        try (Response response = target("/records/invalid!pi/ris")
                .request()
                .get()) {
            assertEquals(400, response.getStatus(), "Exclamation mark in PI should return 400");
        }
        // at-sign - valid URL path character, in PI blocklist
        try (Response response = target("/records/invalid@pi/ris")
                .request()
                .get()) {
            assertEquals(400, response.getStatus(), "At-sign in PI should return 400");
        }
    }

    /**
     * Null PI must be rejected with BadRequestException, not silently accepted.
     * This guards the fix that changed the condition from (pi != null && ...) to (pi == null || ...).
     * @verifies null pi throws bad request
     * @see RecordResource#validatePi
     */
    @Test
    void validatePi_shouldNullPiThrowsBadRequest() {
        assertThrows(jakarta.ws.rs.BadRequestException.class, () -> RecordResource.validatePi(null),
                "null PI should throw BadRequestException");
    }

    /**
     * Valid PIs must pass validatePi without throwing.
     * @verifies valid pi accepted
     * @see RecordResource#validatePi
     */
    @Test
    void validatePi_shouldValidPiAccepted() {
        assertDoesNotThrow(() -> RecordResource.validatePi("PPN615391702"));
        assertDoesNotThrow(() -> RecordResource.validatePi("valid_pi-1.0"));
    }

    /**
     * getRISAsFile() must return HTTP 404 (not 500) when the PI does not exist in the Solr index.
     * This guards against getFirstDoc() returning null and causing a NullPointerException.
     * @verifies return 404 when non existent pi
     * @see RecordResource#getRISAsFile
     */
    @Test
    void getRISAsFile_shouldReturn404WhenNonExistentPi() {
        try (Response response = target(urls.path(RECORDS_RECORD, RECORDS_RIS_FILE).params("NONEXISTENT_PI_99999").build())
                .request()
                .get()) {
            assertEquals(404, response.getStatus(), "Non-existent PI should return HTTP 404, not 500");
        }
    }

    /**
     * getRISAsText() must return HTTP 404 (not 500) when the PI does not exist in the Solr index.
     * This guards against getFirstDoc() returning null and causing a NullPointerException.
     * @verifies return 404 for given input
     * @see RecordResource#getRISAsText()
     */
    @Test
    void getRISAsText_shouldReturn404ForGivenInput() {
        try (Response response = target(urls.path(RECORDS_RECORD, RECORDS_RIS_TEXT).params("NONEXISTENT_PI_99999").build())
                .request()
                .accept(MediaType.TEXT_PLAIN)
                .get()) {
            assertEquals(404, response.getStatus(), "Non-existent PI should return HTTP 404, not 500");
        }
    }
}
