package io.goobi.viewer.api.rest.filters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import jakarta.servlet.http.HttpServletRequest;

class AuthorizationFilterTest extends AbstractTest {

    // Value configured in config_viewer.test.xml
    private static final String CORRECT_TOKEN = "test";

    /**
     * @see AuthorizationFilter#isAuthorized(HttpServletRequest)
     * @verifies return true for correct token in header
     */
    @Test
    void isAuthorized_shouldReturnTrueForCorrectTokenInHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("token")).thenReturn(CORRECT_TOKEN);

        assertTrue(AuthorizationFilter.isAuthorized(request));
    }

    /**
     * @see AuthorizationFilter#isAuthorized(HttpServletRequest)
     * @verifies return true for correct token in query parameter
     */
    @Test
    void isAuthorized_shouldReturnTrueForCorrectTokenInQueryParameter() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("token")).thenReturn(null);
        Mockito.when(request.getParameter("token")).thenReturn(CORRECT_TOKEN);

        assertTrue(AuthorizationFilter.isAuthorized(request));
    }

    /**
     * @see AuthorizationFilter#isAuthorized(HttpServletRequest)
     * @verifies return false for wrong token
     */
    @Test
    void isAuthorized_shouldReturnFalseForWrongToken() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("token")).thenReturn("wrongtoken");

        assertFalse(AuthorizationFilter.isAuthorized(request));
    }

    /**
     * @see AuthorizationFilter#isAuthorized(HttpServletRequest)
     * @verifies return false if no token provided
     */
    @Test
    void isAuthorized_shouldReturnFalseIfNoTokenProvided() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("token")).thenReturn(null);
        Mockito.when(request.getParameter("token")).thenReturn(null);

        assertFalse(AuthorizationFilter.isAuthorized(request));
    }
}
