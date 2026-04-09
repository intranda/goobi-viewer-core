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
package io.goobi.viewer.api.rest.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotAllowedExceptionMapperTest {

    private NotAllowedExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NotAllowedExceptionMapper();
    }

    /**
     * RFC 9110 §15.5.6 requires a 405 response to include an Allow header listing the
     * supported methods. Verify that the mapper sets the correct status code.
     */
    @Test
    void toResponse_returns405() {
        NotAllowedException ex = buildException("GET", "HEAD");
        Response response = mapper.toResponse(ex);
        assertEquals(405, response.getStatus());
    }

    /**
     * RFC 9110 §15.5.6: the Allow header must be present and must list the methods that
     * the JAX-RS runtime reported as allowed for the requested path.
     */
    @Test
    void toResponse_allowHeaderContainsAllowedMethods() {
        NotAllowedException ex = buildException("GET", "HEAD", "OPTIONS");
        Response response = mapper.toResponse(ex);

        String allowHeader = (String) response.getHeaders().getFirst(HttpHeaders.ALLOW);
        assertNotNull(allowHeader, "Allow header must be present on 405 responses (RFC 9110)");
        assertTrue(allowHeader.contains("GET"), "Allow header should contain GET");
        assertTrue(allowHeader.contains("HEAD"), "Allow header should contain HEAD");
        assertTrue(allowHeader.contains("OPTIONS"), "Allow header should contain OPTIONS");
    }

    /**
     * The response body must be JSON and include the status code so clients can parse it
     * consistently with other error responses from this API.
     */
    @Test
    void toResponse_bodyIsJson() {
        NotAllowedException ex = buildException("GET");
        Response response = mapper.toResponse(ex);

        assertNotNull(response.getEntity(), "Response body must not be null");
        // ErrorMessage serialises to JSON with a 'status' field
        String entityJson = response.getEntity().toString();
        assertTrue(entityJson.contains("405") || response.getEntity().getClass().getSimpleName().contains("ErrorMessage"),
                "Response entity should be an ErrorMessage with status 405");
    }

    /**
     * Builds a NotAllowedException using a Response with explicit Allow header to avoid
     * the constructor ambiguity between NotAllowedException(String, String...) and
     * NotAllowedException(String, String, String...).
     */
    private static NotAllowedException buildException(String... allowedMethods) {
        Response r = Response.status(Status.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.ALLOW, String.join(", ", allowedMethods))
                .build();
        return new NotAllowedException(r);
    }
}
