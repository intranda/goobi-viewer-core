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

    @Test
    void testGetAnnotation() throws JsonMappingException, JsonProcessingException {
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

    @Test
    void testGetComment() throws JsonMappingException, JsonProcessingException {
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
}
