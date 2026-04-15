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
package io.goobi.viewer.api.rest.v1.annotations;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;

/**
 * @author florian
 *
 */
class AnnotationResourceTest extends AbstractRestApiTest {

    // PPN648829383 has ALTO data in the testing Solr (viewer-testing-index.goobi.io)
    // and in src/test/resources/data/viewer/data/1/alto/PPN648829383/
    private static final String PI_WITH_ALTO = "PPN648829383";
    // TextLine_214 exists in PPN648829383/00000001.xml
    private static final String ALTO_ELEMENT_ID = "TextLine_214";
    private static final int ALTO_PAGE_NO = 1;

    /**
     * @verifies return non null result
     * @see AnnotationResource#getAnnotation(@Parameter(description = "Identifier of the, schema = @Schema(minimum =, maximum = "9223372036854775807")) @PathParam("id") Long)
     */
    @Test
    void getAnnotation_shouldReturnNonNullResult() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(1).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as json");
            String entity = response.readEntity(String.class);
            WebAnnotation annotation = mapper.readValue(entity, WebAnnotation.class);
            assertNotNull(annotation);
        }
    }

    /**
     * @verifies return non null result
     * @see AnnotationResource#getComment(@Parameter(description = "Identifier of the, schema = @Schema(minimum =, maximum = "9223372036854775807")) @PathParam("id") Long)
     */
    @Test
    void getComment_shouldReturnNonNullResult() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(ANNOTATIONS, ANNOTATIONS_COMMENT).params(1).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as json");
            String entity = response.readEntity(String.class);
            WebAnnotation annotation = mapper.readValue(entity, WebAnnotation.class);
            assertNotNull(annotation);
        }
    }

    /**
     * Verifies that an ALTO annotation URL with a non-existent PI returns 404 and does not throw
     * a NumberFormatException (which would have caused a 500 before this fix).
     * @verifies handle not found case
     * @see AnnotationResource#getAltoAnnotation
     */
    @Test
    void getAltoAnnotation_shouldHandleNotFoundCase() {
        String url = urls.path(ANNOTATIONS, ANNOTATIONS_ALTO).params("NONEXISTENT_PI", 1, "SomeElement").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(404, response.getStatus(), "Non-existent PI should return 404, not 500");
        }
    }

    /**
     * Verifies that an existing ALTO element can be resolved as a Web Annotation.
     * Uses PPN648829383 which has ALTO data in the testing Solr and test resources.
     * @verifies return result when found
     * @see AnnotationResource#getAltoAnnotation
     */
    @Test
    void getAltoAnnotation_shouldReturnResultWhenFound() {
        String url = urls.path(ANNOTATIONS, ANNOTATIONS_ALTO).params(PI_WITH_ALTO, ALTO_PAGE_NO, ALTO_ELEMENT_ID).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(),
                    "Valid ALTO element should return 200 - url: " + url);
            String entity = response.readEntity(String.class);
            assertNotNull(entity, "Response body should not be null");
            assertTrue(entity.contains("Annotation"), "Response should be an annotation: " + entity);
        }
    }

    /**
     * Verifies that a valid PI/page with a non-existent element ID returns 404.
     * Uses PPN648829383 page 1 which has a real ALTO file.
     * @verifies handle not found when element
     * @see AnnotationResource#getAltoAnnotation
     */
    @Test
    void getAltoAnnotation_shouldHandleNotFoundWhenElement() {
        String url = urls.path(ANNOTATIONS, ANNOTATIONS_ALTO).params(PI_WITH_ALTO, ALTO_PAGE_NO, "NONEXISTENT_ELEMENT").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(404, response.getStatus(), "Non-existent element ID should return 404");
        }
    }
}
