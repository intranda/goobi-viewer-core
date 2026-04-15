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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

class WebApplicationExceptionMapperTest {

    private WebApplicationExceptionMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new WebApplicationExceptionMapper();

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("/api/v1/records/TEST/pages/NaN/annotations");
        Mockito.when(request.getMethod()).thenReturn("GET");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Inject @Context fields (not wired by a container in unit tests)
        Field requestField = WebApplicationExceptionMapper.class.getDeclaredField("request");
        requestField.setAccessible(true);
        requestField.set(mapper, request);

        Field responseField = WebApplicationExceptionMapper.class.getDeclaredField("response");
        responseField.setAccessible(true);
        responseField.set(mapper, response);
    }

    /**
     * Jersey wraps NumberFormatException (from @PathParam Integer conversion failures) in a
     * WebApplicationException. Without explicit handling this was treated as a RuntimeException
     * and promoted to HTTP 500 with the Java class name in the response body.
     * Verify that the mapper now correctly returns HTTP 400.
     * @verifies return 400 when number format exception cause
     * @see WebApplicationExceptionMapper#toResponse
     */
    @Test
    void toResponse_shouldReturn400WhenNumberFormatExceptionCause() {
        Response response = mapper.toResponse(buildPathParamException("NaN"));
        assertEquals(400, response.getStatus());
    }

    /**
     * The error message returned to the client must not contain the Java class name
     * "NumberFormatException" — that is an implementation detail that must not leak.
     * @verifies not leak java class name when number format exception cause message
     * @see WebApplicationExceptionMapper#toResponse
     */
    @Test
    void toResponse_shouldNotLeakJavaClassNameWhenNumberFormatExceptionCauseMessage() throws Exception {
        Response response = mapper.toResponse(buildPathParamException("NaN"));

        // Serialize entity to JSON the same way JAX-RS would, then inspect the string
        String json = new ObjectMapper().writeValueAsString(response.getEntity());
        assertFalse(json.contains("NumberFormatException"),
                "Response JSON must not expose Java exception class name to clients: " + json);
    }

    /**
     * Jersey 3.x may wrap the NumberFormatException in an ExtractorException (RuntimeException)
     * before wrapping it in a WebApplicationException. Verify that the mapper still returns 400
     * even when the NumberFormatException is one extra level deep in the cause chain.
     * @verifies return 400 when number format exception nested in runtime exception
     */
    @Test
    void toResponse_shouldReturn400WhenNumberFormatExceptionNestedInRuntimeException() {
        // Simulate: WebApplicationException → RuntimeException → NumberFormatException
        NumberFormatException innerCause = new NumberFormatException("For input string: \"NaN\"");
        RuntimeException extractorException = new RuntimeException("extraction failed", innerCause);
        WebApplicationException wae = new WebApplicationException(extractorException,
                Response.Status.INTERNAL_SERVER_ERROR);

        Response response = mapper.toResponse(wae);
        assertEquals(400, response.getStatus(),
                "Should return 400 even when NumberFormatException is wrapped in a RuntimeException");
    }

    // Simulates Jersey's PathParamException: a WebApplicationException whose cause is
    // a NumberFormatException (exactly what Jersey creates for invalid @PathParam Integer values).
    private static WebApplicationException buildPathParamException(String badValue) {
        NumberFormatException cause = new NumberFormatException("For input string: \"" + badValue + "\"");
        return new WebApplicationException(cause.toString(), cause,
                Response.status(Response.Status.NOT_FOUND).build());
    }
}
